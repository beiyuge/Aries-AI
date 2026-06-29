import 'package:flutter/material.dart';

import '../models/automation_models.dart';

class CapabilityLane extends StatelessWidget {
  const CapabilityLane({
    required this.capabilities,
    super.key,
  });

  final List<AutomationCapabilitySummary> capabilities;

  @override
  Widget build(BuildContext context) {
    return Wrap(
      spacing: 8,
      runSpacing: 8,
      children: [
        for (final capability in capabilities)
          FilterChip(
            selected: capability.available,
            avatar: Icon(capability.available
                ? Icons.check_circle_outline
                : Icons.error_outline),
            label: Text(capability.label),
            onSelected: (_) {},
          ),
      ],
    );
  }
}
