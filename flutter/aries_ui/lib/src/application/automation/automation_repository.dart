import '../../features/automation/models/automation_models.dart';

class AutomationState {
  AutomationState({
    required List<AutomationTask> tasks,
    required List<AutomationCapabilitySummary> capabilities,
    required this.nextId,
  })  : tasks = List.unmodifiable(tasks),
        capabilities = List.unmodifiable(capabilities);

  final List<AutomationTask> tasks;
  final List<AutomationCapabilitySummary> capabilities;
  final int nextId;

  AutomationState copyWith({
    List<AutomationTask>? tasks,
    List<AutomationCapabilitySummary>? capabilities,
    int? nextId,
  }) {
    return AutomationState(
      tasks: tasks ?? this.tasks,
      capabilities: capabilities ?? this.capabilities,
      nextId: nextId ?? this.nextId,
    );
  }
}

abstract interface class AutomationRepository {
  AutomationState load();

  Future<void> save(AutomationState state);
}

class InMemoryAutomationRepository implements AutomationRepository {
  AutomationState? _state;

  @override
  AutomationState load() => _state ??= AutomationState(
        tasks: const [
          AutomationTask(
            id: 'task-readiness',
            title: 'Check device readiness',
            status: AutomationTaskStatus.queued,
            steps: ['Diagnostics', 'Permissions', 'Backend health'],
          ),
        ],
        capabilities: const [
          AutomationCapabilitySummary(
            id: 'ui.tree',
            label: 'UI tree',
            available: true,
          ),
          AutomationCapabilitySummary(
            id: 'input.injection',
            label: 'Input',
            available: true,
          ),
          AutomationCapabilitySummary(
            id: 'screen.capture',
            label: 'Screen',
            available: true,
          ),
          AutomationCapabilitySummary(
            id: 'virtual.display',
            label: 'Virtual display',
            available: true,
          ),
        ],
        nextId: 0,
      );

  @override
  Future<void> save(AutomationState state) async {
    _state = state;
  }
}
