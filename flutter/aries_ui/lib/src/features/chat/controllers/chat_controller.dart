import 'package:flutter/foundation.dart';

import '../../../application/chat/chat_attachment_picker.dart';
import '../../../application/chat/chat_repository.dart';
import '../../../application/chat/chat_runtime.dart';
import '../../../application/chat/chat_title_generator.dart';
import '../models/chat_models.dart';

class ChatController extends ChangeNotifier {
  ChatController({
    ChatRepository? repository,
    ChatRuntime? runtime,
    ChatTitleGenerator titleGenerator = const ChatTitleGenerator(),
    DateTime Function()? clock,
  })  : _repository = repository ?? InMemoryChatRepository(clock: clock),
        _runtime = runtime ?? const UnavailableChatRuntime(),
        _titleGenerator = titleGenerator,
        _clock = clock ?? DateTime.now {
    _state = _repository.load();
  }

  final ChatRepository _repository;
  final ChatRuntime _runtime;
  final ChatTitleGenerator _titleGenerator;
  final DateTime Function() _clock;
  late ChatState _state;
  bool _isGenerating = false;

  List<ChatSession> get sessions => _state.sessions;

  ChatSession get activeSession => _state.activeSession;

  List<ChatAttachment> get pendingAttachments => _state.pendingAttachments;

  String get selectedModelId => _state.selectedModelId;
  bool get isGenerating => _isGenerating;

  List<ChatModelProfile> get availableModels => const [
        ChatModelProfile(
          id: 'local.wrapper',
          label: 'Local wrapper',
          caption: 'native.runtime / local.model',
        ),
        ChatModelProfile(
          id: 'remote.primary',
          label: 'Remote primary',
          caption: 'provider profile',
        ),
        ChatModelProfile(
          id: 'automation.copilot',
          label: 'Automation copilot',
          caption: 'capability aware',
        ),
      ];

  Future<void> selectModel(String modelId) {
    return _save(_state.copyWith(selectedModelId: modelId));
  }

  Future<void> selectSession(String sessionId) {
    return _save(_state.copyWith(activeSessionId: sessionId));
  }

  Future<void> startNewSession() {
    final now = _clock();
    final session = ChatSession(
      id: _id('session'),
      title: 'New session',
      messages: [
        ChatMessage(
          id: _id('message'),
          role: ChatMessageRole.system,
          markdown: '# Aries\nReady for a clean automation run.',
          createdAt: now,
        ),
      ],
      updatedAt: now,
    );
    return _save(
      _state.copyWith(
        sessions: [session, ..._state.sessions],
        activeSessionId: session.id,
      ),
    );
  }

  Future<void> addAttachments(Iterable<PickedChatAttachment> attachments) {
    final additions = [
      for (final attachment in attachments)
        ChatAttachment(
          id: _id('attachment'),
          name: attachment.name,
          mimeType: attachment.mimeType,
          byteLength: attachment.byteLength,
          source: attachment.source,
        ),
    ];
    if (additions.isEmpty) {
      return Future.value();
    }
    return _save(
      _state.copyWith(
        pendingAttachments: [
          ..._state.pendingAttachments,
          ...additions,
        ],
      ),
    );
  }

  Future<void> removeAttachment(String attachmentId) {
    return _save(
      _state.copyWith(
        pendingAttachments: [
          for (final attachment in _state.pendingAttachments)
            if (attachment.id != attachmentId) attachment,
        ],
      ),
    );
  }

  Future<void> send(String text) async {
    if (_isGenerating) {
      return;
    }
    final now = _clock();
    final userText = text.isEmpty ? 'Attached context' : text;
    final userMessage = ChatMessage(
      id: _id('message'),
      role: ChatMessageRole.user,
      markdown: userText,
      attachments: _state.pendingAttachments,
      createdAt: now,
    );
    final responseId = _id('message');
    final assistantMessage = ChatMessage(
      id: responseId,
      role: ChatMessageRole.assistant,
      markdown: '',
      createdAt: now,
    );
    final requestMessages = [
      ..._state.activeSession.messages,
      userMessage,
    ];
    _replaceActive((session) {
      final title = session.title == 'New session'
          ? _titleGenerator.fromText(userText)
          : session.title;
      return session.copyWith(
        title: title,
        messages: [...session.messages, userMessage, assistantMessage],
        updatedAt: now,
      );
    });
    _state = _state.copyWith(pendingAttachments: const []);
    _isGenerating = true;
    notifyListeners();

    var receivedContent = false;
    try {
      await _repository.save(_state);
      final request = ChatGenerationRequest(
        modelId: _state.selectedModelId,
        messages: requestMessages,
        streamResponse: true,
      );
      await for (final event in _runtime.generate(request)) {
        switch (event) {
          case ChatGenerationChunk():
            if (event.text.isEmpty) {
              continue;
            }
            receivedContent = true;
            _updateMessage(
              responseId,
              (message) => message.copyWith(
                markdown: '${message.markdown}${event.text}',
              ),
            );
            notifyListeners();
          case ChatGenerationFailed():
            _updateMessage(
              responseId,
              (message) => message.copyWith(
                markdown: _failureMarkdown(message.markdown, event),
              ),
            );
            notifyListeners();
            return;
          case ChatGenerationDone():
            if (!receivedContent) {
              _updateMessage(
                responseId,
                (message) => message.copyWith(
                  markdown:
                      '**Empty response**\n\nThe provider returned no text.',
                ),
              );
              notifyListeners();
            }
            return;
        }
      }
      if (!receivedContent) {
        _updateMessage(
          responseId,
          (message) => message.copyWith(
            markdown: '**Empty response**\n\nThe provider returned no text.',
          ),
        );
        notifyListeners();
      }
    } finally {
      _isGenerating = false;
      notifyListeners();
      await _repository.save(_state);
    }
  }

  void _replaceActive(ChatSession Function(ChatSession session) update) {
    _state = _state.copyWith(
      sessions: [
        for (final session in _state.sessions)
          if (session.id == _state.activeSessionId)
            update(session)
          else
            session,
      ],
    );
  }

  void _updateMessage(
    String messageId,
    ChatMessage Function(ChatMessage message) update,
  ) {
    _replaceActive(
      (session) => session.copyWith(
        messages: [
          for (final message in session.messages)
            if (message.id == messageId) update(message) else message,
        ],
        updatedAt: _clock(),
      ),
    );
  }

  String _failureMarkdown(
    String partialResponse,
    ChatGenerationFailed failure,
  ) {
    final heading = partialResponse.isEmpty
        ? '**Request failed**'
        : '$partialResponse\n\n**Response interrupted**';
    return '$heading\n\n${failure.message}\n\n`${failure.code}`';
  }

  Future<void> _save(ChatState state) async {
    _state = state;
    notifyListeners();
    await _repository.save(state);
  }

  String _id(String prefix) {
    final nextId = _state.nextId + 1;
    _state = _state.copyWith(nextId: nextId);
    return '$prefix-$nextId';
  }
}
