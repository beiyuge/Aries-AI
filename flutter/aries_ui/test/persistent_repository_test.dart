import 'package:aries_ui/src/features/automation/controllers/automation_controller.dart';
import 'package:aries_ui/src/features/automation/models/automation_models.dart';
import 'package:aries_ui/src/features/chat/controllers/chat_controller.dart';
import 'package:aries_ui/src/features/settings/controllers/settings_controller.dart';
import 'package:aries_ui/src/infrastructure/automation/persistent_automation_repository.dart';
import 'package:aries_ui/src/infrastructure/chat/persistent_chat_repository.dart';
import 'package:aries_ui/src/infrastructure/persistence/persistence_keys.dart';
import 'package:aries_ui/src/infrastructure/persistence/string_store.dart';
import 'package:aries_ui/src/infrastructure/settings/persistent_settings_repository.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  DateTime clock() => DateTime.utc(2026, 6, 29, 12);

  test('chat history and attachments round-trip through JSON storage',
      () async {
    final store = InMemoryStringStore();
    final controller = ChatController(
      repository: PersistentChatRepository(store: store, clock: clock),
      clock: clock,
    );

    await controller.startNewSession();
    await controller.addSampleAttachment();
    await controller.send('Persist this conversation');

    final restored = ChatController(
      repository: PersistentChatRepository(store: store, clock: clock),
      clock: clock,
    );
    expect(restored.activeSession.title, 'Persist this conversation');
    expect(restored.activeSession.messages[1].attachments.single.name,
        'screen-context.json');
    expect(restored.activeSession.updatedAt, clock());
  });

  test('settings round-trip while provider catalog stays application-owned',
      () async {
    final store = InMemoryStringStore();
    final controller = SettingsController(
      repository: PersistentSettingsRepository(store: store),
    );

    await controller.selectProfile('local');
    await controller.setStreamResponses(false);
    await controller.setPreferLocalModel(true);

    final restored = SettingsController(
      repository: PersistentSettingsRepository(store: store),
    );
    expect(restored.selectedProfileId, 'local');
    expect(restored.streamResponses, isFalse);
    expect(restored.preferLocalModel, isTrue);
    expect(restored.profiles.map((profile) => profile.id),
        containsAll(['default', 'local', 'staging']));
  });

  test('automation queue round-trips while capabilities stay application-owned',
      () async {
    final store = InMemoryStringStore();
    final controller = AutomationController(
      repository: PersistentAutomationRepository(store: store),
    );

    await controller.enqueue('capture screen');
    await controller.run(controller.tasks.first.id);

    final restored = AutomationController(
      repository: PersistentAutomationRepository(store: store),
    );
    expect(restored.tasks.first.status, AutomationTaskStatus.completed);
    expect(restored.capabilities, isNotEmpty);
  });

  test('malformed or unsupported state falls back to clean defaults', () {
    final malformedStore = InMemoryStringStore({
      PersistenceKeys.settings: '{broken',
    });
    final futureSchemaStore = InMemoryStringStore({
      PersistenceKeys.automation: '{"schemaVersion":99,"data":{}}',
    });

    expect(
      PersistentSettingsRepository(store: malformedStore)
          .load()
          .selectedProfileId,
      'default',
    );
    expect(
      PersistentAutomationRepository(store: futureSchemaStore)
          .load()
          .tasks
          .single
          .id,
      'task-readiness',
    );
  });
}
