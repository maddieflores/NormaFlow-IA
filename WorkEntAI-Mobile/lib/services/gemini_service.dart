import 'dart:convert';
import 'package:http/http.dart' as http;

/// Servicio para llamar directamente a Google Gemini API
/// cuando el backend no está disponible.
class GeminiService {
  // Coloca aquí tu API Key de Google Gemini (ai.google.dev)
  static const String _apiKey = 'TU_API_KEY_AQUI';

  static const String _baseUrl =
      'https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent';

  /// Contexto del sistema: define el rol del asistente
  static const String _systemContext = '''
Eres un asistente virtual especializado en gestión de trámites para NormalFlow.
Tu función es ayudar a los clientes a:
1. Identificar qué tipo de trámite necesitan (instalación de medidor, reconexión de servicio, etc.)
2. Guiarlos paso a paso para completar los requisitos
3. Responder preguntas sobre el estado de sus solicitudes
4. Explicar los procesos y tiempos estimados

Responde siempre en español, de forma clara, amable y concisa.
Si el usuario pregunta algo fuera de gestión de trámites, redirige amablemente la conversación.
Mantén un tono profesional pero cercano.
''';

  /// Lista de mensajes del historial de conversación (para contexto multi-turno)
  final List<Map<String, dynamic>> _historial = [];

  /// Envía un mensaje y obtiene la respuesta de Gemini
  Future<String> enviarMensaje(String mensaje) async {
    // Agregar el mensaje del usuario al historial
    _historial.add({
      'role': 'user',
      'parts': [{'text': mensaje}],
    });

    final body = {
      'system_instruction': {
        'parts': [{'text': _systemContext}],
      },
      'contents': _historial,
      'generationConfig': {
        'temperature': 0.7,
        'maxOutputTokens': 512,
        'topP': 0.8,
      },
    };

    final res = await http.post(
      Uri.parse('$_baseUrl?key=$_apiKey'),
      headers: {'Content-Type': 'application/json'},
      body: jsonEncode(body),
    ).timeout(const Duration(seconds: 20));

    if (res.statusCode == 200) {
      final data = jsonDecode(res.body);
      final respuesta = data['candidates']?[0]['content']?['parts']?[0]['text']
          as String? ?? 'No pude generar una respuesta. Intenta de nuevo.';

      // Agregar la respuesta del asistente al historial
      _historial.add({
        'role': 'model',
        'parts': [{'text': respuesta}],
      });

      return respuesta;
    } else if (res.statusCode == 400) {
      throw Exception('API Key inválida. Verifica tu clave de Gemini.');
    } else if (res.statusCode == 429) {
      throw Exception('Límite de solicitudes alcanzado. Espera un momento.');
    } else {
      throw Exception('Error ${res.statusCode} de Gemini API.');
    }
  }

  /// Reinicia el historial de conversación
  void reiniciarConversacion() {
    _historial.clear();
  }
}
