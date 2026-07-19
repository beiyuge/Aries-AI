import 'package:flutter/material.dart';

import '../models/settings_models.dart';
import 'settings_section.dart';

class ProviderConfigurationSection extends StatefulWidget {
  const ProviderConfigurationSection({
    required this.profile,
    required this.onSave,
    super.key,
  });

  final ProviderProfile profile;
  final Future<String?> Function({
    required String baseUrl,
    required String model,
  }) onSave;

  @override
  State<ProviderConfigurationSection> createState() =>
      _ProviderConfigurationSectionState();
}

class _ProviderConfigurationSectionState
    extends State<ProviderConfigurationSection> {
  late final TextEditingController _baseUrlController;
  late final TextEditingController _modelController;
  String? _error;
  bool _saving = false;

  @override
  void initState() {
    super.initState();
    _baseUrlController = TextEditingController(text: widget.profile.baseUrl);
    _modelController = TextEditingController(text: widget.profile.model);
  }

  @override
  void didUpdateWidget(ProviderConfigurationSection oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.profile.id != widget.profile.id ||
        oldWidget.profile.baseUrl != widget.profile.baseUrl) {
      _baseUrlController.text = widget.profile.baseUrl;
    }
    if (oldWidget.profile.id != widget.profile.id ||
        oldWidget.profile.model != widget.profile.model) {
      _modelController.text = widget.profile.model;
    }
    if (oldWidget.profile.id != widget.profile.id) {
      _error = null;
    }
  }

  @override
  void dispose() {
    _baseUrlController.dispose();
    _modelController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    if (widget.profile.kind != ProviderKind.remote) {
      return const SizedBox.shrink();
    }
    return SettingsSection(
      title: 'Provider configuration',
      children: [
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 8, 16, 4),
          child: TextField(
            controller: _baseUrlController,
            enabled: widget.profile.editable && !_saving,
            keyboardType: TextInputType.url,
            autocorrect: false,
            decoration: const InputDecoration(labelText: 'Base URL'),
          ),
        ),
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 4, 16, 8),
          child: TextField(
            controller: _modelController,
            enabled: widget.profile.editable && !_saving,
            autocorrect: false,
            decoration: InputDecoration(
              labelText: 'Model',
              errorText: _error,
            ),
          ),
        ),
        if (widget.profile.editable)
          Align(
            alignment: Alignment.centerRight,
            child: Padding(
              padding: const EdgeInsets.fromLTRB(16, 0, 16, 12),
              child: FilledButton.icon(
                onPressed: _saving ? null : _save,
                icon: const Icon(Icons.save_outlined),
                label: const Text('Save configuration'),
              ),
            ),
          ),
      ],
    );
  }

  Future<void> _save() async {
    setState(() {
      _saving = true;
      _error = null;
    });
    final error = await widget.onSave(
      baseUrl: _baseUrlController.text,
      model: _modelController.text,
    );
    if (!mounted) {
      return;
    }
    setState(() {
      _saving = false;
      _error = error;
    });
  }
}
