import 'package:flutter_test/flutter_test.dart';
import 'package:reel_controller/main.dart';
import 'package:shared_preferences/shared_preferences.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  testWidgets('first launch shows the boarding screen', (tester) async {
    SharedPreferences.setMockInitialValues({});

    await tester.pumpWidget(const ReelControllerApp());
    await tester.pumpAndSettle();

    expect(find.text('Scroll reels without touching the screen'), findsOneWidget);
    expect(find.text('Continue'), findsNothing);
    expect(find.text('Next'), findsOneWidget);
  });
}
