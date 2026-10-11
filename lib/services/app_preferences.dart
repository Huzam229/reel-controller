import 'package:shared_preferences/shared_preferences.dart';

class AppPreferences {
  static const _onboardingKey = 'onboarding_done';

  static Future<bool> onboardingDone() async {
    final preferences = await SharedPreferences.getInstance();
    return preferences.getBool(_onboardingKey) ?? false;
  }

  static Future<void> markOnboardingDone() async {
    final preferences = await SharedPreferences.getInstance();
    await preferences.setBool(_onboardingKey, true);
  }
}
