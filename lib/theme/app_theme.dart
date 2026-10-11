import 'package:flutter/material.dart';

class AppTheme {
  static const background = Color(0xFF0C0D10);
  static const surface = Color(0xFF17191F);
  static const surfaceHigh = Color(0xFF22252D);
  static const ink = Color(0xFFF3F0E8);
  static const muted = Color(0xFFA8A396);
  static const line = Color(0xFF2C3038);
  static const accent = Color(0xFFE4B15A);

  static ThemeData get dark {
    final scheme = ColorScheme.dark(
      surface: background,
      primary: accent,
      onPrimary: const Color(0xFF1A1408),
      onSurface: ink,
      secondary: accent,
    );

    return ThemeData(
      useMaterial3: true,
      brightness: Brightness.dark,
      colorScheme: scheme,
      scaffoldBackgroundColor: background,
      fontFamily: 'Roboto',
      textTheme: const TextTheme(
        headlineMedium: TextStyle(
          fontSize: 32,
          height: 1.15,
          fontWeight: FontWeight.w700,
          color: ink,
          letterSpacing: -0.6,
        ),
        titleLarge: TextStyle(
          fontSize: 22,
          fontWeight: FontWeight.w700,
          color: ink,
        ),
        titleMedium: TextStyle(
          fontSize: 17,
          fontWeight: FontWeight.w600,
          color: ink,
        ),
        bodyLarge: TextStyle(
          fontSize: 16,
          height: 1.45,
          color: muted,
        ),
        bodyMedium: TextStyle(
          fontSize: 14,
          height: 1.4,
          color: muted,
        ),
        labelLarge: TextStyle(
          fontSize: 16,
          fontWeight: FontWeight.w700,
          letterSpacing: 0.2,
        ),
      ),
    );
  }
}
