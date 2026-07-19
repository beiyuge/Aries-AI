import '../../application/chat/chat_runtime.dart';
import '../../application/chat/local_model_gateway.dart';
import '../../application/settings/settings_repository.dart';
import 'local_model_prompt_encoder.dart';

class LocalModelChatRuntime implements ChatRuntime {
  LocalModelChatRuntime({
    required this.gateway,
    required this.settings,
    this.promptEncoder = const LocalModelPromptEncoder(),
  });

  final LocalModelGateway gateway;
  final SettingsRepository settings;
  final LocalModelPromptEncoder promptEncoder;
  String? _loadedKey;

  @override
  Stream<ChatGenerationEvent> generate(ChatGenerationRequest request) async* {
    try {
      final modelPath = settings.load().localModelPath;
      if (modelPath.isEmpty) {
        yield const ChatGenerationFailed(
          code: 'local_model.path_missing',
          message: 'Choose a local model file in Settings first.',
          recoverable: true,
        );
        return;
      }
      final loadedKey = '${request.modelId}:$modelPath';
      if (_loadedKey != loadedKey) {
        await gateway.load(modelId: request.modelId, path: modelPath);
        _loadedKey = loadedKey;
      }
      final response = await gateway.generate(
        modelId: request.modelId,
        prompt: promptEncoder.encode(request.messages),
      );
      if (response.isNotEmpty) {
        yield ChatGenerationChunk(response);
      }
      yield const ChatGenerationDone();
    } on LocalModelGatewayException catch (error) {
      yield ChatGenerationFailed(
        code: error.code,
        message: error.message,
        recoverable: error.recoverable,
      );
    }
  }
}
