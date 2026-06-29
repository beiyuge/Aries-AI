enum ChatMessageRole { user, assistant, system }

class ChatAttachment {
  const ChatAttachment({
    required this.id,
    required this.name,
    required this.mimeType,
    required this.sizeLabel,
  });

  final String id;
  final String name;
  final String mimeType;
  final String sizeLabel;
}

class ChatMessage {
  const ChatMessage({
    required this.id,
    required this.role,
    required this.markdown,
    required this.createdAt,
    this.attachments = const [],
  });

  final String id;
  final ChatMessageRole role;
  final String markdown;
  final DateTime createdAt;
  final List<ChatAttachment> attachments;
}

class ChatSession {
  const ChatSession({
    required this.id,
    required this.title,
    required this.messages,
    required this.updatedAt,
  });

  final String id;
  final String title;
  final List<ChatMessage> messages;
  final DateTime updatedAt;

  ChatSession copyWith({
    String? title,
    List<ChatMessage>? messages,
    DateTime? updatedAt,
  }) {
    return ChatSession(
      id: id,
      title: title ?? this.title,
      messages: messages ?? this.messages,
      updatedAt: updatedAt ?? this.updatedAt,
    );
  }
}

class ChatModelProfile {
  const ChatModelProfile({
    required this.id,
    required this.label,
    required this.caption,
  });

  final String id;
  final String label;
  final String caption;
}
