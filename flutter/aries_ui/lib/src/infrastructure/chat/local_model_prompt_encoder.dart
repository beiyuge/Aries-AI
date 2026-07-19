import '../../features/chat/models/chat_models.dart';

class LocalModelPromptEncoder {
  const LocalModelPromptEncoder();

  String encode(List<ChatMessage> messages) {
    final prompt = StringBuffer();
    for (final message in messages) {
      if (prompt.isNotEmpty) {
        prompt.writeln();
      }
      prompt
        ..write(message.role.name.toUpperCase())
        ..writeln(':')
        ..write(message.markdown);
      for (final attachment in message.attachments) {
        prompt
          ..writeln()
          ..write('[Attachment: ${attachment.name}, ')
          ..write('type: ${attachment.mimeType}, ')
          ..write('bytes: ${attachment.byteLength}]');
      }
    }
    return prompt.toString();
  }
}
