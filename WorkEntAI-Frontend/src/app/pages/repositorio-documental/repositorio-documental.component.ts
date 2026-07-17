import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule, Router } from '@angular/router';
import { DocumentoService, DocumentoTramite } from '../../services/documento/documento.service';
import { AuthService } from '../../services/auth/auth.service';
import { DomSanitizer } from '@angular/platform-browser';
import { SidebarComponent, NavItem, ADMIN_NAV_ITEMS } from '../../components/sidebar/sidebar.component';

@Component({
  selector: 'app-repositorio-documental',
  standalone: true,
  imports: [CommonModule, RouterModule, SidebarComponent],
  template: `
    <div class="app-layout">
      <app-sidebar [activeRoute]="esAdmin ? '/admin/tramites' : (esFuncionario ? '/dashboard' : '/cliente')" [navItems]="navItems" />
      <main class="main-content" style="padding: 40px; background: #f8fafc; min-height: 100vh; flex: 1; color: #1e293b;">
        
        <!-- Modern Header Navbar -->
        <div class="collab-header glass-panel" style="margin-left: -40px; margin-right: -40px; margin-top: -40px; margin-bottom: 32px; border-bottom: 1px solid #cbd5e1;">
          <div class="header-left">
            <button class="btn-back" (click)="volver()">
              <span class="icon">⬅</span> Volver
            </button>
            <div class="header-titles">
              <h1>Repositorio Documental</h1>
              <span class="badge-notas">Trámite #{{ tramiteId }}</span>
            </div>
          </div>
          <div class="header-right">
            <button style="background: #2563eb; border: none; color: white; padding: 10px 18px; border-radius: 8px; font-weight: 600; cursor: pointer; display: flex; align-items: center; gap: 8px; transition: all 0.2s;"
                    [routerLink]="['/colaborar', tramiteId + '-notas']">
              ✨ Abrir Editor Colaborativo
            </button>
            <button style="background: white; border: 1px solid #cbd5e1; color: #1e293b; padding: 10px 18px; border-radius: 8px; font-weight: 600; cursor: pointer; display: flex; align-items: center; gap: 8px; transition: all 0.2s;"
                    routerLink="./auditoria">
              📋 Auditoría
            </button>
          </div>
        </div>

        <!-- UPLOAD ZONE -->
        <div class="upload-zone" 
             [class.subiendo]="subiendo"
             (dragover)="onDragOver($event)"
             (dragleave)="onDragLeave($event)"
             (drop)="onDrop($event)"
             [class.drag-active]="isDragging"
             style="background: white; border: 2px dashed #cbd5e1; border-radius: 16px; padding: 36px; text-align: center; position: relative; margin-bottom: 32px; transition: all 0.3s ease; box-shadow: 0 1px 3px rgba(0,0,0,0.05); overflow: hidden;">
          
          <div class="upload-icon-wrapper" style="width: 64px; height: 64px; background: #f1f5f9; border-radius: 50%; display: flex; align-items: center; justify-content: center; margin: 0 auto 16px auto;">
            <span style="font-size: 32px;">☁️</span>
          </div>
          <h3 style="font-size: 18px; font-weight: 700; color: #0f172a; margin: 0 0 8px 0;">Subir Nuevo Documento</h3>
          <p style="color: #64748b; font-size: 14px; margin: 0;">Arrastra tu archivo aquí o <span style="color: #2563eb; font-weight: 600; text-decoration: underline;">haz clic para explorar</span></p>
          <input type="file" (change)="onArchivoSeleccionado($event)" [disabled]="subiendo" title="" style="position: absolute; inset: 0; opacity: 0; cursor: pointer; z-index: 10; font-size: 0;" />
          
          @if (subiendo) {
            <div class="progress-container" style="margin-top: 16px; display: flex; flex-direction: column; align-items: center; gap: 8px; color: #2563eb; font-weight: 600; font-size: 14px;">
              <div class="spinner"></div>
              <span>Subiendo archivo de forma segura...</span>
            </div>
          }
        </div>

        <!-- Alerts -->
        <div class="alerts-container" style="margin-bottom: 24px;">
          @if (error) {
            <div class="alert error fade-in" style="padding: 14px 18px; border-radius: 12px; font-size: 14px; font-weight: 500; display: flex; align-items: center; gap: 10px; background: #fef2f2; color: #b91c1c; border: 1px solid #fecaca;">
              <span>❌</span> {{ error }}
            </div>
          }
          @if (mensajeExito) {
            <div class="alert success fade-in" style="padding: 14px 18px; border-radius: 12px; font-size: 14px; font-weight: 500; display: flex; align-items: center; gap: 10px; background: #f0fdf4; color: #15803d; border: 1px solid #bbf7d0;">
              <span>✅</span> {{ mensajeExito }}
            </div>
          }
        </div>

        <!-- Document Grid Section -->
        <div class="doc-section">
          <div class="section-header" style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px;">
            <h2 style="font-size: 20px; font-weight: 700; color: #0f172a; margin: 0;">Archivos del Trámite</h2>
            <span class="doc-count" style="background: #e2e8f0; color: #475569; padding: 4px 12px; border-radius: 20px; font-size: 13px; font-weight: 600;">
              {{ documentos.length }} documentos
            </span>
          </div>

          @if (cargando) {
            <div class="loading-state" style="text-align: center; padding: 48px; background: white; border-radius: 16px; border: 1px solid #e2e8f0;">
              <div class="spinner large" style="margin: 0 auto 12px auto;"></div>
              <p style="color: #64748b; font-size: 14px; margin: 0;">Cargando repositorio...</p>
            </div>
          } @else {
            <div class="doc-grid" style="display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 20px;">
              @for(doc of documentos; track doc.id) {
                <div class="doc-card fade-in" style="background: white; border-radius: 16px; padding: 20px; position: relative; overflow: hidden; border: 1px solid #e2e8f0; box-shadow: 0 1px 3px rgba(0,0,0,0.05); display: flex; flex-direction: column; gap: 14px; transition: all 0.2s;">
                  <div class="doc-card-header" style="display: flex; justify-content: space-between; align-items: center;">
                    <div class="doc-icon" [ngClass]="getIconClass(doc.tipoMime)" style="font-size: 28px; width: 48px; height: 48px; border-radius: 12px; display: flex; align-items: center; justify-content: center;">
                      {{ getIcon(doc.tipoMime) }}
                    </div>
                    <div class="doc-version" style="background: #f1f5f9; color: #64748b; font-size: 11px; font-weight: 700; padding: 3px 8px; border-radius: 12px;">
                      v{{ doc.version }}
                    </div>
                  </div>
                  
                  <div class="doc-info">
                    <h3 class="doc-name" [title]="doc.nombre" style="font-size: 16px; font-weight: 700; color: #0f172a; margin: 0 0 12px 0; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;">
                      {{ doc.nombre }}
                    </h3>
                    <div class="doc-meta-grid" style="display: grid; grid-template-columns: 1fr 1fr; gap: 10px;">
                      <div class="meta-item">
                        <span class="label" style="font-size: 10px; color: #94a3b8; font-weight: 600; text-transform: uppercase;">Tamaño</span>
                        <span class="value" style="font-size: 13px; color: #334155; font-weight: 500;">{{ formatearTamano(doc.tamanoBytes) }}</span>
                      </div>
                      <div class="meta-item">
                        <span class="label" style="font-size: 10px; color: #94a3b8; font-weight: 600; text-transform: uppercase;">Subido por</span>
                        <span class="value author" style="font-size: 13px; color: #2563eb; font-weight: 600; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;">{{ doc.subidoPorNombre }}</span>
                      </div>
                      <div class="meta-item full-width" style="grid-column: 1 / -1;">
                        <span class="label" style="font-size: 10px; color: #94a3b8; font-weight: 600; text-transform: uppercase;">Fecha</span>
                        <span class="value" style="font-size: 13px; color: #334155; font-weight: 500;">{{ doc.fechaSubida | date:'dd/MM/yyyy HH:mm' }}</span>
                      </div>
                    </div>
                  </div>
                  <div class="doc-actions-overlay">
                    <button class="action-btn view-btn" (click)="previsualizar(doc)" title="Vista Previa">👁️</button>
                    <button class="action-btn download-btn" (click)="descargar(doc)" title="Descargar">⬇️</button>
                    <button class="action-btn delete-btn" (click)="eliminar(doc)" title="Eliminar">🗑️</button>
                  </div>
                </div>
              } @empty {
                <div class="empty-state full-width" style="text-align: center; padding: 48px; background: white; border-radius: 16px; border: 1px dashed #cbd5e1; grid-column: 1 / -1;">
                  <div class="empty-illustration" style="font-size: 48px; margin-bottom: 12px; opacity: 0.5;">📂</div>
                  <h3 style="font-size: 18px; color: #0f172a; margin: 0 0 6px 0;">El repositorio está vacío</h3>
                  <p style="color: #64748b; font-size: 14px; margin: 0;">Comienza arrastrando un documento a la zona de carga superior.</p>
                </div>
              }
            </div>
          }
        </div>

      </main>
    </div>

    <!-- Modern Preview Modal -->
    @if (previewUrl) {
      <div class="modal-overlay" (click)="cerrarPreview()">
        <div class="modal-content" (click)="$event.stopPropagation()">
          <div class="modal-header">
            <div class="modal-title">
              <span class="icon">👁️</span>
              <h3>Vista Previa del Documento</h3>
            </div>
            <button class="btn-close-modal" (click)="cerrarPreview()">✖</button>
          </div>
          <div class="modal-body">
            @if (previewLoading) {
              <div class="modal-loading">
                <div class="spinner large"></div>
                <p>Cargando vista previa segura...</p>
              </div>
            }
            @if (previewIsImage && !previewLoading) {
              <img [src]="previewUrl" alt="Vista Previa" class="preview-media image-preview" />
            } @else if (!previewIsImage && !previewLoading) {
              <iframe [src]="previewUrl" class="preview-media" title="Vista Previa" frameborder="0"></iframe>
            }
          </div>
        </div>
      </div>
    }
  `,
  styles: [`
    .fade-in { animation: fadeIn 0.4s ease-out; }
    @keyframes fadeIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }
    
    .icon-pdf { background: #fef2f2; color: #ef4444; }
    .icon-img { background: #eff6ff; color: #3b82f6; }
    .icon-doc { background: #e0e7ff; color: #4f46e5; }
    .icon-xls { background: #f0fdf4; color: #22c55e; }

    /* Card Actions Overlay */
    .doc-actions-overlay {
      position: absolute; inset: 0; background: rgba(255,255,255,0.9); backdrop-filter: blur(4px);
      display: flex; align-items: center; justify-content: center; gap: 16px;
      opacity: 0; visibility: hidden; transition: all 0.3s ease;
    }
    .doc-card:hover .doc-actions-overlay { opacity: 1; visibility: visible; }
    .doc-card:hover { transform: translateY(-4px); box-shadow: 0 10px 20px rgba(0,0,0,0.06) !important; border-color: #cbd5e1 !important; }
    
    .action-btn { 
      width: 44px; height: 44px; border-radius: 50%; border: none; font-size: 18px; cursor: pointer;
      display: flex; align-items: center; justify-content: center; transition: all 0.2s; box-shadow: 0 4px 12px rgba(0,0,0,0.1);
    }
    .view-btn { background: white; color: #3b82f6; }
    .view-btn:hover { background: #3b82f6; color: white; transform: scale(1.1); }
    .download-btn { background: white; color: #10b981; }
    .download-btn:hover { background: #10b981; color: white; transform: scale(1.1); }
    .delete-btn { background: white; color: #ef4444; }
    .delete-btn:hover { background: #ef4444; color: white; transform: scale(1.1); }

    /* Spinners */
    .spinner { border: 3px solid rgba(37, 99, 235, 0.1); border-left-color: #2563eb; border-radius: 50%; width: 24px; height: 24px; animation: spin 1s linear infinite; }
    .spinner.large { width: 40px; height: 40px; border-width: 4px; }
    @keyframes spin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }

    /* Modern Preview Modal */
    .modal-overlay { position: fixed; inset: 0; background: rgba(15, 23, 42, 0.6); backdrop-filter: blur(8px); z-index: 1000; display: flex; align-items: center; justify-content: center; padding: 32px; }
    .modal-content { background: white; border-radius: 24px; width: 100%; max-width: 1100px; height: 90vh; display: flex; flex-direction: column; overflow: hidden; box-shadow: 0 25px 50px -12px rgba(0,0,0,0.5); animation: modalIn 0.4s cubic-bezier(0.16, 1, 0.3, 1); }
    
    .modal-header { display: flex; justify-content: space-between; align-items: center; padding: 20px 32px; border-bottom: 1px solid #f1f5f9; background: white; }
    .modal-title { display: flex; align-items: center; gap: 12px; }
    .modal-title h3 { margin: 0; font-size: 18px; font-weight: 700; color: #0f172a; }
    
    .btn-close-modal { background: #f1f5f9; border: none; width: 36px; height: 36px; border-radius: 50%; font-size: 16px; color: #64748b; cursor: pointer; transition: all 0.2s; display: flex; align-items: center; justify-content: center; }
    .btn-close-modal:hover { background: #fee2e2; color: #ef4444; transform: rotate(90deg); }
    
    .modal-body { flex: 1; padding: 0; background: #e2e8f0; position: relative; display: flex; align-items: center; justify-content: center; overflow: hidden; }
    .modal-loading { display: flex; flex-direction: column; align-items: center; gap: 16px; color: #475569; font-weight: 500; }
    
    .preview-media { width: 100%; height: 100%; border: none; }
    .image-preview { object-fit: contain; padding: 32px; box-sizing: border-box; }
    
    @keyframes modalIn { from { opacity: 0; transform: translateY(40px) scale(0.95); } to { opacity: 1; transform: translateY(0) scale(1); } }

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
export class RepositorioDocumentalComponent implements OnInit {
  tramiteId = '';
  documentos: DocumentoTramite[] = [];
  cargando = false;
  subiendo = false;
  error = '';
  mensajeExito = '';
  esAdmin = false;
  esFuncionario = false;

  isDragging = false;
  previewUrl: any = null;
  previewIsImage = false;
  previewLoading = false;

  navItems: NavItem[] = [];

  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private documentoService = inject(DocumentoService);
  private authService = inject(AuthService);
  private sanitizer = inject(DomSanitizer);

  ngOnInit() {
    this.tramiteId = this.route.snapshot.paramMap.get('id') || '';
    const rol = this.authService.getRol();
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
      this.cargarDocumentos();
    }
  }

  cargarDocumentos() {
    this.cargando = true;
    this.documentoService.listarDocumentos(this.tramiteId).subscribe({
      next: (docs) => { this.documentos = docs; this.cargando = false; },
      error: () => { this.error = 'Error al cargar el repositorio'; this.cargando = false; }
    });
  }

  volver(): void {
    if (this.esAdmin) {
      this.router.navigate(['/admin/tramites']);
    } else {
      this.router.navigate(['/tramite', this.tramiteId]);
    }
  }

  // --- Drag and Drop Handlers ---
  onDragOver(event: DragEvent) {
    event.preventDefault();
    event.stopPropagation();
    this.isDragging = true;
  }

  onDragLeave(event: DragEvent) {
    event.preventDefault();
    event.stopPropagation();
    this.isDragging = false;
  }

  onDrop(event: DragEvent) {
    event.preventDefault();
    event.stopPropagation();
    this.isDragging = false;

    const files = event.dataTransfer?.files;
    if (files && files.length > 0) {
      this.subirArchivo(files[0]);
    }
  }

  onArchivoSeleccionado(event: Event) {
    const input = event.target as HTMLInputElement;
    if (!input.files?.length) return;
    this.subirArchivo(input.files[0]);
    input.value = '';
  }

  getIcon(mime: string): string {
    if (mime.includes('pdf')) return '📄';
    if (mime.includes('image')) return '🖼️';
    if (mime.includes('word') || mime.includes('document')) return '📝';
    if (mime.includes('excel') || mime.includes('spreadsheet')) return '📊';
    if (mime.includes('video')) return '🎥';
    return '📁';
  }

  getIconClass(mime: string): string {
    if (mime.includes('pdf')) return 'icon-pdf';
    if (mime.includes('image')) return 'icon-img';
    if (mime.includes('word') || mime.includes('document')) return 'icon-doc';
    if (mime.includes('excel') || mime.includes('spreadsheet')) return 'icon-xls';
    return '';
  }

  subirArchivo(archivo: File) {
    this.subiendo = true;
    this.error = '';
    this.documentoService.subirDocumento(this.tramiteId, archivo).subscribe({
      next: (doc) => {
        this.documentos.unshift(doc);
        this.subiendo = false;
        this.mostrarExito('Documento subido correctamente');
      },
      error: (err) => {
        this.subiendo = false;
        this.error = err.error?.message || 'Error al subir el archivo.';
        setTimeout(() => this.error = '', 5000);
      }
    });
  }

  descargar(doc: DocumentoTramite) {
    this.documentoService.obtenerUrlDescarga(doc.id).subscribe({
      next: ({ url }) => window.open(url, '_blank'),
      error: () => {
        this.error = 'No tienes permiso para descargar este documento.';
        setTimeout(() => this.error = '', 4000);
      }
    });
  }

  previsualizar(doc: DocumentoTramite) {
    this.previewIsImage = doc.tipoMime.startsWith('image/');
    this.previewLoading = true;

    this.documentoService.obtenerUrlDescarga(doc.id).subscribe({
      next: ({ url }) => {
        this.previewUrl = this.sanitizer.bypassSecurityTrustResourceUrl(url);
        this.previewLoading = false;
      },
      error: () => {
        this.error = 'No tienes permiso para previsualizar este documento.';
        setTimeout(() => this.error = '', 4000);
        this.previewLoading = false;
        this.previewUrl = null;
      }
    });
  }

  cerrarPreview() {
    this.previewUrl = null;
  }

  eliminar(doc: DocumentoTramite) {
    if (!confirm(`¿Eliminar versión ${doc.version} de ${doc.nombre}?`)) return;
    this.documentoService.eliminarDocumento(doc.id).subscribe({
      next: () => {
        this.documentos = this.documentos.filter(d => d.id !== doc.id);
        this.mostrarExito('Documento eliminado correctamente.');
      },
      error: () => this.error = 'Error al eliminar el documento.'
    });
  }

  private mostrarExito(msg: string) {
    this.mensajeExito = msg;
    setTimeout(() => this.mensajeExito = '', 4000);
  }

  formatearTamano(bytes: number) { return this.documentoService.formatearTamano(bytes); }
}
