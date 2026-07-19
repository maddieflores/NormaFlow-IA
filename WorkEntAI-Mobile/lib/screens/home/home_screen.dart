import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../services/app_provider.dart';
import '../../services/notification_service.dart';
import '../cliente_portal/cliente_portal_screen.dart';
import '../notificaciones/notificaciones_screen.dart';
import '../perfil/perfil_screen.dart';
import '../login/login_screen.dart';
import '../tramite_detalle/tramite_detalle_screen.dart';

import '../dashboard/dashboard_screen.dart';
import '../admin/admin_dashboard_screen.dart';
import '../tarea_detalle/tarea_detalle_screen.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  int _currentIndex = 0;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      context.read<AppProvider>().cargarDatos();
      _registrarNavHandler();
    });
  }

  void _registrarNavHandler() {
    NotificationService.setNavigationHandler((tipo, referenciaId) {
      if (!mounted) return;
      switch (tipo) {
        case 'NUEVA_TAREA':
        case 'TAREA_ASIGNADA':
          // Navegar a notificaciones como pantalla separada
          Navigator.push(
            context,
            MaterialPageRoute(builder: (_) => const NotificacionesScreen()),
          );
          break;
        case 'TRAMITE_COMPLETADO':
        case 'TRAMITE_ACTUALIZADO':
        case 'TAREA_COMPLETADA':
          if (referenciaId.isNotEmpty) {
            Navigator.of(context).push(
              MaterialPageRoute(
                builder: (_) => TramiteDetalleScreen.byId(tramiteId: referenciaId),
              ),
            );
          }
          break;
        default:
          Navigator.push(
            context,
            MaterialPageRoute(builder: (_) => const NotificacionesScreen()),
          );
      }
    });
  }

  @override
  void dispose() {
    NotificationService.clearNavigationHandler();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Consumer<AppProvider>(
      builder: (_, provider, __) {
        final rol = provider.userRol;
        if (rol == 'ADMIN') {
          return AdminDashboardScreen(
            onLogout: () async {
              if (mounted) {
                Navigator.of(context).pushReplacement(
                  MaterialPageRoute(builder: (_) => const LoginScreen()),
                );
              }
            },
          );
        }

        if (rol == 'FUNCIONARIO') {
          return DashboardScreen(
            onTareaTap: (tarea) {
              Navigator.push(
                context,
                MaterialPageRoute(
                  builder: (_) => TareaDetalleScreen(
                    tarea: tarea,
                    onCompletado: () {
                      Navigator.pop(context);
                      context.read<AppProvider>().cargarDatos();
                    },
                  ),
                ),
              );
            },
            onLogout: () async {
              if (mounted) {
                Navigator.of(context).pushReplacement(
                  MaterialPageRoute(builder: (_) => const LoginScreen()),
                );
              }
            },
          );
        }

        // ── CLIENTE: solo 2 tabs (Trámites, Perfil) ──────────────
        final pages = [
          const ClientePortalScreen(),
          const PerfilScreen(),
        ];

        return Scaffold(
          body: IndexedStack(
            index: _currentIndex,
            children: pages,
          ),
          bottomNavigationBar: Container(
            decoration: const BoxDecoration(
              border: Border(
                top: BorderSide(color: Color(0xFFE2E8F0)),
              ),
            ),
            child: BottomNavigationBar(
              currentIndex: _currentIndex,
              onTap: (i) => setState(() => _currentIndex = i),
              backgroundColor: Colors.white,
              selectedItemColor: const Color(0xFF2563EB),
              unselectedItemColor: const Color(0xFF94A3B8),
              selectedLabelStyle: const TextStyle(
                fontSize: 11,
                fontWeight: FontWeight.w600,
              ),
              unselectedLabelStyle: const TextStyle(fontSize: 11),
              elevation: 0,
              items: const [
                BottomNavigationBarItem(
                  icon: Icon(Icons.description_outlined),
                  activeIcon: Icon(Icons.description),
                  label: 'Trámites',
                ),
                BottomNavigationBarItem(
                  icon: Icon(Icons.person_outline),
                  activeIcon: Icon(Icons.person),
                  label: 'Perfil',
                ),
              ],
            ),
          ),
        );
      },
    );
  }
}
