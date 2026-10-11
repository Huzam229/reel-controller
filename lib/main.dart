import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:reel_controller/screens/home_screen.dart';
import 'package:reel_controller/screens/onboarding_screen.dart';
import 'package:reel_controller/screens/permission_screen.dart';
import 'package:reel_controller/services/app_preferences.dart';
import 'package:reel_controller/theme/app_theme.dart';

void main() {
  runApp(const ReelControllerApp());
}

class ReelControllerApp extends StatelessWidget {
  const ReelControllerApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      title: 'Reel Controller',
      theme: AppTheme.dark,
      home: const AppGate(),
    );
  }
}

class AppGate extends StatefulWidget {
  const AppGate({super.key});

  @override
  State<AppGate> createState() => _AppGateState();
}

class _AppGateState extends State<AppGate> {
  static const platform = MethodChannel('reel_controller/accessibility');

  Widget? _start;

  @override
  void initState() {
    super.initState();
    _chooseStart();
  }

  Future<void> _chooseStart() async {
    final finished = await AppPreferences.onboardingDone();
    if (!finished) {
      setState(() => _start = const OnboardingScreen());
      return;
    }

    var granted = false;
    try {
      granted = await platform.invokeMethod<bool>('cameraGranted') ?? false;
    } on PlatformException {
      granted = false;
    }

    if (!mounted) {
      return;
    }
    setState(() {
      _start = granted ? const HomeScreen() : const PermissionScreen();
    });
  }

  @override
  Widget build(BuildContext context) {
    return _start ??
        const Scaffold(
          body: Center(
            child: SizedBox(
              width: 28,
              height: 28,
              child: CircularProgressIndicator(strokeWidth: 2.4),
            ),
          ),
        );
  }
}
