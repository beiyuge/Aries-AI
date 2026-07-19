import 'package:flutter/foundation.dart';

import '../../../application/automation/automation_planner.dart';
import '../../../application/automation/automation_repository.dart';
import '../../../application/automation/automation_runtime.dart';
import '../models/automation_models.dart';

class AutomationController extends ChangeNotifier {
  AutomationController({
    AutomationRepository? repository,
    AutomationPlanner? planner,
    AutomationRuntime? runtime,
  })  : _repository = repository ?? InMemoryAutomationRepository(),
        _planner = planner ?? const AutomationPlanner(),
        _runtime = runtime ?? const UnavailableAutomationRuntime() {
    _state = _repository.load();
  }

  final AutomationRepository _repository;
  final AutomationPlanner _planner;
  final AutomationRuntime _runtime;
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
            steps: _planner.plan(title).steps,
          ),
          ..._state.tasks,
        ],
      ),
    );
  }

  Future<void> run(String taskId) async {
    final task = _taskById(taskId);
    if (task == null || task.status == AutomationTaskStatus.running) {
      return;
    }

    final plan = _planner.plan(task.title);
    await _update(
      taskId,
      (current) => current.copyWith(
        status: AutomationTaskStatus.running,
        steps: [...current.steps, 'Executing native capability'],
      ),
    );

    final result = await _runtime.execute(plan.command);
    final current = _taskById(taskId);
    if (current == null || current.status == AutomationTaskStatus.cancelled) {
      return;
    }

    await _update(
      taskId,
      (latest) => latest.copyWith(
        status: result.success
            ? AutomationTaskStatus.completed
            : AutomationTaskStatus.failed,
        steps: [
          ...latest.steps,
          result.summary,
          if (result.errorCode case final errorCode?) 'Error: $errorCode',
        ],
      ),
    );
  }

  Future<void> cancel(String taskId) {
    final task = _taskById(taskId);
    if (task == null ||
        (task.status != AutomationTaskStatus.queued &&
            task.status != AutomationTaskStatus.running)) {
      return Future.value();
    }
    return _update(
      taskId,
      (task) => task.copyWith(status: AutomationTaskStatus.cancelled),
    );
  }

  AutomationTask? _taskById(String taskId) {
    for (final task in _state.tasks) {
      if (task.id == taskId) {
        return task;
      }
    }
    return null;
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
