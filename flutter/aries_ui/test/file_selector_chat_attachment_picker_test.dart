import 'dart:typed_data';

import 'package:aries_ui/src/application/chat/chat_attachment_picker.dart';
import 'package:aries_ui/src/infrastructure/chat/file_selector_chat_attachment_picker.dart';
import 'package:file_selector/file_selector.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  test('maps selected files to platform-neutral attachment metadata', () async {
    final picker = FileSelectorChatAttachmentPicker(
      selectFiles: () async => [
        XFile.fromData(
          Uint8List.fromList([1, 2, 3]),
          path: '/tmp/context.json',
        ),
      ],
    );

    final attachment = (await picker.pick()).single;

    expect(attachment.name, 'context.json');
    expect(attachment.mimeType, 'application/json');
    expect(attachment.byteLength, 3);
    expect(attachment.source, '/tmp/context.json');
  });

  test('empty platform selection is a cancellation, not an error', () async {
    final picker =
        FileSelectorChatAttachmentPicker(selectFiles: () async => []);

    expect(await picker.pick(), isEmpty);
  });

  test('rejects selections over the file count limit', () async {
    final picker = FileSelectorChatAttachmentPicker(
      maxFiles: 1,
      selectFiles: () async => [
        XFile.fromData(Uint8List(1), path: '/tmp/one.txt'),
        XFile.fromData(Uint8List(1), path: '/tmp/two.txt'),
      ],
    );

    expect(
      picker.pick,
      throwsA(isA<ChatAttachmentPickerException>()),
    );
  });

  test('rejects files over the byte limit', () async {
    final picker = FileSelectorChatAttachmentPicker(
      maxFileBytes: 2,
      selectFiles: () async => [
        XFile.fromData(Uint8List(3), path: '/tmp/large.txt'),
      ],
    );

    expect(
      picker.pick,
      throwsA(
        isA<ChatAttachmentPickerException>().having(
          (error) => error.message,
          'message',
          contains('large.txt'),
        ),
      ),
    );
  });
}
