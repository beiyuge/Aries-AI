import 'package:flutter/material.dart';

import 'settings_section.dart';

class AndroidCapabilitySection extends StatelessWidget {
  const AndroidCapabilitySection({super.key});

  @override
  Widget build(BuildContext context) {
    return const SettingsSection(
      title: 'Android',
      children: [
        ListTile(
          leading: Icon(Icons.security_outlined),
          title: Text('Permissions'),
          subtitle: Text('Diagnostics'),
        ),
        ListTile(
          leading: Icon(Icons.integration_instructions_outlined),
          title: Text('Capability bridge'),
          subtitle: Text('Pigeon host API'),
        ),
      ],
    );
  }
}
