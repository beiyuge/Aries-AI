import '../../application/automation/automation_repository.dart';
import '../../features/automation/models/automation_models.dart';
import '../persistence/json_state_store.dart';

class AutomationStateCodec implements JsonStateCodec<AutomationState> {
  AutomationStateCodec(List<AutomationCapabilitySummary> capabilities)
      : _capabilities = List.unmodifiable(capabilities);

  final List<AutomationCapabilitySummary> _capabilities;

  @override
  Map<String, Object?> encode(AutomationState value) {
    return {
      'tasks': value.tasks.map(_encodeTask).toList(),
      'nextId': value.nextId,
    };
  }

  @override
  AutomationState decode(Map<String, Object?> value) {
    final nextId = JsonValue.integer(value['nextId'], 'nextId');
    if (nextId < 0) {
      throw const FormatException('nextId must be non-negative');
    }
    return AutomationState(
      tasks: JsonValue.list(value['tasks'], 'tasks')
          .map((item) => _decodeTask(JsonValue.map(item, 'task')))
          .toList(),
      capabilities: _capabilities,
      nextId: nextId,
    );
  }

  Map<String, Object?> _encodeTask(AutomationTask task) {
    return {
      'id': task.id,
      'title': task.title,
      'status': task.status.name,
      'steps': task.steps,
    };
  }

  AutomationTask _decodeTask(Map<String, Object?> value) {
    final statusName = JsonValue.string(value['status'], 'task.status');
    final status = switch (statusName) {
      'queued' => AutomationTaskStatus.queued,
      'running' => AutomationTaskStatus.running,
      'completed' => AutomationTaskStatus.completed,
      'cancelled' => AutomationTaskStatus.cancelled,
      _ => throw FormatException('Unknown task status: $statusName'),
    };
    return AutomationTask(
      id: JsonValue.string(value['id'], 'task.id'),
      title: JsonValue.string(value['title'], 'task.title'),
      status: status,
      steps: JsonValue.list(value['steps'], 'task.steps')
          .map((step) => JsonValue.string(step, 'task.step'))
          .toList(),
    );
  }
}
