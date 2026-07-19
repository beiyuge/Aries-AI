import 'package:aries_ui/src/application/chat/chat_attachment_picker.dart';

class FakeChatAttachmentPicker implements ChatAttachmentPicker {
  const FakeChatAttachmentPicker(this.attachments);

  factory FakeChatAttachmentPicker.single({
    String name = 'screen-context.json',
    String mimeType = 'application/json',
    int byteLength = 8192,
    String source = '/tmp/screen-context.json',
  }) {
    return FakeChatAttachmentPicker([
      PickedChatAttachment(
        name: name,
        mimeType: mimeType,
        byteLength: byteLength,
        source: source,
      ),
    ]);
  }

  final List<PickedChatAttachment> attachments;

  @override
  Future<List<PickedChatAttachment>> pick() async => attachments;
}
