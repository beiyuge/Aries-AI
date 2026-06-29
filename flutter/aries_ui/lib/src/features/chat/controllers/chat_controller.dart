import 'package:flutter/foundation.dart';

import '../models/chat_models.dart';

class ChatController extends ChangeNotifier {
  ChatController() {
    _sessions = [_seedSession()];
    _activeSessionId = _sessions.first.id;
  }

  late List<ChatSession> _sessions;
  late String _activeSessionId;
  String _selectedModelId = 'local.wrapper';
  final List<ChatAttachment> _pendingAttachments = [];
  int _nextId = 0;

  List<ChatSession> get sessions => List.unmodifiable(_sessions);

  ChatSession get activeSession =>
      _sessions.firstWhere((session) => session.id == _activeSessionId);

  List<ChatAttachment> get pendingAttachments =>
      List.unmodifiable(_pendingAttachments);

  String get selectedModelId => _selectedModelId;

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

  void selectModel(String modelId) {
    _selectedModelId = modelId;
    notifyListeners();
  }

  void selectSession(String sessionId) {
    _activeSessionId = sessionId;
    notifyListeners();
  }

  void startNewSession() {
    final now = DateTime.now();
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
    _sessions = [session, ..._sessions];
    _activeSessionId = session.id;
    notifyListeners();
  }

  void addSampleAttachment() {
    _pendingAttachments.add(
      ChatAttachment(
        id: _id('attachment'),
        name: 'screen-context.json',
        mimeType: 'application/json',
        sizeLabel: '8 KB',
      ),
    );
    notifyListeners();
  }

  void removeAttachment(String attachmentId) {
    _pendingAttachments
        .removeWhere((attachment) => attachment.id == attachmentId);
    notifyListeners();
  }

  void send(String text) {
    final now = DateTime.now();
    final userText = text.isEmpty ? 'Attached context' : text;
    final userMessage = ChatMessage(
      id: _id('message'),
      role: ChatMessageRole.user,
      markdown: userText,
      attachments: List.unmodifiable(_pendingAttachments),
      createdAt: now,
    );
    final assistantMessage = ChatMessage(
      id: _id('message'),
      role: ChatMessageRole.assistant,
      markdown: _draftAssistantReply(userText),
      createdAt: now.add(const Duration(milliseconds: 400)),
    );
    _pendingAttachments.clear();
    _replaceActive((session) {
      final title =
          session.title == 'New session' ? _titleFrom(userText) : session.title;
      return session.copyWith(
        title: title,
        messages: [...session.messages, userMessage, assistantMessage],
        updatedAt: DateTime.now(),
      );
    });
    notifyListeners();
  }

  void _replaceActive(ChatSession Function(ChatSession session) update) {
    _sessions = [
      for (final session in _sessions)
        if (session.id == _activeSessionId) update(session) else session,
    ];
  }

  ChatSession _seedSession() {
    final now = DateTime.now();
    return ChatSession(
      id: 'session-seed',
      title: 'Device readiness',
      updatedAt: now,
      messages: [
        ChatMessage(
          id: 'message-seed-system',
          role: ChatMessageRole.system,
          markdown:
              '# Aries\nCapabilities are connected through the typed bridge.',
          createdAt: now,
        ),
        ChatMessage(
          id: 'message-seed-assistant',
          role: ChatMessageRole.assistant,
          markdown:
              '- Diagnostics reads native health\n- Automation can target Android backends\n- Local wrappers are available for parity work',
          createdAt: now,
        ),
      ],
    );
  }

  String _draftAssistantReply(String prompt) {
    return '# Draft\n'
        '- Model: $_selectedModelId\n'
        '- Intent: ${_titleFrom(prompt)}\n'
        '- Next: inspect Diagnostics before device actions';
  }

  String _titleFrom(String text) {
    final normalized = text.replaceAll(RegExp(r'\s+'), ' ').trim();
    if (normalized.isEmpty) {
      return 'Attached context';
    }
    return normalized.length <= 28
        ? normalized
        : '${normalized.substring(0, 28)}...';
  }

  String _id(String prefix) {
    _nextId += 1;
    return '$prefix-$_nextId';
  }
}
