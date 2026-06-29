import 'package:flutter/foundation.dart';

import '../models/automation_models.dart';

class AutomationController extends ChangeNotifier {
  List<AutomationTask> _tasks = const [
    AutomationTask(
      id: 'task-readiness',
      title: 'Check device readiness',
      status: AutomationTaskStatus.queued,
      steps: ['Diagnostics', 'Permissions', 'Backend health'],
    ),
  ];
  int _nextId = 0;

  List<AutomationTask> get tasks => List.unmodifiable(_tasks);

  List<AutomationCapabilitySummary> get capabilities => const [
        AutomationCapabilitySummary(
            id: 'ui.tree', label: 'UI tree', available: true),
        AutomationCapabilitySummary(
            id: 'input.injection', label: 'Input', available: true),
        AutomationCapabilitySummary(
            id: 'screen.capture', label: 'Screen', available: true),
        AutomationCapabilitySummary(
            id: 'virtual.display', label: 'Virtual display', available: true),
      ];

  void enqueue(String title) {
    _nextId += 1;
    _tasks = [
      AutomationTask(
        id: 'task-$_nextId',
        title: title,
        status: AutomationTaskStatus.queued,
        steps: _stepsFor(title),
      ),
      ..._tasks,
    ];
    notifyListeners();
  }

  void run(String taskId) {
    _update(taskId, (task) {
      return task.copyWith(
        status: AutomationTaskStatus.completed,
        steps: [...task.steps, 'Result captured'],
      );
    });
  }

  void cancel(String taskId) {
    _update(taskId,
        (task) => task.copyWith(status: AutomationTaskStatus.cancelled));
  }

  void _update(
      String taskId, AutomationTask Function(AutomationTask task) update) {
    _tasks = [
      for (final task in _tasks)
        if (task.id == taskId) update(task) else task,
    ];
    notifyListeners();
  }

  List<String> _stepsFor(String title) {
    final lower = title.toLowerCase();
    if (lower.contains('type') || lower.contains('input')) {
      return ['Dump UI tree', 'Find input target', 'Inject text'];
    }
    if (lower.contains('screen') || lower.contains('capture')) {
      return ['Capture screen', 'Attach frame', 'Summarize state'];
    }
    return ['Plan', 'Check capabilities', 'Execute'];
  }
}
