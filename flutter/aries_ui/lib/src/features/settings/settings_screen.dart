import 'package:flutter/material.dart';

import 'controllers/settings_controller.dart';
import 'widgets/android_capability_section.dart';
import 'widgets/provider_profile_card.dart';
import 'widgets/runtime_section.dart';

class SettingsScreen extends StatefulWidget {
  const SettingsScreen({super.key});

  @override
  State<SettingsScreen> createState() => _SettingsScreenState();
}

class _SettingsScreenState extends State<SettingsScreen> {
  late final SettingsController _controller;

  @override
  void initState() {
    super.initState();
    _controller = SettingsController();
  }

  @override
  void dispose() {
    _controller.dispose();
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
            ProviderProfileCard(
              profiles: _controller.profiles,
              selectedProfileId: _controller.selectedProfileId,
              onSelected: _controller.selectProfile,
            ),
            const SizedBox(height: 16),
            RuntimeSection(
              streamResponses: _controller.streamResponses,
              preferLocalModel: _controller.preferLocalModel,
              onStreamResponsesChanged: _controller.setStreamResponses,
              onPreferLocalModelChanged: _controller.setPreferLocalModel,
            ),
            const SizedBox(height: 16),
            const AndroidCapabilitySection(),
          ],
        );
      },
    );
  }
}
