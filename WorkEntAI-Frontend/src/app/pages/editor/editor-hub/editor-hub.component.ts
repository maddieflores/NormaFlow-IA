import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { PoliticaService } from '../../../services/politica/politica.service';
import { Politica } from '../../../models/models';
import { SidebarComponent, ADMIN_NAV_ITEMS } from '../../../components/sidebar/sidebar.component';

@Component({
  selector: 'app-editor-hub',
  standalone: true,
  imports: [CommonModule, FormsModule, SidebarComponent],
  template: `
    <div class="admin-layout">
      <app-sidebar [activeRoute]="'/editor'" [navItems]="navItems"></app-sidebar>
      <div class="admin-main" style="background: linear-gradient(180deg, #f0f8ff 0%, #ffffff 200px);">
        <div class="hub-container">
          <div class="hub-header">
            <div>
              <h1 class="hub-title">Editor de diagramas</h1>
              <p class="hub-subtitle">Selecciona una política en borrador para abrir el editor UML.</p>
            </div>
            <button class="btn-create" (click)="createNew()">+ Crear Nueva Política</button>
          </div>

          <div class="hub-card">
            <h2 class="card-title">Unirme por link/código (colaborativo)</h2>
            <div class="join-group">
              <input type="text" class="hub-input" placeholder="Pega aquí el link que te compartieron (.../policies/<id>/diagram?collab=1&who=...) o el ID" [(ngModel)]="joinInput">
              <button class="btn-secondary join-btn" (click)="joinById()">Unirme</button>
            </div>
            <p class="card-hint">Tip: si solo te pasan el <strong>ID</strong> de la política, también sirve pegarlo (ej: 6620...).</p>
          </div>

          <div class="hub-card">
            <h2 class="card-title">Políticas en estado BORRADOR</h2>
            
            @if (loading) {
              <div class="loading-state">Cargando borradores...</div>
            } @else if (drafts.length === 0) {
              <div class="empty-state">No tienes políticas en borrador en este momento.</div>
            } @else {
              <table class="hub-table">
                <thead>
                  <tr>
                    <th>Nombre</th>
                    <th>Versión</th>
                    <th style="width: 180px; text-align: right;">Acción</th>
                  </tr>
                </thead>
                <tbody>
                  @for (draft of drafts; track draft.id) {
                    <tr>
                      <td>{{ draft.nombre?.trim() || '(Sin nombre)' }}</td>
                      <td>{{ draft.version || 1 }}</td>
                      <td style="text-align: right; display: flex; gap: 8px; justify-content: flex-end; align-items: center;">
                        <button class="btn-secondary" (click)="openEditor(draft.id)">Abrir editor</button>
                        <button class="btn-delete-policy" (click)="deletePolicy(draft.id)" title="Eliminar política">
                          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M3 6h18M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>
                        </button>
                      </td>
                    </tr>
                  }
                </tbody>
              </table>
            }
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    :host {
      display: block;
      height: 100vh;
      font-family: 'Inter', system-ui, sans-serif;
    }

    .admin-layout {
      display: flex;
      height: 100vh;
      overflow: hidden;
    }
    
    .admin-main {
      flex: 1;
      display: flex;
      flex-direction: column;
      overflow-y: auto;
      background: linear-gradient(180deg, #f0f8ff 0%, #ffffff 200px);
    }
    
    .hub-container {
      padding: 40px;
      width: 100%;
      max-width: 1400px;
    }
    
    .hub-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      margin-bottom: 40px;
    }
    
    .hub-title {
      font-size: 28px;
      font-weight: 700;
      color: #0f172a;
      margin: 0 0 12px 0;
    }
    
    .hub-subtitle {
      font-size: 15px;
      color: #475569;
      margin: 0;
    }
    
    .btn-create {
      background: #3b82f6;
      color: white;
      border: none;
      padding: 12px 24px;
      border-radius: 8px;
      font-size: 15px;
      font-weight: 600;
      cursor: pointer;
      transition: background 0.2s;
      box-shadow: 0 4px 6px -1px rgba(59, 130, 246, 0.2);
    }
    .btn-create:hover { background: #2563eb; }
    
    .hub-card {
      background: white;
      border: 1px solid #e2e8f0;
      border-radius: 12px;
      padding: 24px;
      margin-bottom: 24px;
      box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.05);
    }
    
    .card-title {
      font-size: 15px;
      font-weight: 700;
      color: #334155;
      margin: 0 0 20px 0;
    }
    
    .join-group {
      display: flex;
      gap: 12px;
      margin-bottom: 12px;
    }
    
    .hub-input {
      flex: 1;
      padding: 12px 16px;
      border: 1px solid #cbd5e1;
      border-radius: 8px;
      font-size: 15px;
      outline: none;
      transition: border-color 0.2s;
    }
    .hub-input:focus { border-color: #3b82f6; }
    
    .btn-secondary {
      background: white;
      border: 1px solid #cbd5e1;
      color: #0f172a;
      padding: 10px 20px;
      border-radius: 8px;
      font-size: 14px;
      font-weight: 600;
      cursor: pointer;
      transition: all 0.2s;
    }
    .btn-secondary:hover { background: #f8fafc; border-color: #94a3b8; }
    
    .btn-delete-policy {
      background: white;
      border: 1px solid #e2e8f0;
      color: #ef4444;
      padding: 10px;
      border-radius: 8px;
      cursor: pointer;
      display: flex;
      align-items: center;
      justify-content: center;
      transition: all 0.2s;
    }
    .btn-delete-policy:hover {
      background: #fef2f2;
      border-color: #fca5a5;
      color: #dc2626;
    }
    
    .join-btn { background: #7c9cfc; color: white; border: none; }
    .join-btn:hover { background: #6384f5; }
    
    .card-hint { font-size: 13px; color: #64748b; margin: 0; }
    
    .hub-table {
      width: 100%;
      border-collapse: collapse;
    }
    .hub-table th {
      text-align: left;
      font-size: 13px;
      font-weight: 700;
      color: #0f172a;
      padding: 16px 8px;
      border-bottom: 1px solid #e2e8f0;
    }
    .hub-table td {
      padding: 16px 8px;
      border-bottom: 1px solid #f1f5f9;
      color: #475569;
      font-size: 14px;
    }
    .hub-table tr:last-child td { border-bottom: none; }
    
    .loading-state, .empty-state {
      padding: 30px;
      text-align: center;
      color: #64748b;
      font-size: 15px;
    }
  `]
})
export class EditorHubComponent implements OnInit {
  private router = inject(Router);
  private politicaService = inject(PoliticaService);

  navItems = ADMIN_NAV_ITEMS;
  drafts: Politica[] = [];
  loading = true;
  joinInput = '';

  ngOnInit() {
    this.politicaService.getAll().subscribe({
      next: (politicas) => {
        this.drafts = politicas.filter(p => p.estado === 'BORRADOR');
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  createNew() {
    const nueva: Partial<Politica> = {
      nombre: 'Nueva Política',
      estado: 'BORRADOR',
      version: 1
    };

    this.politicaService.create(nueva).subscribe({
      next: (res) => {
        if (res && res.id) {
          this.router.navigate(['/editor', res.id]);
        }
      }
    });
  }

  joinById() {
    if (!this.joinInput.trim()) return;

    let id = this.joinInput.trim();
    const match = id.match(/\/policies\/([a-zA-Z0-9_-]+)\//);
    if (match && match[1]) {
      id = match[1];
    } else {
      const parts = id.split('/');
      const last = parts[parts.length - 1];
      if (last && last.length > 0 && !id.includes('?')) {
        id = last;
      }
    }

    this.router.navigate(['/editor', id]);
  }

  openEditor(id: string | undefined) {
    if (id) {
      this.router.navigate(['/editor', id]);
    }
  }

  deletePolicy(id: string | undefined) {
    if (!id) return;
    if (confirm('¿Estás seguro de que deseas eliminar esta política en borrador?')) {
      this.politicaService.delete(id).subscribe({
        next: () => {
          this.drafts = this.drafts.filter(p => p.id !== id);
        },
        error: (err) => {
          console.error("Error al eliminar la política", err);
          alert("Ocurrió un error al intentar eliminar la política.");
        }
      });
    }
  }
}
