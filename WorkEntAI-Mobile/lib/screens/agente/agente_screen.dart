import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:file_picker/file_picker.dart' as file_picker;
import 'package:speech_to_text/speech_to_text.dart';
import 'package:speech_to_text/speech_recognition_result.dart';
import '../../services/app_provider.dart';
import '../../services/gemini_service.dart';

/// Pantalla del Agente Inteligente de Atención al Cliente (CU-22/23).
///
/// Permite al cliente chatear con la IA para identificar una política y
/// completar los requisitos del trámite en un diálogo guiado interactivo.
class AgenteScreen extends StatefulWidget {
  const AgenteScreen({super.key});

  @override
  State<AgenteScreen> createState() => _AgenteScreenState();
}

class _AgenteScreenState extends State<AgenteScreen> {
  final GeminiService _gemini = GeminiService();
  final TextEditingController _inputCtrl = TextEditingController();
  final ScrollController _scrollCtrl = ScrollController();
  final SpeechToText _speech = SpeechToText();

  List<Map<String, dynamic>> _mensajes = [];
  final String _fase = 'IDENTIFICACION';

  bool _inicializando = true;
  bool _enviando = false;
  String _error = '';

  // Voz
  bool _speechAvailable = false;
  bool _escuchando = false;
  String _textoInterino = ''; // texto en tiempo real mientras habla

  @override
  void initState() {
    super.initState();
    _initSpeech();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (context.read<AppProvider>().isOffline) {
        setState(() {
          _inicializando = false;
          _error = 'El Asistente IA requiere conexión a internet para funcionar.';
        });
      } else {
        _iniciarSesion();
      }
    });
  }

  Future<void> _initSpeech() async {
    final available = await _speech.initialize(
      onError: (error) {
        if (!mounted) return;
        setState(() {
          _escuchando = false;
          _textoInterino = '';
        });
        if (error.errorMsg != 'error_speech_timeout') {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Text('Voz: ${_friendlyVoiceError(error.errorMsg)}'),
              backgroundColor: const Color(0xFFEF4444),
              behavior: SnackBarBehavior.floating,
            ),
          );
        }
      },
      onStatus: (status) {
        if (!mounted) return;
        if (status == 'done' || status == 'notListening') {
          setState(() => _escuchando = false);
          // Si hay texto transcrito, enviarlo automáticamente
          if (_textoInterino.trim().isNotEmpty) {
            _inputCtrl.text = _textoInterino.trim();
            _textoInterino = '';
            Future.microtask(() => _enviarMensaje());
          }
        }
      },
    );
    if (mounted) setState(() => _speechAvailable = available);
  }

  String _friendlyVoiceError(String error) {
    switch (error) {
      case 'error_no_match':
        return 'No se entendió lo que dijiste. Intenta de nuevo.';
      case 'error_network':
        return 'Se necesita conexión para el reconocimiento de voz.';
      case 'error_permission':
        return 'Permiso de micrófono denegado. Actívalo en Ajustes.';
      case 'error_audio':
        return 'Error de audio. Verifica el micrófono.';
      default:
        return 'Intenta hablar más despacio y claro.';
    }
  }

  @override
  void dispose() {
    _speech.stop();
    _inputCtrl.dispose();
    _scrollCtrl.dispose();
    super.dispose();
  }

  /// Inicia la sesión del asistente con Gemini directamente.
  Future<void> _iniciarSesion() async {
    try {
      setState(() {
        _inicializando = true;
        _error = '';
      });

      // Reiniciar historial de Gemini
      _gemini.reiniciarConversacion();

      // Mensaje de bienvenida local (sin llamada de red)
      setState(() {
        _mensajes = [
          {
            'rol': 'asistente',
            'contenido':
                '¡Hola! Soy tu Asistente IA de NormalFlow. '
                'Cuéntame qué tipo de trámite o solicitud deseas realizar y te guiaré en el proceso. 😊',
            'timestamp': DateTime.now().toIso8601String(),
          }
        ];
        _inicializando = false;
      });
      _scrollToBottom();
    } catch (e) {
      setState(() {
        _error = 'Error al iniciar el asistente: ${e.toString()}';
        _inicializando = false;
      });
    }
  }

  /// Envía un mensaje al asistente Gemini y muestra la respuesta.
  Future<void> _enviarMensaje() async {
    final texto = _inputCtrl.text.trim();
    if (texto.isEmpty || _enviando) return;

    _inputCtrl.clear();
    setState(() {
      _enviando = true;
      _mensajes.add({
        'rol': 'usuario',
        'contenido': texto,
        'timestamp': DateTime.now().toIso8601String(),
      });
    });
    _scrollToBottom();

    try {
      // Llamar directamente a Gemini
      final respuesta = await _gemini.enviarMensaje(texto);

      setState(() {
        _mensajes.add({
          'rol': 'asistente',
          'contenido': respuesta,
          'timestamp': DateTime.now().toIso8601String(),
        });
        _enviando = false;
      });
      _scrollToBottom();
    } on Exception catch (e) {
      final msg = e.toString();
      String respuestaError;

      if (msg.contains('API Key inválida')) {
        respuestaError =
            '⚠️ La API Key de Gemini no está configurada. '
            'Configura tu clave en lib/services/gemini_service.dart.';
      } else if (msg.contains('Límite de solicitudes')) {
        respuestaError =
            'Has enviado muchos mensajes seguidos. '
            'Espera unos segundos e intenta de nuevo.';
      } else if (msg.contains('TimeoutException') ||
          msg.contains('SocketException') ||
          msg.contains('Connection refused')) {
        respuestaError =
            'No pude conectarme. Verifica tu conexión a internet e intenta de nuevo.';
      } else {
        respuestaError =
            'Ocurrió un problema procesando tu mensaje. '
            'Intenta de nuevo en un momento.';
      }

      setState(() {
        _mensajes.add({
          'rol': 'asistente',
          'contenido': respuestaError,
          'timestamp': DateTime.now().toIso8601String(),
        });
        _enviando = false;
      });
      _scrollToBottom();
    }
  }

  Future<void> _volverPortal() async {
    _gemini.reiniciarConversacion();
    if (!mounted) return;
    Navigator.pop(context);
  }

  void _scrollToBottom() {
    Future.delayed(const Duration(milliseconds: 100), () {
      if (_scrollCtrl.hasClients) {
        _scrollCtrl.animateTo(
          _scrollCtrl.position.maxScrollExtent,
          duration: const Duration(milliseconds: 300),
          curve: Curves.easeOut,
        );
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    final isOffline = context.watch<AppProvider>().isOffline;

    return Scaffold(
      backgroundColor: const Color(0xFFF0F4FA),
      appBar: AppBar(
        backgroundColor: Colors.white,
        elevation: 0,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back, color: Color(0xFF1E293B)),
          onPressed: () => Navigator.pop(context),
        ),
        titleSpacing: 0,
        title: Row(
          children: [
            // Dark navy avatar with briefcase icon
            Container(
              width: 40,
              height: 40,
              decoration: const BoxDecoration(
                color: Color(0xFF1E3A8A),
                shape: BoxShape.circle,
              ),
              child: const Icon(
                Icons.work_outline,
                color: Colors.white,
                size: 20,
              ),
            ),
            const SizedBox(width: 10),
            Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text(
                  'Asistente IA',
                  style: TextStyle(
                    fontSize: 15,
                    fontWeight: FontWeight.bold,
                    color: Color(0xFF1E293B),
                  ),
                ),
                Row(
                  children: [
                    Container(
                      width: 7,
                      height: 7,
                      decoration: const BoxDecoration(
                        color: Color(0xFF22C55E),
                        shape: BoxShape.circle,
                      ),
                    ),
                    const SizedBox(width: 4),
                    const Text(
                      'Guía interactiva de trámites',
                      style: TextStyle(
                        fontSize: 11,
                        color: Color(0xFF22C55E),
                        fontWeight: FontWeight.w500,
                      ),
                    ),
                  ],
                ),
              ],
            ),
          ],
        ),
        actions: [
          IconButton(
              icon: const Icon(Icons.refresh_outlined, color: Color(0xFF64748B)),
              onPressed: _inicializando ? null : _iniciarSesion,
              tooltip: 'Reiniciar conversación',
            ),
          const SizedBox(width: 4),
        ],
      ),
      body: isOffline
          ? _buildOfflineView()
          : _inicializando
              ? const Center(child: CircularProgressIndicator(color: Color(0xFF2563EB)))
              : _error.isNotEmpty
                  ? _buildErrorView()
                  : Column(
                      children: [
                        // Banner de estado/fase de conversación
                        _buildFaseBanner(),

                        // Lista de mensajes de chat
                        Expanded(
                          child: ListView.builder(
                            controller: _scrollCtrl,
                            padding: const EdgeInsets.all(16),
                            itemCount: _mensajes.length + (_enviando ? 1 : 0),
                            itemBuilder: (_, i) {
                              if (i == _mensajes.length) {
                                return const _BubbleTyping();
                              }
                              return _ChatBubble(mensaje: _mensajes[i]);
                            },
                          ),
                        ),

                        // Formulario de confirmación si estamos listos para iniciar trámite
                        if (_fase == 'CONFIRMACION' || _fase == 'COMPLETADA')
                          _buildConfirmacionWidget(),

                        // Caja de entrada de texto
                        if (_fase != 'CONFIRMACION' && _fase != 'COMPLETADA')
                          _buildInputWidget(),
                      ],
                    ),
    );
  }

  Widget _buildFaseBanner() {
    String faseText = 'Fase: Identificando trámite';
    Color color = const Color(0xFF3B82F6);
    IconData icon = Icons.search;

    if (_fase == 'REQUISITOS') {
      faseText = 'Fase: Completando requisitos';
      color = const Color(0xFFF59E0B);
      icon = Icons.assignment_outlined;
    } else if (_fase == 'CONFIRMACION') {
      faseText = 'Fase: Confirmación';
      color = const Color(0xFF16A34A);
      icon = Icons.check_circle_outline;
    } else if (_fase == 'COMPLETADA') {
      faseText = 'Fase: Listo para iniciar';
      color = const Color(0xFF16A34A);
      icon = Icons.check_circle_outline;
    }

    return Container(
      margin: const EdgeInsets.fromLTRB(16, 0, 16, 8),
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.1),
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: color.withValues(alpha: 0.3)),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon, size: 14, color: color),
          const SizedBox(width: 6),
          Text(
            faseText,
            style: TextStyle(
              fontSize: 12,
              color: color,
              fontWeight: FontWeight.w600,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildConfirmacionWidget() {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: const BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
        boxShadow: [
          BoxShadow(color: Colors.black12, blurRadius: 10, offset: Offset(0, -2)),
        ],
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          const Row(
            children: [
              Text('✨', style: TextStyle(fontSize: 20)),
              SizedBox(width: 8),
              Expanded(
                child: Text(
                  '¡Información recopilada! Puedo ayudarte a iniciar el trámite.',
                  style: TextStyle(fontSize: 15, fontWeight: FontWeight.bold, color: Color(0xFF1E293B)),
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          if (_fase == 'CONFIRMACION') ...[
            ElevatedButton.icon(
              onPressed: _enviando ? null : () {
                _inputCtrl.text = "Sí, iniciar trámite";
                _enviarMensaje();
              },
              style: ElevatedButton.styleFrom(
                backgroundColor: const Color(0xFF16A34A),
                padding: const EdgeInsets.symmetric(vertical: 14),
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
              ),
              icon: const Icon(Icons.send, color: Colors.white),
              label: _enviando
                  ? const SizedBox(
                      width: 18,
                      height: 18,
                      child: CircularProgressIndicator(color: Colors.white, strokeWidth: 2),
                    )
                  : const Text('Iniciar Trámite Ahora', style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold)),
            ),
            const SizedBox(height: 8),
          ],
          if (_fase == 'COMPLETADA') ...[
            ElevatedButton.icon(
              onPressed: _volverPortal,
              style: ElevatedButton.styleFrom(
                backgroundColor: const Color(0xFF16A34A),
                padding: const EdgeInsets.symmetric(vertical: 14),
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
              ),
              icon: const Icon(Icons.home, color: Colors.white),
              label: const Text('Ir a mis trámites', style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold)),
            ),
          ],
          if (_fase != 'COMPLETADA') ...[
            OutlinedButton(
              onPressed: () => Navigator.pop(context),
              style: OutlinedButton.styleFrom(
                padding: const EdgeInsets.symmetric(vertical: 12),
                side: const BorderSide(color: Color(0xFFE2E8F0)),
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
              ),
              child: const Text('Volver al Portal', style: TextStyle(color: Color(0xFF475569))),
            ),
          ],
        ],
      ),
    );
  }

  Future<void> _escucharVoz() async {
    if (_escuchando) {
      // Detener escucha manualmente
      await _speech.stop();
      setState(() {
        _escuchando = false;
        if (_textoInterino.trim().isNotEmpty) {
          _inputCtrl.text = _textoInterino.trim();
          _textoInterino = '';
        }
      });
      return;
    }

    if (!_speechAvailable) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text(
            'El reconocimiento de voz no está disponible en este dispositivo.',
          ),
          backgroundColor: Color(0xFFEF4444),
          behavior: SnackBarBehavior.floating,
        ),
      );
      return;
    }

    setState(() {
      _escuchando = true;
      _textoInterino = '';
    });

    await _speech.listen(
      onResult: (SpeechRecognitionResult result) {
        setState(() {
          _textoInterino = result.recognizedWords;
          // Si es resultado final, colocar en el campo y enviar
          if (result.finalResult) {
            _inputCtrl.text = result.recognizedWords;
            _textoInterino = '';
            _escuchando = false;
          }
        });
      },
      listenOptions: SpeechListenOptions(
        listenFor: const Duration(seconds: 30),
        pauseFor: const Duration(seconds: 4),
        localeId: 'es_ES',
        cancelOnError: true,
        partialResults: true,
      ),
    );
  }

  Future<void> _adjuntarArchivo() async {
    try {
      final result = await file_picker.FilePicker.platform.pickFiles();
      if (result != null) {
        final file = result.files.single;
        setState(() {
          _mensajes.add({
            'rol': 'usuario',
            'contenido': '📎 Archivo adjunto: ${file.name}',
            'timestamp': DateTime.now().toIso8601String(),
          });
        });
        _scrollToBottom();
        // Simulate assistant acknowledging the file
        Future.delayed(const Duration(seconds: 1), () {
          if (!mounted) return;
          setState(() {
            _mensajes.add({
              'rol': 'asistente',
              'contenido': 'He recibido el archivo ${file.name}. Lo he analizado y adjuntado a los requisitos.',
              'timestamp': DateTime.now().toIso8601String(),
            });
          });
          _scrollToBottom();
        });
      }
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Error al adjuntar archivo')),
      );
    }
  }

  Widget _buildInputWidget() {
    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        // Barra de transcripción en tiempo real
        if (_escuchando)
          Container(
            width: double.infinity,
            padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 10),
            color: const Color(0xFFEFF6FF),
            child: Row(
              children: [
                const Icon(Icons.mic, color: Color(0xFF2563EB), size: 18),
                const SizedBox(width: 8),
                Expanded(
                  child: Text(
                    _textoInterino.isEmpty
                        ? 'Escuchando… habla ahora'
                        : _textoInterino,
                    style: TextStyle(
                      color: _textoInterino.isEmpty
                          ? const Color(0xFF94A3B8)
                          : const Color(0xFF1E293B),
                      fontSize: 14,
                      fontStyle: _textoInterino.isEmpty
                          ? FontStyle.italic
                          : FontStyle.normal,
                    ),
                  ),
                ),
                // Indicador pulsante
                _PulsingDot(),
              ],
            ),
          ),

        // Barra de entrada
        Container(
          color: Colors.white,
          padding: const EdgeInsets.fromLTRB(8, 10, 8, 10),
          child: SafeArea(
            child: Row(
              children: [
                // Paperclip
                IconButton(
                  icon: const Icon(Icons.attach_file_outlined,
                      color: Color(0xFF94A3B8), size: 22),
                  onPressed: _adjuntarArchivo,
                ),
                // Campo de texto
                Expanded(
                  child: Container(
                    decoration: BoxDecoration(
                      color: const Color(0xFFF1F5F9),
                      borderRadius: BorderRadius.circular(28),
                    ),
                    padding: const EdgeInsets.symmetric(horizontal: 18),
                    child: TextField(
                      controller: _inputCtrl,
                      maxLines: null,
                      textInputAction: TextInputAction.send,
                      onSubmitted: (_) => _enviarMensaje(),
                      decoration: const InputDecoration(
                        hintText: 'Escribe un mensaje...',
                        hintStyle:
                            TextStyle(color: Color(0xFFADB5BD), fontSize: 14),
                        border: InputBorder.none,
                        isDense: true,
                        contentPadding: EdgeInsets.symmetric(vertical: 12),
                      ),
                    ),
                  ),
                ),
                const SizedBox(width: 6),
                // Botón micrófono
                GestureDetector(
                  onTap: _escucharVoz,
                  child: Container(
                    padding: const EdgeInsets.all(10),
                    decoration: BoxDecoration(
                      color: _escuchando
                          ? const Color(0xFFEF4444)
                          : const Color(0xFFF1F5F9),
                      shape: BoxShape.circle,
                    ),
                    child: Icon(
                      _escuchando ? Icons.mic : Icons.mic_outlined,
                      color: _escuchando
                          ? Colors.white
                          : const Color(0xFF64748B),
                      size: 20,
                    ),
                  ),
                ),
                const SizedBox(width: 6),
                // Botón enviar
                GestureDetector(
                  onTap: _enviando ? null : _enviarMensaje,
                  child: Container(
                    padding: const EdgeInsets.all(12),
                    decoration: BoxDecoration(
                      color: _enviando
                          ? const Color(0xFF93C5FD)
                          : const Color(0xFF2563EB),
                      shape: BoxShape.circle,
                    ),
                    child: _enviando
                        ? const SizedBox(
                            width: 18,
                            height: 18,
                            child: CircularProgressIndicator(
                              color: Colors.white,
                              strokeWidth: 2,
                            ),
                          )
                        : const Icon(Icons.send_rounded,
                            color: Colors.white, size: 18),
                  ),
                ),
              ],
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildOfflineView() {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(32),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const Text('📴', style: TextStyle(fontSize: 64)),
            const SizedBox(height: 16),
            const Text(
              'Asistente Desconectado',
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: Color(0xFF1E293B)),
            ),
            const SizedBox(height: 8),
            const Text(
              'El Asistente Inteligente IA requiere conexión activa a internet para poder procesar lenguaje natural.',
              textAlign: TextAlign.center,
              style: TextStyle(color: Color(0xFF64748B), fontSize: 13, height: 1.5),
            ),
            const SizedBox(height: 24),
            ElevatedButton(
              onPressed: () => Navigator.pop(context),
              child: const Text('Volver al Portal'),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildErrorView() {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(32),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const Text('⚠️', style: TextStyle(fontSize: 48)),
            const SizedBox(height: 16),
            const Text(
              'Error de Conexión',
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: Color(0xFF1E293B)),
            ),
            const SizedBox(height: 8),
            Text(
              _error,
              textAlign: TextAlign.center,
              style: const TextStyle(color: Color(0xFF64748B), fontSize: 13),
            ),
            const SizedBox(height: 24),
            ElevatedButton(
              onPressed: _iniciarSesion,
              child: const Text('Reintentar Conexión'),
            ),
          ],
        ),
      ),
    );
  }
}

class _ChatBubble extends StatelessWidget {
  final Map<String, dynamic> mensaje;
  const _ChatBubble({required this.mensaje});

  String _formatTime(String? isoString) {
    if (isoString == null) return '';
    try {
      final dt = DateTime.parse(isoString).toLocal();
      return '${dt.hour.toString().padLeft(2, '0')}:${dt.minute.toString().padLeft(2, '0')}';
    } catch (_) {
      return '';
    }
  }

  @override
  Widget build(BuildContext context) {
    final esAsistente = mensaje['rol'] == 'asistente';
    final content = mensaje['contenido'] ?? '';
    final timeStr = _formatTime(mensaje['timestamp'] as String?);

    return Padding(
      padding: const EdgeInsets.only(bottom: 4),
      child: Column(
        crossAxisAlignment:
            esAsistente ? CrossAxisAlignment.start : CrossAxisAlignment.end,
        children: [
          Align(
            alignment: esAsistente ? Alignment.centerLeft : Alignment.centerRight,
            child: Container(
              margin: const EdgeInsets.only(bottom: 4),
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
              constraints: BoxConstraints(
                maxWidth: MediaQuery.of(context).size.width * 0.78,
              ),
              decoration: BoxDecoration(
                color: esAsistente ? Colors.white : const Color(0xFF2563EB),
                borderRadius: BorderRadius.only(
                  topLeft: const Radius.circular(18),
                  topRight: const Radius.circular(18),
                  bottomLeft: Radius.circular(esAsistente ? 4 : 18),
                  bottomRight: Radius.circular(esAsistente ? 18 : 4),
                ),
                boxShadow: [
                  BoxShadow(
                    color: Colors.black.withValues(alpha: 0.04),
                    blurRadius: 6,
                    offset: const Offset(0, 2),
                  ),
                ],
              ),
              child: Text(
                content,
                style: TextStyle(
                  color: esAsistente ? const Color(0xFF1E293B) : Colors.white,
                  fontSize: 14,
                  height: 1.5,
                ),
              ),
            ),
          ),
          if (timeStr.isNotEmpty)
            Padding(
              padding: const EdgeInsets.only(bottom: 10, left: 4, right: 4),
              child: Text(
                timeStr,
                style: const TextStyle(
                  fontSize: 10,
                  color: Color(0xFFADB5BD),
                ),
              ),
            ),
        ],
      ),
    );
  }
}

class _BubbleTyping extends StatelessWidget {
  const _BubbleTyping();

  @override
  Widget build(BuildContext context) {
    return Align(
      alignment: Alignment.centerLeft,
      child: Container(
        margin: const EdgeInsets.only(bottom: 12),
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: const BorderRadius.only(
            topLeft: Radius.circular(16),
            topRight: Radius.circular(16),
            bottomLeft: Radius.circular(4),
            bottomRight: Radius.circular(16),
          ),
          border: Border.all(color: const Color(0xFFE2E8F0)),
        ),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            _dot(1),
            const SizedBox(width: 4),
            _dot(2),
            const SizedBox(width: 4),
            _dot(3),
          ],
        ),
      ),
    );
  }

  Widget _dot(int step) {
    return TweenAnimationBuilder<double>(
      tween: Tween(begin: 0.0, end: 1.0),
      duration: Duration(milliseconds: 300 + step * 150),
      builder: (_, val, __) => Transform.translate(
        offset: Offset(0, -3.0 * val),
        child: Container(
          width: 5,
          height: 5,
          decoration: const BoxDecoration(
            color: Color(0xFF94A3B8),
            shape: BoxShape.circle,
          ),
        ),
      ),
    );
  }
}

// Dot pulsante para indicar que el mic está activo
class _PulsingDot extends StatefulWidget {
  @override
  State<_PulsingDot> createState() => _PulsingDotState();
}

class _PulsingDotState extends State<_PulsingDot>
    with SingleTickerProviderStateMixin {
  late AnimationController _ctrl;
  late Animation<double> _anim;

  @override
  void initState() {
    super.initState();
    _ctrl = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 700),
    )..repeat(reverse: true);
    _anim = Tween<double>(begin: 0.4, end: 1.0).animate(
      CurvedAnimation(parent: _ctrl, curve: Curves.easeInOut),
    );
  }

  @override
  void dispose() {
    _ctrl.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return FadeTransition(
      opacity: _anim,
      child: Container(
        width: 10,
        height: 10,
        decoration: const BoxDecoration(
          color: Color(0xFFEF4444),
          shape: BoxShape.circle,
        ),
      ),
    );
  }
}
