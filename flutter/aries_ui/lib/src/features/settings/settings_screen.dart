import 'package:flutter/material.dart';

import '../../application/settings/provider_credential_store.dart';
import '../../application/settings/settings_repository.dart';
import '../../application/chat/local_model_gateway.dart';
import '../../application/settings/local_model_file_picker.dart';
import 'controllers/settings_controller.dart';
import 'models/settings_models.dart';
import 'widgets/android_capability_section.dart';
import 'widgets/provider_profile_card.dart';
import 'widgets/provider_configuration_section.dart';
import 'widgets/provider_credential_section.dart';
import 'widgets/local_model_configuration_section.dart';
import 'widgets/runtime_section.dart';

class SettingsScreen extends StatefulWidget {
  const SettingsScreen({
    required this.repository,
    required this.credentials,
    required this.localModels,
    required this.localModelFilePicker,
    super.key,
  });

  final SettingsRepository repository;
  final ProviderCredentialStore credentials;
  final LocalModelGateway localModels;
  final LocalModelFilePicker localModelFilePicker;

  @override
  State<SettingsScreen> createState() => _SettingsScreenState();
}

class _SettingsScreenState extends State<SettingsScreen> {
  late SettingsController _controller;

  @override
  void initState() {
    super.initState();
    _controller = SettingsController.withCredentials(
      repository: widget.repository,
      credentials: widget.credentials,
    );
  }

  @override
  void didUpdateWidget(SettingsScreen oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (!identical(widget.repository, oldWidget.repository) ||
        !identical(widget.credentials, oldWidget.credentials)) {
      _controller.dispose();
      _controller = SettingsController.withCredentials(
        repository: widget.repository,
        credentials: widget.credentials,
      );
    }
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
            ProviderConfigurationSection(
              profile: _controller.selectedProfile,
              onSave: _controller.updateSelectedProviderConfiguration,
            ),
            const SizedBox(height: 16),
            ProviderCredentialSection(
              profile: _controller.selectedProfile,
              hasApiKey: _controller.hasApiKey,
              busy: _controller.credentialBusy,
              storageError: _controller.credentialError,
              onSave: _controller.saveSelectedApiKey,
            ),
            const SizedBox(height: 16),
            LocalModelConfigurationSection(
              profile: _controller.selectedProfile,
              configuredPath: _controller.localModelPath,
              gateway: widget.localModels,
              filePicker: widget.localModelFilePicker,
              onPathChanged: _controller.setLocalModelPath,
            ),
            if (_controller.selectedProfile.kind == ProviderKind.local)
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
