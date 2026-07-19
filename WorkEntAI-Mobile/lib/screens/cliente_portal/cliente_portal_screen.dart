import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../models/models.dart';
import '../../services/app_provider.dart';
import '../agente/agente_screen.dart';
import '../notificaciones/notificaciones_screen.dart';
import '../tramite_detalle/tramite_detalle_screen.dart';


class ClientePortalScreen extends StatefulWidget {
  const ClientePortalScreen({super.key});

  @override
  State<ClientePortalScreen> createState() => _ClientePortalScreenState();
}

class _ClientePortalScreenState extends State<ClientePortalScreen>
    with SingleTickerProviderStateMixin {
  late TabController _tabCtrl;

  @override
  void initState() {
    super.initState();
    _tabCtrl = TabController(length: 2, vsync: this);
    // Rebuild when tab changes so the FAB shows/hides correctly
    _tabCtrl.addListener(() {
      if (mounted) setState(() {});
    });
  }

  @override
  void dispose() {
    _tabCtrl.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFF0F4FA),
      appBar: AppBar(
        backgroundColor: Colors.white,
        elevation: 0,
        titleSpacing: 16,
        title: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: const [
            Text(
              'Portal de Trámites',
              style: TextStyle(
                fontSize: 20,
                fontWeight: FontWeight.bold,
                color: Color(0xFF1E293B),
              ),
            ),
            Text(
              'NormalFlow',
              style: TextStyle(
                fontSize: 12,
                color: Color(0xFF64748B),
                fontWeight: FontWeight.w400,
              ),
            ),
          ],
        ),
        actions: [
          Consumer<AppProvider>(
            builder: (_, p, __) => Stack(
              alignment: Alignment.topRight,
              children: [
                IconButton(
                  icon: const Icon(
                    Icons.notifications_outlined,
                    color: Color(0xFF1E293B),
                    size: 26,
                  ),
                  onPressed: () {
                    Navigator.push(
                      context,
                      MaterialPageRoute(
                        builder: (_) => const NotificacionesScreen(),
                      ),
                    );
                  },
                ),
                if (p.unreadCount > 0)
                  Positioned(
                    top: 8,
                    right: 8,
                    child: Container(
                      width: 16,
                      height: 16,
                      decoration: const BoxDecoration(
                        color: Color(0xFFEF4444),
                        shape: BoxShape.circle,
                      ),
                      child: Center(
                        child: Text(
                          '${p.unreadCount > 9 ? '9+' : p.unreadCount}',
                          style: const TextStyle(
                            color: Colors.white,
                            fontSize: 9,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                      ),
                    ),
                  ),
              ],
            ),
          ),
          const SizedBox(width: 4),
        ],
        bottom: PreferredSize(
          preferredSize: const Size.fromHeight(48),
          child: Container(
            margin: const EdgeInsets.fromLTRB(16, 0, 16, 8),
            decoration: BoxDecoration(
              color: const Color(0xFFEEF2F8),
              borderRadius: BorderRadius.circular(12),
            ),
            child: TabBar(
              controller: _tabCtrl,
              labelColor: const Color(0xFF1E293B),
              unselectedLabelColor: const Color(0xFF94A3B8),
              indicator: BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.circular(10),
                boxShadow: [
                  BoxShadow(
                    color: Colors.black.withValues(alpha: 0.08),
                    blurRadius: 4,
                    offset: const Offset(0, 1),
                  ),
                ],
              ),
              indicatorSize: TabBarIndicatorSize.tab,
              dividerColor: Colors.transparent,
              labelStyle: const TextStyle(
                fontSize: 13,
                fontWeight: FontWeight.w600,
              ),
              unselectedLabelStyle: const TextStyle(
                fontSize: 13,
                fontWeight: FontWeight.w500,
              ),
              tabs: const [
                Tab(text: 'Disponibles'),
                Tab(text: 'Mis Trámites'),
              ],
            ),
          ),
        ),
      ),
      body: Consumer<AppProvider>(
        builder: (_, provider, __) {
          if (provider.loadingData) {
            return const _LoadingView();
          }
          if (provider.dataError.isNotEmpty) {
            return _ErrorView(
              error: provider.dataError,
              onRetry: () => provider.cargarDatos(),
            );
          }
          return TabBarView(
            controller: _tabCtrl,
            children: [
              _PoliticasTab(politicas: provider.politicas),
              _TramitesTab(tramites: provider.tramites),
            ],
          );
        },
      ),
      floatingActionButton: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.end,
        children: [
          // Botón Nuevo Trámite
          Consumer<AppProvider>(
            builder: (_, p, __) {
              if (_tabCtrl.index == 1) {
                return Container(
                  width: double.infinity,
                  margin: const EdgeInsets.fromLTRB(16, 0, 16, 12),
                  child: ElevatedButton.icon(
                    onPressed: () {
                      // Acción nuevo trámite (definir)
                    },
                    icon: const Icon(Icons.add, size: 20),
                    label: const Text(
                      'Nuevo Trámite',
                      style: TextStyle(fontSize: 15, fontWeight: FontWeight.w600),
                    ),
                    style: ElevatedButton.styleFrom(
                      backgroundColor: const Color(0xFF1E293B),
                      foregroundColor: Colors.white,
                      padding: const EdgeInsets.symmetric(vertical: 14),
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(14),
                      ),
                      elevation: 0,
                    ),
                  ),
                );
              }
              return const SizedBox.shrink();
            },
          ),
          // Asistente IA FAB
          FloatingActionButton.extended(
            heroTag: 'asistente_ia_fab',
            onPressed: () {
              Navigator.push(
                context,
                MaterialPageRoute(builder: (_) => const AgenteScreen()),
              );
            },
            backgroundColor: const Color(0xFF2563EB),
            icon: const Icon(Icons.chat_bubble_outline, color: Colors.white, size: 18),
            label: const Text(
              'Asistente IA',
              style: TextStyle(
                color: Colors.white,
                fontWeight: FontWeight.bold,
                fontSize: 14,
              ),
            ),
            elevation: 4,
          ),
        ],
      ),
      floatingActionButtonLocation: FloatingActionButtonLocation.centerFloat,
    );
  }
}


// ── Tab: Políticas disponibles ────────────────────────────────
class _PoliticasTab extends StatelessWidget {
  final List<Politica> politicas;
  const _PoliticasTab({required this.politicas});

  @override
  Widget build(BuildContext context) {
    if (politicas.isEmpty) {
      return const _EmptyView(
        icon: '📋',
        title: 'Sin trámites disponibles',
        subtitle: 'No hay trámites activos en este momento.',
      );
    }
    return RefreshIndicator(
      color: const Color(0xFF2563EB),
      onRefresh: () => context.read<AppProvider>().cargarDatos(),
      child: ListView.builder(
        padding: const EdgeInsets.fromLTRB(16, 8, 16, 120),
        itemCount: politicas.length + 1,
        itemBuilder: (_, i) {
          if (i == 0) {
            return const Padding(
              padding: EdgeInsets.only(bottom: 12, top: 4),
              child: Text(
                'SERVICIOS PRIORITARIOS',
                style: TextStyle(
                  fontSize: 11,
                  fontWeight: FontWeight.w700,
                  color: Color(0xFF94A3B8),
                  letterSpacing: 0.8,
                ),
              ),
            );
          }
          return _PoliticaCard(politica: politicas[i - 1]);
        },
      ),
    );
  }
}

class _PoliticaCard extends StatelessWidget {
  final Politica politica;
  const _PoliticaCard({required this.politica});

  // Icono y color según categoría
  IconData _getIcon() {
    final cat = (politica.categoria ?? '').toLowerCase();
    if (cat.contains('medidor') || cat.contains('electric') || cat.contains('instalac')) {
      return Icons.bolt;
    }
    if (cat.contains('reconex') || cat.contains('servicio')) {
      return Icons.power;
    }
    if (cat.contains('document')) return Icons.description_outlined;
    return Icons.work_outline;
  }

  Color _getIconColor() {
    final cat = (politica.categoria ?? '').toLowerCase();
    if (cat.contains('medidor') || cat.contains('electric') || cat.contains('instalac')) {
      return const Color(0xFF2563EB);
    }
    if (cat.contains('reconex') || cat.contains('servicio')) {
      return const Color(0xFF16A34A);
    }
    return const Color(0xFF2563EB);
  }

  @override
  Widget build(BuildContext context) {
    final iconColor = _getIconColor();
    return Container(
      margin: const EdgeInsets.only(bottom: 12),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: const Color(0xFFE8EFF8)),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withValues(alpha: 0.04),
            blurRadius: 8,
            offset: const Offset(0, 2),
          ),
        ],
      ),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                // Texto principal
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        politica.nombre,
                        style: const TextStyle(
                          fontSize: 15,
                          fontWeight: FontWeight.bold,
                          color: Color(0xFF1E293B),
                        ),
                      ),
                      const SizedBox(height: 4),
                      Text(
                        politica.descripcion,
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
                const SizedBox(width: 12),
                // Icono circular
                Container(
                  width: 40,
                  height: 40,
                  decoration: BoxDecoration(
                    color: iconColor.withValues(alpha: 0.1),
                    shape: BoxShape.circle,
                  ),
                  child: Icon(_getIcon(), color: iconColor, size: 20),
                ),
              ],
            ),
            const SizedBox(height: 12),
            // Tags
            Wrap(
              spacing: 6,
              runSpacing: 4,
              children: [
                if (politica.organizacion != null)
                  _Tag(label: politica.organizacion!.toUpperCase()),
                if (politica.tiempoEstimadoDias != null)
                  _Tag(label: '~${politica.tiempoEstimadoDias} DÍAS'),
                if (politica.categoria != null)
                  _Tag(label: politica.categoria!.toUpperCase()),
              ],
            ),
            const SizedBox(height: 12),
            // CTA
            GestureDetector(
              onTap: () => _mostrarDialogoSolicitar(context),
              child: const Text(
                'INICIAR GESTIÓN →',
                style: TextStyle(
                  color: Color(0xFF2563EB),
                  fontSize: 12,
                  fontWeight: FontWeight.w700,
                  letterSpacing: 0.4,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  void _mostrarDialogoSolicitar(BuildContext context) {
    final descCtrl = TextEditingController();
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (_) => _SolicitarSheet(
        politica: politica,
        descCtrl: descCtrl,
      ),
    );
  }
}

class _Tag extends StatelessWidget {
  final String label;
  const _Tag({required this.label});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
      decoration: BoxDecoration(
        color: const Color(0xFFF1F5F9),
        borderRadius: BorderRadius.circular(6),
        border: Border.all(color: const Color(0xFFE2E8F0)),
      ),
      child: Text(
        label,
        style: const TextStyle(
          fontSize: 10,
          fontWeight: FontWeight.w600,
          color: Color(0xFF475569),
          letterSpacing: 0.3,
        ),
      ),
    );
  }
}

class _SolicitarSheet extends StatefulWidget {
  final Politica politica;
  final TextEditingController descCtrl;
  const _SolicitarSheet({required this.politica, required this.descCtrl});

  @override
  State<_SolicitarSheet> createState() => _SolicitarSheetState();
}

class _SolicitarSheetState extends State<_SolicitarSheet> {
  bool _enviando = false;
  String _error = '';

  Future<void> _solicitar() async {
    setState(() {
      _enviando = true;
      _error = '';
    });
    final provider = context.read<AppProvider>();
    final tramite = await provider.solicitarTramite(
      widget.politica.id,
      widget.descCtrl.text.trim(),
    );
    if (!mounted) return;
    setState(() => _enviando = false);
    if (tramite != null) {
      Navigator.pop(context);
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Row(
            children: [
              const Icon(Icons.check_circle, color: Colors.white, size: 18),
              const SizedBox(width: 8),
              Expanded(
                child: Text(
                  '✅ Trámite solicitado: ${tramite.refDisplay}',
                  style: const TextStyle(fontSize: 13),
                ),
              ),
            ],
          ),
          backgroundColor: const Color(0xFF16A34A),
          behavior: SnackBarBehavior.floating,
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
          duration: const Duration(seconds: 4),
        ),
      );
    } else {
      setState(() => _error = provider.actionError);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: EdgeInsets.only(
        bottom: MediaQuery.of(context).viewInsets.bottom,
      ),
      child: Container(
        decoration: const BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
        ),
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Center(
              child: Container(
                width: 40,
                height: 4,
                decoration: BoxDecoration(
                  color: const Color(0xFFE2E8F0),
                  borderRadius: BorderRadius.circular(2),
                ),
              ),
            ),
            const SizedBox(height: 20),
            Text(
              widget.politica.nombre,
              style: const TextStyle(
                fontSize: 18,
                fontWeight: FontWeight.bold,
                color: Color(0xFF1E293B),
              ),
            ),
            const SizedBox(height: 4),
            const Text(
              'Confirma tu solicitud de trámite',
              style: TextStyle(fontSize: 13, color: Color(0xFF64748B)),
            ),
            const SizedBox(height: 20),
            const Text(
              'Descripción / Motivo (opcional)',
              style: TextStyle(
                fontSize: 13,
                fontWeight: FontWeight.w500,
                color: Color(0xFF475569),
              ),
            ),
            const SizedBox(height: 8),
            TextField(
              controller: widget.descCtrl,
              maxLines: 3,
              style: const TextStyle(fontSize: 14, color: Color(0xFF1E293B)),
              decoration: const InputDecoration(
                hintText: 'Describe brevemente el motivo de tu solicitud...',
              ),
            ),
            if (_error.isNotEmpty) ...[
              const SizedBox(height: 12),
              Container(
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: const Color(0xFFFEF2F2),
                  borderRadius: BorderRadius.circular(8),
                  border: Border.all(color: const Color(0xFFFECACA)),
                ),
                child: Text(
                  '❌ $_error',
                  style: const TextStyle(
                    color: Color(0xFFDC2626),
                    fontSize: 13,
                  ),
                ),
              ),
            ],
            const SizedBox(height: 20),
            Row(
              children: [
                Expanded(
                  child: OutlinedButton(
                    onPressed: _enviando ? null : () => Navigator.pop(context),
                    style: OutlinedButton.styleFrom(
                      padding: const EdgeInsets.symmetric(vertical: 14),
                      side: const BorderSide(color: Color(0xFFE2E8F0)),
                      foregroundColor: const Color(0xFF475569),
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(10),
                      ),
                    ),
                    child: const Text('Cancelar'),
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  flex: 2,
                  child: ElevatedButton(
                    onPressed: _enviando ? null : _solicitar,
                    child: _enviando
                        ? const SizedBox(
                            width: 18,
                            height: 18,
                            child: CircularProgressIndicator(
                              color: Colors.white,
                              strokeWidth: 2,
                            ),
                          )
                        : const Text('Confirmar Solicitud'),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 8),
          ],
        ),
      ),
    );
  }
}

// ── Tab: Mis Trámites ─────────────────────────────────────────
class _TramitesTab extends StatelessWidget {
  final List<Tramite> tramites;
  const _TramitesTab({required this.tramites});

  @override
  Widget build(BuildContext context) {
    if (tramites.isEmpty) {
      return const _EmptyView(
        icon: '📂',
        title: 'Sin trámites',
        subtitle: 'Aún no has solicitado ningún trámite.\nVe a "Disponibles" para comenzar.',
      );
    }

    final activos = tramites
        .where((t) => t.estado != 'COMPLETADO' && t.estado != 'RECHAZADO')
        .toList();
    final finalizados = tramites
        .where((t) => t.estado == 'COMPLETADO' || t.estado == 'RECHAZADO')
        .toList();

    return RefreshIndicator(
      color: const Color(0xFF2563EB),
      onRefresh: () => context.read<AppProvider>().cargarDatos(),
      child: ListView(
        padding: const EdgeInsets.fromLTRB(16, 8, 16, 120),
        children: [
          if (activos.isNotEmpty) ...[
            const Padding(
              padding: EdgeInsets.only(bottom: 12, top: 4),
              child: Text(
                'TRÁMITES RECIENTES',
                style: TextStyle(
                  fontSize: 11,
                  fontWeight: FontWeight.w700,
                  color: Color(0xFF94A3B8),
                  letterSpacing: 0.8,
                ),
              ),
            ),
            ...activos.map((t) => _TramiteCard(tramite: t)),
            const SizedBox(height: 16),
          ],
          if (finalizados.isNotEmpty) ...[
            const Padding(
              padding: EdgeInsets.only(bottom: 12),
              child: Text(
                'FINALIZADOS',
                style: TextStyle(
                  fontSize: 11,
                  fontWeight: FontWeight.w700,
                  color: Color(0xFF94A3B8),
                  letterSpacing: 0.8,
                ),
              ),
            ),
            ...finalizados.map((t) => _TramiteCard(tramite: t)),
          ],
        ],
      ),
    );
  }
}

class _TramiteCard extends StatelessWidget {
  final Tramite tramite;
  const _TramiteCard({required this.tramite});

  bool get _isActive =>
      tramite.estado != 'COMPLETADO' && tramite.estado != 'RECHAZADO';

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: () => Navigator.push(
        context,
        MaterialPageRoute(
          builder: (_) => TramiteDetalleScreen(tramite: tramite),
        ),
      ),
      child: Container(
        margin: const EdgeInsets.only(bottom: 12),
        decoration: BoxDecoration(
          color: _isActive ? const Color(0xFF0F172A) : Colors.white,
          borderRadius: BorderRadius.circular(16),
          border: _isActive
              ? null
              : Border.all(color: const Color(0xFFE2E8F0)),
          boxShadow: [
            BoxShadow(
              color: Colors.black.withValues(alpha: _isActive ? 0.15 : 0.04),
              blurRadius: 12,
              offset: const Offset(0, 4),
            ),
          ],
        ),
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // Header: badge estado + ID
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                    decoration: BoxDecoration(
                      color: _isActive
                          ? const Color(0xFF1D4ED8)
                          : tramite.estadoColor.withValues(alpha: 0.12),
                      borderRadius: BorderRadius.circular(20),
                    ),
                    child: Text(
                      tramite.estadoLabel.toUpperCase(),
                      style: TextStyle(
                        color: _isActive ? Colors.white : tramite.estadoColor,
                        fontSize: 10,
                        fontWeight: FontWeight.w700,
                        letterSpacing: 0.4,
                      ),
                    ),
                  ),
                  Text(
                    'ID: ${tramite.refDisplay}',
                    style: TextStyle(
                      color: _isActive
                          ? const Color(0xFF94A3B8)
                          : const Color(0xFF94A3B8),
                      fontSize: 11,
                      fontWeight: FontWeight.w500,
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 10),
              Text(
                tramite.nombrePolitica ?? 'Trámite',
                style: TextStyle(
                  fontSize: 16,
                  fontWeight: FontWeight.bold,
                  color: _isActive ? Colors.white : const Color(0xFF1E293B),
                ),
              ),
              const SizedBox(height: 6),
              Row(
                children: [
                  Icon(
                    Icons.calendar_today_outlined,
                    size: 12,
                    color: _isActive
                        ? const Color(0xFF94A3B8)
                        : const Color(0xFF94A3B8),
                  ),
                  const SizedBox(width: 4),
                  Text(
                    'Iniciado: ${tramite.fechaInicioFormatted}',
                    style: TextStyle(
                      fontSize: 12,
                      color: _isActive
                          ? const Color(0xFF94A3B8)
                          : const Color(0xFF64748B),
                    ),
                  ),
                ],
              ),

              // Progress bar for active tramites
              if (_isActive && tramite.historial.isNotEmpty) ...[
                const SizedBox(height: 12),
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Text(
                      'Progreso',
                      style: TextStyle(
                        fontSize: 11,
                        color: _isActive
                            ? const Color(0xFF94A3B8)
                            : const Color(0xFF64748B),
                      ),
                    ),
                    Text(
                      '${((tramite.historial.length / (tramite.historial.length + 2)) * 100).round()}%',
                      style: const TextStyle(
                        fontSize: 11,
                        color: Color(0xFF94A3B8),
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 6),
                ClipRRect(
                  borderRadius: BorderRadius.circular(4),
                  child: LinearProgressIndicator(
                    value: tramite.historial.length > 0
                        ? (tramite.historial.length / (tramite.historial.length + 2))
                        : 0.1,
                    backgroundColor: const Color(0xFF1E3A5F),
                    valueColor: const AlwaysStoppedAnimation<Color>(Color(0xFF3B82F6)),
                    minHeight: 5,
                  ),
                ),
              ],
            ],
          ),
        ),
      ),
    );
  }
}

// ── Shared widgets ────────────────────────────────────────────
class _LoadingView extends StatelessWidget {
  const _LoadingView();

  @override
  Widget build(BuildContext context) {
    return const Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          CircularProgressIndicator(
            color: Color(0xFF2563EB),
            strokeWidth: 2.5,
          ),
          SizedBox(height: 16),
          Text(
            'Cargando...',
            style: TextStyle(color: Color(0xFF64748B), fontSize: 14),
          ),
        ],
      ),
    );
  }
}

class _ErrorView extends StatelessWidget {
  final String error;
  final VoidCallback onRetry;
  const _ErrorView({required this.error, required this.onRetry});

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(32),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const Text('⚠️', style: TextStyle(fontSize: 48)),
            const SizedBox(height: 16),
            Text(
              error,
              textAlign: TextAlign.center,
              style: const TextStyle(color: Color(0xFF64748B), fontSize: 14),
            ),
            const SizedBox(height: 20),
            ElevatedButton.icon(
              onPressed: onRetry,
              icon: const Icon(Icons.refresh, size: 18),
              label: const Text('Reintentar'),
            ),
          ],
        ),
      ),
    );
  }
}

class _EmptyView extends StatelessWidget {
  final String icon;
  final String title;
  final String subtitle;
  const _EmptyView({
    required this.icon,
    required this.title,
    required this.subtitle,
  });

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(32),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Text(icon, style: const TextStyle(fontSize: 56)),
            const SizedBox(height: 16),
            Text(
              title,
              style: const TextStyle(
                fontSize: 17,
                fontWeight: FontWeight.bold,
                color: Color(0xFF1E293B),
              ),
            ),
            const SizedBox(height: 8),
            Text(
              subtitle,
              textAlign: TextAlign.center,
              style: const TextStyle(
                color: Color(0xFF64748B),
                fontSize: 13,
                height: 1.5,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
