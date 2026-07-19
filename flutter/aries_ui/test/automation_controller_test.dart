import 'dart:async';

import 'package:aries_ui/src/application/automation/automation_command.dart';
import 'package:aries_ui/src/application/automation/automation_repository.dart';
import 'package:aries_ui/src/application/automation/automation_runtime.dart';
import 'package:aries_ui/src/features/automation/controllers/automation_controller.dart';
import 'package:aries_ui/src/features/automation/models/automation_models.dart';
import 'package:flutter_test/flutter_test.dart';

import 'support/fake_automation_runtime.dart';

void main() {
  test('runs a planned command and records the native result', () async {
    final runtime = FakeAutomationRuntime.success('Captured 1080x2400');
    final controller = AutomationController(
      repository: InMemoryAutomationRepository(),
      runtime: runtime,
    );

    await controller.enqueue('capture screen');
    await controller.run(controller.tasks.first.id);

    expect(runtime.commands.single, isA<CaptureScreenCommand>());
    expect(controller.tasks.first.status, AutomationTaskStatus.completed);
    expect(controller.tasks.first.steps.last, 'Captured 1080x2400');
  });

  test('records typed native failures', () async {
    final runtime = FakeAutomationRuntime(
      const AutomationExecutionResult(
        success: false,
        summary: 'MediaProjection consent is required.',
        recoverable: true,
        errorCode: 'screen_capture.media_projection_required',
      ),
    );
    final controller = AutomationController(runtime: runtime);

    await controller.run(controller.tasks.first.id);

    expect(controller.tasks.first.status, AutomationTaskStatus.failed);
    expect(
      controller.tasks.first.steps,
      contains('Error: screen_capture.media_projection_required'),
    );
  });

  test('a cancelled task is not overwritten by a late native result', () async {
    final runtime = _DeferredAutomationRuntime();
    final controller = AutomationController(runtime: runtime);
    final taskId = controller.tasks.first.id;

    final run = controller.run(taskId);
    await Future<void>.delayed(Duration.zero);
    await controller.cancel(taskId);
    runtime.complete('late success');
    await run;

    expect(controller.tasks.first.status, AutomationTaskStatus.cancelled);
    expect(controller.tasks.first.steps, isNot(contains('late success')));
  });
}

class _DeferredAutomationRuntime implements AutomationRuntime {
  final Completer<AutomationExecutionResult> _result = Completer();

  @override
  Future<AutomationExecutionResult> execute(AutomationCommand command) {
    return _result.future;
  }

  void complete(String summary) {
    _result.complete(
      AutomationExecutionResult(
        success: true,
        summary: summary,
        recoverable: false,
      ),
    );
  }
}
