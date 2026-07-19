import 'dart:typed_data';

import 'automation_command.dart';

class AutomationExecutionResult {
  const AutomationExecutionResult({
    required this.success,
    required this.summary,
    required this.recoverable,
    this.text,
    this.bytes,
    this.mimeType,
    this.errorCode,
  });

  final bool success;
  final String summary;
  final bool recoverable;
  final String? text;
  final Uint8List? bytes;
  final String? mimeType;
  final String? errorCode;
}

abstract interface class AutomationRuntime {
  Future<AutomationExecutionResult> execute(AutomationCommand command);
}

class UnavailableAutomationRuntime implements AutomationRuntime {
  const UnavailableAutomationRuntime();

  @override
  Future<AutomationExecutionResult> execute(AutomationCommand command) async {
    return const AutomationExecutionResult(
      success: false,
      summary: 'No automation runtime is configured.',
      recoverable: true,
      errorCode: 'automation.runtime_unavailable',
    );
  }
}
