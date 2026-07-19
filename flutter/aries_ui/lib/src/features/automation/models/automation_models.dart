import 'dart:typed_data';

enum AutomationTaskStatus { queued, running, completed, failed, cancelled }

enum AutomationArtifactKind { image, text, binary }

class AutomationArtifact {
  const AutomationArtifact({
    required this.id,
    required this.kind,
    required this.mimeType,
    required this.summary,
    required this.byteLength,
    this.textPreview,
    this.bytes,
  });

  final String id;
  final AutomationArtifactKind kind;
  final String mimeType;
  final String summary;
  final int byteLength;
  final String? textPreview;
  final Uint8List? bytes;
}

class AutomationTask {
  const AutomationTask({
    required this.id,
    required this.title,
    required this.status,
    required this.steps,
    this.artifacts = const [],
  });

  final String id;
  final String title;
  final AutomationTaskStatus status;
  final List<String> steps;
  final List<AutomationArtifact> artifacts;

  AutomationTask copyWith({
    AutomationTaskStatus? status,
    List<String>? steps,
    List<AutomationArtifact>? artifacts,
  }) {
    return AutomationTask(
      id: id,
      title: title,
      status: status ?? this.status,
      steps: steps ?? this.steps,
      artifacts: artifacts ?? this.artifacts,
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
