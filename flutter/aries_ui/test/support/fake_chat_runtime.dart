import 'package:aries_ui/src/application/chat/chat_runtime.dart';

class FakeChatRuntime implements ChatRuntime {
  FakeChatRuntime(this.events);

  factory FakeChatRuntime.text(String text) {
    return FakeChatRuntime([
      ChatGenerationChunk(text),
      const ChatGenerationDone(),
    ]);
  }

  final List<ChatGenerationEvent> events;
  final List<ChatGenerationRequest> requests = [];

  @override
  Stream<ChatGenerationEvent> generate(ChatGenerationRequest request) async* {
    requests.add(request);
    for (final event in events) {
      yield event;
    }
  }
}
