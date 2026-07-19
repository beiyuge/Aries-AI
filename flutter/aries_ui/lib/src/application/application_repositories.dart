import 'automation/automation_repository.dart';
import 'chat/chat_repository.dart';
import 'settings/settings_repository.dart';

class ApplicationRepositories {
  const ApplicationRepositories({
    required this.chat,
    required this.settings,
    required this.automation,
  });

  factory ApplicationRepositories.inMemory({DateTime Function()? clock}) {
    return ApplicationRepositories(
      chat: InMemoryChatRepository(clock: clock),
      settings: InMemorySettingsRepository(),
      automation: InMemoryAutomationRepository(),
    );
  }

  final ChatRepository chat;
  final SettingsRepository settings;
  final AutomationRepository automation;
}
