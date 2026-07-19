import 'package:aries_ui/src/application/chat/chat_runtime.dart';
import 'package:aries_ui/src/application/chat/local_model_gateway.dart';
import 'package:aries_ui/src/application/settings/settings_repository.dart';
import 'package:aries_ui/src/features/chat/models/chat_models.dart';
import 'package:aries_ui/src/infrastructure/chat/local_model_chat_runtime.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  test('maps host output to shared chat events without exposing local paths',
      () async {
    final gateway = _FakeLocalModelGateway(response: 'local result');
    final settings = InMemorySettingsRepository();
    await settings.save(
      settings.load().copyWith(localModelPath: '/models/local.gguf'),
    );
    final runtime = LocalModelChatRuntime(
      gateway: gateway,
      settings: settings,
    );

    final events = await runtime.generate(_request()).toList();

    expect(gateway.modelId, 'local.default');
    expect(gateway.loadedPath, '/models/local.gguf');
    expect(gateway.prompt, contains('screen.png'));
    expect(gateway.prompt, isNot(contains('/private/device/screen.png')));
    expect((events.first as ChatGenerationChunk).text, 'local result');
    expect(events.last, isA<ChatGenerationDone>());
  });

  test('maps typed host errors to shared chat failures', () async {
    final settings = InMemorySettingsRepository();
    await settings.save(
      settings.load().copyWith(localModelPath: '/models/local.gguf'),
    );
    final runtime = LocalModelChatRuntime(
      gateway: _FakeLocalModelGateway(
        error: const LocalModelGatewayException(
          code: 'local_model.not_loaded',
          message: 'Load a model first.',
          recoverable: true,
        ),
      ),
      settings: settings,
    );

    final events = await runtime.generate(_request()).toList();

    final failure = events.single as ChatGenerationFailed;
    expect(failure.code, 'local_model.not_loaded');
    expect(failure.recoverable, isTrue);
  });

  test('reports a missing configured model before calling the host', () async {
    final gateway = _FakeLocalModelGateway(response: 'unused');
    final runtime = LocalModelChatRuntime(
      gateway: gateway,
      settings: InMemorySettingsRepository(),
    );

    final events = await runtime.generate(_request()).toList();

    expect(
      (events.single as ChatGenerationFailed).code,
      'local_model.path_missing',
    );
    expect(gateway.modelId, isNull);
  });
}

ChatGenerationRequest _request() {
  return ChatGenerationRequest(
    modelId: 'local.default',
    streamResponse: true,
    messages: [
      ChatMessage(
        id: 'message-1',
        role: ChatMessageRole.user,
        markdown: 'Inspect',
        attachments: const [
          ChatAttachment(
            id: 'attachment-1',
            name: 'screen.png',
            mimeType: 'image/png',
            byteLength: 2048,
            source: '/private/device/screen.png',
          ),
        ],
        createdAt: DateTime.utc(2026),
      ),
    ],
  );
}

class _FakeLocalModelGateway implements LocalModelGateway {
  _FakeLocalModelGateway({this.response = '', this.error});

  final String response;
  final LocalModelGatewayException? error;
  String? modelId;
  String? prompt;
  String? loadedPath;

  @override
  Future<String> generate({
    required String modelId,
    required String prompt,
  }) async {
    this.modelId = modelId;
    this.prompt = prompt;
    final failure = error;
    if (failure != null) {
      throw failure;
    }
    return response;
  }

  @override
  Future<void> load({required String modelId, required String path}) async {
    loadedPath = path;
  }

  @override
  Future<void> unload(String modelId) async {}
}
