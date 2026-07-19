import 'package:aries_ui/src/app.dart';
import 'package:aries_ui/src/application/application_repositories.dart';
import 'package:aries_ui/src/application/chat/local_model_gateway.dart';
import 'package:aries_ui/src/application/settings/local_model_file_picker.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'support/fake_chat_attachment_picker.dart';
import 'support/fake_automation_runtime.dart';

void main() {
  testWidgets('re0 shell opens diagnostics tab', (tester) async {
    await tester.pumpWidget(const AriesRe0App());

    await tester.tap(find.text('Diagnostics'));
    await tester.pump();

    expect(find.byType(AriesRe0App), findsOneWidget);
  });

  testWidgets('automation queues and completes a task', (tester) async {
    await tester.pumpWidget(
      AriesRe0App(
        automationRuntime: FakeAutomationRuntime.success('Captured 1080x2400'),
      ),
    );

    await tester.tap(find.text('Automation'));
    await tester.pumpAndSettle();

    await tester.enterText(find.byType(TextField), 'capture screen');
    await tester.tap(find.text('Queue').last);
    await tester.pump();

    expect(find.text('capture screen'), findsOneWidget);
    await tester.tap(find.text('Run').first);
    await tester.pump();

    expect(find.text('Completed'), findsWidgets);
  });

  testWidgets('settings switches provider profile', (tester) async {
    await tester.pumpWidget(const AriesRe0App());

    await tester.tap(find.text('Settings'));
    await tester.pumpAndSettle();

    expect(find.text('Provider'), findsOneWidget);
    await tester.tap(find.text('Local'));
    await tester.pump();

    expect(find.text('native runtime'), findsOneWidget);

    await tester.tap(find.text('Chat').last);
    await tester.pumpAndSettle();
    await tester.tap(find.text('Settings'));
    await tester.pumpAndSettle();

    expect(find.text('native runtime'), findsOneWidget);
  });

  testWidgets('chat attachment button uses the platform-neutral picker',
      (tester) async {
    await tester.pumpWidget(
      AriesRe0App(
        attachmentPicker: FakeChatAttachmentPicker.single(
          name: 'notes.txt',
          mimeType: 'text/plain',
          byteLength: 1536,
          source: '/tmp/notes.txt',
        ),
      ),
    );

    await tester.tap(find.byTooltip('Attach'));
    await tester.pump();

    expect(find.textContaining('notes.txt'), findsOneWidget);
    expect(find.textContaining('2 KB'), findsOneWidget);
  });

  testWidgets('settings loads and restores a selected local model',
      (tester) async {
    final repositories = ApplicationRepositories.inMemory();
    final gateway = _FakeLocalModelGateway();
    final picker = _FakeLocalModelFilePicker();
    await tester.pumpWidget(
      AriesRe0App(
        repositories: repositories,
        localModels: gateway,
        localModelFilePicker: picker,
      ),
    );

    await tester.tap(find.text('Settings'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Local'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Choose model'));
    await tester.pumpAndSettle();

    expect(find.text('model.gguf'), findsOneWidget);
    expect(gateway.loads.single, ('local.default', '/models/model.gguf'));

    await tester.pumpWidget(const SizedBox.shrink());
    await tester.pumpWidget(
      AriesRe0App(
        repositories: repositories,
        localModels: gateway,
        localModelFilePicker: picker,
      ),
    );
    await tester.tap(find.text('Settings'));
    await tester.pumpAndSettle();

    expect(find.text('model.gguf'), findsOneWidget);
    expect(gateway.loads, hasLength(2));
  });
}

class _FakeLocalModelGateway implements LocalModelGateway {
  final List<(String, String)> loads = [];

  @override
  Future<String> generate({
    required String modelId,
    required String prompt,
  }) async =>
      'local';

  @override
  Future<void> load({required String modelId, required String path}) async {
    loads.add((modelId, path));
  }

  @override
  Future<void> unload(String modelId) async {}
}

class _FakeLocalModelFilePicker implements LocalModelFilePicker {
  @override
  Future<PickedLocalModelFile?> pick() async {
    return const PickedLocalModelFile(
      name: 'model.gguf',
      path: '/models/model.gguf',
      byteLength: 4096,
    );
  }
}
