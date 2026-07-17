import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription, forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs';
import { Politica, Tramite } from '../../models/models';
import { TramiteService } from '../../services/tramite/tramite.service';
import { PoliticaService } from '../../services/politica/politica.service';
import { WebSocketService } from '../../services/websocket/websocket.service';
import { AuthService } from '../../services/auth/auth.service';
import { SidebarComponent, NavItem, ADMIN_NAV_ITEMS } from '../../components/sidebar/sidebar.component';

import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-tramite-detalle',
  standalone: true,
  imports: [CommonModule, DatePipe, SidebarComponent, RouterModule],
  template: `
    <div class="app-layout">
      <app-sidebar [activeRoute]="'/cliente'" [navItems]="navItems" />
      <main class="main-content" style="padding: 40px; background: #f8fafc; min-height: 100vh; flex: 1; color: #1e293b;">

        <!-- Modern Header Navbar -->
        <div class="collab-header glass-panel" style="margin-left: -40px; margin-right: -40px; margin-top: -40px; margin-bottom: 32px; border-bottom: 1px solid #cbd5e1;">
          <div class="header-left">
            <button class="btn-back" (click)="volverAlPortal()">
              <span class="icon">⬅</span> Volver
            </button>
            <div class="header-titles">
              <h1>Trámite {{ tramite?.numeroReferencia || '...' }}</h1>
              <span class="badge-notas">{{ tramite?.nombrePolitica }}</span>
            </div>
          </div>
          <div class="header-right">
            @if (tramite?.estado === 'COMPLETADO') {
              <button style="background: white; border: 1px solid #cbd5e1; color: #1e293b; padding: 10px 16px; border-radius: 8px; font-weight: 600; cursor: pointer; display: flex; align-items: center; gap: 8px; transition: all 0.2s;"
                      (click)="descargarPdf()">
                📄 Descargar PDF
              </button>
            }
            <button style="background: #2563eb; border: none; color: white; padding: 10px 16px; border-radius: 8px; font-weight: 600; cursor: pointer; display: flex; align-items: center; gap: 8px; transition: all 0.2s;"
                    [routerLink]="['/tramite', tramite?.id, 'documentos']">
              📂 Ver Documentos
            </button>
          </div>
        </div>

        @if (loading) {
          <div style="display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 60px; gap: 12px; background: white; border-radius: 12px; border: 1px solid #e2e8f0;">
            <div style="width: 32px; height: 32px; border: 3px solid rgba(37, 99, 235, 0.1); border-top-color: #2563eb; border-radius: 50%; animation: spin 0.8s linear infinite;"></div>
            <p style="color: #64748b; font-size: 14px;">Cargando trámite...</p>
          </div>
        }

        @if (!loading && tramite) {
          <!-- Estado actual -->
          <div style="background: white; border-radius: 12px; padding: 20px; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.05); border: 1px solid #e2e8f0; margin-bottom: 24px; display: flex; align-items: center; gap: 16px;">
            <div [class.icon-nuevo]="tramite.estado === 'NUEVO'"
                 [class.icon-proceso]="tramite.estado === 'EN_PROCESO'"
                 [class.icon-completado]="tramite.estado === 'COMPLETADO'"
                 [class.icon-rechazado]="tramite.estado === 'RECHAZADO'"
                 style="width: 48px; height: 48px; border-radius: 10px; display: flex; align-items: center; justify-content: center; font-size: 20px; flex-shrink: 0;">
              {{ getEstadoIcon(tramite.estado) }}
            </div>
            <div>
              <h2 style="font-size: 16px; font-weight: 700; color: #0f172a; margin: 0 0 4px 0;">{{ getEstadoLabel(tramite.estado) }}</h2>
              <p style="font-size: 13px; color: #64748b; margin: 0;">
                @if (tramite.estado === 'EN_PROCESO') {
                  Actualmente en: <strong>{{ tramite.nombreNodoActual }}</strong> — Departamento: <strong>{{ tramite.departamentoActual }}</strong>
                } @else if (tramite.estado === 'COMPLETADO') {
                  Trámite finalizado el {{ tramite.fechaFin | date:'dd/MM/yyyy HH:mm' }}
                } @else if (tramite.estado === 'NUEVO') {
                  Solicitud recibida, pendiente de revisión por el administrador
                } @else {
                  Trámite rechazado
                }
              </p>
            </div>
            <span class="badge ms-auto"
                  [class.badge-nuevo]="tramite.estado === 'NUEVO'"
                  [class.badge-proceso]="tramite.estado === 'EN_PROCESO'"
                  [class.badge-completado]="tramite.estado === 'COMPLETADO'"
                  [class.badge-rechazado]="tramite.estado === 'RECHAZADO'"
                  style="padding: 6px 12px; border-radius: 20px; font-size: 12px; font-weight: 700; text-transform: uppercase;">
              {{ tramite.estado }}
            </span>
          </div>

          <!-- Progreso visual completo -->
          <div style="background: white; border-radius: 12px; padding: 24px; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.05); border: 1px solid #e2e8f0; margin-bottom: 24px;">
            <h3 style="font-size: 15px; font-weight: 700; color: #0f172a; margin: 0 0 20px 0; display: flex; align-items: center; gap: 8px;">
              📍 Progreso del Trámite
            </h3>
            <div class="progreso-steps">
              @for (paso of pasos; track paso.nodoId; let i = $index) {
                <div class="paso" [class.paso-completado]="paso.completado"
                     [class.paso-actual]="paso.actual"
                     [class.paso-pendiente]="!paso.completado && !paso.actual"
                     [class.paso-especial]="paso.esEspecial">
                  <div class="paso-circle">
                    @if (paso.completado) { ✓ }
                    @else if (paso.actual) { ● }
                    @else if (paso.esEspecial) { ⋯ }
                    @else { {{ i + 1 }} }
                  </div>
                  <div class="paso-info">
                    <p class="paso-nombre" style="color: #0f172a;">{{ paso.nombre }}</p>
                    <p class="paso-dept" style="color: #64748b;">{{ paso.departamento }}</p>
                    @if (paso.fecha) {
                      <p class="paso-fecha" style="color: #94a3b8;">{{ paso.fecha | date:'dd/MM HH:mm' }}</p>
                    }
                    @if (paso.duracionMinutos) {
                      <p class="paso-dur" style="color: #2563eb; font-weight: 600;">{{ paso.duracionMinutos }} min</p>
                    }
                  </div>
                  @if (i < pasos.length - 1) {
                    <div class="paso-linea" [class.linea-completada]="paso.completado"
                         [class.linea-actual]="paso.actual"></div>
                  }
                </div>
              }
            </div>
            @if (pasos.length === 0) {
              <div style="text-align: center; color: #64748b; padding: 20px; font-size: 14px;">El trámite aún no tiene etapas definidas</div>
            }
          </div>

          <!-- Info general y Historial -->
          <div class="info-grid" style="display: grid; grid-template-columns: 1fr 1fr; gap: 24px; margin-bottom: 24px;">
            
            <!-- Información General -->
            <div style="background: white; border-radius: 12px; padding: 24px; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.05); border: 1px solid #e2e8f0;">
              <h3 style="font-size: 15px; font-weight: 700; color: #0f172a; margin: 0 0 20px 0; display: flex; align-items: center; gap: 8px;">
                📋 Información General
              </h3>
              <div class="info-list">
                <div class="info-row" style="border-bottom: 1px solid #f1f5f9; padding-bottom: 12px; margin-bottom: 12px;">
                  <span class="info-label">Referencia</span>
                  <span class="info-value mono" style="font-weight: 600; color: #0f172a;">{{ tramite.numeroReferencia }}</span>
                </div>
                <div class="info-row" style="border-bottom: 1px solid #f1f5f9; padding-bottom: 12px; margin-bottom: 12px;">
                  <span class="info-label">Política</span>
                  <span class="info-value" style="color: #334155;">{{ tramite.nombrePolitica }}</span>
                </div>
                <div class="info-row" style="border-bottom: 1px solid #f1f5f9; padding-bottom: 12px; margin-bottom: 12px;">
                  <span class="info-label">Descripción</span>
                  <span class="info-value" style="color: #475569; max-width: 250px; text-align: right; word-break: break-word;">{{ tramite.descripcion || '—' }}</span>
                </div>
                <div class="info-row" style="border-bottom: 1px solid #f1f5f9; padding-bottom: 12px; margin-bottom: 12px;">
                  <span class="info-label">Prioridad</span>
                  <span class="info-value">
                    <span class="badge"
                          [class.badge-nuevo]="tramite.prioridad === 'ALTA'"
                          [class.badge-proceso]="tramite.prioridad === 'MEDIA'"
                          [class.badge-completado]="tramite.prioridad === 'BAJA'"
                          style="padding: 4px 8px; border-radius: 12px; font-size: 11px; font-weight: 700;">
                      {{ tramite.prioridad }}
                    </span>
                  </span>
                </div>
                <div class="info-row" style="border-bottom: 1px solid #f1f5f9; padding-bottom: 12px; margin-bottom: 12px;">
                  <span class="info-label">Fecha inicio</span>
                  <span class="info-value mono" style="color: #334155;">{{ tramite.fechaInicio | date:'dd/MM/yyyy HH:mm' }}</span>
                </div>
                @if (tramite.fechaFin) {
                  <div class="info-row" style="border-bottom: 1px solid #f1f5f9; padding-bottom: 12px; margin-bottom: 12px;">
                    <span class="info-label">Fecha fin</span>
                    <span class="info-value mono" style="color: #334155;">{{ tramite.fechaFin | date:'dd/MM/yyyy HH:mm' }}</span>
                  </div>
                  <div class="info-row">
                    <span class="info-label">Duración total</span>
                    <span class="info-value" style="color: #334155; font-weight: 600;">{{ formatDuracion(tramite.duracionMinutos) }}</span>
                  </div>
                }
              </div>
            </div>

            <!-- Historial de Pasos -->
            <div style="background: white; border-radius: 12px; padding: 24px; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.05); border: 1px solid #e2e8f0;">
              <h3 style="font-size: 15px; font-weight: 700; color: #0f172a; margin: 0 0 20px 0; display: flex; align-items: center; gap: 8px;">
                📜 Historial de Pasos
              </h3>
              @if (tramite.historial.length === 0) {
                <div style="text-align: center; color: #64748b; padding: 20px; font-size: 14px;">El trámite aún no ha iniciado</div>
              } @else {
                <div class="historial-list">
                  @for (h of tramite.historial; track h.nodoId) {
                    <div class="historial-item" style="border-bottom: 1px solid #f1f5f9; padding-bottom: 12px; margin-bottom: 12px;">
                      <div class="historial-dot" [class.dot-completado]="h.accion === 'COMPLETADO'"
                           [class.dot-rechazado]="h.accion === 'RECHAZADO'"
                           style="margin-top: 6px;"></div>
                      <div class="historial-content">
                        <p class="historial-nodo" style="color: #0f172a; font-weight: 600; margin-bottom: 2px;">{{ h.nombreNodo }}</p>
                        <p class="historial-meta" style="color: #64748b; font-size: 12px; margin-bottom: 4px;">
                          {{ h.departamento }} · {{ h.nombreFuncionario || 'Sistema' }}
                          @if (h.duracionMinutos) { · {{ h.duracionMinutos }} min }
                        </p>
                        @if (h.observacion) {
                          <p class="historial-obs" style="color: #475569; font-size: 13px; font-style: italic; background: #f8fafc; padding: 8px 12px; border-radius: 8px; margin: 4px 0;">
                            "{{ h.observacion }}"
                          </p>
                        }
                        @if (h.resultadoDecision) {
                          <span class="badge" [class.badge-completado]="h.resultadoDecision === 'APROBADO'"
                                [class.badge-nuevo]="h.resultadoDecision === 'RECHAZADO'"
                                style="padding: 4px 8px; border-radius: 12px; font-size: 11px; font-weight: 700; margin-top: 4px; display: inline-block;">
                            {{ h.resultadoDecision }}
                          </span>
                        }
                        <p class="historial-fecha" style="color: #94a3b8; font-size: 11px; margin-top: 4px;">{{ h.fecha | date:'dd/MM/yyyy HH:mm' }}</p>
                      </div>
                    </div>
                  }
                </div>
              }
            </div>

          </div>

          <!-- Datos recopilados -->
          @if (tramite.estado === 'COMPLETADO' && tramite.datosFormulario && objectKeys(tramite.datosFormulario).length > 0) {
            <div style="background: white; border-radius: 12px; padding: 24px; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.05); border: 1px solid #e2e8f0; margin-bottom: 24px;">
              <h3 style="font-size: 15px; font-weight: 700; color: #0f172a; margin: 0 0 20px 0; display: flex; align-items: center; gap: 8px;">
                📝 Datos Recopilados
              </h3>
              <div class="datos-grid" style="display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap: 16px;">
                @for (key of objectKeys(tramite.datosFormulario); track key) {
                  <div class="dato-item" style="background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px; padding: 14px;">
                    <span class="dato-key" style="display: block; font-size: 11px; color: #64748b; font-weight: 600; text-transform: uppercase; margin-bottom: 4px;">{{ key }}</span>
                    <span class="dato-val" style="font-size: 14px; font-weight: 600; color: #0f172a;">{{ tramite.datosFormulario![key] }}</span>
                  </div>
                }
              </div>
            </div>
          }

        }

      </main>
    </div>
  `,
  styles: [`
    .ms-auto { margin-left: auto; }

    .estado-card { margin-bottom: 20px; }
    .estado-header { display: flex; align-items: center; gap: 16px; flex-wrap: wrap; }
    .estado-icon {
      width: 52px; height: 52px; border-radius: 14px;
      display: flex; align-items: center; justify-content: center;
      font-size: 24px; flex-shrink: 0;
    }
    .icon-nuevo      { background: hsl(355,80%,55%,0.12); }
    .icon-proceso    { background: hsl(20,89%,48%,0.12); }
    .icon-completado { background: hsl(142,60%,38%,0.12); }
    .icon-rechazado  { background: hsl(220,15%,65%,0.12); }

    /* Progreso */
    .progreso-steps {
      display: flex; align-items: flex-start; gap: 0;
      overflow-x: auto; padding-bottom: 8px;
      scrollbar-width: thin;
    }
    .paso {
      display: flex; flex-direction: column; align-items: center;
      min-width: 110px; max-width: 140px; position: relative; flex: 1;
    }
    .paso-circle {
      width: 36px; height: 36px; border-radius: 50%;
      display: flex; align-items: center; justify-content: center;
      font-size: 14px; font-weight: 700; border: 2px solid #e2e8f0;
      background: #f8fafc; color: #64748b; z-index: 1;
      transition: all 0.35s cubic-bezier(.4,0,.2,1);
    }
    .paso-completado .paso-circle {
      background: #10b981; border-color: #10b981; color: white;
      box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.18);
    }
    .paso-actual .paso-circle {
      background: #2563eb; border-color: #2563eb; color: white;
      box-shadow: 0 0 0 4px rgba(37, 99, 235, 0.22);
    }
    .paso-especial .paso-circle {
      background: #fafafa; border-color: #f59e0b;
      color: #f59e0b;
    }
    .paso-info { text-align: center; margin-top: 8px; }
    .paso-nombre { font-size: 11px; font-weight: 600; margin: 0 0 2px; }
    .paso-dept   { font-size: 10px; color: #64748b; margin: 0; }
    .paso-fecha  { font-size: 10px; color: #94a3b8; margin: 2px 0 0; }
    .paso-dur    { font-size: 10px; color: #2563eb; margin: 0; }
    .paso-linea {
      position: absolute; top: 18px; left: 50%; width: 100%;
      height: 2px; background: #e2e8f0; z-index: 0;
      transition: background 0.35s;
    }
    .linea-completada { background: #10b981 !important; }
    .linea-actual     { background: linear-gradient(90deg, #10b981 0%, #2563eb 100%) !important; }

    /* Info grid */
    .info-list { display: flex; flex-direction: column; }
    .info-row { display: flex; justify-content: space-between; align-items: flex-start; gap: 12px; }
    .info-label { font-size: 12px; color: #64748b; flex-shrink: 0; }
    .info-value { font-size: 13px; color: #0f172a; text-align: right; }

    /* Historial */
    .historial-list { display: flex; flex-direction: column; }
    .historial-item { display: flex; gap: 12px; }
    .historial-dot {
      width: 10px; height: 10px; border-radius: 50%;
      flex-shrink: 0; margin-top: 4px;
    }
    .dot-completado { background: #10b981; }
    .dot-rechazado  { background: #ef4444; }
    .historial-content { flex: 1; }

    @media (max-width: 900px) { .info-grid { grid-template-columns: 1fr !important; } }

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
  `]
})
export class TramiteDetalleComponent implements OnInit, OnDestroy {
  tramite: Tramite | null = null;
  politica: Politica | null = null;
  loading = true;
  pasos: PasoProgreso[] = [];
  objectKeys = Object.keys;

  navItems: NavItem[] = [
    { icon: '🏠', label: 'Portal', route: '/cliente' },
    { icon: '👤', label: 'Mi Perfil', route: '/perfil' },
  ];

  private wsSub!: Subscription;

  constructor(
    private route: ActivatedRoute,
    private tramiteService: TramiteService,
    private politicaService: PoliticaService,
    private wsService: WebSocketService,
    private authService: AuthService,
    public router: Router
  ) { }

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id')!;
    this.cargarTramite(id);
    const user = this.authService.getUser();
    if (user) {
      if (user.rol === 'ADMIN') {
        this.navItems = ADMIN_NAV_ITEMS;
      }
      this.wsService.conectar(user.id, user.rol, user.departamento);
      this.wsSub = this.wsService.notificaciones$.subscribe(() => this.cargarTramite(id));
    }
  }

  volverAlPortal(): void {
    const user = this.authService.getUser();
    if (user?.rol === 'ADMIN') {
      this.router.navigate(['/admin/tramites']);
    } else {
      this.router.navigate(['/cliente']);
    }
  }

  cargarTramite(id: string): void {
    this.tramiteService.getById(id).pipe(catchError(() => of(null))).subscribe(t => {
      this.tramite = t;
      if (t) {
        // Cargar la politica para obtener TODOS los nodos del flujo (CU-15)
        this.politicaService.getById(t.politicaId)
          .pipe(catchError(() => of(null)))
          .subscribe(p => {
            this.politica = p;
            this.construirPasos(t, p);
          });
      }
      this.loading = false;
    });
  }

  /**
   * Construye la lista de pasos del progreso visual (CU-15).
   *
   * Si se dispone de la Política, usa sus nodos para mostrar el flujo COMPLETO
   * (completados + actual + pendientes). Si no, solo muestra los completados del historial.
   *
   * Se filtran nodos START/END/PARALLEL/JOIN para mostrar únicamente
   * las etapas de trabajo significativas (TASK y DECISION).
   */
  construirPasos(t: Tramite, p: Politica | null): void {
    // Mapa de historial por nodoId para acceso O(1)
    const historialMap = new Map(t.historial.map(h => [h.nodoId, h]));

    if (p?.nodos?.length) {
      // Ordenar nodos de trabajo (excluir START/END/PARALLEL/JOIN)
      const nodosTrabajo = p.nodos.filter(
        n => n.tipo === 'TASK' || n.tipo === 'DECISION'
      );

      this.pasos = nodosTrabajo.map(nodo => {
        const hist = historialMap.get(nodo.id);
        const esActual = nodo.id === t.nodoActualId;
        return {
          nodoId: nodo.id,
          nombre: nodo.nombre,
          departamento: nodo.departamento || '',
          completado: !!hist,
          actual: esActual && !hist,
          esEspecial: nodo.tipo === 'DECISION',
          fecha: hist?.fecha ?? null,
          duracionMinutos: hist?.duracionMinutos ?? null
        } as PasoProgreso;
      });
    } else {
      // Fallback: solo historial completado + nodo actual
      const completados = t.historial.map(h => ({
        nodoId: h.nodoId, nombre: h.nombreNodo,
        departamento: h.departamento || '', completado: true,
        actual: false, esEspecial: false,
        fecha: h.fecha, duracionMinutos: h.duracionMinutos ?? null
      } as PasoProgreso));

      const actual: PasoProgreso[] = t.estado === 'EN_PROCESO' ? [{
        nodoId: t.nodoActualId, nombre: t.nombreNodoActual || 'En proceso',
        departamento: t.departamentoActual || '', completado: false,
        actual: true, esEspecial: false, fecha: null, duracionMinutos: null
      }] : [];

      this.pasos = [...completados, ...actual];
    }
  }

  descargarPdf(): void {
    if (!this.tramite) return;
    this.tramiteService.descargarPdf(this.tramite.id).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `tramite-${this.tramite!.numeroReferencia}.pdf`;
        a.click();
        URL.revokeObjectURL(url);
      },
      error: () => alert('Error al descargar el PDF')
    });
  }

  getEstadoIcon(estado: string): string {
    return { NUEVO: '🔴', EN_PROCESO: '🟡', COMPLETADO: '🟢', RECHAZADO: '⚫' }[estado] ?? '⚪';
  }

  getEstadoLabel(estado: string): string {
    return { NUEVO: 'Solicitud Recibida', EN_PROCESO: 'En Proceso', COMPLETADO: 'Completado', RECHAZADO: 'Rechazado' }[estado] ?? estado;
  }

  formatDuracion(min?: number | null): string {
    if (!min) return '—';
    if (min < 60) return `${min} min`;
    const h = Math.floor(min / 60);
    const m = min % 60;
    return m > 0 ? `${h}h ${m}min` : `${h}h`;
  }

  ngOnDestroy(): void {
    if (this.wsSub) this.wsSub.unsubscribe();
  }
}

/**
 * Representa un paso en la visualización del progreso del trámite (CU-15).
 * ISP: interfaz pequeña y específica para este componente.
 */
interface PasoProgreso {
  nodoId: string;
  nombre: string;
  departamento: string;
  /** El paso ya fue completado (aparece en el historial) */
  completado: boolean;
  /** Es el nodo donde está actualmente el trámite */
  actual: boolean;
  /** Es un nodo DECISION — se muestra con icono especial */
  esEspecial: boolean;
  fecha: string | null;
  duracionMinutos: number | null;
}
