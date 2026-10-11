import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:reel_controller/theme/app_theme.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  static const platform = MethodChannel('reel_controller/accessibility');

  Timer? _timer;
  String _status = 'Starting hand control';

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => _start());
    _timer = Timer.periodic(
      const Duration(milliseconds: 200),
      (_) => _refreshStatus(),
    );
  }

  Future<void> _start() async {
    try {
      await platform.invokeMethod('startTracking');
    } on PlatformException catch (error) {
      if (!mounted) {
        return;
      }
      setState(() {
        _status = error.message ?? 'Could not start hand control';
      });
    }
  }

  Future<void> _refreshStatus() async {
    try {
      final status = await platform.invokeMethod<String>('trackingStatus');
      if (!mounted || status == null || status == _status) {
        return;
      }
      setState(() => _status = status);
    } on PlatformException {
      // The activity is not ready yet.
    }
  }

  @override
  void dispose() {
    _timer?.cancel();
    platform.invokeMethod('stopTracking');
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.fromLTRB(24, 20, 24, 32),
          children: [
            Text(
              'Reel Controller',
              style: Theme.of(context).textTheme.titleLarge,
            ),
            const SizedBox(height: 6),
            Text(
              'Keep this screen open, then switch to your reel app.',
              style: Theme.of(context).textTheme.bodyLarge,
            ),
            const SizedBox(height: 24),
            _StatusCard(status: _status),
            const SizedBox(height: 28),
            Text('Gestures', style: Theme.of(context).textTheme.titleLarge),
            const SizedBox(height: 12),
            const _GuideCard(
              icon: Icons.front_hand_outlined,
              title: 'Fist',
              body: 'The rest pose. It gets the next move ready and does not scroll.',
            ),
            const SizedBox(height: 10),
            const _GuideCard(
              icon: Icons.looks_one_outlined,
              title: 'Index finger',
              body: 'Point only the index finger up. That is the next reel.',
            ),
            const SizedBox(height: 10),
            const _GuideCard(
              icon: Icons.looks_two_outlined,
              title: 'Index and middle',
              body: 'Raise those two together. Ring and pinky stay down. That is the previous reel.',
            ),
            const SizedBox(height: 28),
            Text('Before the first scroll', style: Theme.of(context).textTheme.titleLarge),
            const SizedBox(height: 12),
            const _Step(number: '1', text: 'Allow the camera when this app asks.'),
            const _Step(number: '2', text: 'Turn on Reel Controller in Accessibility.'),
            const _Step(
              number: '3',
              text: 'If Android blocks it, allow restricted settings for this app.',
            ),
            const _Step(
              number: '4',
              text: 'Open a reel, make one pose, then return to a fist before the next.',
            ),
          ],
        ),
      ),
    );
  }
}

class _StatusCard extends StatelessWidget {
  const _StatusCard({required this.status});

  final String status;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.fromLTRB(22, 22, 22, 24),
      decoration: BoxDecoration(
        color: AppTheme.surface,
        borderRadius: BorderRadius.circular(24),
        border: Border.all(color: AppTheme.line),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Container(
                width: 8,
                height: 8,
                decoration: const BoxDecoration(
                  color: AppTheme.accent,
                  shape: BoxShape.circle,
                ),
              ),
              const SizedBox(width: 8),
              Text(
                'LIVE',
                style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                      color: AppTheme.accent,
                      fontWeight: FontWeight.w700,
                      letterSpacing: 1.2,
                    ),
              ),
            ],
          ),
          const SizedBox(height: 18),
          Text(
            status,
            style: Theme.of(context).textTheme.headlineMedium,
          ),
          const SizedBox(height: 8),
          Text(
            _hint(status),
            style: Theme.of(context).textTheme.bodyLarge,
          ),
        ],
      ),
    );
  }

  String _hint(String status) {
    switch (status) {
      case 'Fist':
        return 'Ready for the next pose.';
      case 'Index':
        return 'Index finger seen.';
      case 'Two fingers':
        return 'Index and middle fingers seen.';
      case 'Next reel':
        return 'Moving to the next video.';
      case 'Previous reel':
        return 'Moving to the previous video.';
      case 'Hand seen':
        return 'Make a fist, then show one or two fingers.';
      default:
        return 'Watching the camera.';
    }
  }
}

class _GuideCard extends StatelessWidget {
  const _GuideCard({
    required this.icon,
    required this.title,
    required this.body,
  });

  final IconData icon;
  final String title;
  final String body;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppTheme.surface,
        borderRadius: BorderRadius.circular(18),
        border: Border.all(color: AppTheme.line),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(icon, color: AppTheme.accent),
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
      ),
    );
  }
}

class _Step extends StatelessWidget {
  const _Step({required this.number, required this.text});

  final String number;
  final String text;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 14),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            width: 28,
            height: 28,
            alignment: Alignment.center,
            decoration: BoxDecoration(
              color: AppTheme.surfaceHigh,
              borderRadius: BorderRadius.circular(9),
            ),
            child: Text(
              number,
              style: const TextStyle(
                color: AppTheme.accent,
                fontWeight: FontWeight.w700,
              ),
            ),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Padding(
              padding: const EdgeInsets.only(top: 3),
              child: Text(text, style: Theme.of(context).textTheme.bodyLarge),
            ),
          ),
        ],
      ),
    );
  }
}
