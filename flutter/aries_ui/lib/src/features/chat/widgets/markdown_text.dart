import 'package:flutter/material.dart';

class MarkdownText extends StatelessWidget {
  const MarkdownText(
    this.text, {
    this.color,
    super.key,
  });

  final String text;
  final Color? color;

  @override
  Widget build(BuildContext context) {
    final lines = text.split('\n');
    final textTheme = Theme.of(context).textTheme;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      mainAxisSize: MainAxisSize.min,
      children: [
        for (final line in lines) _line(context, textTheme, line),
      ],
    );
  }

  Widget _line(BuildContext context, TextTheme textTheme, String line) {
    if (line.startsWith('# ')) {
      return Padding(
        padding: const EdgeInsets.only(bottom: 6),
        child: Text(
          line.substring(2),
          style: textTheme.titleMedium?.copyWith(color: color),
        ),
      );
    }
    if (line.startsWith('- ')) {
      return Padding(
        padding: const EdgeInsets.only(bottom: 4),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('• ', style: TextStyle(color: color)),
            Expanded(
              child: Text(
                line.substring(2),
                style: textTheme.bodyMedium?.copyWith(color: color),
              ),
            ),
          ],
        ),
      );
    }
    return Padding(
      padding: const EdgeInsets.only(bottom: 4),
      child: Text(
        line,
        style: textTheme.bodyMedium?.copyWith(color: color),
      ),
    );
  }
}
