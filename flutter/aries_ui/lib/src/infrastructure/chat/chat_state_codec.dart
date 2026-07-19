import '../../application/chat/chat_repository.dart';
import '../../features/chat/models/chat_models.dart';
import '../persistence/json_state_store.dart';

class ChatStateCodec implements JsonStateCodec<ChatState> {
  const ChatStateCodec();

  @override
  Map<String, Object?> encode(ChatState value) {
    return {
      'sessions': value.sessions.map(_encodeSession).toList(),
      'activeSessionId': value.activeSessionId,
      'selectedModelId': value.selectedModelId,
      'pendingAttachments':
          value.pendingAttachments.map(_encodeAttachment).toList(),
      'nextId': value.nextId,
    };
  }

  @override
  ChatState decode(Map<String, Object?> value) {
    final sessions = JsonValue.list(value['sessions'], 'sessions')
        .map((item) => _decodeSession(JsonValue.map(item, 'session')))
        .toList();
    if (sessions.isEmpty) {
      throw const FormatException('sessions must not be empty');
    }
    final activeSessionId =
        JsonValue.string(value['activeSessionId'], 'activeSessionId');
    if (!sessions.any((session) => session.id == activeSessionId)) {
      throw const FormatException('activeSessionId is unknown');
    }
    final nextId = JsonValue.integer(value['nextId'], 'nextId');
    if (nextId < 0) {
      throw const FormatException('nextId must be non-negative');
    }
    return ChatState(
      sessions: sessions,
      activeSessionId: activeSessionId,
      selectedModelId:
          JsonValue.string(value['selectedModelId'], 'selectedModelId'),
      pendingAttachments:
          JsonValue.list(value['pendingAttachments'], 'pendingAttachments')
              .map(
                (item) => _decodeAttachment(JsonValue.map(item, 'attachment')),
              )
              .toList(),
      nextId: nextId,
    );
  }

  Map<String, Object?> _encodeSession(ChatSession session) {
    return {
      'id': session.id,
      'title': session.title,
      'messages': session.messages.map(_encodeMessage).toList(),
      'updatedAt': session.updatedAt.toIso8601String(),
    };
  }

  ChatSession _decodeSession(Map<String, Object?> value) {
    return ChatSession(
      id: JsonValue.string(value['id'], 'session.id'),
      title: JsonValue.string(value['title'], 'session.title'),
      messages: JsonValue.list(value['messages'], 'session.messages')
          .map((item) => _decodeMessage(JsonValue.map(item, 'message')))
          .toList(),
      updatedAt: DateTime.parse(
        JsonValue.string(value['updatedAt'], 'session.updatedAt'),
      ),
    );
  }

  Map<String, Object?> _encodeMessage(ChatMessage message) {
    return {
      'id': message.id,
      'role': message.role.name,
      'markdown': message.markdown,
      'createdAt': message.createdAt.toIso8601String(),
      'attachments': message.attachments.map(_encodeAttachment).toList(),
    };
  }

  ChatMessage _decodeMessage(Map<String, Object?> value) {
    final roleName = JsonValue.string(value['role'], 'message.role');
    final role = switch (roleName) {
      'user' => ChatMessageRole.user,
      'assistant' => ChatMessageRole.assistant,
      'system' => ChatMessageRole.system,
      _ => throw FormatException('Unknown message role: $roleName'),
    };
    return ChatMessage(
      id: JsonValue.string(value['id'], 'message.id'),
      role: role,
      markdown: JsonValue.string(value['markdown'], 'message.markdown'),
      createdAt: DateTime.parse(
        JsonValue.string(value['createdAt'], 'message.createdAt'),
      ),
      attachments: JsonValue.list(value['attachments'], 'message.attachments')
          .map(
            (item) => _decodeAttachment(JsonValue.map(item, 'attachment')),
          )
          .toList(),
    );
  }

  Map<String, Object?> _encodeAttachment(ChatAttachment attachment) {
    return {
      'id': attachment.id,
      'name': attachment.name,
      'mimeType': attachment.mimeType,
      'sizeLabel': attachment.sizeLabel,
    };
  }

  ChatAttachment _decodeAttachment(Map<String, Object?> value) {
    return ChatAttachment(
      id: JsonValue.string(value['id'], 'attachment.id'),
      name: JsonValue.string(value['name'], 'attachment.name'),
      mimeType: JsonValue.string(value['mimeType'], 'attachment.mimeType'),
      sizeLabel: JsonValue.string(value['sizeLabel'], 'attachment.sizeLabel'),
    );
  }
}
