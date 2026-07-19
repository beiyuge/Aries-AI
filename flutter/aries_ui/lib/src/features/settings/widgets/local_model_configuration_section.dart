import 'package:flutter/material.dart';

import '../../../application/chat/local_model_gateway.dart';
import '../../../application/settings/local_model_file_picker.dart';
import '../models/settings_models.dart';
import 'settings_section.dart';

class LocalModelConfigurationSection extends StatefulWidget {
  const LocalModelConfigurationSection({
    required this.profile,
    required this.configuredPath,
    required this.gateway,
    required this.filePicker,
    required this.onPathChanged,
    super.key,
  });

  final ProviderProfile profile;
  final String configuredPath;
  final LocalModelGateway gateway;
  final LocalModelFilePicker filePicker;
  final Future<void> Function(String path) onPathChanged;

  @override
  State<LocalModelConfigurationSection> createState() =>
      _LocalModelConfigurationSectionState();
}

class _LocalModelConfigurationSectionState
    extends State<LocalModelConfigurationSection> {
  bool _busy = false;
  String? _status;
  String? _restoredKey;

  @override
  void initState() {
    super.initState();
    _scheduleRestore();
  }

  @override
  void didUpdateWidget(LocalModelConfigurationSection oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.profile.id != widget.profile.id ||
        oldWidget.configuredPath != widget.configuredPath ||
        !identical(oldWidget.gateway, widget.gateway)) {
      _scheduleRestore();
    }
  }

  @override
  Widget build(BuildContext context) {
    if (widget.profile.kind != ProviderKind.local) {
      return const SizedBox.shrink();
    }
    final configured = widget.configuredPath.isNotEmpty;
    return SettingsSection(
      title: 'Local model',
      children: [
        ListTile(
          leading: Icon(
            configured ? Icons.memory : Icons.memory_outlined,
          ),
          title: Text(configured
              ? _fileName(widget.configuredPath)
              : 'No model loaded'),
          subtitle:
              Text(_status ?? (configured ? 'Configured' : 'Not configured')),
        ),
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 0, 16, 12),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.end,
            children: [
              if (configured)
                TextButton.icon(
                  onPressed: _busy ? null : _unload,
                  icon: const Icon(Icons.eject_outlined),
                  label: const Text('Unload'),
                ),
              const SizedBox(width: 8),
              FilledButton.icon(
                onPressed: _busy ? null : _pickAndLoad,
                icon: const Icon(Icons.folder_open_outlined),
                label: const Text('Choose model'),
              ),
            ],
          ),
        ),
      ],
    );
  }

  void _scheduleRestore() {
    if (widget.profile.kind != ProviderKind.local ||
        widget.configuredPath.isEmpty) {
      return;
    }
    final restoreKey = '${widget.profile.model}:${widget.configuredPath}';
    if (_restoredKey == restoreKey) {
      return;
    }
    _restoredKey = restoreKey;
    WidgetsBinding.instance.addPostFrameCallback((_) => _restore(restoreKey));
  }

  Future<void> _restore(String restoreKey) async {
    if (!mounted || _restoredKey != restoreKey) {
      return;
    }
    await _run(
      () => widget.gateway.load(
        modelId: widget.profile.model,
        path: widget.configuredPath,
      ),
      successStatus: 'Loaded',
    );
  }

  Future<void> _pickAndLoad() async {
    try {
      final file = await widget.filePicker.pick();
      if (file == null || !mounted) {
        return;
      }
      final loaded = await _run(
        () => widget.gateway.load(
          modelId: widget.profile.model,
          path: file.path,
        ),
        successStatus: '${file.name} · ${_formatBytes(file.byteLength)}',
      );
      if (loaded && mounted) {
        _restoredKey = '${widget.profile.model}:${file.path}';
        await widget.onPathChanged(file.path);
      }
    } on LocalModelFilePickerException catch (error) {
      if (mounted) {
        setState(() => _status = error.message);
      }
    }
  }

  Future<void> _unload() async {
    final unloaded = await _run(
      () => widget.gateway.unload(widget.profile.model),
      successStatus: 'Unloaded',
    );
    if (unloaded && mounted) {
      _restoredKey = null;
      await widget.onPathChanged('');
    }
  }

  Future<bool> _run(
    Future<void> Function() operation, {
    required String successStatus,
  }) async {
    setState(() {
      _busy = true;
      _status = null;
    });
    try {
      await operation();
      if (mounted) {
        setState(() => _status = successStatus);
      }
      return true;
    } on LocalModelGatewayException catch (error) {
      if (mounted) {
        setState(() => _status = error.message);
      }
      return false;
    } finally {
      if (mounted) {
        setState(() => _busy = false);
      }
    }
  }

  String _fileName(String path) {
    return path.split(RegExp(r'[/\\]')).last;
  }

  String _formatBytes(int bytes) {
    const mebibyte = 1024 * 1024;
    if (bytes >= mebibyte) {
      return '${(bytes / mebibyte).toStringAsFixed(1)} MB';
    }
    return '${(bytes / 1024).ceil()} KB';
  }
}
