import 'package:flutter/material.dart';

import '../models/chat_models.dart';
import 'chat_model_menu.dart';

class ChatToolbar extends StatelessWidget {
  const ChatToolbar({
    super.key,
    required this.activeSession,
    required this.selectedModelId,
    required this.availableModels,
    required this.onModelChanged,
  });

  final ChatSession activeSession;
  final String selectedModelId;
  final List<ChatModelProfile> availableModels;
  final ValueChanged<String> onModelChanged;

  @override
  Widget build(BuildContext context) {
    final textTheme = Theme.of(context).textTheme;
    return Padding(
      padding: const EdgeInsets.fromLTRB(8, 6, 12, 6),
      child: Row(
        children: [
          Builder(
            builder: (context) => IconButton(
              tooltip: 'History',
              icon: const Icon(Icons.history_outlined),
              onPressed: () => Scaffold.of(context).openDrawer(),
            ),
          ),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              mainAxisSize: MainAxisSize.min,
              children: [
                Text(activeSession.title, style: textTheme.titleMedium),
                Text(
                  '${activeSession.messages.length} messages',
                  style: textTheme.bodySmall,
                ),
              ],
            ),
          ),
          ChatModelMenu(
            selectedModelId: selectedModelId,
            models: availableModels,
            onSelected: onModelChanged,
          ),
        ],
      ),
    );
  }
}
