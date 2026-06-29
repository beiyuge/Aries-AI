import 'package:aries_ui/src/app.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  testWidgets('re0 shell shows app title and chat tab', (tester) async {
    await tester.pumpWidget(const AriesRe0App());

    expect(find.text('Aries AI re0'), findsOneWidget);
    expect(find.text('Chat'), findsWidgets);
  });
}
