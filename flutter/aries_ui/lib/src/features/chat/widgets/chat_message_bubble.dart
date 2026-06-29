import 'package:flutter/material.dart';

import '../models/chat_models.dart';
import 'markdown_text.dart';

class ChatMessageBubble extends StatelessWidget {
  const ChatMessageBubble({
    required this.message,
    super.key,
  });

  final ChatMessage message;

  @override
  Widget build(BuildContext context) {
    final colorScheme = Theme.of(context).colorScheme;
    final isUser = message.role == ChatMessageRole.user;
    final alignment = isUser ? Alignment.centerRight : Alignment.centerLeft;
    final background = switch (message.role) {
      ChatMessageRole.user => colorScheme.primaryContainer,
      ChatMessageRole.assistant => colorScheme.surfaceContainerHighest,
      ChatMessageRole.system => colorScheme.tertiaryContainer,
    };
    final foreground = switch (message.role) {
      ChatMessageRole.user => colorScheme.onPrimaryContainer,
      ChatMessageRole.assistant => colorScheme.onSurface,
      ChatMessageRole.system => colorScheme.onTertiaryContainer,
    };

    return Align(
      alignment: alignment,
      child: ConstrainedBox(
        constraints: const BoxConstraints(maxWidth: 640),
        child: DecoratedBox(
          decoration: BoxDecoration(
            color: background,
            borderRadius: BorderRadius.circular(8),
          ),
          child: Padding(
            padding: const EdgeInsets.all(12),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              mainAxisSize: MainAxisSize.min,
              children: [
                MarkdownText(message.markdown, color: foreground),
                if (message.attachments.isNotEmpty) ...[
                  const SizedBox(height: 8),
                  Wrap(
                    spacing: 6,
                    runSpacing: 6,
                    children: [
                      for (final attachment in message.attachments)
                        Chip(
                          avatar: const Icon(Icons.attach_file, size: 16),
                          label: Text(attachment.name),
                        ),
                    ],
                  ),
                ],
              ],
            ),
          ),
        ),
      ),
    );
  }
}
