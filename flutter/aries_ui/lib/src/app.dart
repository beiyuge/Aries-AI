import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import 'application/application_repositories.dart';
import 'application/application_repository_scope.dart';
import 'application/chat/chat_attachment_picker.dart';
import 'application/chat/chat_runtime.dart';
import 'application/chat/local_model_gateway.dart';
import 'application/automation/automation_runtime.dart';
import 'application/settings/local_model_file_picker.dart';
import 'features/automation/automation_screen.dart';
import 'features/chat/chat_screen.dart';
import 'features/diagnostics/diagnostics_screen.dart';
import 'features/settings/settings_screen.dart';
import 'infrastructure/chat/file_selector_chat_attachment_picker.dart';
import 'infrastructure/chat/default_chat_runtime.dart';
import 'infrastructure/chat/local_model_chat_runtime.dart';
import 'infrastructure/chat/pigeon_local_model_gateway.dart';
import 'infrastructure/automation/default_automation_runtime.dart';
import 'infrastructure/automation/pigeon_automation_gateway.dart';
import 'infrastructure/settings/file_selector_local_model_file_picker.dart';

class AriesRe0App extends StatefulWidget {
  const AriesRe0App({
    this.repositories,
    this.attachmentPicker,
    this.chatRuntime,
    this.localModels,
    this.localModelFilePicker,
    this.automationRuntime,
    super.key,
  });

  final ApplicationRepositories? repositories;
  final ChatAttachmentPicker? attachmentPicker;
  final ChatRuntime? chatRuntime;
  final LocalModelGateway? localModels;
  final LocalModelFilePicker? localModelFilePicker;
  final AutomationRuntime? automationRuntime;

  @override
  State<AriesRe0App> createState() => _AriesRe0AppState();
}

class _AriesRe0AppState extends State<AriesRe0App> {
  late ApplicationRepositories _repositories;
  late ChatAttachmentPicker _attachmentPicker;
  late ChatRuntime _chatRuntime;
  late LocalModelGateway _localModels;
  late LocalModelFilePicker _localModelFilePicker;
  late AutomationRuntime _automationRuntime;
  late final GoRouter _router;

  @override
  void initState() {
    super.initState();
    _repositories = widget.repositories ?? ApplicationRepositories.inMemory();
    _attachmentPicker =
        widget.attachmentPicker ?? FileSelectorChatAttachmentPicker();
    _localModels = widget.localModels ?? PigeonLocalModelGateway();
    _localModelFilePicker =
        widget.localModelFilePicker ?? FileSelectorLocalModelFilePicker();
    _chatRuntime = widget.chatRuntime ?? _createDefaultChatRuntime();
    _automationRuntime = widget.automationRuntime ??
        DefaultAutomationRuntime(gateway: PigeonAutomationGateway());
    _router = _createRouter(
      attachmentPicker: () => _attachmentPicker,
      chatRuntime: () => _chatRuntime,
      localModels: () => _localModels,
      localModelFilePicker: () => _localModelFilePicker,
      automationRuntime: () => _automationRuntime,
    );
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
    if (!identical(widget.localModels, oldWidget.localModels)) {
      _localModels = widget.localModels ?? PigeonLocalModelGateway();
    }
    if (!identical(
      widget.localModelFilePicker,
      oldWidget.localModelFilePicker,
    )) {
      _localModelFilePicker =
          widget.localModelFilePicker ?? FileSelectorLocalModelFilePicker();
    }
    if (!identical(widget.chatRuntime, oldWidget.chatRuntime) ||
        (!identical(widget.repositories, oldWidget.repositories) &&
            widget.chatRuntime == null) ||
        (!identical(widget.localModels, oldWidget.localModels) &&
            widget.chatRuntime == null)) {
      _chatRuntime = widget.chatRuntime ?? _createDefaultChatRuntime();
    }
    if (!identical(widget.automationRuntime, oldWidget.automationRuntime)) {
      _automationRuntime = widget.automationRuntime ??
          DefaultAutomationRuntime(gateway: PigeonAutomationGateway());
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

  ChatRuntime _createDefaultChatRuntime() {
    return DefaultChatRuntime(
      settings: _repositories.settings,
      credentials: _repositories.providerCredentials,
      localRuntime: LocalModelChatRuntime(
        gateway: _localModels,
        settings: _repositories.settings,
      ),
    );
  }
}

GoRouter _createRouter({
  required ChatAttachmentPicker Function() attachmentPicker,
  required ChatRuntime Function() chatRuntime,
  required LocalModelGateway Function() localModels,
  required LocalModelFilePicker Function() localModelFilePicker,
  required AutomationRuntime Function() automationRuntime,
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
                runtime: chatRuntime(),
              ),
            ),
            GoRoute(
              path: '/automation',
              builder: (context, state) => AutomationScreen(
                repository: ApplicationRepositoryScope.of(context).automation,
                runtime: automationRuntime(),
              ),
            ),
            GoRoute(
                path: '/diagnostics',
                builder: (context, state) => const DiagnosticsScreen()),
            GoRoute(
              path: '/settings',
              builder: (context, state) => SettingsScreen(
                repository: ApplicationRepositoryScope.of(context).settings,
                credentials:
                    ApplicationRepositoryScope.of(context).providerCredentials,
                localModels: localModels(),
                localModelFilePicker: localModelFilePicker(),
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
