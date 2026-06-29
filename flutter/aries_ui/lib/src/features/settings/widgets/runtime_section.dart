import 'package:flutter/material.dart';

import 'settings_section.dart';

class RuntimeSection extends StatelessWidget {
  const RuntimeSection({
    super.key,
    required this.streamResponses,
    required this.preferLocalModel,
    required this.onStreamResponsesChanged,
    required this.onPreferLocalModelChanged,
  });

  final bool streamResponses;
  final bool preferLocalModel;
  final ValueChanged<bool> onStreamResponsesChanged;
  final ValueChanged<bool> onPreferLocalModelChanged;

  @override
  Widget build(BuildContext context) {
    return SettingsSection(
      title: 'Runtime',
      children: [
        SwitchListTile(
          value: streamResponses,
          onChanged: onStreamResponsesChanged,
          title: const Text('Streaming'),
          secondary: const Icon(Icons.waterfall_chart_outlined),
        ),
        SwitchListTile(
          value: preferLocalModel,
          onChanged: onPreferLocalModelChanged,
          title: const Text('Prefer local model'),
          secondary: const Icon(Icons.memory_outlined),
        ),
      ],
    );
  }
}
