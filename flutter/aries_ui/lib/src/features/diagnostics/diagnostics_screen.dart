import 'package:flutter/material.dart';

import '../../generated/capabilities.g.dart';

class DiagnosticsScreen extends StatefulWidget {
  const DiagnosticsScreen({super.key});

  @override
  State<DiagnosticsScreen> createState() => _DiagnosticsScreenState();
}

class _DiagnosticsScreenState extends State<DiagnosticsScreen> {
  final CapabilityHostApi _hostApi = CapabilityHostApi();
  late Future<List<CapabilityHealthDto>> _healthFuture = _loadHealth();

  Future<List<CapabilityHealthDto>> _loadHealth() async {
    final capabilityIds = await _hostApi.listCapabilities();
    final health = <CapabilityHealthDto>[];
    for (final id in capabilityIds) {
      health.add(await _hostApi.getCapabilityHealth(id));
    }
    return health;
  }

  void _refresh() {
    setState(() {
      _healthFuture = _loadHealth();
    });
  }

  @override
  Widget build(BuildContext context) {
    return FutureBuilder<List<CapabilityHealthDto>>(
      future: _healthFuture,
      builder: (context, snapshot) {
        if (snapshot.connectionState != ConnectionState.done) {
          return const Center(child: CircularProgressIndicator());
        }
        if (snapshot.hasError) {
          return _DiagnosticsError(
            error: snapshot.error.toString(),
            onRetry: _refresh,
          );
        }

        final health = snapshot.data ?? const <CapabilityHealthDto>[];
        return RefreshIndicator(
          onRefresh: () async => _refresh(),
          child: ListView.separated(
            padding: const EdgeInsets.all(16),
            itemCount: health.length,
            separatorBuilder: (_, __) => const SizedBox(height: 8),
            itemBuilder: (context, index) {
              final item = health[index];
              return _CapabilityTile(
                health: item,
                onSelfTest: () => _runSelfTest(context, item.id),
                onOpenSettings: () => _openSettings(context, item.id),
              );
            },
          ),
        );
      },
    );
  }

  Future<void> _runSelfTest(BuildContext context, String id) async {
    final messenger = ScaffoldMessenger.of(context);
    final result = await _hostApi.runCapabilitySelfTest(id);
    if (!context.mounted) return;
    messenger.showSnackBar(SnackBar(content: Text(result)));
  }

  Future<void> _openSettings(BuildContext context, String id) async {
    await _hostApi.openCapabilitySettings(id);
  }
}

class _CapabilityTile extends StatelessWidget {
  const _CapabilityTile({
    required this.health,
    required this.onSelfTest,
    required this.onOpenSettings,
  });

  final CapabilityHealthDto health;
  final VoidCallback onSelfTest;
  final VoidCallback onOpenSettings;

  @override
  Widget build(BuildContext context) {
    final colorScheme = Theme.of(context).colorScheme;
    final statusColor = health.available
        ? colorScheme.primary
        : health.supported
            ? colorScheme.error
            : colorScheme.outline;
    final missingText = health.missingRequirements.isEmpty
        ? health.lastErrorMessage ?? 'No missing requirements reported'
        : health.missingRequirements.join('\n');
    final diagnosticsText = health.diagnostics.isEmpty
        ? ''
        : '\n${health.diagnostics.join('\n')}';

    return Card(
      child: ListTile(
        leading: Icon(
          health.available
              ? Icons.check_circle_outline
              : health.supported
                  ? Icons.error_outline
                  : Icons.do_not_disturb_on_outlined,
          color: statusColor,
        ),
        title: Text(health.id),
        subtitle: Text('${health.state}\n$missingText$diagnosticsText'),
        isThreeLine: true,
        trailing: Wrap(
          spacing: 4,
          children: [
            IconButton(
              tooltip: 'Run self test',
              icon: const Icon(Icons.play_arrow_outlined),
              onPressed: onSelfTest,
            ),
            IconButton(
              tooltip: 'Open settings',
              icon: const Icon(Icons.settings_outlined),
              onPressed: onOpenSettings,
            ),
          ],
        ),
      ),
    );
  }
}

class _DiagnosticsError extends StatelessWidget {
  const _DiagnosticsError({
    required this.error,
    required this.onRetry,
  });

  final String error;
  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(
              'Native bridge unavailable',
              style: Theme.of(context).textTheme.titleLarge,
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 12),
            Text(error, textAlign: TextAlign.center),
            const SizedBox(height: 16),
            FilledButton.icon(
              onPressed: onRetry,
              icon: const Icon(Icons.refresh),
              label: const Text('Retry'),
            ),
          ],
        ),
      ),
    );
  }
}
