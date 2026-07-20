import 'package:flutter/services.dart';

import '../../application/automation/automation_gateway.dart';
import '../../application/automation/automation_runtime.dart';
import '../../generated/capabilities.g.dart';

class PigeonAutomationGateway implements AutomationGateway {
  PigeonAutomationGateway({AutomationHostApi? hostApi})
      : _hostApi = hostApi ?? AutomationHostApi();

  final AutomationHostApi _hostApi;

  @override
  Future<AutomationExecutionResult> checkReadiness() {
    return _invoke(_hostApi.checkReadiness);
  }

  @override
  Future<AutomationExecutionResult> requestScreenCaptureConsent() {
    return _invoke(_hostApi.requestScreenCaptureConsent);
  }

  @override
  Future<AutomationExecutionResult> stopScreenCaptureSession() {
    return _invoke(_hostApi.stopScreenCaptureSession);
  }

  @override
  Future<AutomationExecutionResult> dumpUiTree(String detail) {
    return _invoke(() => _hostApi.dumpUiTree(detail));
  }

  @override
  Future<AutomationExecutionResult> captureScreen() {
    return _invoke(_hostApi.captureScreen);
  }

  @override
  Future<AutomationExecutionResult> startVirtualDisplay({
    required int width,
    required int height,
    required int densityDpi,
  }) {
    return _invoke(
      () => _hostApi.startVirtualDisplay(width, height, densityDpi),
    );
  }

  @override
  Future<AutomationExecutionResult> launchOnVirtualDisplay(
    String applicationId,
  ) {
    return _invoke(() => _hostApi.launchOnVirtualDisplay(applicationId));
  }

  @override
  Future<AutomationExecutionResult> captureVirtualDisplay() {
    return _invoke(_hostApi.captureVirtualDisplay);
  }

  @override
  Future<AutomationExecutionResult> stopVirtualDisplay() {
    return _invoke(_hostApi.stopVirtualDisplay);
  }

  @override
  Future<AutomationExecutionResult> tap(int x, int y) {
    return _invoke(() => _hostApi.tap(x, y));
  }

  @override
  Future<AutomationExecutionResult> swipe({
    required int fromX,
    required int fromY,
    required int toX,
    required int toY,
    required int durationMs,
  }) {
    return _invoke(
      () => _hostApi.swipe(fromX, fromY, toX, toY, durationMs),
    );
  }

  @override
  Future<AutomationExecutionResult> typeText(String text) {
    return _invoke(() => _hostApi.typeText(text));
  }

  @override
  Future<AutomationExecutionResult> pressKey(int keyCode) {
    return _invoke(() => _hostApi.pressKey(keyCode));
  }

  Future<AutomationExecutionResult> _invoke(
    Future<AutomationResultDto> Function() operation,
  ) async {
    try {
      final result = await operation();
      return AutomationExecutionResult(
        success: result.success,
        summary: result.summary,
        recoverable: result.recoverable,
        text: result.text,
        bytes: result.bytes,
        mimeType: result.mimeType,
        errorCode: result.errorCode,
      );
    } on PlatformException catch (error) {
      return AutomationExecutionResult(
        success: false,
        summary: error.message ?? 'The platform automation call failed.',
        recoverable: true,
        errorCode: error.code,
      );
    }
  }
}
