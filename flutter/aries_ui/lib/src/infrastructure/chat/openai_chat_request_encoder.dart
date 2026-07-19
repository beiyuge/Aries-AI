import 'dart:convert';

import '../../application/chat/chat_runtime.dart';
import '../../features/chat/models/chat_models.dart';
import 'remote_chat_configuration.dart';

class OpenAiChatRequestEncoder {
  const OpenAiChatRequestEncoder();

  String encode(
    RemoteChatConfiguration configuration,
    ChatGenerationRequest request,
  ) {
    return jsonEncode({
      'model': configuration.model,
      'stream': request.streamResponse,
      'messages': [
        for (final message in request.messages)
          {
            'role': message.role.name,
            'content': _content(message),
          },
      ],
    });
  }

  String _content(ChatMessage message) {
    if (message.attachments.isEmpty) {
      return message.markdown;
    }
    final buffer = StringBuffer(message.markdown);
    for (final attachment in message.attachments) {
      buffer
        ..writeln()
        ..writeln()
        ..write('[Attachment: ${attachment.name}, ')
        ..write('type: ${attachment.mimeType}, ')
        ..write('bytes: ${attachment.byteLength}]');
    }
    return buffer.toString();
  }
}
