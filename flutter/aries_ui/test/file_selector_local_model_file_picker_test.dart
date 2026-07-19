import 'dart:typed_data';

import 'package:aries_ui/src/infrastructure/settings/file_selector_local_model_file_picker.dart';
import 'package:file_selector/file_selector.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  test('maps a selected model to a platform-neutral file location', () async {
    final picker = FileSelectorLocalModelFilePicker(
      selectFile: () async => XFile.fromData(
        Uint8List.fromList([1, 2, 3]),
        path: '/tmp/model.gguf',
      ),
    );

    final model = await picker.pick();

    expect(model?.name, 'model.gguf');
    expect(model?.path, '/tmp/model.gguf');
    expect(model?.byteLength, 3);
  });

  test('a cancelled model selection returns null', () async {
    final picker = FileSelectorLocalModelFilePicker(
      selectFile: () async => null,
    );

    expect(await picker.pick(), isNull);
  });
}
