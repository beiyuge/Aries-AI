import '../../application/chat/chat_runtime.dart';
import '../../application/settings/provider_credential_store.dart';
import '../../application/settings/settings_repository.dart';
import '../../features/settings/models/settings_models.dart';
import 'openai_compatible_chat_runtime.dart';
import 'remote_chat_configuration.dart';

typedef RemoteChatRuntimeFactory = ChatRuntime Function(
  RemoteChatConfiguration configuration,
);

class DefaultChatRuntime implements ChatRuntime {
  DefaultChatRuntime({
    required this.settings,
    required this.credentials,
    ChatRuntime? localRuntime,
    RemoteChatRuntimeFactory? remoteFactory,
    ProviderConfigurationValidator configurationValidator =
        const ProviderConfigurationValidator(),
  })  : _localRuntime = localRuntime ?? const UnavailableChatRuntime(),
        _configurationValidator = configurationValidator,
        _remoteFactory = remoteFactory ??
            ((configuration) => OpenAiCompatibleChatRuntime(
                  configuration: configuration,
                ));

  final SettingsRepository settings;
  final ProviderCredentialStore credentials;
  final ChatRuntime _localRuntime;
  final RemoteChatRuntimeFactory _remoteFactory;
  final ProviderConfigurationValidator _configurationValidator;

  @override
  Stream<ChatGenerationEvent> generate(ChatGenerationRequest request) async* {
    final state = settings.load();
    final profile = _selectedProfile(state);
    if (profile == null) {
      yield const ChatGenerationFailed(
        code: 'provider.profile_missing',
        message: 'The selected provider profile no longer exists.',
        recoverable: true,
      );
      return;
    }
    if (request.modelId == 'local.wrapper' ||
        state.preferLocalModel ||
        profile.kind == ProviderKind.local) {
      final localModel = _localModel(state) ?? request.modelId;
      yield* _localRuntime.generate(
        request.copyWith(
          modelId: localModel,
          streamResponse: state.streamResponses,
        ),
      );
      return;
    }

    final configurationError = _configurationValidator.validate(
      baseUrl: profile.baseUrl,
      model: profile.model,
    );
    if (configurationError != null) {
      yield ChatGenerationFailed(
        code: 'provider.configuration_invalid',
        message: configurationError,
        recoverable: true,
      );
      return;
    }

    try {
      final apiKey = await credentials.readApiKey(profile.id);
      if (apiKey == null || apiKey.trim().isEmpty) {
        yield const ChatGenerationFailed(
          code: 'provider.api_key_missing',
          message: 'Add an API key for the selected provider in Settings.',
          recoverable: true,
        );
        return;
      }
      final runtime = _remoteFactory(
        RemoteChatConfiguration(
          baseUrl: _configurationValidator.normalizeBaseUrl(profile.baseUrl),
          model: profile.model,
          apiKey: apiKey.trim(),
        ),
      );
      yield* runtime.generate(
        request.copyWith(streamResponse: state.streamResponses),
      );
    } on ProviderCredentialException catch (error) {
      yield ChatGenerationFailed(
        code: 'provider.credential_unavailable',
        message: error.message,
        recoverable: true,
      );
    }
  }

  ProviderProfile? _selectedProfile(SettingsState state) {
    for (final profile in state.profiles) {
      if (profile.id == state.selectedProfileId) {
        return profile;
      }
    }
    return null;
  }

  String? _localModel(SettingsState state) {
    for (final profile in state.profiles) {
      if (profile.kind == ProviderKind.local) {
        return profile.model;
      }
    }
    return null;
  }
}
