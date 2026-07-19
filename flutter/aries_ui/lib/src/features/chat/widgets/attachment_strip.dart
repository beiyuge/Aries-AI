import 'package:flutter/material.dart';

import '../models/chat_models.dart';

class AttachmentStrip extends StatelessWidget {
  const AttachmentStrip({
    required this.attachments,
    required this.onRemove,
    super.key,
  });

  final List<ChatAttachment> attachments;
  final ValueChanged<String> onRemove;

  @override
  Widget build(BuildContext context) {
    if (attachments.isEmpty) {
      return const SizedBox.shrink();
    }
    return SizedBox(
      height: 56,
      child: ListView.separated(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
        scrollDirection: Axis.horizontal,
        itemBuilder: (context, index) {
          final attachment = attachments[index];
          return InputChip(
            avatar: const Icon(Icons.description_outlined, size: 18),
            label: Text(
              '${attachment.name} · ${formatAttachmentSize(attachment.byteLength)}',
            ),
            onDeleted: () => onRemove(attachment.id),
          );
        },
        separatorBuilder: (_, __) => const SizedBox(width: 8),
        itemCount: attachments.length,
      ),
    );
  }
}

String formatAttachmentSize(int bytes) {
  const kibibyte = 1024;
  const mebibyte = kibibyte * 1024;
  if (bytes <= 0) {
    return 'Size unavailable';
  }
  if (bytes >= mebibyte) {
    final precision = bytes >= 10 * mebibyte ? 0 : 1;
    return '${(bytes / mebibyte).toStringAsFixed(precision)} MB';
  }
  return '${(bytes / kibibyte).ceil()} KB';
}
