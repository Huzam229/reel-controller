import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

class CameraScreen extends StatefulWidget {
  const CameraScreen({super.key});

  @override
  State<CameraScreen> createState() => _CameraScreenState();
}

class _CameraScreenState extends State<CameraScreen> {
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

      if (!mounted || status == null) {
        return;
      }

      setState(() {
        _status = status;
      });
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
      body: Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Text(
            _status,
            textAlign: TextAlign.center,
            style: const TextStyle(
              fontSize: 32,
              fontWeight: FontWeight.bold,
            ),
          ),
        ),
      ),
    );
  }
}
