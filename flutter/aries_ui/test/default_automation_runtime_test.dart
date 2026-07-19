import 'package:aries_ui/src/application/automation/automation_command.dart';
import 'package:aries_ui/src/application/automation/automation_gateway.dart';
import 'package:aries_ui/src/application/automation/automation_runtime.dart';
import 'package:aries_ui/src/infrastructure/automation/default_automation_runtime.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  test('routes typed commands to the platform-neutral gateway', () async {
    final gateway = _FakeAutomationGateway();
    final runtime = DefaultAutomationRuntime(gateway: gateway);

    await runtime.execute(const CheckReadinessCommand());
    await runtime.execute(const StartScreenCaptureCommand());
    await runtime.execute(const StopScreenCaptureCommand());
    await runtime.execute(const DumpUiTreeCommand(detail: 'full'));
    await runtime.execute(const CaptureScreenCommand());
    await runtime.execute(const TapCommand(x: 12, y: 34));
    await runtime.execute(
      const SwipeCommand(
        fromX: 1,
        fromY: 2,
        toX: 3,
        toY: 4,
        durationMs: 500,
      ),
    );
    await runtime.execute(const TypeTextCommand('hello'));
    await runtime.execute(const PressKeyCommand(4));

    expect(gateway.calls, [
      'readiness',
      'capture-consent',
      'capture-stop',
      'tree:full',
      'capture',
      'tap:12,34',
      'swipe:1,2,3,4,500',
      'type:hello',
      'key:4',
    ]);
  });

  test('reports unsupported commands before reaching the gateway', () async {
    final gateway = _FakeAutomationGateway();
    final runtime = DefaultAutomationRuntime(gateway: gateway);

    final result = await runtime.execute(
      const UnsupportedAutomationCommand('open calendar'),
    );

    expect(result.success, isFalse);
    expect(result.errorCode, 'automation.command_unsupported');
    expect(gateway.calls, isEmpty);
  });
}

class _FakeAutomationGateway implements AutomationGateway {
  final List<String> calls = [];

  AutomationExecutionResult _record(String call) {
    calls.add(call);
    return AutomationExecutionResult(
      success: true,
      summary: call,
      recoverable: false,
    );
  }

  @override
  Future<AutomationExecutionResult> checkReadiness() async {
    return _record('readiness');
  }

  @override
  Future<AutomationExecutionResult> requestScreenCaptureConsent() async {
    return _record('capture-consent');
  }

  @override
  Future<AutomationExecutionResult> stopScreenCaptureSession() async {
    return _record('capture-stop');
  }

  @override
  Future<AutomationExecutionResult> dumpUiTree(String detail) async {
    return _record('tree:$detail');
  }

  @override
  Future<AutomationExecutionResult> captureScreen() async {
    return _record('capture');
  }

  @override
  Future<AutomationExecutionResult> tap(int x, int y) async {
    return _record('tap:$x,$y');
  }

  @override
  Future<AutomationExecutionResult> swipe({
    required int fromX,
    required int fromY,
    required int toX,
    required int toY,
    required int durationMs,
  }) async {
    return _record('swipe:$fromX,$fromY,$toX,$toY,$durationMs');
  }

  @override
  Future<AutomationExecutionResult> typeText(String text) async {
    return _record('type:$text');
  }

  @override
  Future<AutomationExecutionResult> pressKey(int keyCode) async {
    return _record('key:$keyCode');
  }
}
