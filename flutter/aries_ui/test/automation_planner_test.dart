import 'package:aries_ui/src/application/automation/automation_command.dart';
import 'package:aries_ui/src/application/automation/automation_planner.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  const planner = AutomationPlanner();

  test('plans readiness and artifact commands', () {
    expect(planner.plan('Check device readiness').command,
        isA<CheckReadinessCommand>());
    expect(
      (planner.plan('dump full UI tree').command as DumpUiTreeCommand).detail,
      'full',
    );
    expect(planner.plan('capture screen').command, isA<CaptureScreenCommand>());
  });

  test('plans screen session lifecycle before generic capture matching', () {
    expect(
      planner.plan('start screen capture').command,
      isA<StartScreenCaptureCommand>(),
    );
    expect(
      planner.plan('stop screen capture').command,
      isA<StopScreenCaptureCommand>(),
    );
  });

  test('plans the complete virtual display lifecycle', () {
    final start = planner
        .plan('start virtual display 1088x1920 dpi 480')
        .command as StartVirtualDisplayCommand;
    final launch = planner
        .plan('launch com.android.settings on virtual display')
        .command as LaunchOnVirtualDisplayCommand;

    expect((start.width, start.height, start.densityDpi), (1088, 1920, 480));
    expect(launch.applicationId, 'com.android.settings');
    expect(
      planner.plan('capture virtual display').command,
      isA<CaptureVirtualDisplayCommand>(),
    );
    expect(
      planner.plan('stop virtual display').command,
      isA<StopVirtualDisplayCommand>(),
    );
  });

  test('plans input commands with typed arguments', () {
    final tap = planner.plan('tap 12, 34').command as TapCommand;
    final swipe = planner.plan('swipe 1 2 3 4 450').command as SwipeCommand;
    final type = planner.plan('type hello Aries').command as TypeTextCommand;
    final key = planner.plan('key 4').command as PressKeyCommand;

    expect((tap.x, tap.y), (12, 34));
    expect(
      (swipe.fromX, swipe.fromY, swipe.toX, swipe.toY, swipe.durationMs),
      (1, 2, 3, 4, 450),
    );
    expect(type.text, 'hello Aries');
    expect(key.keyCode, 4);
  });

  test('keeps unsupported text as an explicit command', () {
    final command = planner.plan('open the calendar').command;

    expect(command, isA<UnsupportedAutomationCommand>());
    expect(
        (command as UnsupportedAutomationCommand).input, 'open the calendar');
  });
}
