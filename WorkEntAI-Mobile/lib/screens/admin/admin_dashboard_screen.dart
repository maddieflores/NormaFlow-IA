import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../services/app_provider.dart';
import '../login/login_screen.dart';

class AdminDashboardScreen extends StatefulWidget {
  final VoidCallback onLogout;
  const AdminDashboardScreen({super.key, required this.onLogout});

  @override
  State<AdminDashboardScreen> createState() => _AdminDashboardScreenState();
}

class _AdminDashboardScreenState extends State<AdminDashboardScreen> {
  int _currentIndex = 0;

  String _saludo() {
    final h = DateTime.now().hour;
    if (h < 12) return 'Buenos días';
    if (h < 19) return 'Buenas tardes';
    return 'Buenas noches';
  }

  @override
  Widget build(BuildContext context) {
    final tabs = [
      _ResumenTab(saludo: _saludo()),
      const _KpisTab(),
      const _UsuariosTab(),
      const _CuellosBotellTab(),
      const _PoliticasTab(),
    ];

    return Scaffold(
      backgroundColor: Colors.white,
      body: tabs[_currentIndex],
      bottomNavigationBar: _buildBottomNav(),
    );
  }

  Widget _buildBottomNav() {
    const items = [
      _NavItem(Icons.grid_view_rounded, 'Resumen'),
      _NavItem(Icons.show_chart_rounded, 'KPIs'),
      _NavItem(Icons.person_outline_rounded, 'Usuarios'),
      _NavItem(Icons.warning_amber_rounded, 'Cuellos de botella'),
      _NavItem(Icons.description_outlined, 'Políticas'),
    ];

    return Container(
      decoration: const BoxDecoration(
        color: Colors.white,
        border: Border(top: BorderSide(color: Color(0xFFE2E8F0))),
      ),
      child: SafeArea(
        child: SizedBox(
          height: 60,
          child: Row(
            children: List.generate(items.length, (i) {
              final selected = _currentIndex == i;
              return Expanded(
                child: GestureDetector(
                  onTap: () => setState(() => _currentIndex = i),
                  behavior: HitTestBehavior.opaque,
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Icon(
                        items[i].icon,
                        size: 22,
                        color: selected ? const Color(0xFF0F172A) : const Color(0xFF94A3B8),
                      ),
                      const SizedBox(height: 3),
                      Text(
                        items[i].label,
                        textAlign: TextAlign.center,
                        maxLines: 2,
                        style: TextStyle(
                          fontSize: 9,
                          height: 1.1,
                          fontWeight: selected ? FontWeight.w700 : FontWeight.w400,
                          color: selected ? const Color(0xFF0F172A) : const Color(0xFF94A3B8),
                        ),
                      ),
                      if (selected)
                        Container(
                          margin: const EdgeInsets.only(top: 2),
                          width: 20,
                          height: 2,
                          decoration: BoxDecoration(
                            color: const Color(0xFF0F172A),
                            borderRadius: BorderRadius.circular(1),
                          ),
                        ),
                    ],
                  ),
                ),
              );
            }),
          ),
        ),
      ),
    );
  }
}

// ── Shared header ─────────────────────────────────────────────────────────────
class _AdminHeader extends StatelessWidget {
  final VoidCallback onLogout;
  const _AdminHeader({required this.onLogout});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(20, 0, 16, 0),
      child: Row(
        children: [
          Container(
            width: 38,
            height: 38,
            decoration: BoxDecoration(
              color: const Color(0xFF0F172A),
              borderRadius: BorderRadius.circular(10),
            ),
            child: const Center(
              child: Icon(Icons.hub_rounded, color: Colors.white, size: 20),
            ),
          ),
          const SizedBox(width: 10),
          const Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text('NormalFlow',
                  style: TextStyle(fontSize: 15, fontWeight: FontWeight.bold, color: Color(0xFF0F172A))),
              Text('ADMIN',
                  style: TextStyle(fontSize: 10, color: Color(0xFF2563EB), fontWeight: FontWeight.w700, letterSpacing: 1)),
            ],
          ),
          const Spacer(),
          IconButton(
            icon: const Icon(Icons.settings_outlined, color: Color(0xFF64748B), size: 22),
            onPressed: () {},
          ),
          IconButton(
            icon: const Icon(Icons.logout_rounded, color: Color(0xFF64748B), size: 22),
            onPressed: onLogout,
          ),
        ],
      ),
    );
  }
}

// ── Nav item model ─────────────────────────────────────────────────────────────
class _NavItem {
  final IconData icon;
  final String label;
  const _NavItem(this.icon, this.label);
}

// ══════════════════════════════════════════════════════════════════════════════
// TAB 1: RESUMEN
// ══════════════════════════════════════════════════════════════════════════════
class _ResumenTab extends StatelessWidget {
  final String saludo;
  const _ResumenTab({required this.saludo});

  static const _activities = [
    _ActivityItem('Nuevo trámite iniciado por Empresa ABC', 'Hace 5 min · Área Comercial', 'NUEVO'),
    _ActivityItem('Cuello de botella resuelto en Finanzas', 'Hace 2 horas · por María Torres', 'RESUELTO'),
    _ActivityItem('Funcionario Ana López completó 5 tareas', 'Hace 4 horas · Departamento técnico', 'PROGRESO'),
    _ActivityItem('Política de vacaciones actualizada', 'Ayer · versión 2.3', 'SISTEMA'),
  ];

  @override
  Widget build(BuildContext context) {
    return Consumer<AppProvider>(
      builder: (_, provider, __) {
        return SafeArea(
          child: ListView(
            children: [
              const SizedBox(height: 12),
              _AdminHeader(onLogout: () async {
                await provider.logout();
                if (!context.mounted) return;
                Navigator.of(context).pushAndRemoveUntil(
                  MaterialPageRoute(builder: (_) => const LoginScreen()),
                  (_) => false,
                );
              }),
              const SizedBox(height: 20),
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 20),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const SizedBox(height: 4),
                    RichText(
                      text: TextSpan(
                        style: const TextStyle(fontSize: 26, color: Color(0xFF0F172A), height: 1.2),
                        children: [
                          TextSpan(text: '$saludo,\n', style: const TextStyle(fontWeight: FontWeight.bold)),
                          TextSpan(
                            text: '${provider.userNombre.split(' ').first}.',
                            style: const TextStyle(
                              fontStyle: FontStyle.italic,
                              fontWeight: FontWeight.bold,
                              color: Color(0xFF0F172A),
                            ),
                          ),
                        ],
                      ),
                    ),
                    const SizedBox(height: 8),
                    const Text(
                      'Un vistazo diario a los trámites, el equipo y los focos que requieren tu atención.',
                      style: TextStyle(fontSize: 13, color: Color(0xFF64748B), height: 1.4),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 24),
              const Divider(height: 1, color: Color(0xFFE2E8F0), indent: 20, endIndent: 20),
              const SizedBox(height: 20),

              // ── Trámites Totales (full width) ────────────────────────────
              const Padding(
                padding: EdgeInsets.symmetric(horizontal: 20),
                child: _BigStatCard(
                  label: 'TRÁMITES TOTALES',
                  value: '1,284',
                  icon: Icons.folder_open_outlined,
                  subtitle: '+12.4% vs mes pasado',
                  subtitleColor: Color(0xFF16A34A),
                ),
              ),
              const SizedBox(height: 12),

              // ── Usuarios Activos + Cuellos ────────────────────────────────
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 20),
                child: Row(
                  children: [
                    Expanded(
                      child: _SmallStatCard(
                        label: 'USUARIOS\nACTIVOS',
                        value: '${provider.tramites.isNotEmpty ? 14 : 14}',
                        icon: Icons.people_alt_outlined,
                        subtitle: '3 nuevos esta semana',
                        subtitleColor: const Color(0xFF64748B),
                      ),
                    ),
                    const SizedBox(width: 12),
                    const Expanded(
                      child: _SmallStatCard(
                        label: 'CUELLOS\nDETECTADOS',
                        value: '3',
                        icon: Icons.warning_amber_outlined,
                        subtitle: 'Requieren revisión',
                        subtitleColor: Color(0xFFEF4444),
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 12),

              // ── Tasa de éxito (full width) ────────────────────────────────
              const Padding(
                padding: EdgeInsets.symmetric(horizontal: 20),
                child: _BigStatCard(
                  label: 'TASA DE ÉXITO',
                  value: '98%',
                  icon: Icons.trending_up_rounded,
                  subtitle: 'Sobre 1,258 trámites',
                  subtitleColor: Color(0xFF64748B),
                ),
              ),
              const SizedBox(height: 12),

              // ── Tiempo Promedio + Completados Hoy ─────────────────────────
              const Padding(
                padding: EdgeInsets.symmetric(horizontal: 20),
                child: Row(
                  children: [
                    Expanded(
                      child: _SmallStatCard(
                        label: 'TIEMPO\nPROMEDIO',
                        value: '2.4d',
                        icon: Icons.timer_outlined,
                        subtitle: '-0.6d vs mes pasado',
                        subtitleColor: Color(0xFF16A34A),
                      ),
                    ),
                    SizedBox(width: 12),
                    Expanded(
                      child: _SmallStatCard(
                        label: 'COMPLETADOS\nHOY',
                        value: '47',
                        icon: Icons.check_circle_outline_rounded,
                        subtitle: '7 en la última hora',
                        subtitleColor: Color(0xFF64748B),
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 28),

              // ── Bitácora ──────────────────────────────────────────────────
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 20),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text('BITÁCORA',
                        style: TextStyle(fontSize: 11, color: Colors.grey[400], letterSpacing: 2, fontWeight: FontWeight.w600)),
                    const SizedBox(height: 4),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        const Text('Actividad reciente',
                            style: TextStyle(fontSize: 22, fontWeight: FontWeight.bold, color: Color(0xFF0F172A))),
                        TextButton(
                          onPressed: () {},
                          child: const Text('VER TODO',
                              style: TextStyle(fontSize: 11, fontWeight: FontWeight.w700,
                                  color: Color(0xFF64748B), letterSpacing: 1)),
                        ),
                      ],
                    ),
                    const SizedBox(height: 4),
                    const Divider(color: Color(0xFFE2E8F0)),
                  ],
                ),
              ),

              ...List.generate(_activities.length, (i) {
                final a = _activities[i];
                return _ActivityRow(index: i + 1, item: a);
              }),
              const SizedBox(height: 24),
            ],
          ),
        );
      },
    );
  }
}

// ── Stat card widgets ─────────────────────────────────────────────────────────
class _BigStatCard extends StatelessWidget {
  final String label;
  final String value;
  final IconData icon;
  final String subtitle;
  final Color subtitleColor;

  const _BigStatCard({
    required this.label,
    required this.value,
    required this.icon,
    required this.subtitle,
    required this.subtitleColor,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: const Color(0xFFE2E8F0)),
        boxShadow: [BoxShadow(color: Colors.black.withValues(alpha: 0.03), blurRadius: 8, offset: const Offset(0, 2))],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Container(
                width: 34,
                height: 34,
                decoration: BoxDecoration(
                  color: const Color(0xFFF1F5F9),
                  borderRadius: BorderRadius.circular(8),
                ),
                child: Icon(icon, size: 18, color: const Color(0xFF64748B)),
              ),
              Text(label,
                  style: const TextStyle(fontSize: 10, letterSpacing: 1.5, fontWeight: FontWeight.w700, color: Color(0xFF94A3B8))),
            ],
          ),
          const SizedBox(height: 12),
          Text(value,
              style: const TextStyle(fontSize: 40, fontWeight: FontWeight.bold, color: Color(0xFF0F172A), height: 1)),
          const SizedBox(height: 6),
          Text(subtitle, style: TextStyle(fontSize: 12, color: subtitleColor, fontWeight: FontWeight.w500)),
        ],
      ),
    );
  }
}

class _SmallStatCard extends StatelessWidget {
  final String label;
  final String value;
  final IconData icon;
  final String subtitle;
  final Color subtitleColor;

  const _SmallStatCard({
    required this.label,
    required this.value,
    required this.icon,
    required this.subtitle,
    required this.subtitleColor,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: const Color(0xFFE2E8F0)),
        boxShadow: [BoxShadow(color: Colors.black.withValues(alpha: 0.03), blurRadius: 8, offset: const Offset(0, 2))],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Container(
                width: 28,
                height: 28,
                decoration: BoxDecoration(
                  color: const Color(0xFFF1F5F9),
                  borderRadius: BorderRadius.circular(7),
                ),
                child: Icon(icon, size: 15, color: const Color(0xFF64748B)),
              ),
              const SizedBox(width: 6),
              Expanded(
                child: Text(label,
                    style: const TextStyle(fontSize: 9, letterSpacing: 1.2, fontWeight: FontWeight.w700, color: Color(0xFF94A3B8))),
              ),
            ],
          ),
          const SizedBox(height: 10),
          Text(value,
              style: const TextStyle(fontSize: 30, fontWeight: FontWeight.bold, color: Color(0xFF0F172A), height: 1)),
          const SizedBox(height: 4),
          Text(subtitle, style: TextStyle(fontSize: 11, color: subtitleColor, fontWeight: FontWeight.w500)),
        ],
      ),
    );
  }
}

// ── Activity item ─────────────────────────────────────────────────────────────
class _ActivityItem {
  final String title;
  final String meta;
  final String badge;
  const _ActivityItem(this.title, this.meta, this.badge);
}

class _ActivityRow extends StatelessWidget {
  final int index;
  final _ActivityItem item;
  const _ActivityRow({required this.index, required this.item});

  Color get _badgeBg {
    switch (item.badge) {
      case 'NUEVO': return const Color(0xFF0F172A);
      case 'RESUELTO': return const Color(0xFFE2E8F0);
      case 'SISTEMA': return const Color(0xFF0F172A);
      default: return const Color(0xFFE2E8F0);
    }
  }

  Color get _badgeFg {
    switch (item.badge) {
      case 'NUEVO': return Colors.white;
      case 'SISTEMA': return Colors.white;
      default: return const Color(0xFF64748B);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 0),
      child: Column(
        children: [
          Padding(
            padding: const EdgeInsets.symmetric(vertical: 14),
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  index.toString().padLeft(2, '0'),
                  style: const TextStyle(fontSize: 20, fontWeight: FontWeight.bold,
                      color: Color(0xFFCBD5E1), height: 1.1),
                ),
                const SizedBox(width: 14),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(item.title,
                          style: const TextStyle(fontSize: 13, fontWeight: FontWeight.w600,
                              color: Color(0xFF2563EB), height: 1.3)),
                      const SizedBox(height: 3),
                      Text(item.meta,
                          style: const TextStyle(fontSize: 11, color: Color(0xFF94A3B8))),
                    ],
                  ),
                ),
                const SizedBox(width: 10),
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                  decoration: BoxDecoration(
                    color: _badgeBg,
                    borderRadius: BorderRadius.circular(6),
                  ),
                  child: Text(item.badge,
                      style: TextStyle(fontSize: 9, fontWeight: FontWeight.w700,
                          color: _badgeFg, letterSpacing: 0.5)),
                ),
              ],
            ),
          ),
          const Divider(height: 1, color: Color(0xFFE2E8F0)),
        ],
      ),
    );
  }
}

// ══════════════════════════════════════════════════════════════════════════════
// TAB 2: KPIs
// ══════════════════════════════════════════════════════════════════════════════
class _KpisTab extends StatelessWidget {
  const _KpisTab();

  @override
  Widget build(BuildContext context) {
    return Consumer<AppProvider>(
      builder: (_, provider, __) {
        final tareas = provider.tareas;
        final completadas = provider.tareasCompletadas;
        final pendientes = provider.tareasPendientes;
        final enProceso = provider.tareasEnProceso;
        final tasaCompletado = tareas.isEmpty ? 0.0 : (completadas.length / tareas.length) * 100;

        return SafeArea(
          child: ListView(
            children: [
              const SizedBox(height: 12),
              _AdminHeader(onLogout: () async {
                await provider.logout();
                if (!context.mounted) return;
                Navigator.of(context).pushAndRemoveUntil(
                  MaterialPageRoute(builder: (_) => const LoginScreen()),
                  (_) => false,
                );
              }),
              const SizedBox(height: 20),
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 20),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    RichText(
                      text: const TextSpan(
                        style: TextStyle(fontSize: 26, color: Color(0xFF0F172A), height: 1.2),
                        children: [
                          TextSpan(text: 'Indicadores ', style: TextStyle(fontWeight: FontWeight.bold)),
                          TextSpan(text: 'clave', style: TextStyle(fontStyle: FontStyle.italic, fontWeight: FontWeight.bold)),
                        ],
                      ),
                    ),
                    const SizedBox(height: 8),
                    const Text(
                      'Métricas de eficiencia agregadas y por área.\nActualizado hace 4 minutos.',
                      style: TextStyle(fontSize: 13, color: Color(0xFF64748B), height: 1.4),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 24),
              const Divider(height: 1, color: Color(0xFFE2E8F0), indent: 20, endIndent: 20),
              const SizedBox(height: 20),

              // ── Estado de Tareas ──────────────────────────────────────────
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 20),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text('RESUMEN GENERAL',
                        style: TextStyle(fontSize: 11, color: Colors.grey[400], letterSpacing: 2, fontWeight: FontWeight.w600)),
                    const SizedBox(height: 4),
                    const Text('Estado de tareas',
                        style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold, color: Color(0xFF0F172A))),
                    const SizedBox(height: 16),
                    Row(
                      children: [
                        Expanded(child: _KpiCard(icon: Icons.list_alt_outlined, value: '${tareas.length}', label: 'TOTAL TAREAS')),
                        const SizedBox(width: 12),
                        Expanded(child: _KpiCard(icon: Icons.check_circle_outline, value: '${completadas.length}', label: 'COMPLETADAS')),
                      ],
                    ),
                    const SizedBox(height: 12),
                    Row(
                      children: [
                        Expanded(child: _KpiCard(icon: Icons.pending_outlined, value: '${pendientes.length}', label: 'PENDIENTES')),
                        const SizedBox(width: 12),
                        Expanded(child: _KpiCard(icon: Icons.hourglass_empty_outlined, value: '${enProceso.length}', label: 'EN PROCESO')),
                      ],
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 28),

              // ── Eficiencia ────────────────────────────────────────────────
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 20),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text('EFICIENCIA',
                        style: TextStyle(fontSize: 11, color: Colors.grey[400], letterSpacing: 2, fontWeight: FontWeight.w600)),
                    const SizedBox(height: 4),
                    const Text('Rendimiento del equipo',
                        style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold, color: Color(0xFF0F172A))),
                    const SizedBox(height: 16),

                    // Tasa completado
                    Container(
                      padding: const EdgeInsets.all(16),
                      decoration: BoxDecoration(
                        color: Colors.white,
                        borderRadius: BorderRadius.circular(14),
                        border: Border.all(color: const Color(0xFFE2E8F0)),
                      ),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          const Row(
                            mainAxisAlignment: MainAxisAlignment.spaceBetween,
                            children: [
                              Text('TASA COMPLETADO',
                                  style: TextStyle(fontSize: 10, letterSpacing: 1.5, fontWeight: FontWeight.w700, color: Color(0xFF94A3B8))),
                              Icon(Icons.trending_up_rounded, color: Color(0xFF64748B), size: 18),
                            ],
                          ),
                          const SizedBox(height: 10),
                          Text('${tasaCompletado.toStringAsFixed(1)}%',
                              style: const TextStyle(fontSize: 34, fontWeight: FontWeight.bold, color: Color(0xFF0F172A))),
                          const SizedBox(height: 8),
                          LinearProgressIndicator(
                            value: tasaCompletado / 100,
                            backgroundColor: const Color(0xFFE2E8F0),
                            color: const Color(0xFF0F172A),
                            minHeight: 4,
                            borderRadius: BorderRadius.circular(2),
                          ),
                        ],
                      ),
                    ),
                    const SizedBox(height: 12),

                    // Prom. duración
                    Container(
                      padding: const EdgeInsets.all(16),
                      decoration: BoxDecoration(
                        color: Colors.white,
                        borderRadius: BorderRadius.circular(14),
                        border: Border.all(color: const Color(0xFFE2E8F0)),
                      ),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          const Row(
                            mainAxisAlignment: MainAxisAlignment.spaceBetween,
                            children: [
                              Text('PROM. DURACIÓN',
                                  style: TextStyle(fontSize: 10, letterSpacing: 1.5, fontWeight: FontWeight.w700, color: Color(0xFF94A3B8))),
                              Icon(Icons.timer_outlined, color: Color(0xFF64748B), size: 18),
                            ],
                          ),
                          const SizedBox(height: 10),
                          Container(height: 3, width: 60, decoration: BoxDecoration(color: const Color(0xFF0F172A), borderRadius: BorderRadius.circular(2))),
                          const SizedBox(height: 10),
                          const Text('Aún no hay datos suficientes.',
                              style: TextStyle(fontSize: 12, color: Color(0xFF94A3B8))),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 28),

              // ── Tareas en curso ───────────────────────────────────────────
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 20),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text('PROGRESO',
                        style: TextStyle(fontSize: 11, color: Colors.grey[400], letterSpacing: 2, fontWeight: FontWeight.w600)),
                    const SizedBox(height: 4),
                    const Text('Tareas en curso',
                        style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold, color: Color(0xFF0F172A))),
                    const SizedBox(height: 16),
                    if (enProceso.isEmpty)
                      Container(
                        width: double.infinity,
                        padding: const EdgeInsets.symmetric(vertical: 40),
                        decoration: BoxDecoration(
                          color: Colors.white,
                          borderRadius: BorderRadius.circular(14),
                          border: Border.all(color: const Color(0xFFE2E8F0)),
                        ),
                        child: const Column(
                          children: [
                            Icon(Icons.assignment_outlined, size: 48, color: Color(0xFFCBD5E1)),
                            SizedBox(height: 12),
                            Text('Sin tareas aún', style: TextStyle(fontSize: 15, fontWeight: FontWeight.w600, color: Color(0xFF94A3B8))),
                          ],
                        ),
                      )
                    else
                      ...enProceso.map((t) => Container(
                        margin: const EdgeInsets.only(bottom: 10),
                        padding: const EdgeInsets.all(14),
                        decoration: BoxDecoration(
                          color: Colors.white,
                          borderRadius: BorderRadius.circular(12),
                          border: Border.all(color: const Color(0xFFE2E8F0)),
                        ),
                        child: Text(t.nombreNodo ?? t.instrucciones ?? 'Tarea en proceso',
                            style: const TextStyle(fontSize: 13, color: Color(0xFF1E293B))),
                      )),
                  ],
                ),
              ),
              const SizedBox(height: 24),
            ],
          ),
        );
      },
    );
  }
}

class _KpiCard extends StatelessWidget {
  final IconData icon;
  final String value;
  final String label;
  const _KpiCard({required this.icon, required this.value, required this.label});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: const Color(0xFFE2E8F0)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(icon, size: 22, color: const Color(0xFF94A3B8)),
          const SizedBox(height: 10),
          Text(value,
              style: const TextStyle(fontSize: 30, fontWeight: FontWeight.bold, color: Color(0xFF0F172A), height: 1)),
          const SizedBox(height: 6),
          Text(label,
              style: const TextStyle(fontSize: 9, letterSpacing: 1.5, fontWeight: FontWeight.w700, color: Color(0xFF94A3B8))),
        ],
      ),
    );
  }
}

// ══════════════════════════════════════════════════════════════════════════════
// TAB 3: USUARIOS
// ══════════════════════════════════════════════════════════════════════════════
class _UsuariosTab extends StatelessWidget {
  const _UsuariosTab();

  static const _users = [
    {'nombre': 'Carlos Mendoza', 'rol': 'ADMIN', 'email': 'admin@normalflow.com'},
    {'nombre': 'Juan Pérez', 'rol': 'FUNCIONARIO', 'email': 'juanp@normalflow.com'},
    {'nombre': 'Ana López', 'rol': 'FUNCIONARIO', 'email': 'anal@normalflow.com'},
    {'nombre': 'María Torres', 'rol': 'FUNCIONARIO', 'email': 'maríat@normalflow.com'},
    {'nombre': 'Empresa ABC S.R.L.', 'rol': 'CLIENTE', 'email': 'cliente@abc.com'},
  ];

  Color _roleColor(String rol) {
    switch (rol) {
      case 'ADMIN': return const Color(0xFF7C3AED);
      case 'FUNCIONARIO': return const Color(0xFF2563EB);
      default: return const Color(0xFFF59E0B);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Consumer<AppProvider>(builder: (_, provider, __) {
      return SafeArea(
        child: Column(
          children: [
            const SizedBox(height: 12),
            _AdminHeader(onLogout: () async {
              await provider.logout();
              if (!context.mounted) return;
              Navigator.of(context).pushAndRemoveUntil(
                MaterialPageRoute(builder: (_) => const LoginScreen()),
                (_) => false,
              );
            }),
            const SizedBox(height: 16),
            const Padding(
              padding: EdgeInsets.symmetric(horizontal: 20),
              child: Align(
                alignment: Alignment.centerLeft,
                child: Text('Usuarios del sistema',
                    style: TextStyle(fontSize: 22, fontWeight: FontWeight.bold, color: Color(0xFF0F172A))),
              ),
            ),
            const SizedBox(height: 12),
            const Divider(height: 1, color: Color(0xFFE2E8F0)),
            Expanded(
              child: ListView.separated(
                padding: const EdgeInsets.all(16),
                itemCount: _users.length,
                separatorBuilder: (_, __) => const SizedBox(height: 10),
                itemBuilder: (_, i) {
                  final u = _users[i];
                  final c = _roleColor(u['rol']!);
                  return Container(
                    padding: const EdgeInsets.all(14),
                    decoration: BoxDecoration(
                      color: Colors.white,
                      borderRadius: BorderRadius.circular(14),
                      border: Border.all(color: const Color(0xFFE2E8F0)),
                    ),
                    child: Row(
                      children: [
                        CircleAvatar(
                          radius: 20,
                          backgroundColor: c.withValues(alpha: 0.1),
                          child: Text(u['nombre']![0], style: TextStyle(color: c, fontWeight: FontWeight.bold)),
                        ),
                        const SizedBox(width: 12),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(u['nombre']!, style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 14, color: Color(0xFF0F172A))),
                              Text(u['email']!, style: const TextStyle(fontSize: 11, color: Color(0xFF94A3B8))),
                            ],
                          ),
                        ),
                        Container(
                          padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                          decoration: BoxDecoration(color: c.withValues(alpha: 0.1), borderRadius: BorderRadius.circular(6)),
                          child: Text(u['rol']!, style: TextStyle(fontSize: 10, color: c, fontWeight: FontWeight.w700)),
                        ),
                      ],
                    ),
                  );
                },
              ),
            ),
          ],
        ),
      );
    });
  }
}

// ══════════════════════════════════════════════════════════════════════════════
// TAB 4: CUELLOS DE BOTELLA
// ══════════════════════════════════════════════════════════════════════════════
class _CuellosBotellTab extends StatelessWidget {
  const _CuellosBotellTab();

  @override
  Widget build(BuildContext context) {
    return Consumer<AppProvider>(builder: (_, provider, __) {
      return SafeArea(
        child: Column(
          children: [
            const SizedBox(height: 12),
            _AdminHeader(onLogout: () async {
              await provider.logout();
              if (!context.mounted) return;
              Navigator.of(context).pushAndRemoveUntil(
                MaterialPageRoute(builder: (_) => const LoginScreen()),
                (_) => false,
              );
            }),
            const SizedBox(height: 16),
            const Padding(
              padding: EdgeInsets.symmetric(horizontal: 20),
              child: Align(
                alignment: Alignment.centerLeft,
                child: Text('Cuellos de botella',
                    style: TextStyle(fontSize: 22, fontWeight: FontWeight.bold, color: Color(0xFF0F172A))),
              ),
            ),
            const SizedBox(height: 12),
            const Divider(height: 1, color: Color(0xFFE2E8F0)),
            Expanded(
              child: ListView(
                padding: const EdgeInsets.all(16),
                children: [
                  Container(
                    padding: const EdgeInsets.all(14),
                    decoration: BoxDecoration(
                      color: const Color(0xFFFFFBEB),
                      borderRadius: BorderRadius.circular(12),
                      border: Border.all(color: const Color(0xFFFCD34D)),
                    ),
                    child: const Row(
                      children: [
                        Icon(Icons.auto_graph, color: Color(0xFFD97706), size: 28),
                        SizedBox(width: 12),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text('Motor de IA Analítico', style: TextStyle(fontWeight: FontWeight.bold, color: Color(0xFF92400E))),
                              Text('Analizando tiempos de ejecución mediante LSTM.',
                                  style: TextStyle(fontSize: 12, color: Color(0xFFB45309))),
                            ],
                          ),
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 16),
                  _buildAnomalia('Trámite de Importación', 'Departamento Legal', '3 días retraso', 0.85),
                  _buildAnomalia('Solicitud de Vacaciones', 'Departamento Finanzas', '1 día retraso', 0.65),
                ],
              ),
            ),
          ],
        ),
      );
    });
  }

  Widget _buildAnomalia(String tramite, String depto, String retraso, double conf) {
    return Container(
      margin: const EdgeInsets.only(bottom: 14),
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: const Color(0xFFEF4444), width: 1.5),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Row(
            children: [
              Icon(Icons.warning_rounded, color: Color(0xFFEF4444), size: 18),
              SizedBox(width: 6),
              Text('Cuello detectado', style: TextStyle(color: Color(0xFFEF4444), fontWeight: FontWeight.bold, fontSize: 13)),
            ],
          ),
          const Divider(height: 20),
          Text(tramite, style: const TextStyle(fontSize: 15, fontWeight: FontWeight.bold, color: Color(0xFF0F172A))),
          const SizedBox(height: 4),
          Text('Atascado en: $depto', style: const TextStyle(fontSize: 12, color: Color(0xFF64748B))),
          const SizedBox(height: 12),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                decoration: BoxDecoration(color: const Color(0xFFFEE2E2), borderRadius: BorderRadius.circular(6)),
                child: Text(retraso, style: const TextStyle(color: Color(0xFFB91C1C), fontSize: 12, fontWeight: FontWeight.bold)),
              ),
              Text('Confianza IA: ${(conf * 100).toInt()}%', style: const TextStyle(color: Color(0xFF64748B), fontSize: 12)),
            ],
          ),
          const SizedBox(height: 12),
          SizedBox(
            width: double.infinity,
            child: ElevatedButton.icon(
              onPressed: () {},
              icon: const Icon(Icons.flash_on_rounded, size: 16),
              label: const Text('Reasignar tareas'),
              style: ElevatedButton.styleFrom(
                backgroundColor: const Color(0xFF0F172A),
                foregroundColor: Colors.white,
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

// ══════════════════════════════════════════════════════════════════════════════
// TAB 5: POLÍTICAS
// ══════════════════════════════════════════════════════════════════════════════
class _PoliticasTab extends StatelessWidget {
  const _PoliticasTab();

  @override
  Widget build(BuildContext context) {
    return Consumer<AppProvider>(builder: (_, provider, __) {
      return SafeArea(
        child: Column(
          children: [
            const SizedBox(height: 12),
            _AdminHeader(onLogout: () async {
              await provider.logout();
              if (!context.mounted) return;
              Navigator.of(context).pushAndRemoveUntil(
                MaterialPageRoute(builder: (_) => const LoginScreen()),
                (_) => false,
              );
            }),
            const SizedBox(height: 16),
            const Divider(height: 1, color: Color(0xFFE2E8F0)),
            Expanded(
              child: SingleChildScrollView(
                child: Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 32),
                  child: Column(
                    children: [
                      const SizedBox(height: 40),

                      // —— Ícono ——————————————————————————————————————
                      Container(
                        width: 72,
                        height: 72,
                        decoration: BoxDecoration(
                          color: const Color(0xFFF1F5F9),
                          borderRadius: BorderRadius.circular(18),
                        ),
                        child: const Icon(Icons.hub_rounded, size: 36, color: Color(0xFF64748B)),
                      ),
                      const SizedBox(height: 28),

                      // —— Título ————————————————————————————————————
                      const Text(
                        'Políticas y\ntrámites',
                        textAlign: TextAlign.center,
                        style: TextStyle(
                          fontSize: 30,
                          fontWeight: FontWeight.bold,
                          color: Color(0xFF0F172A),
                          height: 1.15,
                        ),
                      ),
                      const SizedBox(height: 16),

                      // —— Descripción ——————————————————————————————
                      const Text(
                        'Administra los flujos de trabajo, roles y reglas de aprobación desde la versión Web. Aquí puedes sincronizar los cambios recientes.',
                        textAlign: TextAlign.center,
                        style: TextStyle(
                          fontSize: 13,
                          color: Color(0xFF64748B),
                          height: 1.55,
                        ),
                      ),
                      const SizedBox(height: 32),

                      // —— Botón Sincronizar ——————————————————————————
                      SizedBox(
                        width: double.infinity,
                        child: ElevatedButton.icon(
                          onPressed: () {},
                          icon: const Icon(Icons.sync_rounded, size: 18),
                          label: const Text(
                            'Sincronizar políticas',
                            style: TextStyle(fontWeight: FontWeight.w600, fontSize: 15),
                          ),
                          style: ElevatedButton.styleFrom(
                            backgroundColor: const Color(0xFF0F172A),
                            foregroundColor: Colors.white,
                            padding: const EdgeInsets.symmetric(vertical: 14),
                            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                            elevation: 0,
                          ),
                        ),
                      ),
                      const SizedBox(height: 10),

                      // —— Botón Abrir en Web ———————————————————————
                      SizedBox(
                        width: double.infinity,
                        child: OutlinedButton(
                          onPressed: () {},
                          style: OutlinedButton.styleFrom(
                            foregroundColor: const Color(0xFF0F172A),
                            side: const BorderSide(color: Color(0xFFE2E8F0), width: 1.5),
                            padding: const EdgeInsets.symmetric(vertical: 14),
                            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                          ),
                          child: const Text(
                            'Abrir en Web',
                            style: TextStyle(fontWeight: FontWeight.w600, fontSize: 15),
                          ),
                        ),
                      ),
                      const SizedBox(height: 36),

                      // —— Stats: Flujos / Reglas / Versiones ————————————
                      Container(
                        padding: const EdgeInsets.symmetric(vertical: 20, horizontal: 12),
                        decoration: BoxDecoration(
                          border: Border.all(color: const Color(0xFFE2E8F0)),
                          borderRadius: BorderRadius.circular(14),
                        ),
                        child: const Row(
                          mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                          children: [
                            _PoliticaStat(label: 'FLUJOS', value: '12'),
                            _StatDivider(),
                            _PoliticaStat(label: 'REGLAS', value: '48'),
                            _StatDivider(),
                            _PoliticaStat(label: 'VERSIONES', value: '2.3'),
                          ],
                        ),
                      ),
                      const SizedBox(height: 32),
                    ],
                  ),
                ),
              ),
            ),
          ],
        ),
      );
    });
  }
}

class _PoliticaStat extends StatelessWidget {
  final String label;
  final String value;
  const _PoliticaStat({required this.label, required this.value});

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          label,
          style: const TextStyle(
            fontSize: 10,
            letterSpacing: 1.5,
            fontWeight: FontWeight.w700,
            color: Color(0xFF2563EB),
          ),
        ),
        const SizedBox(height: 4),
        Text(
          value,
          style: const TextStyle(
            fontSize: 28,
            fontWeight: FontWeight.bold,
            color: Color(0xFF0F172A),
          ),
        ),
      ],
    );
  }
}

class _StatDivider extends StatelessWidget {
  const _StatDivider();
  @override
  Widget build(BuildContext context) {
    return Container(width: 1, height: 40, color: const Color(0xFFE2E8F0));
  }
}
