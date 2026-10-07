import 'package:flutter/material.dart';
import 'screens/gesture_screen.dart';

void main() {
  runApp(const ReelControllerApp());
}

class ReelControllerApp extends StatelessWidget {
  const ReelControllerApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      home: const GestureScreen(),
    );
  }
}