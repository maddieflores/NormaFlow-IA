import { Component, OnInit } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { catchError, of } from 'rxjs';
import { Usuario } from '../../models/models';
import { UsuarioService } from '../../services/usuario/usuario.service';
import { DepartamentoService } from '../../services/departamento/departamento.service';
import { AuthService } from '../../services/auth/auth.service';
import { SidebarComponent, NavItem, ADMIN_NAV_ITEMS } from '../../components/sidebar/sidebar.component';

@Component({
  selector: 'app-users',
  standalone: true,
  imports: [CommonModule, DatePipe, FormsModule, SidebarComponent],
  template: `
    <div class="app-layout">
      <app-sidebar activeRoute="/users" [navItems]="navItems"></app-sidebar>
      <main class="main-content" style="background-color: #f8fafc; padding: 40px; color: #1e293b;">

        <!-- HEADER -->
        <div style="display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 24px;">
          <div>
            <h1 style="font-size: 24px; font-weight: 700; color: #0f172a; margin-bottom: 8px;">Gestión de Usuarios</h1>
            <p style="color: #64748b; font-size: 14px; margin: 0;">Administra usuarios, roles y departamentos</p>
          </div>
          <div style="display: flex; gap: 12px;">
            <button
              style="background: #2563eb; border: none; color: white; padding: 10px 16px; border-radius: 8px; font-weight: 600; cursor: pointer;"
              (click)="abrirModalCrear()">
              Nuevo usuario
            </button>
          </div>
        </div>

        <!-- FILTER PILLS -->
        <div style="display: flex; gap: 12px; margin-bottom: 32px;">
          <button
            [ngStyle]="selectedRol === '' ? {'background': '#2563eb', 'color': 'white'} : {'background': 'white', 'color': '#1e293b'}"
            style="border: none; padding: 8px 16px; border-radius: 20px; font-weight: 600; font-size: 14px; cursor: pointer; box-shadow: 0 1px 3px rgba(0,0,0,0.1);"
            (click)="setRolFilter('')">Todos</button>
          <button
            [ngStyle]="selectedRol === 'ADMIN' ? {'background': '#2563eb', 'color': 'white'} : {'background': 'white', 'color': '#1e293b'}"
            style="border: none; padding: 8px 16px; border-radius: 20px; font-weight: 600; font-size: 14px; cursor: pointer; box-shadow: 0 1px 3px rgba(0,0,0,0.1);"
            (click)="setRolFilter('ADMIN')">Admins</button>
          <button
            [ngStyle]="selectedRol === 'FUNCIONARIO' ? {'background': '#2563eb', 'color': 'white'} : {'background': 'white', 'color': '#1e293b'}"
            style="border: none; padding: 8px 16px; border-radius: 20px; font-weight: 600; font-size: 14px; cursor: pointer; box-shadow: 0 1px 3px rgba(0,0,0,0.1);"
            (click)="setRolFilter('FUNCIONARIO')">Funcionarios</button>
          <button
            [ngStyle]="selectedRol === 'CLIENTE' ? {'background': '#2563eb', 'color': 'white'} : {'background': 'white', 'color': '#1e293b'}"
            style="border: none; padding: 8px 16px; border-radius: 20px; font-weight: 600; font-size: 14px; cursor: pointer; box-shadow: 0 1px 3px rgba(0,0,0,0.1);"
            (click)="setRolFilter('CLIENTE')">Clientes</button>
        </div>

        <!-- TABLE CARD -->
        <div style="background: white; border-radius: 12px; padding: 24px; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.05), 0 2px 4px -1px rgba(0,0,0,0.03); border: 1px solid #e2e8f0; margin-bottom: 24px;">
          <div style="display: flex; gap: 12px; margin-bottom: 24px; align-items: center; justify-content: space-between; flex-wrap: wrap;">
            <input 
              style="width: 320px; padding: 10px 16px; border: 1px solid #cbd5e1; border-radius: 8px; font-size: 14px; outline: none; background: white;"
              type="text" 
              placeholder="🔍 Buscar por nombre o email..."
              [(ngModel)]="searchQuery" 
              (input)="applyFilters()" />
            <button
              style="background: #2563eb; color: white; border: none; padding: 10px 24px; border-radius: 8px; font-weight: 600; cursor: pointer;"
              (click)="cargarUsuarios()">
              Recargar
            </button>
          </div>

          @if (loading) {
            <div style="padding: 40px; text-align: center; color: #64748b;">
              <div class="spinner" style="margin: 0 auto 12px;"></div>
              <p style="margin: 0;">Cargando usuarios...</p>
            </div>
          } @else {
            @if (filteredUsuarios.length === 0) {
              <div style="padding: 40px; text-align: center; color: #64748b;">No se encontraron usuarios.</div>
            } @else {
              <div style="overflow-x: auto;">
                <table style="width: 100%; border-collapse: collapse; text-align: left;">
                  <thead>
                    <tr style="border-bottom: 2px solid #e2e8f0;">
                      <th style="padding: 12px 8px; font-size: 13px; font-weight: 700; color: #0f172a;">Usuario</th>
                      <th style="padding: 12px 8px; font-size: 13px; font-weight: 700; color: #0f172a;">Rol</th>
                      <th style="padding: 12px 8px; font-size: 13px; font-weight: 700; color: #0f172a;">Departamento</th>
                      <th style="padding: 12px 8px; font-size: 13px; font-weight: 700; color: #0f172a;">Estado</th>
                      <th style="padding: 12px 8px; font-size: 13px; font-weight: 700; color: #0f172a;">Registro</th>
                      <th style="padding: 12px 8px;"></th>
                    </tr>
                  </thead>
                  <tbody>
                    @for (u of filteredUsuarios; track u.id) {
                      <tr style="border-bottom: 1px solid #f1f5f9;">
                        <td style="padding: 16px 8px; font-size: 14px; color: #334155; font-weight: 500;">
                          <div style="display:flex;align-items:center;gap:10px">
                            <div class="avatar-sm" [style.background]="getAvatarColor(u.rol)">
                              {{ u.nombre.charAt(0).toUpperCase() }}
                            </div>
                            <div>
                              <p style="font-size:14px;font-weight:600;color:#1e293b;margin:0 0 2px">{{ u.nombre }}</p>
                              <p style="font-size:11px;color:#64748b;margin:0">{{ u.email }}</p>
                            </div>
                          </div>
                        </td>
                        <td style="padding: 16px 8px; font-size: 14px; color: #475569;">
                          @if (u.rol === 'ADMIN') {
                            <span style="background: rgba(139, 92, 246, 0.1); color: #8b5cf6; padding: 4px 10px; border-radius: 12px; font-size: 12px; font-weight: 700; border: 1px solid rgba(139, 92, 246, 0.2);">ADMIN</span>
                          } @else if (u.rol === 'FUNCIONARIO') {
                            <span style="background: rgba(37, 99, 235, 0.1); color: #2563eb; padding: 4px 10px; border-radius: 12px; font-size: 12px; font-weight: 700; border: 1px solid rgba(37, 99, 235, 0.2);">FUNCIONARIO</span>
                          } @else {
                            <span style="background: rgba(13, 148, 136, 0.1); color: #0d9488; padding: 4px 10px; border-radius: 12px; font-size: 12px; font-weight: 700; border: 1px solid rgba(13, 148, 136, 0.2);">CLIENTE</span>
                          }
                        </td>
                        <td style="padding: 16px 8px; color:#475569; font-size:14px">{{ u.departamento || '—' }}</td>
                        <td style="padding: 16px 8px;">
                          @if (u.activo) {
                            <span style="background: rgba(16,185,129,0.1); color: #10b981; padding: 4px 10px; border-radius: 12px; font-size: 12px; font-weight: 700; border: 1px solid rgba(16,185,129,0.2);">
                              <span style="display: inline-block; width: 6px; height: 6px; background: #10b981; border-radius: 50%; margin-right: 4px;"></span>
                              Activo
                            </span>
                          } @else {
                            <span style="background: rgba(239,68,68,0.1); color: #ef4444; padding: 4px 10px; border-radius: 12px; font-size: 12px; font-weight: 700; border: 1px solid rgba(239,68,68,0.2);">
                              <span style="display: inline-block; width: 6px; height: 6px; background: #ef4444; border-radius: 50%; margin-right: 4px;"></span>
                              Inactivo
                            </span>
                          }
                        </td>
                        <td style="padding: 16px 8px; font-family: monospace; font-size:12px; color:#64748b">
                          {{ u.fechaCreacion ? (u.fechaCreacion | date:'dd/MM/yyyy') : '—' }}
                        </td>
                        <td style="padding: 16px 8px; text-align: right; display: flex; gap: 8px; justify-content: flex-end; align-items: center;">
                          <button class="btn-action-row"
                            (click)="abrirModalEditar(u)">
                            Editar
                          </button>
                          <button 
                            style="background: white; border: 1px solid #fca5a5; color: #ef4444; padding: 6px 12px; border-radius: 20px; font-size: 12px; font-weight: 600; cursor: pointer;"
                            (click)="eliminarUsuario(u)">
                            Eliminar
                          </button>
                        </td>
                      </tr>
                    }
                  </tbody>
                </table>
              </div>
            }
          }
        </div>
      </main>
    </div>

    <!-- Modal Usuario -->
    @if (showModalUsuario) {
      <div class="modal-overlay" (click)="cerrarModal()" style="display: flex; align-items: center; justify-content: center; position: fixed; inset: 0; background: rgba(0,0,0,0.5); z-index: 1000;">
        <div class="modal-card" (click)="$event.stopPropagation()" style="background: white; border-radius: 16px; padding: 32px; box-shadow: 0 10px 15px -3px rgba(0,0,0,0.1); width: 100%; max-width: 500px; border: 1px solid #e2e8f0;">
          <h3 style="font-size: 18px; font-weight: 700; color: #0f172a; margin: 0 0 8px 0;">{{ editandoUsuario ? '✏️ Editar Usuario' : '+ Nuevo Usuario' }}</h3>
          <p style="color: #64748b; font-size: 14px; margin: 0 0 24px 0;">{{ editandoUsuario ? 'Modifica los datos del usuario' : 'Crea un nuevo usuario en el sistema' }}</p>

          <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-bottom: 16px;">
            <div>
              <label style="display: block; font-size: 13px; font-weight: 600; color: #475569; margin-bottom: 6px;">Nombre completo *</label>
              <input type="text" style="width: 100%; padding: 10px 14px; border: 1px solid #cbd5e1; border-radius: 8px; font-size: 14px; outline: none;" [(ngModel)]="formUsuario.nombre" placeholder="Nombre completo" />
            </div>
            <div>
              <label style="display: block; font-size: 13px; font-weight: 600; color: #475569; margin-bottom: 6px;">Correo electrónico *</label>
              <input type="email" style="width: 100%; padding: 10px 14px; border: 1px solid #cbd5e1; border-radius: 8px; font-size: 14px; outline: none; background: #f8fafc;" [(ngModel)]="formUsuario.email"
                placeholder="correo@empresa.com" [disabled]="!!editandoUsuario" />
            </div>
          </div>

          @if (!editandoUsuario) {
            <div style="margin-bottom: 16px;">
              <label style="display: block; font-size: 13px; font-weight: 600; color: #475569; margin-bottom: 6px;">Contraseña *</label>
              <input type="password" style="width: 100%; padding: 10px 14px; border: 1px solid #cbd5e1; border-radius: 8px; font-size: 14px; outline: none;" [(ngModel)]="formUsuario.password" placeholder="••••••••" />
            </div>
          }

          <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-bottom: 24px;">
            <div>
              <label style="display: block; font-size: 13px; font-weight: 600; color: #475569; margin-bottom: 6px;">Rol *</label>
              <select style="width: 100%; padding: 10px 14px; border: 1px solid #cbd5e1; border-radius: 8px; font-size: 14px; outline: none; background: white;" [(ngModel)]="formUsuario.rol">
                <option value="ADMIN">ADMIN</option>
                <option value="FUNCIONARIO">FUNCIONARIO</option>
                <option value="CLIENTE">CLIENTE</option>
              </select>
            </div>
            <div>
              <label style="display: block; font-size: 13px; font-weight: 600; color: #475569; margin-bottom: 6px;">Departamento</label>
              <select style="width: 100%; padding: 10px 14px; border: 1px solid #cbd5e1; border-radius: 8px; font-size: 14px; outline: none; background: white;" [(ngModel)]="formUsuario.departamento">
                <option value="">Sin departamento</option>
                @for (d of departamentos; track d.id) {
                  <option [value]="d.nombre">{{ d.nombre }}</option>
                }
              </select>
            </div>
          </div>

          @if (modalError) {
            <div style="background: #fef2f2; border: 1px solid #fca5a5; color: #b91c1c; padding: 12px; border-radius: 8px; font-size: 13px; margin-bottom: 16px; display: flex; align-items: center; gap: 6px;">❌ {{ modalError }}</div>
          }
          @if (modalExito) {
            <div style="background: #f0fdf4; border: 1px solid #bbf7d0; color: #16a34a; padding: 12px; border-radius: 8px; font-size: 13px; margin-bottom: 16px; display: flex; align-items: center; gap: 6px;">✅ {{ modalExito }}</div>
          }

          <div style="display: flex; justify-content: flex-end; gap: 12px;">
            <button style="background: white; border: 1px solid #cbd5e1; color: #1e293b; padding: 10px 18px; border-radius: 8px; font-size: 14px; font-weight: 600; cursor: pointer; transition: all 0.2s;" (click)="cerrarModal()">Cancelar</button>
            <button style="background: #2563eb; border: none; color: white; padding: 10px 18px; border-radius: 8px; font-size: 14px; font-weight: 600; cursor: pointer; transition: all 0.2s;" (click)="guardarUsuario()" [disabled]="guardando">
              {{ guardando ? '⏳...' : (editandoUsuario ? 'Guardar cambios' : 'Crear usuario') }}
            </button>
          </div>
        </div>
      </div>
    }
  `,
  styles: [`
    .tabs { display: flex; gap: 4px; margin-bottom: 20px; border-bottom: 1px solid var(--border); }
    .tab {
      padding: 10px 18px; background: none; border: none;
      color: var(--text-muted); cursor: pointer; font-size: 13px;
      font-family: inherit; border-bottom: 2px solid transparent;
      transition: all 0.2s; margin-bottom: -1px;
    }
    .tab:hover { color: var(--text); }
    .tab.active { color: var(--primary); border-bottom-color: var(--primary); font-weight: 600; }

    .stats-row { display: flex; gap: 10px; margin-bottom: 16px; flex-wrap: wrap; }
    .stat-pill {
      display: flex; align-items: center; gap: 8px;
      padding: 8px 14px; background: var(--card);
      border: 1px solid var(--border); border-radius: 20px;
    }
    .stat-pill.green { border-color: hsl(282,69%,45%,0.3); }
    .stat-pill.blue  { border-color: hsl(216,85%,50%,0.3); }
    .stat-pill.cyan  { border-color: hsl(193,88%,38%,0.3); }
    .stat-num { font-size: 18px; font-weight: 700; color: var(--text); }
    .stat-lbl { font-size: 11px; color: var(--text-muted); }

    .filters-row { display: flex; gap: 10px; margin-bottom: 16px; flex-wrap: wrap; }

    .avatar-sm {
      width: 34px; height: 34px; border-radius: 50%; flex-shrink: 0;
      display: flex; align-items: center; justify-content: center;
      font-weight: bold; font-size: 13px; color: white;
    }

    .badge-admin       { background: hsl(282,69%,45%,0.12); color: var(--purple); }
    .badge-funcionario { background: hsl(216,85%,50%,0.12); color: var(--primary); }
    .badge-cliente     { background: hsl(193,88%,38%,0.12); color: var(--accent); }

    .btn-icon {
      width: 28px; height: 28px; border-radius: 6px;
      background: var(--bg-2); border: 1px solid var(--border-2);
      cursor: pointer; display: flex; align-items: center; justify-content: center;
      font-size: 13px; transition: all 0.15s;
    }
    .btn-icon:hover { background: var(--card-hover); }
    .btn-icon.danger:hover { background: hsl(355,80%,55%,0.12); border-color: var(--danger); }

    .dept-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(260px, 1fr)); gap: 16px; }
    .dept-card {}
    .dept-header { display: flex; gap: 12px; align-items: flex-start; margin-bottom: 12px; }
    .dept-icon {
      width: 40px; height: 40px; border-radius: 10px;
      background: hsl(216,85%,50%,0.12); display: flex;
      align-items: center; justify-content: center; font-size: 20px; flex-shrink: 0;
    }
    .dept-nombre { font-size: 14px; font-weight: 600; margin: 0 0 4px; }
    .dept-desc { font-size: 12px; color: var(--text-muted); margin: 0; }
    .dept-stats { font-size: 11px; color: var(--text-faint); }
    .dept-stat { background: var(--bg-2); padding: 3px 8px; border-radius: 6px; }

    .form-row-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; margin-bottom: 14px; }
    .table-wrap { overflow-x: auto; }

    @media (max-width: 700px) { .form-row-2 { grid-template-columns: 1fr; } }
  `]
})
export class UsersComponent implements OnInit {
  navItems = ADMIN_NAV_ITEMS;

  activeTab = 'usuarios';
  usuarios: Usuario[] = [];
  filteredUsuarios: Usuario[] = [];
  departamentos: any[] = [];
  loading = true;
  searchQuery = '';
  selectedRol = '';

  // Modal usuario
  showModalUsuario = false;
  editandoUsuario: Usuario | null = null;
  formUsuario: any = { nombre: '', email: '', password: '', rol: 'FUNCIONARIO', departamento: '' };
  guardando = false;
  modalError = '';
  modalExito = '';

  constructor(
    private usuarioService: UsuarioService,
    private deptService: DepartamentoService,
    private authService: AuthService,
    public router: Router
  ) { }

  ngOnInit(): void {
    this.cargarUsuarios();
    this.cargarDepartamentos();
  }

  cargarUsuarios(): void {
    this.loading = true;
    this.usuarioService.getAll().pipe(catchError(() => of([]))).subscribe(data => {
      this.usuarios = data;
      this.applyFilters();
      this.loading = false;
    });
  }

  cargarDepartamentos(): void {
    this.deptService.getAll().pipe(catchError(() => of([]))).subscribe(data => {
      this.departamentos = data;
    });
  }

  applyFilters(): void {
    let r = [...this.usuarios];
    if (this.searchQuery.trim()) {
      const q = this.searchQuery.toLowerCase();
      r = r.filter(u => u.nombre.toLowerCase().includes(q) || u.email.toLowerCase().includes(q));
    }
    if (this.selectedRol) r = r.filter(u => u.rol === this.selectedRol);
    this.filteredUsuarios = r;
  }

  setRolFilter(rol: string): void {
    this.selectedRol = rol;
    this.applyFilters();
  }

  getCount(rol: string): number { return this.usuarios.filter(u => u.rol === rol).length; }
  getFuncionariosDept(nombre: string): number { return this.usuarios.filter(u => u.departamento === nombre).length; }

  getAvatarColor(rol: string): string {
    const m: Record<string, string> = {
      ADMIN: 'linear-gradient(135deg,hsl(282,69%,45%),hsl(216,85%,50%))',
      FUNCIONARIO: 'linear-gradient(135deg,hsl(216,85%,50%),hsl(193,88%,38%))',
      CLIENTE: 'linear-gradient(135deg,hsl(193,88%,38%),hsl(142,60%,38%))',
    };
    return m[rol] || 'linear-gradient(135deg,#666,#888)';
  }

  getRolClass(rol: string): string {
    return { ADMIN: 'badge-admin', FUNCIONARIO: 'badge-funcionario', CLIENTE: 'badge-cliente' }[rol] || '';
  }

  // ── Modal Usuario ──────────────────────────────────────
  abrirModalCrear(): void {
    this.editandoUsuario = null;
    this.formUsuario = { nombre: '', email: '', password: '123456', rol: 'FUNCIONARIO', departamento: '' };
    this.modalError = ''; this.modalExito = '';
    this.showModalUsuario = true;
  }

  abrirModalEditar(u: Usuario): void {
    this.editandoUsuario = u;
    this.formUsuario = { nombre: u.nombre, email: u.email, rol: u.rol, departamento: u.departamento || '' };
    this.modalError = ''; this.modalExito = '';
    this.showModalUsuario = true;
  }

  cerrarModal(): void { this.showModalUsuario = false; }

  guardarUsuario(): void {
    if (!this.formUsuario.nombre || !this.formUsuario.email) {
      this.modalError = 'Nombre y email son requeridos'; return;
    }
    this.guardando = true; this.modalError = '';
    if (this.editandoUsuario) {
      this.usuarioService.update(this.editandoUsuario.id, this.formUsuario).subscribe({
        next: () => {
          this.modalExito = 'Usuario actualizado';
          this.guardando = false;
          this.cargarUsuarios();
          setTimeout(() => this.cerrarModal(), 1500);
        },
        error: (e) => { this.modalError = e.error?.error || 'Error al actualizar'; this.guardando = false; }
      });
    } else {
      this.usuarioService.register(this.formUsuario).subscribe({
        next: () => {
          this.modalExito = 'Usuario creado exitosamente';
          this.guardando = false;
          this.cargarUsuarios();
          setTimeout(() => this.cerrarModal(), 1500);
        },
        error: (e) => { this.modalError = e.error?.error || 'Error al crear usuario'; this.guardando = false; }
      });
    }
  }

  eliminarUsuario(u: Usuario): void {
    if (!confirm(`¿Eliminar a ${u.nombre}?`)) return;
    this.usuarioService.delete(u.id).subscribe({
      next: () => this.cargarUsuarios(),
      error: (e) => alert(e.error?.error || 'Error al eliminar')
    });
  }
}
