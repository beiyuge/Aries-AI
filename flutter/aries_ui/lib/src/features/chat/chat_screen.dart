import 'package:flutter/services.dart';
import 'package:flutter/material.dart';

import '../../application/chat/chat_attachment_picker.dart';
import '../../application/chat/chat_repository.dart';
import '../../application/chat/chat_runtime.dart';
import 'controllers/chat_controller.dart';
import 'widgets/attachment_strip.dart';
import 'widgets/chat_composer.dart';
import 'widgets/chat_history_drawer.dart';
import 'widgets/chat_message_bubble.dart';
import 'widgets/chat_toolbar.dart';

class ChatScreen extends StatefulWidget {
  const ChatScreen({
    required this.repository,
    required this.attachmentPicker,
    required this.runtime,
    super.key,
  });

  final ChatRepository repository;
  final ChatAttachmentPicker attachmentPicker;
  final ChatRuntime runtime;

  @override
  State<ChatScreen> createState() => _ChatScreenState();
}

class _ChatScreenState extends State<ChatScreen> {
  late ChatController _controller;
  final TextEditingController _composerController = TextEditingController();

  @override
  void initState() {
    super.initState();
    _controller = ChatController(
      repository: widget.repository,
      runtime: widget.runtime,
    );
  }

  @override
  void didUpdateWidget(ChatScreen oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (!identical(widget.repository, oldWidget.repository) ||
        !identical(widget.runtime, oldWidget.runtime)) {
      _controller.dispose();
      _controller = ChatController(
        repository: widget.repository,
        runtime: widget.runtime,
      );
    }
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
                  onAttach: _pickAttachments,
                  sending: _controller.isGenerating,
                ),
              ],
            ),
          ),
        );
      },
    );
  }

  Future<void> _pickAttachments() async {
    try {
      final attachments = await widget.attachmentPicker.pick();
      await _controller.addAttachments(attachments);
    } on ChatAttachmentPickerException catch (error) {
      if (!mounted) {
        return;
      }
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(error.message)),
      );
    } on MissingPluginException catch (error) {
      if (!mounted) {
        return;
      }
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(error.message ?? 'File picker unavailable.')),
      );
    }
  }

  Future<void> _send() async {
    final text = _composerController.text.trim();
    if (text.isEmpty && _controller.pendingAttachments.isEmpty) {
      return;
    }
    await _controller.send(text);
    _composerController.clear();
  }
}
