import 'package:shared_preferences/shared_preferences.dart';

import '../application/application_repositories.dart';
import 'automation/persistent_automation_repository.dart';
import 'chat/persistent_chat_repository.dart';
import 'persistence/persistence_keys.dart';
import 'persistence/shared_preferences_string_store.dart';
import 'settings/persistent_settings_repository.dart';

class ApplicationRepositoryFactory {
  const ApplicationRepositoryFactory._();

  static Future<ApplicationRepositories> createPersistent({
    DateTime Function()? clock,
  }) async {
    final preferences = await SharedPreferencesWithCache.create(
      cacheOptions: const SharedPreferencesWithCacheOptions(
        allowList: PersistenceKeys.all,
      ),
    );
    final store = SharedPreferencesStringStore(preferences);
    return ApplicationRepositories(
      chat: PersistentChatRepository(store: store, clock: clock),
      settings: PersistentSettingsRepository(store: store),
      automation: PersistentAutomationRepository(store: store),
    );
  }
}
