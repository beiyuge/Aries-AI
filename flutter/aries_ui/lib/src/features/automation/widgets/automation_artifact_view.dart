import 'package:flutter/material.dart';

import '../models/automation_models.dart';

class AutomationArtifactView extends StatelessWidget {
  const AutomationArtifactView({required this.artifact, super.key});

  final AutomationArtifact artifact;

  @override
  Widget build(BuildContext context) {
    final textTheme = Theme.of(context).textTheme;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            Icon(_iconFor(artifact.kind)),
            const SizedBox(width: 8),
            Expanded(
              child: Text(
                '${artifact.mimeType} · ${_formatBytes(artifact.byteLength)}',
                style: textTheme.labelLarge,
              ),
            ),
          ],
        ),
        if (artifact.kind == AutomationArtifactKind.image &&
            artifact.bytes != null) ...[
          const SizedBox(height: 8),
          ClipRRect(
            borderRadius: BorderRadius.circular(8),
            child: Image.memory(
              artifact.bytes!,
              fit: BoxFit.contain,
              width: double.infinity,
              height: 240,
              gaplessPlayback: true,
            ),
          ),
        ],
        if (artifact.textPreview case final preview?) ...[
          const SizedBox(height: 8),
          SelectionArea(
            child: Text(
              preview,
              style: textTheme.bodySmall,
              maxLines: 12,
              overflow: TextOverflow.ellipsis,
            ),
          ),
        ],
      ],
    );
  }

  IconData _iconFor(AutomationArtifactKind kind) => switch (kind) {
        AutomationArtifactKind.image => Icons.image_outlined,
        AutomationArtifactKind.text => Icons.data_object_outlined,
        AutomationArtifactKind.binary => Icons.attach_file_outlined,
      };

  String _formatBytes(int bytes) {
    if (bytes < 1024) {
      return '$bytes B';
    }
    if (bytes < 1024 * 1024) {
      return '${(bytes / 1024).toStringAsFixed(1)} KB';
    }
    return '${(bytes / (1024 * 1024)).toStringAsFixed(1)} MB';
  }
}
