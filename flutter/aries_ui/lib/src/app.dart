import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import 'application/application_repositories.dart';
import 'application/application_repository_scope.dart';
import 'application/chat/chat_attachment_picker.dart';
import 'features/automation/automation_screen.dart';
import 'features/chat/chat_screen.dart';
import 'features/diagnostics/diagnostics_screen.dart';
import 'features/settings/settings_screen.dart';
import 'infrastructure/chat/file_selector_chat_attachment_picker.dart';

class AriesRe0App extends StatefulWidget {
  const AriesRe0App({this.repositories, this.attachmentPicker, super.key});

  final ApplicationRepositories? repositories;
  final ChatAttachmentPicker? attachmentPicker;

  @override
  State<AriesRe0App> createState() => _AriesRe0AppState();
}

class _AriesRe0AppState extends State<AriesRe0App> {
  late ApplicationRepositories _repositories;
  late ChatAttachmentPicker _attachmentPicker;
  late final GoRouter _router;

  @override
  void initState() {
    super.initState();
    _repositories = widget.repositories ?? ApplicationRepositories.inMemory();
    _attachmentPicker =
        widget.attachmentPicker ?? FileSelectorChatAttachmentPicker();
    _router = _createRouter(attachmentPicker: () => _attachmentPicker);
  }

  @override
  void didUpdateWidget(AriesRe0App oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (!identical(widget.repositories, oldWidget.repositories)) {
      _repositories = widget.repositories ?? ApplicationRepositories.inMemory();
    }
    if (!identical(widget.attachmentPicker, oldWidget.attachmentPicker)) {
      _attachmentPicker =
          widget.attachmentPicker ?? FileSelectorChatAttachmentPicker();
    }
  }

  @override
  void dispose() {
    _router.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return ApplicationRepositoryScope(
      repositories: _repositories,
      child: MaterialApp.router(
        title: 'Aries AI re0',
        theme: ThemeData(
          useMaterial3: true,
          colorSchemeSeed: const Color(0xFF006A60),
          cardTheme: CardThemeData(
            shape:
                RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
          ),
        ),
        routerConfig: _router,
      ),
    );
  }
}

GoRouter _createRouter({
  required ChatAttachmentPicker Function() attachmentPicker,
}) =>
    GoRouter(
      initialLocation: '/',
      routes: [
        ShellRoute(
          builder: (context, state, child) => AriesScaffold(child: child),
          routes: [
            GoRoute(
              path: '/',
              builder: (context, state) => ChatScreen(
                repository: ApplicationRepositoryScope.of(context).chat,
                attachmentPicker: attachmentPicker(),
              ),
            ),
            GoRoute(
              path: '/automation',
              builder: (context, state) => AutomationScreen(
                repository: ApplicationRepositoryScope.of(context).automation,
              ),
            ),
            GoRoute(
                path: '/diagnostics',
                builder: (context, state) => const DiagnosticsScreen()),
            GoRoute(
              path: '/settings',
              builder: (context, state) => SettingsScreen(
                repository: ApplicationRepositoryScope.of(context).settings,
              ),
            ),
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
