import 'package:flutter/material.dart';

import 'controllers/automation_controller.dart';
import 'widgets/automation_task_card.dart';
import 'widgets/capability_lane.dart';
import 'widgets/task_composer.dart';

class AutomationScreen extends StatefulWidget {
  const AutomationScreen({super.key});

  @override
  State<AutomationScreen> createState() => _AutomationScreenState();
}

class _AutomationScreenState extends State<AutomationScreen> {
  late final AutomationController _controller;
  final TextEditingController _taskController = TextEditingController();

  @override
  void initState() {
    super.initState();
    _controller = AutomationController();
  }

  @override
  void dispose() {
    _controller.dispose();
    _taskController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: _controller,
      builder: (context, _) {
        return ListView(
          padding: const EdgeInsets.all(16),
          children: [
            TaskComposer(
              controller: _taskController,
              onSubmit: _submit,
            ),
            const SizedBox(height: 16),
            CapabilityLane(capabilities: _controller.capabilities),
            const SizedBox(height: 16),
            Text('Queue', style: Theme.of(context).textTheme.titleLarge),
            const SizedBox(height: 8),
            for (final task in _controller.tasks) ...[
              AutomationTaskCard(
                task: task,
                onRun: () => _controller.run(task.id),
                onCancel: () => _controller.cancel(task.id),
              ),
              const SizedBox(height: 8),
            ],
          ],
        );
      },
    );
  }

  void _submit() {
    final text = _taskController.text.trim();
    if (text.isEmpty) {
      return;
    }
    _controller.enqueue(text);
    _taskController.clear();
  }
}
