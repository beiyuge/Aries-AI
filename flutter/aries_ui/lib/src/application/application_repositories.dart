import 'automation/automation_repository.dart';
import 'chat/chat_repository.dart';
import 'settings/settings_repository.dart';
import 'settings/provider_credential_store.dart';

class ApplicationRepositories {
  const ApplicationRepositories({
    required this.chat,
    required this.settings,
    required this.automation,
    required this.providerCredentials,
  });

  factory ApplicationRepositories.inMemory({DateTime Function()? clock}) {
    return ApplicationRepositories(
      chat: InMemoryChatRepository(clock: clock),
      settings: InMemorySettingsRepository(),
      automation: InMemoryAutomationRepository(),
      providerCredentials: InMemoryProviderCredentialStore(),
    );
  }

  final ChatRepository chat;
  final SettingsRepository settings;
  final AutomationRepository automation;
  final ProviderCredentialStore providerCredentials;
}
