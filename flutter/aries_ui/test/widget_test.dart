import 'package:aries_ui/src/app.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  testWidgets('re0 shell opens diagnostics tab', (tester) async {
    await tester.pumpWidget(const AriesRe0App());

    await tester.tap(find.text('Diagnostics'));
    await tester.pump();

    expect(find.byType(AriesRe0App), findsOneWidget);
  });
}
