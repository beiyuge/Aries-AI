import 'automation_command.dart';

class AutomationPlan {
  AutomationPlan({
    required this.command,
    required List<String> steps,
  }) : steps = List.unmodifiable(steps);

  final AutomationCommand command;
  final List<String> steps;
}

class AutomationPlanner {
  const AutomationPlanner();

  AutomationPlan plan(String input) {
    final normalized = input.trim();
    final lower = normalized.toLowerCase();
    if (lower.contains('readiness') ||
        lower.contains('diagnostics') ||
        lower.contains('health')) {
      return AutomationPlan(
        command: const CheckReadinessCommand(),
        steps: const ['Read native health', 'Summarize readiness'],
      );
    }
    if (lower.contains('ui tree') || lower.contains('dump tree')) {
      final detail = lower.contains('full')
          ? 'full'
          : lower.contains('minimal')
              ? 'minimal'
              : 'summary';
      return AutomationPlan(
        command: DumpUiTreeCommand(detail: detail),
        steps: const ['Dump UI tree', 'Capture node summary'],
      );
    }
    if (lower.contains('screen') || lower.contains('capture')) {
      return AutomationPlan(
        command: const CaptureScreenCommand(),
        steps: const ['Capture screen', 'Record frame metadata'],
      );
    }

    final tap = RegExp(
      r'^(?:tap|click)\s+(\d+)[,\s]+(\d+)$',
      caseSensitive: false,
    ).firstMatch(normalized);
    if (tap != null) {
      return AutomationPlan(
        command: TapCommand(
          x: int.parse(tap.group(1)!),
          y: int.parse(tap.group(2)!),
        ),
        steps: const ['Validate coordinates', 'Inject tap'],
      );
    }

    final swipe = RegExp(
      r'^swipe\s+(\d+)[,\s]+(\d+)[,\s]+(\d+)[,\s]+(\d+)(?:[,\s]+(\d+))?$',
      caseSensitive: false,
    ).firstMatch(normalized);
    if (swipe != null) {
      return AutomationPlan(
        command: SwipeCommand(
          fromX: int.parse(swipe.group(1)!),
          fromY: int.parse(swipe.group(2)!),
          toX: int.parse(swipe.group(3)!),
          toY: int.parse(swipe.group(4)!),
          durationMs: int.tryParse(swipe.group(5) ?? '') ?? 300,
        ),
        steps: const ['Validate gesture', 'Inject swipe'],
      );
    }

    final type = RegExp(
      r'^(?:type|input)\s+(.+)$',
      caseSensitive: false,
    ).firstMatch(normalized);
    if (type != null) {
      return AutomationPlan(
        command: TypeTextCommand(type.group(1)!),
        steps: const ['Resolve focused input', 'Inject text'],
      );
    }

    final key = RegExp(
      r'^key\s+(\d+)$',
      caseSensitive: false,
    ).firstMatch(normalized);
    if (key != null) {
      return AutomationPlan(
        command: PressKeyCommand(int.parse(key.group(1)!)),
        steps: const ['Validate key code', 'Inject key'],
      );
    }

    return AutomationPlan(
      command: UnsupportedAutomationCommand(normalized),
      steps: const ['Parse command', 'Report unsupported command'],
    );
  }
}
