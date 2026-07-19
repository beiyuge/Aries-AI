import 'dart:convert';

sealed class OpenAiStreamFrame {
  const OpenAiStreamFrame();
}

class OpenAiStreamChunk extends OpenAiStreamFrame {
  const OpenAiStreamChunk(this.text);

  final String text;
}

class OpenAiStreamDone extends OpenAiStreamFrame {
  const OpenAiStreamDone();
}

class OpenAiStreamIgnored extends OpenAiStreamFrame {
  const OpenAiStreamIgnored();
}

class OpenAiChatResponseDecoder {
  const OpenAiChatResponseDecoder();

  OpenAiStreamFrame decodeStreamLine(String line) {
    if (!line.startsWith('data:')) {
      return const OpenAiStreamIgnored();
    }
    final data = line.substring(5).trim();
    if (data.isEmpty) {
      return const OpenAiStreamIgnored();
    }
    if (data == '[DONE]') {
      return const OpenAiStreamDone();
    }
    final root = _jsonObject(data);
    final choice = _firstChoice(root);
    final delta = _object(choice['delta']);
    final content = delta?['content'];
    return content is String && content.isNotEmpty
        ? OpenAiStreamChunk(content)
        : const OpenAiStreamIgnored();
  }

  String decodeResponse(String body) {
    final root = _jsonObject(body);
    final choice = _firstChoice(root);
    final message = _object(choice['message']);
    final content = message?['content'];
    if (content is! String) {
      throw const FormatException('Provider response has no message content.');
    }
    return content;
  }

  Map<String, Object?> _jsonObject(String value) {
    final decoded = jsonDecode(value);
    final object = _object(decoded);
    if (object == null) {
      throw const FormatException('Provider response must be a JSON object.');
    }
    return object;
  }

  Map<String, Object?> _firstChoice(Map<String, Object?> root) {
    final choices = root['choices'];
    if (choices is! List || choices.isEmpty) {
      throw const FormatException('Provider response has no choices.');
    }
    final choice = _object(choices.first);
    if (choice == null) {
      throw const FormatException('Provider choice must be a JSON object.');
    }
    return choice;
  }

  Map<String, Object?>? _object(Object? value) {
    if (value is! Map) {
      return null;
    }
    return value.map((key, value) => MapEntry(key.toString(), value));
  }
}
