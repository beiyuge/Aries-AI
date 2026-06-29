import 'package:flutter/material.dart';

import '../models/chat_models.dart';

class ChatHistoryDrawer extends StatelessWidget {
  const ChatHistoryDrawer({
    required this.sessions,
    required this.selectedSessionId,
    required this.onSessionSelected,
    required this.onNewSession,
    super.key,
  });

  final List<ChatSession> sessions;
  final String selectedSessionId;
  final ValueChanged<String> onSessionSelected;
  final VoidCallback onNewSession;

  @override
  Widget build(BuildContext context) {
    return Drawer(
      child: SafeArea(
        child: Column(
          children: [
            Padding(
              padding: const EdgeInsets.all(16),
              child: Row(
                children: [
                  Expanded(
                    child: Text('History',
                        style: Theme.of(context).textTheme.titleLarge),
                  ),
                  IconButton(
                    tooltip: 'New chat',
                    icon: const Icon(Icons.add_comment_outlined),
                    onPressed: () {
                      Navigator.of(context).pop();
                      onNewSession();
                    },
                  ),
                ],
              ),
            ),
            Expanded(
              child: ListView.builder(
                itemCount: sessions.length,
                itemBuilder: (context, index) {
                  final session = sessions[index];
                  return ListTile(
                    selected: session.id == selectedSessionId,
                    leading: const Icon(Icons.forum_outlined),
                    title: Text(session.title,
                        maxLines: 1, overflow: TextOverflow.ellipsis),
                    subtitle: Text('${session.messages.length} messages'),
                    onTap: () {
                      Navigator.of(context).pop();
                      onSessionSelected(session.id);
                    },
                  );
                },
              ),
            ),
          ],
        ),
      ),
    );
  }
}
