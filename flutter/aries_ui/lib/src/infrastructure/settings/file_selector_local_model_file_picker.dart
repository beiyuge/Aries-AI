import 'package:file_selector/file_selector.dart';
import 'package:flutter/services.dart';

import '../../application/settings/local_model_file_picker.dart';

class FileSelectorLocalModelFilePicker implements LocalModelFilePicker {
  FileSelectorLocalModelFilePicker({Future<XFile?> Function()? selectFile})
      : _selectFile = selectFile ?? openFile;

  final Future<XFile?> Function() _selectFile;

  @override
  Future<PickedLocalModelFile?> pick() async {
    try {
      final file = await _selectFile();
      if (file == null) {
        return null;
      }
      return PickedLocalModelFile(
        name: file.name,
        path: file.path,
        byteLength: await file.length(),
      );
    } on PlatformException catch (error) {
      throw LocalModelFilePickerException(
        error.message ?? 'The system file picker could not be opened.',
      );
    }
  }
}
