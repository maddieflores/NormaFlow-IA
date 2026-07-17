import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { catchError, of } from 'rxjs';
import { DepartamentoService, Departamento } from '../../services/departamento/departamento.service';
import { UsuarioService } from '../../services/usuario/usuario.service';
import { Usuario } from '../../models/models';
import { SidebarComponent, NavItem, ADMIN_NAV_ITEMS } from '../../components/sidebar/sidebar.component';

@Component({
  selector: 'app-departamentos',
  standalone: true,
  imports: [CommonModule, FormsModule, SidebarComponent],
  template: `
    <div class="app-layout">
      <app-sidebar activeRoute="/departamentos" [navItems]="navItems" />
      <main class="main-content" style="background-color: #f8fafc; padding: 40px; color: #1e293b;">

        <!-- HEADER -->
        <div style="display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 24px;">
          <div>
            <h1 style="font-size: 24px; font-weight: 700; color: #0f172a; margin-bottom: 8px;">Gestión de Departamentos</h1>
            <p style="color: #64748b; font-size: 14px; margin: 0;">Administra las áreas funcionales del sistema</p>
          </div>
          <div style="display: flex; gap: 12px;">
            <button
              style="background: #2563eb; border: none; color: white; padding: 10px 16px; border-radius: 8px; font-weight: 600; cursor: pointer;"
              (click)="abrirModalDept()">
              Nuevo departamento
            </button>
          </div>
        </div>

        @if (loadingDept) {
          <div style="padding: 40px; text-align: center; color: #64748b;">
            <div class="spinner" style="margin: 0 auto 12px;"></div>
            <p style="margin: 0;">Cargando departamentos...</p>
          </div>
        } @else {
          <!-- GRID -->
          <div style="display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 24px;">
            @for (d of departamentos; track d.id) {
              <div style="background: white; border-radius: 12px; padding: 24px; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.05), 0 2px 4px -1px rgba(0,0,0,0.03); border: 1px solid #e2e8f0; display: flex; flex-direction: column; justify-content: space-between; min-height: 200px;">
                <div>
                  <div style="display: flex; gap: 14px; align-items: flex-start; margin-bottom: 16px;">
                    <div style="width: 44px; height: 44px; border-radius: 8px; background: rgba(37,99,235,0.1); color: #2563eb; display: flex; align-items: center; justify-content: center; font-size: 22px; flex-shrink: 0;">
                      🏢
                    </div>
                    <div>
                      <h3 style="font-size: 16px; font-weight: 700; color: #0f172a; margin: 0 0 4px 0;">{{ d.nombre }}</h3>
                      <p style="font-size: 13px; color: #64748b; margin: 0; line-height: 1.4; display: -webkit-box; -webkit-line-clamp: 2; line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;">
                        {{ d.descripcion || 'Sin descripción' }}
                      </p>
                    </div>
                  </div>
                </div>

                <div>
                  <div style="margin-bottom: 16px;">
                    <span style="background: rgba(37, 99, 235, 0.08); color: #2563eb; padding: 4px 10px; border-radius: 12px; font-size: 12px; font-weight: 700; border: 1px solid rgba(37, 99, 235, 0.15);">
                      {{ getFuncionariosDept(d.nombre) }} funcionarios
                    </span>
                  </div>
                  <div style="display: flex; gap: 8px; border-top: 1px solid #f1f5f9; padding-top: 16px;">
                    <button class="btn-action-row" style="flex: 1; text-align: center;" 
                      (click)="abrirModalEditarDept(d)">
                      Editar
                    </button>
                    <button 
                      style="background: white; border: 1px solid #fca5a5; color: #ef4444; padding: 6px 12px; border-radius: 20px; font-size: 12px; font-weight: 600; cursor: pointer; text-align: center;" 
                      (click)="eliminarDept(d)">
                      Eliminar
                    </button>
                  </div>
                </div>
              </div>
            }
            @if (departamentos.length === 0) {
              <div style="grid-column: 1 / -1; padding: 40px; text-align: center; color: #64748b; background: white; border-radius: 12px; border: 1px solid #e2e8f0;">
                No hay departamentos registrados.
              </div>
            }
          </div>
        }

      </main>
    </div>

    <!-- Modal Departamento -->
    @if (showModalDept) {
      <div class="modal-overlay" (click)="cerrarModalDept()" style="display: flex; align-items: center; justify-content: center; position: fixed; inset: 0; background: rgba(0,0,0,0.5); z-index: 1000;">
        <div class="modal-card" (click)="$event.stopPropagation()" style="background: white; border-radius: 16px; padding: 32px; box-shadow: 0 10px 15px -3px rgba(0,0,0,0.1); width: 100%; max-width: 500px; border: 1px solid #e2e8f0;">
          <h3 style="font-size: 18px; font-weight: 700; color: #0f172a; margin: 0 0 8px 0;">{{ editandoDept ? '✏️ Editar Departamento' : '+ Nuevo Departamento' }}</h3>
          <p style="color: #64748b; font-size: 14px; margin: 0 0 24px 0;">{{ editandoDept ? 'Modifica los detalles del departamento' : 'Crea un nuevo departamento en el sistema' }}</p>

          <div style="margin-bottom: 16px;">
            <label style="display: block; font-size: 13px; font-weight: 600; color: #475569; margin-bottom: 6px;">Nombre *</label>
            <input type="text" style="width: 100%; padding: 10px 14px; border: 1px solid #cbd5e1; border-radius: 8px; font-size: 14px; outline: none;" [(ngModel)]="formDept.nombre" placeholder="Ej: Dept. Técnico" />
          </div>

          <div style="margin-bottom: 24px;">
            <label style="display: block; font-size: 13px; font-weight: 600; color: #475569; margin-bottom: 6px;">Descripción</label>
            <textarea style="width: 100%; padding: 10px 14px; border: 1px solid #cbd5e1; border-radius: 8px; font-size: 14px; outline: none; resize: vertical;" [(ngModel)]="formDept.descripcion" rows="3" placeholder="Descripción del departamento"></textarea>
          </div>

          @if (deptError) {
            <div style="background: #fef2f2; border: 1px solid #fca5a5; color: #b91c1c; padding: 12px; border-radius: 8px; font-size: 13px; margin-bottom: 16px; display: flex; align-items: center; gap: 6px;">❌ {{ deptError }}</div>
          }

          <div style="display: flex; justify-content: flex-end; gap: 12px;">
            <button style="background: white; border: 1px solid #cbd5e1; color: #1e293b; padding: 10px 18px; border-radius: 8px; font-size: 14px; font-weight: 600; cursor: pointer; transition: all 0.2s;" (click)="cerrarModalDept()">Cancelar</button>
            <button style="background: #2563eb; border: none; color: white; padding: 10px 18px; border-radius: 8px; font-size: 14px; font-weight: 600; cursor: pointer; transition: all 0.2s;" (click)="guardarDept()" [disabled]="guardandoDept">
              {{ guardandoDept ? '⏳...' : 'Guardar' }}
            </button>
          </div>
        </div>
      </div>
    }
  `,
  styles: []
})
export class DepartamentosComponent implements OnInit {
  navItems = ADMIN_NAV_ITEMS;

  departamentos: Departamento[] = [];
  usuarios: Usuario[] = [];
  loadingDept = true;

  // Modal departamento
  showModalDept = false;
  editandoDept: Departamento | null = null;
  formDept: any = { nombre: '', descripcion: '' };
  guardandoDept = false;
  deptError = '';

  constructor(
    private deptService: DepartamentoService,
    private usuarioService: UsuarioService,
    public router: Router
  ) { }

  ngOnInit(): void {
    this.cargarDepartamentos();
    this.cargarUsuarios();
  }

  cargarDepartamentos(): void {
    this.loadingDept = true;
    this.deptService.getAll().pipe(catchError(() => of([]))).subscribe(data => {
      this.departamentos = data;
      this.loadingDept = false;
    });
  }

  cargarUsuarios(): void {
    this.usuarioService.getAll().pipe(catchError(() => of([]))).subscribe(data => {
      this.usuarios = data;
    });
  }

  getFuncionariosDept(nombre: string): number {
    return this.usuarios.filter(u => u.departamento === nombre).length;
  }

  // ── Modal Departamento ─────────────────────────────────
  abrirModalDept(): void {
    this.editandoDept = null;
    this.formDept = { nombre: '', descripcion: '' };
    this.deptError = '';
    this.showModalDept = true;
  }

  abrirModalEditarDept(d: Departamento): void {
    this.editandoDept = d;
    this.formDept = { nombre: d.nombre, descripcion: d.descripcion || '' };
    this.deptError = '';
    this.showModalDept = true;
  }

  cerrarModalDept(): void { this.showModalDept = false; }

  guardarDept(): void {
    if (!this.formDept.nombre) { this.deptError = 'El nombre es requerido'; return; }
    this.guardandoDept = true; this.deptError = '';
    const obs = this.editandoDept
      ? this.deptService.update(this.editandoDept.id, this.formDept)
      : this.deptService.create(this.formDept);
    obs.subscribe({
      next: () => {
        this.guardandoDept = false;
        this.cargarDepartamentos();
        this.cerrarModalDept();
      },
      error: (e) => { this.deptError = e.error?.error || 'Error al guardar'; this.guardandoDept = false; }
    });
  }

  eliminarDept(d: Departamento): void {
    if (!confirm(`¿Eliminar departamento "${d.nombre}"?`)) return;
    this.deptService.delete(d.id).subscribe({
      next: () => this.cargarDepartamentos(),
      error: (e) => alert(e.error?.error || 'Error al eliminar')
    });
  }
}
