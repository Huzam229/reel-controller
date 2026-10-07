import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../gesture_service.dart';

class GestureScreen extends StatefulWidget {
  const GestureScreen({super.key});

  @override
  State<GestureScreen> createState() => _GestureScreenState();
}

class _GestureScreenState extends State<GestureScreen> {

  // Communication between Flutter and Android
  static const platform = MethodChannel(
    'reel_controller/accessibility',
  );

  Timer? _timer;

  String _label = 'Waiting for gesture';

  String _swipeStatus = '';

  @override
  void initState() {
    super.initState();

    _poll();

    _timer = Timer.periodic(
      const Duration(milliseconds: 100),
          (_) => _poll(),
    );
  }

  // --------------------------------------------------
  // Get gesture from Flask
  // --------------------------------------------------

  Future<void> _poll() async {

    final gesture = await GestureService.getGesture();

    if (!mounted) {
      return;
    }

    setState(() {
      _label = _labelFor(gesture);
    });
  }

  // --------------------------------------------------
  // Test Android swipe
  // --------------------------------------------------

  Future<void> _testSwipeUp() async {

    try {

      await platform.invokeMethod('swipeUp');

      if (!mounted) {
        return;
      }


      if (!mounted) {
        return;
      }

      setState(() {
        _swipeStatus = 'Swipe sent';
      });

    } on PlatformException catch (e) {

      if (!mounted) {
        return;
      }

      setState(() {
        _swipeStatus = e.message ?? 'Swipe failed';
      });
    }
  }

  // --------------------------------------------------
  // Convert gesture to screen label
  // --------------------------------------------------

  String _labelFor(String? gesture) {

    switch (gesture) {

      case 'NEXT_REEL':
        return 'NEXT REEL';

      case 'PREVIOUS_REEL':
        return 'PREVIOUS REEL';

      case null:
        return 'Waiting for gesture';

      default:
        return gesture;
    }
  }

  @override
  void dispose() {

    _timer?.cancel();
    super.dispose();
  }

  // --------------------------------------------------
  // UI
  // --------------------------------------------------

  @override
  Widget build(BuildContext context) {

    return Scaffold(

      body: ListView.builder(

        itemCount: 40,

        itemBuilder: (context, index) {

          if (index == 0) {

            return Padding(

              padding: const EdgeInsets.fromLTRB(24, 48, 24, 24),

              child: Column(

                children: [

                  Text(
                    _label,
                    textAlign: TextAlign.center,
                    style: const TextStyle(
                      fontSize: 30,
                      fontWeight: FontWeight.bold,
                    ),
                  ),

                  const SizedBox(height: 16),

                  if (_swipeStatus.isNotEmpty)
                    Text(
                      _swipeStatus,
                      textAlign: TextAlign.center,
                    ),

                  const SizedBox(height: 30),

                  ElevatedButton(
                    onPressed: _testSwipeUp,
                    child: const Text(
                      'TEST SWIPE UP',
                    ),
                  ),

                ],
              ),
            );
          }

          return ListTile(
            title: Text('Scroll item $index'),
          );
        },
      ),
    );
  }
}