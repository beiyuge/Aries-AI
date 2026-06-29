enum AutomationTaskStatus { queued, running, completed, cancelled }

class AutomationTask {
  const AutomationTask({
    required this.id,
    required this.title,
    required this.status,
    required this.steps,
  });

  final String id;
  final String title;
  final AutomationTaskStatus status;
  final List<String> steps;

  AutomationTask copyWith({
    AutomationTaskStatus? status,
    List<String>? steps,
  }) {
    return AutomationTask(
      id: id,
      title: title,
      status: status ?? this.status,
      steps: steps ?? this.steps,
    );
  }
}

class AutomationCapabilitySummary {
  const AutomationCapabilitySummary({
    required this.id,
    required this.label,
    required this.available,
  });

  final String id;
  final String label;
  final bool available;
}
