import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../models/models.dart';
import '../../services/app_provider.dart';

class NotificacionesScreen extends StatelessWidget {
  const NotificacionesScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFF0F4FA),
      body: SafeArea(
        child: Consumer<AppProvider>(
          builder: (_, provider, __) {
            final notifs = provider.notificaciones;
            final unread = notifs.where((n) => !n.leida).length;

            return Column(
              children: [
                // ── Header ──────────────────────────────────────
                Padding(
                  padding: const EdgeInsets.fromLTRB(20, 20, 20, 0),
                  child: Row(
                    children: [
                      // Bell icon circle
                      Container(
                        width: 48,
                        height: 48,
                        decoration: BoxDecoration(
                          color: const Color(0xFFEEF2FF),
                          shape: BoxShape.circle,
                        ),
                        child: const Icon(
                          Icons.notifications_outlined,
                          color: Color(0xFF4F46E5),
                          size: 24,
                        ),
                      ),
                      const SizedBox(width: 14),
                      Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          const Text(
                            'Notificaciones',
                            style: TextStyle(
                              fontSize: 22,
                              fontWeight: FontWeight.bold,
                              color: Color(0xFF1E293B),
                            ),
                          ),
                          if (unread > 0)
                            Text(
                              '$unread sin leer',
                              style: const TextStyle(
                                fontSize: 13,
                                color: Color(0xFF64748B),
                              ),
                            ),
                        ],
                      ),
                      const Spacer(),
                      // Back button
                      IconButton(
                        icon: const Icon(Icons.close, color: Color(0xFF94A3B8)),
                        onPressed: () => Navigator.pop(context),
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 16),

                // ── Content ─────────────────────────────────────
                Expanded(
                  child: provider.loadingData
                      ? const Center(
                          child: CircularProgressIndicator(
                            color: Color(0xFF2563EB),
                          ),
                        )
                      : notifs.isEmpty
                          ? _buildEmpty(context, provider)
                          : RefreshIndicator(
                              color: const Color(0xFF2563EB),
                              onRefresh: () => provider.cargarNotificaciones(),
                              child: ListView.builder(
                                padding: const EdgeInsets.fromLTRB(16, 4, 16, 24),
                                itemCount: notifs.length,
                                itemBuilder: (_, i) => _NotifCard(
                                  notif: notifs[i],
                                  onTap: () => provider.marcarLeida(notifs[i].id),
                                ),
                              ),
                            ),
                ),

                // ── Mark all read ────────────────────────────────
                if (notifs.isNotEmpty && unread > 0)
                  Padding(
                    padding: const EdgeInsets.fromLTRB(16, 0, 16, 12),
                    child: TextButton(
                      onPressed: () => provider.marcarTodasLeidas(),
                      child: const Text(
                        'Marcar todas como leídas',
                        style: TextStyle(
                          color: Color(0xFF2563EB),
                          fontSize: 13,
                        ),
                      ),
                    ),
                  ),
              ],
            );
          },
        ),
      ),
    );
  }

  Widget _buildEmpty(BuildContext context, AppProvider provider) {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          const Text('🔔', style: TextStyle(fontSize: 56)),
          const SizedBox(height: 16),
          const Text(
            'Sin notificaciones',
            style: TextStyle(
              fontSize: 17,
              fontWeight: FontWeight.bold,
              color: Color(0xFF1E293B),
            ),
          ),
          const SizedBox(height: 8),
          const Text(
            'Aquí aparecerán las actualizaciones\nde tus trámites.',
            textAlign: TextAlign.center,
            style: TextStyle(
              color: Color(0xFF64748B),
              fontSize: 13,
              height: 1.5,
            ),
          ),
          const SizedBox(height: 24),
          OutlinedButton.icon(
            onPressed: () => provider.cargarNotificaciones(),
            icon: const Icon(Icons.refresh, size: 18),
            label: const Text('Actualizar'),
            style: OutlinedButton.styleFrom(
              foregroundColor: const Color(0xFF2563EB),
              side: const BorderSide(color: Color(0xFF2563EB)),
            ),
          ),
        ],
      ),
    );
  }
}

class _NotifCard extends StatelessWidget {
  final Notificacion notif;
  final VoidCallback onTap;
  const _NotifCard({required this.notif, required this.onTap});

  Color _getColor() {
    switch (notif.tipo) {
      case 'TRAMITE_COMPLETADO':
        return const Color(0xFF22C55E);
      case 'TRAMITE_RECHAZADO':
        return const Color(0xFFEF4444);
      case 'TRAMITE_AVANZADO':
        return const Color(0xFF2563EB);
      case 'CUELLO_BOTELLA':
        return const Color(0xFFF59E0B);
      default:
        return const Color(0xFF6366F1);
    }
  }

  IconData _getIcon() {
    switch (notif.tipo) {
      case 'TRAMITE_COMPLETADO':
        return Icons.check_circle_outline;
      case 'TRAMITE_RECHAZADO':
        return Icons.cancel_outlined;
      case 'TRAMITE_AVANZADO':
        return Icons.info_outline;
      case 'CUELLO_BOTELLA':
        return Icons.schedule_outlined;
      default:
        return Icons.schedule_outlined;
    }
  }

  /// Extracts a short readable title from the notification message
  String get _title {
    switch (notif.tipo) {
      case 'TRAMITE_COMPLETADO':
        return 'Trámite completado';
      case 'TRAMITE_RECHAZADO':
        return 'Trámite rechazado';
      case 'TRAMITE_AVANZADO':
        return 'Documento requerido';
      case 'CUELLO_BOTELLA':
        return 'Nuevo trámite en curso';
      default:
        return 'Notificación';
    }
  }

  @override
  Widget build(BuildContext context) {
    final color = _getColor();
    return GestureDetector(
      onTap: onTap,
      child: Container(
        margin: const EdgeInsets.only(bottom: 10),
        padding: const EdgeInsets.all(14),
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(14),
          border: Border.all(
            color: notif.leida
                ? const Color(0xFFE8EFF8)
                : const Color(0xFFBFDBFE),
          ),
          boxShadow: [
            BoxShadow(
              color: Colors.black.withValues(alpha: 0.03),
              blurRadius: 6,
              offset: const Offset(0, 2),
            ),
          ],
        ),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Icon circle
            Container(
              width: 40,
              height: 40,
              decoration: BoxDecoration(
                color: color.withValues(alpha: 0.1),
                shape: BoxShape.circle,
              ),
              child: Icon(_getIcon(), color: color, size: 20),
            ),
            const SizedBox(width: 12),

            // Content
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Text(
                        _title,
                        style: TextStyle(
                          fontSize: 14,
                          fontWeight: notif.leida
                              ? FontWeight.w600
                              : FontWeight.bold,
                          color: const Color(0xFF1E293B),
                        ),
                      ),
                      Text(
                        notif.fechaFormatted,
                        style: const TextStyle(
                          fontSize: 11,
                          color: Color(0xFF94A3B8),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 3),
                  Text(
                    notif.mensaje,
                    style: const TextStyle(
                      fontSize: 13,
                      color: Color(0xFF64748B),
                      height: 1.4,
                    ),
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                  ),
                ],
              ),
            ),

            // Unread dot
            if (!notif.leida) ...[
              const SizedBox(width: 8),
              Container(
                width: 8,
                height: 8,
                margin: const EdgeInsets.only(top: 4),
                decoration: const BoxDecoration(
                  color: Color(0xFF2563EB),
                  shape: BoxShape.circle,
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }
}
