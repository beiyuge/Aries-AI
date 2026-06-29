import 'package:flutter/material.dart';

import 'controllers/chat_controller.dart';
import 'widgets/attachment_strip.dart';
import 'widgets/chat_composer.dart';
import 'widgets/chat_history_drawer.dart';
import 'widgets/chat_message_bubble.dart';
import 'widgets/chat_toolbar.dart';

class ChatScreen extends StatefulWidget {
  const ChatScreen({super.key});

  @override
  State<ChatScreen> createState() => _ChatScreenState();
}

class _ChatScreenState extends State<ChatScreen> {
  late final ChatController _controller;
  final TextEditingController _composerController = TextEditingController();

  @override
  void initState() {
    super.initState();
    _controller = ChatController();
  }

  @override
  void dispose() {
    _composerController.dispose();
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: _controller,
      builder: (context, _) {
        return Scaffold(
          drawer: ChatHistoryDrawer(
            sessions: _controller.sessions,
            selectedSessionId: _controller.activeSession.id,
            onSessionSelected: _controller.selectSession,
            onNewSession: _controller.startNewSession,
          ),
          body: SafeArea(
            child: Column(
              children: [
                ChatToolbar(
                  activeSession: _controller.activeSession,
                  selectedModelId: _controller.selectedModelId,
                  availableModels: _controller.availableModels,
                  onModelChanged: _controller.selectModel,
                ),
                const Divider(height: 1),
                Expanded(
                  child: ListView.separated(
                    padding: const EdgeInsets.fromLTRB(16, 16, 16, 12),
                    reverse: true,
                    itemBuilder: (context, index) {
                      final message = _controller
                          .activeSession.messages.reversed
                          .elementAt(index);
                      return ChatMessageBubble(message: message);
                    },
                    separatorBuilder: (_, __) => const SizedBox(height: 12),
                    itemCount: _controller.activeSession.messages.length,
                  ),
                ),
                AttachmentStrip(
                  attachments: _controller.pendingAttachments,
                  onRemove: _controller.removeAttachment,
                ),
                ChatComposer(
                  controller: _composerController,
                  onSend: _send,
                  onAttach: _controller.addSampleAttachment,
                ),
              ],
            ),
          ),
        );
      },
    );
  }

  void _send() {
    final text = _composerController.text.trim();
    if (text.isEmpty && _controller.pendingAttachments.isEmpty) {
      return;
    }
    _controller.send(text);
    _composerController.clear();
  }
}
