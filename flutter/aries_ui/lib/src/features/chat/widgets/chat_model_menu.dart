import 'package:flutter/material.dart';

import '../models/chat_models.dart';

class ChatModelMenu extends StatelessWidget {
  const ChatModelMenu({
    required this.selectedModelId,
    required this.models,
    required this.onSelected,
    super.key,
  });

  final String selectedModelId;
  final List<ChatModelProfile> models;
  final ValueChanged<String> onSelected;

  @override
  Widget build(BuildContext context) {
    final selected = models.firstWhere((model) => model.id == selectedModelId);
    return MenuAnchor(
      builder: (context, controller, child) {
        return TextButton.icon(
          onPressed: () =>
              controller.isOpen ? controller.close() : controller.open(),
          icon: const Icon(Icons.memory_outlined),
          label: Text(selected.label),
        );
      },
      menuChildren: [
        for (final model in models)
          MenuItemButton(
            leadingIcon: Icon(
              model.id == selectedModelId ? Icons.check : Icons.circle_outlined,
            ),
            onPressed: () => onSelected(model.id),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              mainAxisSize: MainAxisSize.min,
              children: [
                Text(model.label),
                Text(model.caption,
                    style: Theme.of(context).textTheme.bodySmall),
              ],
            ),
          ),
      ],
    );
  }
}
