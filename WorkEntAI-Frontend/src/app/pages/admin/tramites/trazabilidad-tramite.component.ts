import { Component, OnInit, inject } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { TramiteService } from '../../../services/tramite/tramite.service';
import { PoliticaService } from '../../../services/politica/politica.service';
import { AuthService } from '../../../services/auth/auth.service';
import { Tramite, Politica, Nodo } from '../../../models/models';
import { SidebarComponent, NavItem, ADMIN_NAV_ITEMS } from '../../../components/sidebar/sidebar.component';

interface TimelineEvent {
  title: string;
  subtitle: string;
  fecha: any;
}

@Component({
  selector: 'app-trazabilidad-tramite',
  standalone: true,
  imports: [CommonModule, DatePipe, SidebarComponent],
  template: `
    <div class="app-layout">
      <app-sidebar [activeRoute]="'/admin/tramites'" [navItems]="navItems" />
      <main class="main-content" style="padding: 40px; background: #f8fafc; min-height: 100vh; flex: 1; color: #1e293b;">
        
        <!-- Modern Header Navbar -->
        <div class="collab-header glass-panel" style="margin-left: -40px; margin-right: -40px; margin-top: -40px; margin-bottom: 32px; border-bottom: 1px solid #cbd5e1;">
          <div class="header-left">
            <button class="btn-back" (click)="volver()">
              <span class="icon">⬅</span> Volver
            </button>
            <div class="header-titles">
              <h1>Monitoreo y trazabilidad</h1>
              <span class="badge-notas">Trámite #{{ tramiteId }}</span>
            </div>
          </div>
          <div class="header-right">
            <button 
              style="background: white; border: 1px solid #cbd5e1; color: #1e293b; padding: 10px 18px; border-radius: 8px; font-weight: 600; cursor: pointer; display: flex; align-items: center; gap: 8px; transition: all 0.2s;"
              (click)="cargarDatos()" [disabled]="loading">
              🔄 Actualizar
            </button>
          </div>
        </div>

        @if (loading) {
          <div style="display: flex; flex-direction: column; align-items: center; justify-content: center; min-height: 300px; gap: 12px;">
            <div style="width: 32px; height: 32px; border: 3px solid rgba(37, 99, 235, 0.1); border-top-color: #2563eb; border-radius: 50%; animation: spin 0.8s linear infinite;"></div>
            <p style="color: #64748b; font-size: 14px;">Cargando monitoreo del trámite...</p>
          </div>
        } @else if (error) {
          <div style="background: #fef2f2; border: 1px solid #fca5a5; color: #b91c1c; padding: 16px; border-radius: 12px; font-weight: 500;">
            {{ error }}
          </div>
        } @else if (tramite) {
          
          <!-- CARDS GRID -->
          <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 20px; margin-bottom: 28px;">
            
            <!-- Card 1: Estado -->
            <div style="background: white; border: 1px solid #e2e8f0; border-radius: 16px; padding: 20px; box-shadow: 0 1px 3px 0 rgba(0,0,0,0.05);">
              <span style="display: block; font-size: 12px; font-weight: 600; color: #64748b; text-transform: uppercase; margin-bottom: 8px;">Estado actual</span>
              <span style="display: inline-block; padding: 6px 12px; border-radius: 20px; font-size: 13px; font-weight: 700; background: #e0f2fe; color: #0369a1;">
                {{ tramite.estado }}
              </span>
            </div>

            <!-- Card 2: Politica -->
            <div style="background: white; border: 1px solid #e2e8f0; border-radius: 16px; padding: 20px; box-shadow: 0 1px 3px 0 rgba(0,0,0,0.05);">
              <span style="display: block; font-size: 12px; font-weight: 600; color: #64748b; text-transform: uppercase; margin-bottom: 8px;">Política</span>
              <span style="display: block; font-size: 15px; font-weight: 700; color: #0f172a; word-break: break-all;">
                {{ tramite.nombrePolitica || tramite.politicaId }}
              </span>
            </div>

            <!-- Card 3: Responsable -->
            <div style="background: white; border: 1px solid #e2e8f0; border-radius: 16px; padding: 20px; box-shadow: 0 1px 3px 0 rgba(0,0,0,0.05);">
              <span style="display: block; font-size: 12px; font-weight: 600; color: #64748b; text-transform: uppercase; margin-bottom: 8px;">Responsable actual</span>
              <span style="display: block; font-size: 15px; font-weight: 700; color: #0f172a;">
                {{ tramite.departamentoActual || 'SISTEMA' }}
              </span>
            </div>

            <!-- Card 4: Actividad -->
            <div style="background: white; border: 1px solid #e2e8f0; border-radius: 16px; padding: 20px; box-shadow: 0 1px 3px 0 rgba(0,0,0,0.05);">
              <span style="display: block; font-size: 12px; font-weight: 600; color: #64748b; text-transform: uppercase; margin-bottom: 8px;">Actividad actual</span>
              <span style="display: block; font-size: 15px; font-weight: 700; color: #0f172a;">
                {{ tramite.nombreNodoActual || 'Fin del proceso' }}
              </span>
            </div>

          </div>

          <!-- ACTIVITIES GRID -->
          <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); gap: 24px; margin-bottom: 28px;">
            
            <!-- Completadas -->
            <div style="background: white; border: 1px solid #e2e8f0; border-radius: 16px; padding: 24px; box-shadow: 0 1px 3px 0 rgba(0,0,0,0.05);">
              <h3 style="font-size: 16px; font-weight: 700; color: #0f172a; margin: 0 0 16px;">Actividades completadas</h3>
              <div style="display: flex; flex-direction: column; gap: 12px;">
                @for (h of tramite.historial; track h.nodoId) {
                  <div class="activity-completed" style="border-radius: 12px; padding: 14px; display: flex; flex-direction: column; gap: 4px;">
                    <span style="font-weight: 700; font-size: 14px;">{{ h.nombreNodo }}</span>
                    <span style="font-size: 12px; opacity: 0.85;">
                      {{ h.nodoId }} · {{ h.fecha | date:'M/d/yy, h:mm a' }}
                    </span>
                  </div>
                }
                @if (tramite.historial.length === 0) {
                  <p style="color: #64748b; font-size: 14px; margin: 0;">Ninguna actividad completada aún.</p>
                }
              </div>
            </div>

            <!-- Pendientes -->
            <div style="background: white; border: 1px solid #e2e8f0; border-radius: 16px; padding: 24px; box-shadow: 0 1px 3px 0 rgba(0,0,0,0.05);">
              <h3 style="font-size: 16px; font-weight: 700; color: #0f172a; margin: 0 0 16px;">Actividades pendientes</h3>
              <div style="display: flex; flex-direction: column; gap: 12px;">
                @for (n of pendingActivities; track n.id) {
                  <div class="activity-pending" style="border-radius: 12px; padding: 14px; display: flex; flex-direction: column; gap: 4px;">
                    <span style="font-weight: 700; font-size: 14px;">{{ n.nombre }}</span>
                    <span style="font-size: 12px;">
                      {{ n.id }} · Departamento: {{ n.departamento || 'No asignado' }}
                    </span>
                  </div>
                }
                @if (pendingActivities.length === 0) {
                  <p style="color: #64748b; font-size: 14px; margin: 0;">No hay actividades pendientes.</p>
                }
              </div>
            </div>

          </div>

          <!-- TIMELINE -->
          <div style="background: white; border: 1px solid #e2e8f0; border-radius: 16px; padding: 24px; box-shadow: 0 1px 3px 0 rgba(0,0,0,0.05);">
            <h3 style="font-size: 16px; font-weight: 700; color: #0f172a; margin: 0 0 20px;">Línea de tiempo del trámite</h3>
            
            <div style="position: relative; padding-left: 24px; border-left: 2px solid #e2e8f0; display: flex; flex-direction: column; gap: 24px; margin-left: 10px;">
              @for (event of timelineEvents; track event.fecha; let last = $last) {
                <div style="position: relative;">
                  <!-- Dot -->
                  <div style="position: absolute; left: -31px; top: 4px; width: 12px; height: 12px; border-radius: 50%; background: #10b981; border: 3px solid white; box-shadow: 0 0 0 2px #10b981;"></div>
                  
                  <!-- Content -->
                  <div style="display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; flex-wrap: wrap;">
                    <div>
                      <h4 style="font-size: 15px; font-weight: 700; color: #0f172a; margin: 0 0 4px;">{{ event.title }}</h4>
                      <p style="font-size: 13px; color: #64748b; margin: 0;">{{ event.subtitle }}</p>
                    </div>
                    <span style="font-size: 13px; color: #64748b; font-family: monospace; white-space: nowrap;">
                      {{ event.fecha | date:'yyyy-MM-dd HH:mm' }}
                    </span>
                  </div>
                </div>
              }
            </div>
          </div>

        }

      </main>
    </div>

    <style>
      @keyframes spin {
        to { transform: rotate(360deg); }
      }
      
      /* Header styles copy from editor */
      .collab-header { 
        display: flex; justify-content: space-between; align-items: center; 
        padding: 16px 24px; background: rgba(255, 255, 255, 0.95); 
        backdrop-filter: blur(10px); border-bottom: 1px solid #cbd5e1; z-index: 50; 
        box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05);
      }
      
      .header-left, .header-right { display: flex; align-items: center; gap: 16px; min-width: 250px; }
      .header-right { justify-content: flex-end; gap: 12px; }
      .header-center { flex: 1; display: flex; justify-content: center; }
      
      .btn-back {
        display: flex; align-items: center; gap: 8px;
        padding: 8px 16px; background: white; border: 1px solid #cbd5e1;
        border-radius: 8px; font-weight: 600; color: #475569;
        cursor: pointer; transition: all 0.2s;
      }
      .btn-back:hover { background: #f8fafc; border-color: #94a3b8; color: #1e293b; }
      
      .header-titles h1 { font-size: 18px; font-weight: 700; color: #0f172a; margin: 0; }
      .badge-notas { font-size: 11px; background: #e0f2fe; color: #0369a1; padding: 2px 8px; border-radius: 4px; font-weight: 600; margin-top: 2px; display: inline-block; }
    </style>
  `
})
export class TrazabilidadTramiteComponent implements OnInit {
  tramiteId = '';
  tramite: Tramite | null = null;
  politica: Politica | null = null;
  pendingActivities: Nodo[] = [];
  timelineEvents: TimelineEvent[] = [];

  loading = false;
  error = '';

  navItems: NavItem[] = [
    { icon: '🏠', label: 'Portal', route: '/cliente' },
    { icon: '👤', label: 'Mi Perfil', route: '/perfil' },
  ];

  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private tramiteService = inject(TramiteService);
  private politicaService = inject(PoliticaService);
  private authService = inject(AuthService);

  ngOnInit(): void {
    this.tramiteId = this.route.snapshot.paramMap.get('id') || '';
    const user = this.authService.getUser();
    if (user) {
      if (user.rol === 'ADMIN') {
        this.navItems = ADMIN_NAV_ITEMS;
      }
    }
    if (this.tramiteId) {
      this.cargarDatos();
    } else {
      this.error = 'No se proporcionó el ID del trámite';
    }
  }

  cargarDatos(): void {
    this.loading = true;
    this.error = '';

    this.tramiteService.getById(this.tramiteId).subscribe({
      next: (t) => {
        this.tramite = t;

        // Cargar política para ver flujos
        this.politicaService.getById(t.politicaId).subscribe({
          next: (p) => {
            this.politica = p;
            this.procesarFlujosYTimeline(t, p);
            this.loading = false;
          },
          error: (err) => {
            this.loading = false;
            this.procesarFlujosYTimeline(t, null);
          }
        });
      },
      error: (err) => {
        this.error = 'Error al cargar el trámite';
        this.loading = false;
      }
    });
  }

  procesarFlujosYTimeline(t: Tramite, p: Politica | null): void {
    // 1. Calcular actividades pendientes
    this.pendingActivities = [];
    if (p && p.nodos) {
      const historialMap = new Map(t.historial.map(h => [h.nodoId, h]));
      this.pendingActivities = p.nodos.filter(
        n => (n.tipo === 'TASK' || n.tipo === 'DECISION') &&
          !historialMap.has(n.id) &&
          n.id !== t.nodoActualId &&
          t.estado !== 'COMPLETADO'
      );
    }

    // 2. Construir la línea de tiempo de eventos
    const events: TimelineEvent[] = [];

    // Primer evento: Creación del trámite
    events.push({
      title: 'Trámite creado',
      subtitle: 'Proceso creado',
      fecha: t.fechaInicio
    });

    // Agregar eventos del historial
    if (t.historial && t.historial.length > 0) {
      t.historial.forEach((h) => {
        let actionDesc = 'Paso completado';
        if (h.accion === 'RECHAZADO') actionDesc = 'Paso rechazado';
        if (h.accion === 'INICIADO') actionDesc = 'Paso iniciado';

        events.push({
          title: h.nombreNodo,
          subtitle: `${actionDesc} en departamento ${h.departamento || ''} por ${h.nombreFuncionario || 'Sistema'}`,
          fecha: h.fecha
        });
      });
    }

    // Agregar evento actual si está en proceso
    if (t.estado === 'EN_PROCESO' && t.nombreNodoActual) {
      events.push({
        title: 'Tarea asignada',
        subtitle: `Tarea asignada a departamento ${t.departamentoActual || ''} (${t.nombreNodoActual})`,
        fecha: t.fechaUltimaActualizacion || t.fechaInicio
      });
    }

    // Ordenar los eventos por fecha
    events.sort((a, b) => new Date(a.fecha).getTime() - new Date(b.fecha).getTime());

    this.timelineEvents = events;
  }

  volver(): void {
    const user = this.authService.getUser();
    if (user?.rol === 'ADMIN') {
      this.router.navigate(['/admin/tramites']);
    } else {
      this.router.navigate(['/cliente']);
    }
  }
}
