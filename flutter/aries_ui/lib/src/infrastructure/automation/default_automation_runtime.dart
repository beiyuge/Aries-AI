import '../../application/automation/automation_command.dart';
import '../../application/automation/automation_gateway.dart';
import '../../application/automation/automation_runtime.dart';

class DefaultAutomationRuntime implements AutomationRuntime {
  const DefaultAutomationRuntime({required this.gateway});

  final AutomationGateway gateway;

  @override
  Future<AutomationExecutionResult> execute(AutomationCommand command) {
    return switch (command) {
      CheckReadinessCommand() => gateway.checkReadiness(),
      StartScreenCaptureCommand() => gateway.requestScreenCaptureConsent(),
      StopScreenCaptureCommand() => gateway.stopScreenCaptureSession(),
      DumpUiTreeCommand() => gateway.dumpUiTree(command.detail),
      CaptureScreenCommand() => gateway.captureScreen(),
      TapCommand() => gateway.tap(command.x, command.y),
      SwipeCommand() => gateway.swipe(
          fromX: command.fromX,
          fromY: command.fromY,
          toX: command.toX,
          toY: command.toY,
          durationMs: command.durationMs,
        ),
      TypeTextCommand() => gateway.typeText(command.text),
      PressKeyCommand() => gateway.pressKey(command.keyCode),
      UnsupportedAutomationCommand() => Future.value(
          AutomationExecutionResult(
            success: false,
            summary: "Unsupported automation command: '${command.input}'.",
            recoverable: true,
            errorCode: 'automation.command_unsupported',
          ),
        ),
    };
  }
}
