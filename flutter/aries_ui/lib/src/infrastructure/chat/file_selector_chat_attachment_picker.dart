import 'package:file_selector/file_selector.dart';
import 'package:flutter/services.dart';
import 'package:mime/mime.dart';

import '../../application/chat/chat_attachment_picker.dart';

class FileSelectorChatAttachmentPicker implements ChatAttachmentPicker {
  FileSelectorChatAttachmentPicker({
    this.maxFiles = 10,
    this.maxFileBytes = 20 * 1024 * 1024,
    Future<List<XFile>> Function()? selectFiles,
  }) : _selectFiles = selectFiles ?? openFiles;

  final int maxFiles;
  final int maxFileBytes;
  final Future<List<XFile>> Function() _selectFiles;

  @override
  Future<List<PickedChatAttachment>> pick() async {
    final files = await _openFiles();
    if (files.length > maxFiles) {
      throw ChatAttachmentPickerException(
        'Select up to $maxFiles files at a time.',
      );
    }

    final attachments = <PickedChatAttachment>[];
    for (final file in files) {
      final byteLength = await file.length();
      if (byteLength > maxFileBytes) {
        throw ChatAttachmentPickerException(
          '${file.name} exceeds the ${_formatBytes(maxFileBytes)} limit.',
        );
      }
      attachments.add(
        PickedChatAttachment(
          name: file.name,
          mimeType: file.mimeType ??
              lookupMimeType(file.name) ??
              'application/octet-stream',
          byteLength: byteLength,
          source: file.path,
        ),
      );
    }
    return attachments;
  }

  Future<List<XFile>> _openFiles() async {
    try {
      return await _selectFiles();
    } on PlatformException catch (error) {
      throw ChatAttachmentPickerException(
        error.message ?? 'The system file picker could not be opened.',
      );
    }
  }
}

String _formatBytes(int bytes) {
  const kibibyte = 1024;
  const mebibyte = kibibyte * 1024;
  if (bytes >= mebibyte) {
    return '${(bytes / mebibyte).toStringAsFixed(0)} MB';
  }
  return '${(bytes / kibibyte).ceil()} KB';
}
