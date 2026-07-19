import '../../features/chat/models/chat_models.dart';

class ChatGenerationRequest {
  ChatGenerationRequest({
    required this.modelId,
    required List<ChatMessage> messages,
    required this.streamResponse,
  }) : messages = List.unmodifiable(messages);

  final String modelId;
  final List<ChatMessage> messages;
  final bool streamResponse;

  ChatGenerationRequest copyWith({String? modelId, bool? streamResponse}) {
    return ChatGenerationRequest(
      modelId: modelId ?? this.modelId,
      messages: messages,
      streamResponse: streamResponse ?? this.streamResponse,
    );
  }
}

sealed class ChatGenerationEvent {
  const ChatGenerationEvent();
}

class ChatGenerationChunk extends ChatGenerationEvent {
  const ChatGenerationChunk(this.text);

  final String text;
}

class ChatGenerationDone extends ChatGenerationEvent {
  const ChatGenerationDone();
}

class ChatGenerationFailed extends ChatGenerationEvent {
  const ChatGenerationFailed({
    required this.code,
    required this.message,
    required this.recoverable,
  });

  final String code;
  final String message;
  final bool recoverable;
}

abstract interface class ChatRuntime {
  Stream<ChatGenerationEvent> generate(ChatGenerationRequest request);
}

class UnavailableChatRuntime implements ChatRuntime {
  const UnavailableChatRuntime();

  @override
  Stream<ChatGenerationEvent> generate(ChatGenerationRequest request) async* {
    yield const ChatGenerationFailed(
      code: 'chat.runtime_unavailable',
      message: 'No chat runtime is configured.',
      recoverable: true,
    );
  }
}
