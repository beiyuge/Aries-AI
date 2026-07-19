class PickedChatAttachment {
  const PickedChatAttachment({
    required this.name,
    required this.mimeType,
    required this.byteLength,
    required this.source,
  });

  final String name;
  final String mimeType;
  final int byteLength;
  final String source;
}

abstract interface class ChatAttachmentPicker {
  Future<List<PickedChatAttachment>> pick();
}

class ChatAttachmentPickerException implements Exception {
  const ChatAttachmentPickerException(this.message);

  final String message;

  @override
  String toString() => message;
}
