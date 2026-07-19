import 'package:aries_ui/src/application/automation/automation_repository.dart';
import 'package:aries_ui/src/application/chat/chat_repository.dart';
import 'package:aries_ui/src/application/settings/settings_repository.dart';
import 'package:aries_ui/src/features/automation/controllers/automation_controller.dart';
import 'package:aries_ui/src/features/automation/models/automation_models.dart';
import 'package:aries_ui/src/features/chat/controllers/chat_controller.dart';
import 'package:aries_ui/src/features/settings/controllers/settings_controller.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  DateTime clock() => DateTime(2026, 6, 29, 12);

  test('chat state survives controller recreation through its repository', () {
    final repository = InMemoryChatRepository(clock: clock);
    final controller = ChatController(repository: repository, clock: clock);

    controller.startNewSession();
    controller.selectModel('automation.copilot');
    controller.addSampleAttachment();
    controller.send('Inspect the active screen');

    final restored = ChatController(repository: repository, clock: clock);
    expect(restored.selectedModelId, 'automation.copilot');
    expect(restored.pendingAttachments, isEmpty);
    expect(restored.activeSession.title, 'Inspect the active screen');
    expect(
      restored.activeSession.messages.last.markdown,
      contains('automation.copilot'),
    );
    expect(restored.activeSession.messages[1].attachments, hasLength(1));
  });

  test(
    'settings state survives controller recreation through its repository',
    () {
      final repository = InMemorySettingsRepository();
      final controller = SettingsController(repository: repository);

      controller.selectProfile('local');
      controller.setStreamResponses(false);
      controller.setPreferLocalModel(true);

      final restored = SettingsController(repository: repository);
      expect(restored.selectedProfileId, 'local');
      expect(restored.streamResponses, isFalse);
      expect(restored.preferLocalModel, isTrue);
    },
  );

  test(
    'automation state survives controller recreation through its repository',
    () {
      final repository = InMemoryAutomationRepository();
      final controller = AutomationController(repository: repository);

      controller.enqueue('capture screen');
      controller.run(controller.tasks.first.id);

      final restored = AutomationController(repository: repository);
      expect(restored.tasks.first.title, 'capture screen');
      expect(restored.tasks.first.status, AutomationTaskStatus.completed);
      expect(restored.tasks.first.steps.last, 'Result captured');
    },
  );
}
