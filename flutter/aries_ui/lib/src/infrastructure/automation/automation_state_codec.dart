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
      'artifacts': task.artifacts.map(_encodeArtifact).toList(),
    };
  }

  AutomationTask _decodeTask(Map<String, Object?> value) {
    final statusName = JsonValue.string(value['status'], 'task.status');
    final status = switch (statusName) {
      'queued' => AutomationTaskStatus.queued,
      'running' => AutomationTaskStatus.running,
      'completed' => AutomationTaskStatus.completed,
      'failed' => AutomationTaskStatus.failed,
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
      artifacts: value['artifacts'] == null
          ? const []
          : JsonValue.list(value['artifacts'], 'task.artifacts')
              .map((item) => _decodeArtifact(JsonValue.map(item, 'artifact')))
              .toList(),
    );
  }

  Map<String, Object?> _encodeArtifact(AutomationArtifact artifact) {
    return {
      'id': artifact.id,
      'kind': artifact.kind.name,
      'mimeType': artifact.mimeType,
      'summary': artifact.summary,
      'byteLength': artifact.byteLength,
      'textPreview': artifact.textPreview,
    };
  }

  AutomationArtifact _decodeArtifact(Map<String, Object?> value) {
    final kindName = JsonValue.string(value['kind'], 'artifact.kind');
    final kind = switch (kindName) {
      'image' => AutomationArtifactKind.image,
      'text' => AutomationArtifactKind.text,
      'binary' => AutomationArtifactKind.binary,
      _ => throw FormatException('Unknown artifact kind: $kindName'),
    };
    final byteLength = JsonValue.integer(
      value['byteLength'],
      'artifact.byteLength',
    );
    if (byteLength < 0) {
      throw const FormatException('artifact.byteLength must be non-negative');
    }
    return AutomationArtifact(
      id: JsonValue.string(value['id'], 'artifact.id'),
      kind: kind,
      mimeType: JsonValue.string(value['mimeType'], 'artifact.mimeType'),
      summary: JsonValue.string(value['summary'], 'artifact.summary'),
      byteLength: byteLength,
      textPreview: value['textPreview'] == null
          ? null
          : JsonValue.string(value['textPreview'], 'artifact.textPreview'),
    );
  }
}
