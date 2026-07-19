import 'package:aries_ui/src/application/chat/chat_runtime.dart';
import 'package:aries_ui/src/application/settings/provider_credential_store.dart';
import 'package:aries_ui/src/application/settings/settings_repository.dart';
import 'package:aries_ui/src/features/chat/models/chat_models.dart';
import 'package:aries_ui/src/infrastructure/chat/default_chat_runtime.dart';
import 'package:aries_ui/src/infrastructure/chat/remote_chat_configuration.dart';
import 'package:flutter_test/flutter_test.dart';

import 'support/fake_chat_runtime.dart';

void main() {
  test('returns a typed failure when the selected provider has no API key',
      () async {
    final runtime = DefaultChatRuntime(
      settings: InMemorySettingsRepository(),
      credentials: InMemoryProviderCredentialStore(),
    );

    final events = await runtime.generate(_request()).toList();

    final failure = events.single as ChatGenerationFailed;
    expect(failure.code, 'provider.api_key_missing');
    expect(failure.recoverable, isTrue);
  });

  test('routes through selected remote configuration and runtime settings',
      () async {
    final settings = InMemorySettingsRepository();
    final state = settings.load();
    await settings.save(state.copyWith(streamResponses: false));
    final credentials = InMemoryProviderCredentialStore();
    await credentials.writeApiKey('default', '  secret  ');
    late RemoteChatConfiguration capturedConfiguration;
    final remote = FakeChatRuntime.text('remote response');
    final runtime = DefaultChatRuntime(
      settings: settings,
      credentials: credentials,
      remoteFactory: (configuration) {
        capturedConfiguration = configuration;
        return remote;
      },
    );

    final events = await runtime.generate(_request()).toList();

    expect(capturedConfiguration.model, 'glm-4-flash');
    expect(capturedConfiguration.apiKey, 'secret');
    expect(remote.requests.single.streamResponse, isFalse);
    expect(
        events.whereType<ChatGenerationChunk>().single.text, 'remote response');
  });

  test('selected local provider routes to the platform local runtime',
      () async {
    final settings = InMemorySettingsRepository();
    await settings.save(settings.load().copyWith(selectedProfileId: 'local'));
    final local = FakeChatRuntime.text('local response');
    final runtime = DefaultChatRuntime(
      settings: settings,
      credentials: InMemoryProviderCredentialStore(),
      localRuntime: local,
    );

    final events = await runtime.generate(_request()).toList();

    expect(local.requests.single.modelId, 'local.default');
    expect(
        events.whereType<ChatGenerationChunk>().single.text, 'local response');
  });

  test('revalidates persisted endpoints before reading secure credentials',
      () async {
    final settings = InMemorySettingsRepository();
    final state = settings.load();
    await settings.save(
      state.copyWith(
        selectedProfileId: 'staging',
        profiles: [
          for (final profile in state.profiles)
            if (profile.id == 'staging')
              profile.copyWith(baseUrl: 'http://public.example/v1')
            else
              profile,
        ],
      ),
    );
    final runtime = DefaultChatRuntime(
      settings: settings,
      credentials: _FailOnReadCredentialStore(),
      remoteFactory: (_) => throw StateError('must not create runtime'),
    );

    final events = await runtime.generate(_request()).toList();

    expect(
      (events.single as ChatGenerationFailed).code,
      'provider.configuration_invalid',
    );
  });
}

ChatGenerationRequest _request() {
  return ChatGenerationRequest(
    modelId: 'remote.primary',
    streamResponse: true,
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

class _FailOnReadCredentialStore implements ProviderCredentialStore {
  @override
  Future<String?> readApiKey(String profileId) {
    throw StateError('must not read credentials');
  }

  @override
  Future<void> writeApiKey(String profileId, String? apiKey) async {}
}
