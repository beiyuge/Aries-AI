import 'package:aries_ui/src/app.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'support/fake_chat_attachment_picker.dart';

void main() {
  testWidgets('re0 shell shows app title and chat tab', (tester) async {
    await tester.pumpWidget(const AriesRe0App());

    expect(find.text('Aries AI re0'), findsOneWidget);
    expect(find.text('Chat'), findsWidgets);
  });

  testWidgets('chat sends messages with attachments', (tester) async {
    await tester.pumpWidget(
      AriesRe0App(attachmentPicker: FakeChatAttachmentPicker.single()),
    );

    await tester.tap(find.byTooltip('Attach'));
    await tester.pump();
    expect(find.textContaining('screen-context.json'), findsOneWidget);

    await tester.enterText(
        find.byType(TextField), 'capture the current screen');
    await tester.tap(find.text('Send'));
    await tester.pump();

    expect(find.text('capture the current screen'), findsOneWidget);
    expect(find.text('Draft'), findsOneWidget);
    expect(find.text('screen-context.json'), findsWidgets);
  });
}
