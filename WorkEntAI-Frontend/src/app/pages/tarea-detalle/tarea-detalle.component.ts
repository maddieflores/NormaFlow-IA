import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { catchError, of } from 'rxjs';
import { Tarea, Tramite } from '../../models/models';
import { TareaService } from '../../services/tarea/tarea.service';
import { TramiteService } from '../../services/tramite/tramite.service';
import { AIService } from '../../services/ai/ai.service';
import { SidebarComponent, NavItem, ADMIN_NAV_ITEMS } from '../../components/sidebar/sidebar.component';
import { AuthService } from '../../services/auth/auth.service';
import { TaskInfoComponent } from './task-info/task-info.component';
import { TaskFormComponent } from './task-form/task-form.component';
import { TaskAiAssistantComponent } from './task-ai-assistant/task-ai-assistant.component';

import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-tarea-detalle',
  standalone: true,
  imports: [CommonModule, SidebarComponent, TaskInfoComponent, TaskFormComponent, TaskAiAssistantComponent, RouterModule],
  template: `
    <div class="app-layout">
      <app-sidebar activeRoute="/dashboard" [navItems]="navItems" />
      <main class="main-content" style="background-color: #f8fafc; padding: 40px; color: #1e293b; overflow-y: auto; flex: 1;">

        <!-- HEADER -->
        <div style="display: flex; justify-content: space-between; align-items: flex-end; margin-bottom: 24px; flex-wrap: wrap; gap: 16px; margin-top: 16px;">
          <div>
            <button 
              style="background: none; border: none; color: #64748b; cursor: pointer; font-size: 13px; font-weight: 600; display: inline-flex; align-items: center; gap: 4px; padding: 0; outline: none;"
              (click)="router.navigate(['/dashboard'])">
              ← Volver al Dashboard
            </button>
            <div style="display: flex; align-items: center; gap: 12px; margin-top: 8px;">
              <h1 style="font-size: 24px; font-weight: 700; color: #0f172a; margin: 0;">{{ tarea?.nombreNodo || 'Detalle de Tarea' }}</h1>
              <span 
                [style.background]="tarea?.estado === 'PENDIENTE' ? 'rgba(37, 99, 235, 0.1)' : tarea?.estado === 'EN_PROCESO' ? 'rgba(245, 158, 11, 0.1)' : 'rgba(16, 185, 129, 0.1)'"
                [style.color]="tarea?.estado === 'PENDIENTE' ? '#2563eb' : tarea?.estado === 'EN_PROCESO' ? '#f59e0b' : '#10b981'"
                [style.border]="tarea?.estado === 'PENDIENTE' ? '1px solid rgba(37, 99, 235, 0.2)' : tarea?.estado === 'EN_PROCESO' ? '1px solid rgba(245, 158, 11, 0.2)' : '1px solid rgba(16, 185, 129, 0.2)'"
                style="padding: 4px 10px; border-radius: 12px; font-size: 12px; font-weight: 700; display: inline-block;">
                {{ tarea?.estado }}
              </span>
            </div>
            <p style="color: #64748b; font-size: 14px; margin: 4px 0 0 0;">
              Trámite: <strong>{{ tarea?.numeroReferenciaTramite }}</strong> · {{ tarea?.nombrePolitica }} · Depto: <strong>{{ tarea?.departamento }}</strong>
            </p>
          </div>
          <div>
            <button 
              style="background: white; border: 1px solid #cbd5e1; color: #475569; padding: 8px 16px; border-radius: 8px; font-size: 13px; font-weight: 600; cursor: pointer; display: inline-flex; align-items: center; gap: 6px;"
              [routerLink]="['/tramite', tarea?.tramiteId, 'documentos']">
              📂 Repositorio Documental
            </button>
          </div>
        </div>

        @if (loading) {
          <div style="padding: 40px; text-align: center; color: #64748b;"><div class="spinner"></div><p style="margin-top: 10px;">Cargando tarea...</p></div>
        }

        @if (!loading && tarea) {
          <div class="tarea-layout">
            
            <!-- Left Panel -->
            <div class="tarea-left">
              <app-task-info 
                [tarea]="tarea" 
                [tramite]="tramite"
                (viewTramite)="router.navigate(['/tramite', $event])" />
            </div>

            <!-- Center/Right Panel -->
            <div class="tarea-right">
              
              <div style="background: white; border-radius: 12px; padding: 24px; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.05), 0 2px 4px -1px rgba(0,0,0,0.03); border: 1px solid #e2e8f0; display: flex; flex-direction: column; min-height: 600px;">
                <div style="display: flex; justify-content: space-between; align-items: center; padding-bottom: 16px; border-bottom: 1px solid #e2e8f0; margin-bottom: 20px;">
                  <h3 style="font-size: 16px; font-weight: 700; color: #0f172a; margin: 0;">📝 Formulario a Completar</h3>
                  @if (tarea.estado !== 'COMPLETADO') {
                    <button 
                      [style.background]="showAI ? 'rgba(139, 92, 246, 0.1)' : 'white'"
                      [style.color]="showAI ? '#8b5cf6' : '#64748b'"
                      [style.border]="showAI ? '1px solid #8b5cf6' : '1px solid #cbd5e1'"
                      style="border-radius: 8px; padding: 6px 12px; font-size: 12px; font-weight: 600; cursor: pointer; display: flex; align-items: center; gap: 6px; transition: all 0.2s;"
                      (click)="showAI = !showAI">
                      <span class="ai-sparkles">✨</span> IA Autocompletado
                    </button>
                  }
                </div>

                @if (showAI && tarea.estado !== 'COMPLETADO') {
                  <div style="margin-bottom: 20px;">
                    <app-task-ai-assistant 
                      [loading]="loadingIA" 
                      [error]="iaError"
                      (extract)="extraerDatosIA($event)" />
                  </div>
                }

                <div style="flex: 1;">
                  <app-task-form
                    [camposFormulario]="camposFormulario"
                    [formularioDatos]="formularioDatos"
                    [disabled]="tarea.estado === 'COMPLETADO' || tarea.estado === 'RECHAZADO'" />
                </div>

                <!-- Footer Actions -->
                <div style="padding-top: 20px; border-top: 1px solid #e2e8f0; margin-top: 20px; display: flex; flex-direction: column; gap: 16px;">
                  @if (formError) {
                    <div class="toast-inline error">❌ {{ formError }}</div>
                  }
                  @if (formExito) {
                    <div class="toast-inline success">✅ {{ formExito }}</div>
                  }
                  
                  @if (tarea.estado === 'COMPLETADO') {
                    <div class="completado-banner">
                      <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 11.08V12a10 10 0 11-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
                      Tarea completada exitosamente el {{ tarea.fechaCompletado | date:'dd/MM/yyyy HH:mm' }}
                    </div>
                  } @else if (tarea.estado !== 'RECHAZADO') {
                    <div style="display: flex; gap: 12px; justify-content: flex-end; align-items: center; flex-wrap: wrap;">
                      @if (tarea.estado === 'PENDIENTE') {
                        <button 
                          style="background: white; border: 1px solid #cbd5e1; color: #475569; padding: 10px 18px; border-radius: 8px; font-size: 13px; font-weight: 600; cursor: pointer;"
                          (click)="cambiarEstado('EN_PROCESO')" 
                          [disabled]="guardando">
                          Iniciar revisión
                        </button>
                      }
                      <button 
                        style="background: white; border: 1px solid #fca5a5; color: #ef4444; padding: 10px 18px; border-radius: 8px; font-size: 13px; font-weight: 600; cursor: pointer;"
                        (click)="cambiarEstado('RECHAZADO')" 
                        [disabled]="guardando">
                        Rechazar Trámite
                      </button>
                      <button 
                        style="background: #2563eb; color: white; border: none; padding: 10px 18px; border-radius: 8px; font-size: 13px; font-weight: 600; cursor: pointer; display: inline-flex; align-items: center; gap: 6px;"
                        (click)="completarTarea()" 
                        [disabled]="guardando">
                        @if (guardando) { <span class="spinner-sm"></span> Procesando... }
                        @else { Aprobar y Continuar }
                      </button>
                    </div>
                  }
                </div>
              </div>

            </div>
          </div>
        }
      </main>
    </div>
  `,
  styles: [`
    .btn-back { display: inline-flex; align-items: center; background: none; border: none; color: var(--text-muted); cursor: pointer; font-size: 13px; font-weight: 600; font-family: inherit; padding: 0; transition: color 0.2s; }
    .btn-back:hover { color: var(--primary); }

    .tarea-layout { display: grid; grid-template-columns: 320px 1fr; gap: 24px; align-items: start; }
    
    .panel-form { display: flex; flex-direction: column; min-height: 600px; padding: 0; overflow: hidden; }
    .form-header { display: flex; justify-content: space-between; align-items: center; padding: 20px 24px; border-bottom: 1px solid var(--border); background: var(--bg-2); }
    .section-title { font-size: 16px; font-weight: 700; margin: 0; color: var(--text); }
    
    .btn-outline-sm { background: var(--card); border: 1px solid var(--border-2); color: var(--text-muted); border-radius: 8px; padding: 6px 12px; font-size: 12px; font-weight: 600; cursor: pointer; transition: all 0.2s; display: flex; align-items: center; gap: 6px; }
    .btn-outline-sm:hover { border-color: var(--purple); color: var(--purple); }
    .btn-outline-sm.active { background: hsl(282, 69%, 45%, 0.1); border-color: var(--purple); color: var(--purple); }
    .ai-sparkles { font-size: 14px; }
    
    .ai-wrapper { padding: 20px 24px 0; }
    .form-wrapper { padding: 24px; flex: 1; overflow-y: auto; }
    
    .form-footer { padding: 20px 24px; border-top: 1px solid var(--border); background: var(--bg-2); display: flex; flex-direction: column; gap: 16px; }
    .acciones { display: flex; gap: 12px; justify-content: flex-end; align-items: center; }
    
    .btn-danger-outline { background: transparent; border: 1px solid var(--danger); color: var(--danger); border-radius: 8px; padding: 9px 18px; font-size: 13px; font-weight: 600; cursor: pointer; transition: all 0.2s; font-family: inherit; }
    .btn-danger-outline:hover { background: hsl(355, 80%, 55%, 0.1); }
    
    .toast-inline { padding: 12px 16px; border-radius: 8px; font-size: 13px; font-weight: 500; display: flex; align-items: center; gap: 8px; }
    .toast-inline.error { background: hsl(355, 80%, 55%, 0.1); color: var(--danger); border: 1px solid hsl(355, 80%, 55%, 0.2); }
    .toast-inline.success { background: hsl(142, 60%, 38%, 0.1); color: var(--success); border: 1px solid hsl(142, 60%, 38%, 0.2); }
    
    .completado-banner { background: hsl(142, 60%, 38%, 0.05); border: 1px dashed var(--success); border-radius: 10px; padding: 16px; font-size: 13px; font-weight: 600; color: var(--success); display: flex; align-items: center; justify-content: center; gap: 10px; }
    
    .spinner-sm { width: 14px; height: 14px; border: 2px solid rgba(255,255,255,0.3); border-top-color: currentColor; border-radius: 50%; animation: spin 0.8s linear infinite; display: inline-block; }

    @media (max-width: 1024px) {
      .tarea-layout { grid-template-columns: 1fr; }
    }
  `]
})
export class TareaDetalleComponent implements OnInit {
  tarea: Tarea | null = null;
  tramite: Tramite | null = null;
  loading = true;
  guardando = false;

  formError = '';
  formExito = '';
  formularioDatos: { [key: string]: any } = {};
  camposFormulario: any[] = [];

  showAI = false;
  loadingIA = false;
  iaError = '';

  navItems: NavItem[] = [];
  private authService = inject(AuthService);

  constructor(
    private route: ActivatedRoute,
    private tareaService: TareaService,
    private tramiteService: TramiteService,
    private aiService: AIService,
    public router: Router
  ) { }

  ngOnInit(): void {
    this.initNavItems();
    const id = this.route.snapshot.paramMap.get('id')!;
    this.tareaService.getById(id).pipe(catchError(() => of(null))).subscribe(t => {
      this.tarea = t;
      if (t) {
        this.formularioDatos = { ...t.formularioDatos };
        this.cargarCamposFormulario(t);
        this.tramiteService.getById(t.tramiteId).pipe(catchError(() => of(null))).subscribe(tr => {
          this.tramite = tr;
        });
      }
      this.loading = false;
    });
  }

  private initNavItems() {
    const role = this.authService.getUser()?.rol;
    if (role === 'ADMIN') {
      this.navItems = ADMIN_NAV_ITEMS;
    } else if (role === 'CLIENTE') {
      this.navItems = [
        { icon: 'ph ph-folder-open', label: 'Trámites', route: '/cliente' },
        { icon: 'ph ph-gear', label: 'Configuración', route: '/configuracion' },
        { icon: 'ph ph-user', label: 'Mi Perfil', route: '/perfil' }
      ];
    } else {
      this.navItems = [
        { icon: 'ph ph-kanban', label: 'Mis Tareas', route: '/dashboard' },
        { icon: 'ph ph-gear', label: 'Configuración', route: '/configuracion' },
        { icon: 'ph ph-user', label: 'Mi Perfil', route: '/perfil' }
      ];
    }
  }

  cargarCamposFormulario(tarea: Tarea): void {
    if (tarea.camposFormulario && tarea.camposFormulario.length > 0) {
      this.camposFormulario = tarea.camposFormulario;
    } else {
      this.camposFormulario = [
        { nombre: 'aprobado', tipo: 'boolean', etiqueta: 'Decisión', requerido: true }
      ];
    }
  }

  extraerDatosIA(event: { texto: string, file: File | null }): void {
    if (!event.texto.trim() || !this.tarea) return;
    this.loadingIA = true;
    this.iaError = '';

    // Si hay un archivo (File), idealmente lo enviaríamos a S3 y usaríamos Textract
    // Pero como estamos mockeando, simplemente enviamos el texto simulado a la IA
    const textoAProcesar = event.file ? `(Contenido del archivo ${event.file.name}) ` + event.texto : event.texto;

    this.aiService.extraerDatosDocumento(textoAProcesar, this.tarea.nombreNodo || '')
      .pipe(catchError(() => of(null)))
      .subscribe(res => {
        this.loadingIA = false;
        if (!res) { this.iaError = 'Error al conectar con la IA'; return; }
        try {
          const datos = JSON.parse(res);
          Object.keys(datos).forEach(k => {
            if (datos[k] !== null && datos[k] !== undefined) {
              this.formularioDatos[k] = String(datos[k]);
            }
          });
          this.showAI = false;
          this.formExito = 'Datos extraídos correctamente.';
          setTimeout(() => this.formExito = '', 3000);
        } catch {
          this.iaError = 'La IA no pudo extraer datos estructurados';
        }
      });
  }

  cambiarEstado(estado: string): void {
    if (!this.tarea) return;
    this.guardando = true;
    this.tareaService.actualizarEstado(this.tarea.id, estado).subscribe({
      next: (t) => {
        this.tarea = t;
        this.guardando = false;
        this.formExito = `Estado actualizado a ${estado}`;
        setTimeout(() => this.formExito = '', 3000);
      },
      error: (err) => {
        this.formError = err.error?.error || 'Error al actualizar estado';
        this.guardando = false;
      }
    });
  }

  completarTarea(): void {
    if (!this.tarea) return;

    // Validate
    for (const campo of this.camposFormulario) {
      if (campo.requerido && !this.formularioDatos[campo.nombre]) {
        this.formError = `El campo "${campo.etiqueta || campo.nombre}" es requerido`;
        return;
      }
    }

    this.guardando = true;
    this.formError = '';
    this.tareaService.completar(this.tarea.id, this.formularioDatos).subscribe({
      next: () => {
        this.formExito = '✅ Tarea completada. El trámite avanzó.';
        this.guardando = false;
        setTimeout(() => this.router.navigate(['/dashboard']), 1500);
      },
      error: (err) => {
        this.formError = err.error?.error || 'Error al completar la tarea';
        this.guardando = false;
      }
    });
  }
}
