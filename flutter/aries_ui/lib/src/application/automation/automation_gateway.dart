import 'automation_runtime.dart';

abstract interface class AutomationGateway {
  Future<AutomationExecutionResult> checkReadiness();

  Future<AutomationExecutionResult> requestScreenCaptureConsent();

  Future<AutomationExecutionResult> stopScreenCaptureSession();

  Future<AutomationExecutionResult> dumpUiTree(String detail);

  Future<AutomationExecutionResult> captureScreen();

  Future<AutomationExecutionResult> tap(int x, int y);

  Future<AutomationExecutionResult> swipe({
    required int fromX,
    required int fromY,
    required int toX,
    required int toY,
    required int durationMs,
  });

  Future<AutomationExecutionResult> typeText(String text);

  Future<AutomationExecutionResult> pressKey(int keyCode);
}
