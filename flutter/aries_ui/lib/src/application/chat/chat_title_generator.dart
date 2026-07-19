class ChatTitleGenerator {
  const ChatTitleGenerator();

  String fromText(String text) {
    final normalized = text.replaceAll(RegExp(r'\s+'), ' ').trim();
    if (normalized.isEmpty) {
      return 'Attached context';
    }
    return normalized.length <= 28
        ? normalized
        : '${normalized.substring(0, 28)}...';
  }
}
