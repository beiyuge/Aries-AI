import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import 'features/automation/automation_screen.dart';
import 'features/chat/chat_screen.dart';
import 'features/diagnostics/diagnostics_screen.dart';
import 'features/settings/settings_screen.dart';

class AriesRe0App extends StatelessWidget {
  const AriesRe0App({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp.router(
      title: 'Aries AI re0',
      theme: ThemeData(
        useMaterial3: true,
        colorSchemeSeed: const Color(0xFF006A60),
        cardTheme: CardThemeData(
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
        ),
      ),
      routerConfig: _router,
    );
  }
}

final _router = GoRouter(
  initialLocation: '/',
  routes: [
    ShellRoute(
      builder: (context, state, child) => AriesScaffold(child: child),
      routes: [
        GoRoute(path: '/', builder: (context, state) => const ChatScreen()),
        GoRoute(
            path: '/automation',
            builder: (context, state) => const AutomationScreen()),
        GoRoute(
            path: '/diagnostics',
            builder: (context, state) => const DiagnosticsScreen()),
        GoRoute(
            path: '/settings',
            builder: (context, state) => const SettingsScreen()),
      ],
    ),
  ],
);

class AriesScaffold extends StatelessWidget {
  const AriesScaffold({required this.child, super.key});

  final Widget child;

  @override
  Widget build(BuildContext context) {
    final location = GoRouterState.of(context).uri.path;
    return Scaffold(
      appBar: AppBar(title: const Text('Aries AI re0')),
      body: child,
      bottomNavigationBar: NavigationBar(
        selectedIndex: switch (location) {
          '/automation' => 1,
          '/diagnostics' => 2,
          '/settings' => 3,
          _ => 0,
        },
        onDestinationSelected: (index) {
          final route = switch (index) {
            1 => '/automation',
            2 => '/diagnostics',
            3 => '/settings',
            _ => '/',
          };
          context.go(route);
        },
        destinations: const [
          NavigationDestination(
              icon: Icon(Icons.chat_bubble_outline), label: 'Chat'),
          NavigationDestination(
              icon: Icon(Icons.smart_toy_outlined), label: 'Automation'),
          NavigationDestination(
              icon: Icon(Icons.monitor_heart_outlined), label: 'Diagnostics'),
          NavigationDestination(
              icon: Icon(Icons.settings_outlined), label: 'Settings'),
        ],
      ),
    );
  }
}
