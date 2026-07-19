import 'package:flutter/foundation.dart';

import '../../../application/automation/automation_repository.dart';
import '../models/automation_models.dart';

class AutomationController extends ChangeNotifier {
  AutomationController({
    AutomationRepository? repository,
    AutomationPlanner? planner,
  })  : _repository = repository ?? InMemoryAutomationRepository(),
        _planner = planner ?? const AutomationPlanner() {
    _state = _repository.load();
  }

  final AutomationRepository _repository;
  final AutomationPlanner _planner;
  late AutomationState _state;

  List<AutomationTask> get tasks => _state.tasks;

  List<AutomationCapabilitySummary> get capabilities => _state.capabilities;

  Future<void> enqueue(String title) {
    final nextId = _state.nextId + 1;
    return _save(
      _state.copyWith(
        nextId: nextId,
        tasks: [
          AutomationTask(
            id: 'task-$nextId',
            title: title,
            status: AutomationTaskStatus.queued,
            steps: _planner.stepsFor(title),
          ),
          ..._state.tasks,
        ],
      ),
    );
  }

  Future<void> run(String taskId) {
    return _update(taskId, (task) {
      return task.copyWith(
        status: AutomationTaskStatus.completed,
        steps: [...task.steps, 'Result captured'],
      );
    });
  }

  Future<void> cancel(String taskId) {
    return _update(
      taskId,
      (task) => task.copyWith(status: AutomationTaskStatus.cancelled),
    );
  }

  Future<void> _update(
    String taskId,
    AutomationTask Function(AutomationTask task) update,
  ) {
    return _save(
      _state.copyWith(
        tasks: [
          for (final task in _state.tasks)
            if (task.id == taskId) update(task) else task,
        ],
      ),
    );
  }

  Future<void> _save(AutomationState state) async {
    _state = state;
    notifyListeners();
    await _repository.save(state);
  }
}
