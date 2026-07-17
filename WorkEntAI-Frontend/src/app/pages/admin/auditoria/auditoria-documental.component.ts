import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule, Router } from '@angular/router';
import { DocumentoService } from '../../../services/documento/documento.service';
import { AuthService } from '../../../services/auth/auth.service';
import { SidebarComponent, NavItem, ADMIN_NAV_ITEMS } from '../../../components/sidebar/sidebar.component';

@Component({
  selector: 'app-auditoria-documental',
  standalone: true,
  imports: [CommonModule, RouterModule, SidebarComponent],
  template: `
    <div class="app-layout">
      <app-sidebar [activeRoute]="esAdmin ? '/admin/tramites' : (esFuncionario ? '/dashboard' : '/cliente')" [navItems]="navItems" />
      <main class="main-content" style="padding: 40px; background: #f8fafc; min-height: 100vh; flex: 1; color: #1e293b;">
        
        <!-- Modern Header Navbar -->
        <div class="collab-header glass-panel" style="margin-left: -40px; margin-right: -40px; margin-top: -40px; margin-bottom: 32px; border-bottom: 1px solid #cbd5e1;">
          <div class="header-left">
            <button class="btn-back" (click)="volverAlRepo()">
              <span class="icon">⬅</span> Volver al Repositorio
            </button>
            <div class="header-titles">
              <h1>Auditoría Documental</h1>
              <span class="badge-notas">Trámite #{{ tramiteId }}</span>
            </div>
          </div>
        </div>

        <!-- CONTENT CONTAINER CARD -->
        <div style="background: white; border-radius: 16px; padding: 24px; border: 1px solid #e2e8f0; box-shadow: 0 1px 3px rgba(0,0,0,0.05); overflow: hidden;">
          @if (cargando) {
            <div style="text-align: center; padding: 40px; color: #64748b;">
              <div class="spinner" style="margin: 0 auto 12px auto;"></div>
              <span>Cargando historial de auditoría...</span>
            </div>
          } @else if (error) {
            <div style="background: #fef2f2; border: 1px solid #fca5a5; color: #b91c1c; padding: 16px; border-radius: 12px; font-weight: 500; margin-bottom: 20px;">
              ❌ {{ error }}
            </div>
          } @else {
            <table style="width: 100%; border-collapse: collapse; text-align: left;">
              <thead>
                <tr style="background: #f8fafc; border-bottom: 1px solid #cbd5e1;">
                  <th style="padding: 14px 20px; font-size: 12px; font-weight: 600; color: #475569; text-transform: uppercase; letter-spacing: 0.05em;">Fecha/Hora</th>
                  <th style="padding: 14px 20px; font-size: 12px; font-weight: 600; color: #475569; text-transform: uppercase; letter-spacing: 0.05em;">Acción</th>
                  <th style="padding: 14px 20px; font-size: 12px; font-weight: 600; color: #475569; text-transform: uppercase; letter-spacing: 0.05em;">Usuario</th>
                  <th style="padding: 14px 20px; font-size: 12px; font-weight: 600; color: #475569; text-transform: uppercase; letter-spacing: 0.05em;">Rol</th>
                  <th style="padding: 14px 20px; font-size: 12px; font-weight: 600; color: #475569; text-transform: uppercase; letter-spacing: 0.05em;">Resultado</th>
                  <th style="padding: 14px 20px; font-size: 12px; font-weight: 600; color: #475569; text-transform: uppercase; letter-spacing: 0.05em;">Detalle</th>
                </tr>
              </thead>
              <tbody>
                @for (log of auditoria; track log.id) {
                  <tr style="border-bottom: 1px solid #f1f5f9;">
                    <td style="padding: 16px 20px; font-size: 13px; color: #64748b; font-family: monospace;">{{ log.timestamp | date:'dd/MM/yyyy HH:mm:ss' }}</td>
                    <td style="padding: 16px 20px; font-size: 13px;">
                      <span class="badge" 
                            [class]="getBadgeClass(log.accion)"
                            [class.clickable]="isClickable(log)"
                            [title]="isClickable(log) ? 'Ver documento de este log' : ''"
                            (click)="clickAccion(log)">
                        {{ getAccionLabel(log.accion) }}
                      </span>
                    </td>
                    <td style="padding: 16px 20px; font-size: 13px;">
                      <div style="display: flex; flex-direction: column;">
                        <span style="font-weight: 600; color: #0f172a;">{{ log.nombreUsuario }}</span>
                        <span style="font-size: 11px; color: #94a3b8; font-family: monospace;">ID: {{ log.usuarioId.substring(0,8) }}...</span>
                      </div>
                    </td>
                    <td style="padding: 16px 20px; font-size: 13px;">
                      <span style="background: #f1f5f9; color: #475569; padding: 4px 8px; border-radius: 6px; font-size: 11px; font-weight: 600;">
                        {{ log.rolUsuario }}
                      </span>
                    </td>
                    <td style="padding: 16px 20px; font-size: 13px;">
                      <span style="font-weight: 700;" [style.color]="log.resultado === 'DENEGADO' ? '#dc2626' : '#16a34a'">
                        {{ log.resultado }}
                      </span>
                    </td>
                    <td style="padding: 16px 20px; font-size: 13px; color: #64748b; max-width: 300px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;" [title]="log.detalle">
                      {{ log.detalle }}
                    </td>
                  </tr>
                }
                @if (auditoria.length === 0) {
                  <tr>
                    <td colspan="6" style="text-align: center; color: #64748b; padding: 40px; font-size: 14px;">No hay registros de auditoría para este trámite.</td>
                  </tr>
                }
              </tbody>
            </table>
          }
        </div>

      </main>
    </div>
  `,
  styles: [`
    .badge { padding: 4px 10px; border-radius: 20px; font-size: 11px; font-weight: 600; letter-spacing: 0.05em; display: inline-block; }
    .badge-subir { background: #dcfce7; color: #166534; }
    .badge-ver { background: #dbeafe; color: #1e40af; }
    .badge-eliminar { background: #fee2e2; color: #991b1b; }
    .badge-permisos { background: #fef3c7; color: #92400e; }
    .badge.clickable { cursor: pointer; transition: all 0.2s; }
    .badge.clickable:hover { filter: brightness(0.9); transform: translateY(-1px); }

    /* Dark Mode overrides for audit badges */
    :host-context([data-theme="dark"]) .badge-subir { background: rgba(16,185,129,0.15) !important; color: #10b981 !important; border: 1px solid rgba(16,185,129,0.3) !important; }
    :host-context([data-theme="dark"]) .badge-ver { background: rgba(59,130,246,0.15) !important; color: #60a5fa !important; border: 1px solid rgba(59,130,246,0.3) !important; }
    :host-context([data-theme="dark"]) .badge-eliminar { background: rgba(239,68,68,0.15) !important; color: #f87171 !important; border: 1px solid rgba(239,68,68,0.3) !important; }
    :host-context([data-theme="dark"]) .badge-permisos { background: rgba(245,158,11,0.15) !important; color: #fbbf24 !important; border: 1px solid rgba(245,158,11,0.3) !important; }

    .spinner { border: 3px solid rgba(37, 99, 235, 0.1); border-left-color: #2563eb; border-radius: 50%; width: 24px; height: 24px; animation: spin 1s linear infinite; }
    @keyframes spin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }

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
export class AuditoriaDocumentalComponent implements OnInit {
  tramiteId = '';
  auditoria: any[] = [];
  cargando = false;
  error = '';
  esAdmin = false;
  esFuncionario = false;

  navItems: NavItem[] = [];

  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private documentoService = inject(DocumentoService);
  private authService = inject(AuthService);

  ngOnInit() {
    this.tramiteId = this.route.snapshot.paramMap.get('id') || '';
    const user = this.authService.getUser();
    const rol = user?.rol;
    this.esAdmin = rol === 'ADMIN';
    this.esFuncionario = rol === 'FUNCIONARIO';

    if (this.esAdmin) {
      this.navItems = ADMIN_NAV_ITEMS;
    } else if (this.esFuncionario) {
      this.navItems = [
        { icon: 'ph ph-kanban', label: 'Mis Tareas', route: '/dashboard' },
        { icon: 'ph ph-gear', label: 'Configuración', route: '/configuracion' },
        { icon: 'ph ph-user', label: 'Mi Perfil', route: '/perfil' }
      ];
    } else {
      this.navItems = [
        { icon: 'ph ph-folder-open', label: 'Trámites', route: '/cliente' },
        { icon: 'ph ph-gear', label: 'Configuración', route: '/configuracion' },
        { icon: 'ph ph-user', label: 'Mi Perfil', route: '/perfil' }
      ];
    }
    if (this.tramiteId) {
      this.cargarAuditoria();
    }
  }

  cargarAuditoria() {
    this.cargando = true;
    this.documentoService.obtenerAuditoriaTramite(this.tramiteId).subscribe({
      next: (logs: any[]) => {
        this.auditoria = logs;
        this.cargando = false;
      },
      error: (err: any) => {
        this.error = 'Error al cargar el historial de auditoría';
        this.cargando = false;
      }
    });
  }

  volverAlRepo(): void {
    this.router.navigate(['/tramite', this.tramiteId, 'documentos']);
  }

  getBadgeClass(accion: string): string {
    switch (accion) {
      case 'SUBIR': return 'badge-subir';
      case 'VER_URL': return 'badge-ver';
      case 'ELIMINAR': return 'badge-eliminar';
      case 'CAMBIAR_PERMISOS': return 'badge-permisos';
      default: return 'badge-ver';
    }
  }

  getAccionLabel(accion: string): string {
    switch (accion) {
      case 'SUBIR': return 'SUBIDA';
      case 'VER_URL': return 'VISUALIZACIÓN';
      case 'ELIMINAR': return 'ELIMINACIÓN';
      case 'CAMBIAR_PERMISOS': return 'PERMISOS';
      default: return accion;
    }
  }

  isClickable(log: any): boolean {
    return (log.accion === 'VER_URL' || log.accion === 'SUBIR') && log.resultado === 'OK' && !!log.documentoId;
  }

  clickAccion(log: any): void {
    if (!this.isClickable(log)) return;
    this.documentoService.obtenerUrlDescarga(log.documentoId).subscribe({
      next: ({ url }) => window.open(url, '_blank'),
      error: () => alert('No se pudo acceder al documento. Es posible que haya sido eliminado o no tengas permisos suficientes.')
    });
  }
}
