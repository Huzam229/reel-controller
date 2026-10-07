import 'dart:convert';
import 'package:http/http.dart' as http;

class GestureService {
  static const String baseUrl = 'http://10.0.2.2:5000';

  static Future<String?> getGesture() async {
    try {
      final response = await http.get(
        Uri.parse('$baseUrl/gesture'),
      );

      if (response.statusCode == 200) {
        final data = jsonDecode(response.body);

        return data['gesture'];
      }

      return null;
    } catch (e) {
      print('Gesture connection error: $e');
      return null;
    }
  }
}
