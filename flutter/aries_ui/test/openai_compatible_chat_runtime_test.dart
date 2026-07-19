import 'dart:convert';

import 'package:aries_ui/src/application/chat/chat_runtime.dart';
import 'package:aries_ui/src/features/chat/models/chat_models.dart';
import 'package:aries_ui/src/infrastructure/chat/openai_chat_request_encoder.dart';
import 'package:aries_ui/src/infrastructure/chat/openai_compatible_chat_runtime.dart';
import 'package:aries_ui/src/infrastructure/chat/remote_chat_configuration.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';

void main() {
  const configuration = RemoteChatConfiguration(
    baseUrl: 'https://provider.example/v1/',
    model: 'test-model',
    apiKey: 'test-secret',
  );

  test('streams OpenAI-compatible SSE chunks and completion', () async {
    late http.Request captured;
    final client = MockClient((request) async {
      captured = request;
      return http.Response(
        'data: {"choices":[{"delta":{"content":"Hel"}}]}\n\n'
        'data: {"choices":[{"delta":{"content":"lo"}}]}\n\n'
        'data: [DONE]\n\n',
        200,
        headers: {'content-type': 'text/event-stream'},
      );
    });
    final runtime = OpenAiCompatibleChatRuntime(
      configuration: configuration,
      client: client,
    );

    final events = await runtime.generate(_request(stream: true)).toList();

    expect(captured.url,
        Uri.parse('https://provider.example/v1/chat/completions'));
    expect(captured.followRedirects, isFalse);
    expect(captured.headers['Authorization'], 'Bearer test-secret');
    expect(
      events.whereType<ChatGenerationChunk>().map((event) => event.text),
      ['Hel', 'lo'],
    );
    expect(events.last, isA<ChatGenerationDone>());
  });

  test('decodes a non-streaming response', () async {
    final client = MockClient(
      (_) async => http.Response(
        '{"choices":[{"message":{"content":"Complete"}}]}',
        200,
      ),
    );
    final runtime = OpenAiCompatibleChatRuntime(
      configuration: configuration,
      client: client,
    );

    final events = await runtime.generate(_request(stream: false)).toList();

    expect((events.first as ChatGenerationChunk).text, 'Complete');
    expect(events.last, isA<ChatGenerationDone>());
  });

  test('does not follow redirects or forward credentials', () async {
    final client = MockClient((request) async {
      expect(request.followRedirects, isFalse);
      expect(request.headers['Authorization'], 'Bearer test-secret');
      return http.Response('', 302,
          headers: {'location': 'https://other.test'});
    });
    final runtime = OpenAiCompatibleChatRuntime(
      configuration: configuration,
      client: client,
    );

    final events = await runtime.generate(_request(stream: true)).toList();

    final failure = events.single as ChatGenerationFailed;
    expect(failure.code, 'provider.http_302');
    expect(failure.recoverable, isFalse);
  });

  test('maps malformed provider content to a typed failure', () async {
    final client =
        MockClient((_) async => http.Response('{"choices":[]}', 200));
    final runtime = OpenAiCompatibleChatRuntime(
      configuration: configuration,
      client: client,
    );

    final events = await runtime.generate(_request(stream: false)).toList();

    expect(events.single, isA<ChatGenerationFailed>());
    expect(
      (events.single as ChatGenerationFailed).code,
      'provider.invalid_response',
    );
  });

  test('request body includes attachment metadata but not its local path', () {
    final request = ChatGenerationRequest(
      modelId: 'remote.primary',
      streamResponse: true,
      messages: [
        ChatMessage(
          id: 'message-1',
          role: ChatMessageRole.user,
          markdown: 'Inspect this',
          attachments: const [
            ChatAttachment(
              id: 'attachment-1',
              name: 'screen.png',
              mimeType: 'image/png',
              byteLength: 2048,
              source: '/private/sensitive/screen.png',
            ),
          ],
          createdAt: DateTime.utc(2026),
        ),
      ],
    );

    final encoded = const OpenAiChatRequestEncoder().encode(
      configuration,
      request,
    );
    final root = jsonDecode(encoded) as Map<String, dynamic>;
    final messages = root['messages'] as List<dynamic>;
    final content =
        (messages.single as Map<String, dynamic>)['content'] as String;

    expect(content, contains('screen.png'));
    expect(content, contains('2048'));
    expect(content, isNot(contains('/private/sensitive')));
  });
}

ChatGenerationRequest _request({required bool stream}) {
  return ChatGenerationRequest(
    modelId: 'remote.primary',
    streamResponse: stream,
    messages: [
      ChatMessage(
        id: 'message-1',
        role: ChatMessageRole.user,
        markdown: 'Hello',
        createdAt: DateTime.utc(2026),
      ),
    ],
  );
}
