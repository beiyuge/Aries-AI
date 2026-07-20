sealed class AutomationCommand {
  const AutomationCommand();
}

class CheckReadinessCommand extends AutomationCommand {
  const CheckReadinessCommand();
}

class StartScreenCaptureCommand extends AutomationCommand {
  const StartScreenCaptureCommand();
}

class StopScreenCaptureCommand extends AutomationCommand {
  const StopScreenCaptureCommand();
}

class DumpUiTreeCommand extends AutomationCommand {
  const DumpUiTreeCommand({this.detail = 'summary'});

  final String detail;
}

class CaptureScreenCommand extends AutomationCommand {
  const CaptureScreenCommand();
}

class StartVirtualDisplayCommand extends AutomationCommand {
  const StartVirtualDisplayCommand({
    required this.width,
    required this.height,
    required this.densityDpi,
  });

  final int width;
  final int height;
  final int densityDpi;
}

class LaunchOnVirtualDisplayCommand extends AutomationCommand {
  const LaunchOnVirtualDisplayCommand(this.applicationId);

  final String applicationId;
}

class CaptureVirtualDisplayCommand extends AutomationCommand {
  const CaptureVirtualDisplayCommand();
}

class StopVirtualDisplayCommand extends AutomationCommand {
  const StopVirtualDisplayCommand();
}

class TapCommand extends AutomationCommand {
  const TapCommand({required this.x, required this.y});

  final int x;
  final int y;
}

class SwipeCommand extends AutomationCommand {
  const SwipeCommand({
    required this.fromX,
    required this.fromY,
    required this.toX,
    required this.toY,
    required this.durationMs,
  });

  final int fromX;
  final int fromY;
  final int toX;
  final int toY;
  final int durationMs;
}

class TypeTextCommand extends AutomationCommand {
  const TypeTextCommand(this.text);

  final String text;
}

class PressKeyCommand extends AutomationCommand {
  const PressKeyCommand(this.keyCode);

  final int keyCode;
}

class UnsupportedAutomationCommand extends AutomationCommand {
  const UnsupportedAutomationCommand(this.input);

  final String input;
}
