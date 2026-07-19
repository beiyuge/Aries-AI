import 'package:flutter/material.dart';

import '../models/automation_models.dart';

class AutomationTaskCard extends StatelessWidget {
  const AutomationTaskCard({
    required this.task,
    required this.onRun,
    required this.onCancel,
    super.key,
  });

  final AutomationTask task;
  final VoidCallback onRun;
  final VoidCallback onCancel;

  @override
  Widget build(BuildContext context) {
    final colorScheme = Theme.of(context).colorScheme;
    final canCancel = task.status == AutomationTaskStatus.queued ||
        task.status == AutomationTaskStatus.running;
    final canRun = task.status != AutomationTaskStatus.running;
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Icon(
                  _iconFor(task.status),
                  color: task.status == AutomationTaskStatus.failed
                      ? colorScheme.error
                      : colorScheme.primary,
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: Text(task.title,
                      style: Theme.of(context).textTheme.titleMedium),
                ),
                Text(_labelFor(task.status)),
              ],
            ),
            const SizedBox(height: 8),
            Wrap(
              spacing: 6,
              runSpacing: 6,
              children: [
                for (final step in task.steps) Chip(label: Text(step)),
              ],
            ),
            const SizedBox(height: 8),
            Row(
              mainAxisAlignment: MainAxisAlignment.end,
              children: [
                TextButton.icon(
                  onPressed: canCancel ? onCancel : null,
                  icon: const Icon(Icons.cancel_outlined),
                  label: const Text('Cancel'),
                ),
                const SizedBox(width: 8),
                FilledButton.icon(
                  onPressed: canRun ? onRun : null,
                  icon: const Icon(Icons.play_arrow_outlined),
                  label: const Text('Run'),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  IconData _iconFor(AutomationTaskStatus status) {
    return switch (status) {
      AutomationTaskStatus.queued => Icons.schedule_outlined,
      AutomationTaskStatus.running => Icons.sync_outlined,
      AutomationTaskStatus.completed => Icons.check_circle_outline,
      AutomationTaskStatus.failed => Icons.error_outline,
      AutomationTaskStatus.cancelled => Icons.do_not_disturb_on_outlined,
    };
  }

  String _labelFor(AutomationTaskStatus status) {
    return switch (status) {
      AutomationTaskStatus.queued => 'Queued',
      AutomationTaskStatus.running => 'Running',
      AutomationTaskStatus.completed => 'Completed',
      AutomationTaskStatus.failed => 'Failed',
      AutomationTaskStatus.cancelled => 'Cancelled',
    };
  }
}
