import '../../features/chat/models/chat_models.dart';

class ChatState {
  ChatState({
    required List<ChatSession> sessions,
    required this.activeSessionId,
    required this.selectedModelId,
    required List<ChatAttachment> pendingAttachments,
    required this.nextId,
  })  : sessions = List.unmodifiable(sessions),
        pendingAttachments = List.unmodifiable(pendingAttachments);

  final List<ChatSession> sessions;
  final String activeSessionId;
  final String selectedModelId;
  final List<ChatAttachment> pendingAttachments;
  final int nextId;

  ChatSession get activeSession =>
      sessions.firstWhere((session) => session.id == activeSessionId);

  ChatState copyWith({
    List<ChatSession>? sessions,
    String? activeSessionId,
    String? selectedModelId,
    List<ChatAttachment>? pendingAttachments,
    int? nextId,
  }) {
    return ChatState(
      sessions: sessions ?? this.sessions,
      activeSessionId: activeSessionId ?? this.activeSessionId,
      selectedModelId: selectedModelId ?? this.selectedModelId,
      pendingAttachments: pendingAttachments ?? this.pendingAttachments,
      nextId: nextId ?? this.nextId,
    );
  }
}

abstract interface class ChatRepository {
  ChatState load();

  Future<void> save(ChatState state);
}

class InMemoryChatRepository implements ChatRepository {
  InMemoryChatRepository({DateTime Function()? clock})
      : _clock = clock ?? DateTime.now;

  final DateTime Function() _clock;
  ChatState? _state;

  @override
  ChatState load() => _state ??= _seedState();

  @override
  Future<void> save(ChatState state) async {
    _state = state;
  }

  ChatState _seedState() {
    final now = _clock();
    final session = ChatSession(
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
          markdown: '- Diagnostics reads native health\n'
              '- Automation can target Android backends\n'
              '- Local wrappers are available for parity work',
          createdAt: now,
        ),
      ],
    );
    return ChatState(
      sessions: [session],
      activeSessionId: session.id,
      selectedModelId: 'remote.primary',
      pendingAttachments: const [],
      nextId: 0,
    );
  }
}
