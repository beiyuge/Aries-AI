import 'package:flutter/foundation.dart';

import '../../../application/chat/chat_repository.dart';
import '../models/chat_models.dart';

class ChatController extends ChangeNotifier {
  ChatController({
    ChatRepository? repository,
    ChatDraftResponder? draftResponder,
    DateTime Function()? clock,
  })  : _repository = repository ?? InMemoryChatRepository(clock: clock),
        _draftResponder = draftResponder ?? const ChatDraftResponder(),
        _clock = clock ?? DateTime.now {
    _state = _repository.load();
  }

  final ChatRepository _repository;
  final ChatDraftResponder _draftResponder;
  final DateTime Function() _clock;
  late ChatState _state;

  List<ChatSession> get sessions => _state.sessions;

  ChatSession get activeSession => _state.activeSession;

  List<ChatAttachment> get pendingAttachments => _state.pendingAttachments;

  String get selectedModelId => _state.selectedModelId;

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

  Future<void> addSampleAttachment() {
    return _save(
      _state.copyWith(
        pendingAttachments: [
          ..._state.pendingAttachments,
          ChatAttachment(
            id: _id('attachment'),
            name: 'screen-context.json',
            mimeType: 'application/json',
            sizeLabel: '8 KB',
          ),
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

  Future<void> send(String text) {
    final now = _clock();
    final userText = text.isEmpty ? 'Attached context' : text;
    final userMessage = ChatMessage(
      id: _id('message'),
      role: ChatMessageRole.user,
      markdown: userText,
      attachments: _state.pendingAttachments,
      createdAt: now,
    );
    final assistantMessage = ChatMessage(
      id: _id('message'),
      role: ChatMessageRole.assistant,
      markdown: _draftResponder.reply(
        modelId: _state.selectedModelId,
        prompt: userText,
      ),
      createdAt: now.add(const Duration(milliseconds: 400)),
    );
    _replaceActive((session) {
      final title = session.title == 'New session'
          ? _draftResponder.titleFrom(userText)
          : session.title;
      return session.copyWith(
        title: title,
        messages: [...session.messages, userMessage, assistantMessage],
        updatedAt: now,
      );
    });
    return _save(_state.copyWith(pendingAttachments: const []));
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
