import 'package:flutter/material.dart';

import '../models/settings_models.dart';
import 'settings_section.dart';

class ProviderCredentialSection extends StatefulWidget {
  const ProviderCredentialSection({
    required this.profile,
    required this.hasApiKey,
    required this.busy,
    required this.storageError,
    required this.onSave,
    super.key,
  });

  final ProviderProfile profile;
  final bool hasApiKey;
  final bool busy;
  final String? storageError;
  final Future<String?> Function(String apiKey) onSave;

  @override
  State<ProviderCredentialSection> createState() =>
      _ProviderCredentialSectionState();
}

class _ProviderCredentialSectionState extends State<ProviderCredentialSection> {
  final TextEditingController _controller = TextEditingController();
  bool _obscure = true;
  String? _error;

  @override
  void didUpdateWidget(ProviderCredentialSection oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.profile.id != widget.profile.id) {
      _controller.clear();
      _error = null;
    }
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    if (!widget.profile.requiresApiKey) {
      return const SizedBox.shrink();
    }
    return SettingsSection(
      title: 'Credential',
      children: [
        ListTile(
          leading: Icon(
            widget.hasApiKey ? Icons.key : Icons.key_off_outlined,
          ),
          title: Text(widget.hasApiKey ? 'API key saved' : 'API key required'),
          subtitle: widget.storageError == null
              ? const Text('Stored in platform secure storage')
              : Text(widget.storageError!),
        ),
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 0, 16, 8),
          child: TextField(
            controller: _controller,
            enabled: !widget.busy,
            obscureText: _obscure,
            autocorrect: false,
            enableSuggestions: false,
            decoration: InputDecoration(
              labelText: 'New API key',
              errorText: _error,
              suffixIcon: IconButton(
                tooltip: _obscure ? 'Show API key' : 'Hide API key',
                onPressed: () => setState(() => _obscure = !_obscure),
                icon: Icon(
                  _obscure ? Icons.visibility_outlined : Icons.visibility_off,
                ),
              ),
            ),
          ),
        ),
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 0, 16, 12),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.end,
            children: [
              if (widget.hasApiKey)
                TextButton.icon(
                  onPressed: widget.busy ? null : () => _save(''),
                  icon: const Icon(Icons.delete_outline),
                  label: const Text('Clear'),
                ),
              const SizedBox(width: 8),
              FilledButton.icon(
                onPressed: widget.busy ? null : () => _save(_controller.text),
                icon: const Icon(Icons.lock_outline),
                label: const Text('Save key'),
              ),
            ],
          ),
        ),
      ],
    );
  }

  Future<void> _save(String apiKey) async {
    final error = await widget.onSave(apiKey);
    if (!mounted) {
      return;
    }
    setState(() => _error = error);
    if (error == null) {
      _controller.clear();
    }
  }
}
