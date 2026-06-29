import 'package:flutter/material.dart';

import '../models/settings_models.dart';

class ProviderProfileCard extends StatelessWidget {
  const ProviderProfileCard({
    required this.profiles,
    required this.selectedProfileId,
    required this.onSelected,
    super.key,
  });

  final List<ProviderProfile> profiles;
  final String selectedProfileId;
  final ValueChanged<String> onSelected;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('Provider', style: Theme.of(context).textTheme.titleLarge),
            const SizedBox(height: 8),
            SegmentedButton<String>(
              segments: [
                for (final profile in profiles)
                  ButtonSegment<String>(
                    value: profile.id,
                    label: Text(profile.name),
                    icon: const Icon(Icons.hub_outlined),
                  ),
              ],
              selected: {selectedProfileId},
              onSelectionChanged: (selection) => onSelected(selection.single),
            ),
            const SizedBox(height: 8),
            Text(profiles
                .firstWhere((profile) => profile.id == selectedProfileId)
                .endpointLabel),
          ],
        ),
      ),
    );
  }
}
