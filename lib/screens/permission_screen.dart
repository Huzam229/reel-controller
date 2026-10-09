import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:reel_controller/screens/home_screen.dart';
import 'package:reel_controller/theme/app_theme.dart';
import 'package:reel_controller/widgets/primary_button.dart';

class PermissionScreen extends StatefulWidget {
  const PermissionScreen({super.key});

  @override
  State<PermissionScreen> createState() => _PermissionScreenState();
}

class _PermissionScreenState extends State<PermissionScreen>
    with WidgetsBindingObserver {
  static const platform = MethodChannel('reel_controller/accessibility');

  bool _busy = false;
  String? _message;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    WidgetsBinding.instance.addPostFrameCallback((_) => _openHomeIfAllowed());
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) {
      _openHomeIfAllowed();
    }
  }

  Future<void> _openHomeIfAllowed() async {
    try {
      final granted = await platform.invokeMethod<bool>('cameraGranted');
      if (granted == true && mounted) {
        await platform.invokeMethod('startTracking');
        if (!mounted) {
          return;
        }
        Navigator.of(context).pushReplacement(
          MaterialPageRoute(builder: (_) => const HomeScreen()),
        );
      }
    } on PlatformException {
      // The permission check can wait until the activity is ready.
    }
  }

  Future<void> _allow() async {
    setState(() {
      _busy = true;
      _message = null;
    });
    try {
      await platform.invokeMethod('startTracking');
      if (!mounted) {
        return;
      }
      Navigator.of(context).pushReplacement(
        MaterialPageRoute(builder: (_) => const HomeScreen()),
      );
    } on PlatformException catch (error) {
      if (!mounted) {
        return;
      }
      setState(() {
        _message = error.message ?? 'Open Settings and allow the camera';
      });
    } finally {
      if (mounted) {
        setState(() => _busy = false);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.fromLTRB(24, 28, 24, 24),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Container(
                width: 72,
                height: 72,
                decoration: BoxDecoration(
                  color: AppTheme.surfaceHigh,
                  borderRadius: BorderRadius.circular(22),
                ),
                child: const Icon(
                  Icons.photo_camera_outlined,
                  color: AppTheme.accent,
                  size: 34,
                ),
              ),
              const SizedBox(height: 28),
              Text(
                'Allow the camera',
                style: Theme.of(context).textTheme.headlineMedium,
              ),
              const SizedBox(height: 16),
              Text(
                'The camera watches your hand on this phone. It is not recorded, and it is not sent anywhere.',
                style: Theme.of(context).textTheme.bodyLarge,
              ),
              const SizedBox(height: 28),
              const _ReasonRow(
                icon: Icons.visibility_outlined,
                title: 'See your fingers',
                body: 'One finger moves forward. Two fingers move back.',
              ),
              const SizedBox(height: 16),
              const _ReasonRow(
                icon: Icons.phone_android_outlined,
                title: 'Stays on the phone',
                body: 'Hand tracking runs here, even while a reel app is open.',
              ),
              const Spacer(),
              if (_message != null) ...[
                Text(
                  _message!,
                  style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                        color: AppTheme.ink,
                      ),
                ),
                const SizedBox(height: 16),
              ],
              PrimaryButton(
                label: _busy ? 'Waiting' : 'Allow camera',
                onPressed: _busy ? null : _allow,
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _ReasonRow extends StatelessWidget {
  const _ReasonRow({
    required this.icon,
    required this.title,
    required this.body,
  });

  final IconData icon;
  final String title;
  final String body;

  @override
  Widget build(BuildContext context) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Container(
          width: 42,
          height: 42,
          decoration: BoxDecoration(
            color: AppTheme.surface,
            borderRadius: BorderRadius.circular(14),
            border: Border.all(color: AppTheme.line),
          ),
          child: Icon(icon, color: AppTheme.accent, size: 20),
        ),
        const SizedBox(width: 14),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(title, style: Theme.of(context).textTheme.titleMedium),
              const SizedBox(height: 4),
              Text(body, style: Theme.of(context).textTheme.bodyMedium),
            ],
          ),
        ),
      ],
    );
  }
}
