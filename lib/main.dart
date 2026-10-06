import 'package:flutter/material.dart';
import 'screens/camera_screen.dart';

void main() {
  runApp(const ReelControllerApp());
}

class ReelControllerApp extends StatelessWidget {
  const ReelControllerApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      home: const CameraScreen(),
    );
  }
}