import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SidebarComponent, NavItem, ADMIN_NAV_ITEMS } from '../../../components/sidebar/sidebar.component';
import { PoliticaService } from '../../../services/politica/politica.service';
import { AIService } from '../../../services/ai/ai.service';
import { Politica } from '../../../models/models';

export interface AIResult {
  totalTareas: number;
  promedioMinutos: number;
  cuellos: { nodo: string; excesoPct: number }[];
  recomendacion: string;
}

@Component({
  selector: 'app-admin-cuellos',
  standalone: true,
  imports: [CommonModule, SidebarComponent],
  template: `
    <div class="app-layout">
      <app-sidebar activeRoute="/admin/cuellos-botella" [navItems]="navItems" />
      <main class="main-content" style="background-color: #f8fafc; padding: 40px; color: #1e293b; overflow-y: auto; flex: 1;">
        
        <!-- HEADER -->
        <div style="margin-bottom: 24px;">
          <h1 style="font-size: 24px; font-weight: 700; color: #0f172a; margin-bottom: 8px;">Análisis de Cuellos de Botella (IA)</h1>
          <p style="color: #64748b; font-size: 14px; margin: 0;">Identifica demoras por funcionario y departamento utilizando el motor de IA.</p>
        </div>

        <!-- SELECTOR CARD -->
        <div style="background: white; border-radius: 12px; padding: 24px; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.05), 0 2px 4px -1px rgba(0,0,0,0.03); border: 1px solid #e2e8f0; margin-bottom: 24px;">
          <h3 style="font-size: 16px; font-weight: 700; color: #0f172a; margin: 0 0 16px 0;">Selecciona una Política para analizar</h3>
          <div style="display: flex; gap: 16px; align-items: center; flex-wrap: wrap;">
            <select 
              style="flex: 1; max-width: 400px; padding: 10px 14px; border: 1px solid #cbd5e1; border-radius: 8px; font-size: 14px; outline: none; background: white;"
              (change)="onPoliticaSelect($event)">
              <option value="">Seleccione...</option>
              @for(p of politicas; track p.id) {
                <option [value]="p.id">{{ p.nombre }}</option>
              }
            </select>
            <button 
              style="background: #2563eb; border: none; color: white; padding: 10px 24px; border-radius: 8px; font-weight: 600; cursor: pointer; display: flex; align-items: center; gap: 8px; transition: all 0.2s;"
              [disabled]="!selectedId || loading" 
              (click)="analizar()">
              @if (loading) { <span class="spinner-sm"></span> }
              @else { 🔍 Analizar Rendimiento }
            </button>
          </div>
        </div>

        @if (error) {
          <div style="background: #fef2f2; border: 1px solid #fca5a5; color: #b91c1c; padding: 16px; border-radius: 8px; font-size: 14px; font-weight: 500; margin-bottom: 24px;">
            ❌ {{ error }}
          </div>
        }

        @if (aiResult) {
          <!-- STATS GRID -->
          <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 24px; margin-bottom: 24px;">
            <div style="background: white; border-radius: 12px; padding: 24px; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.05), 0 2px 4px -1px rgba(0,0,0,0.03); border: 1px solid #e2e8f0; display: flex; align-items: center; gap: 16px;">
              <div style="width: 44px; height: 44px; border-radius: 8px; background: rgba(139, 92, 246, 0.1); color: #8b5cf6; display: flex; align-items: center; justify-content: center; font-size: 22px; flex-shrink: 0;">
                📊
              </div>
              <div>
                <span style="font-size: 11px; font-weight: 700; color: #64748b; text-transform: uppercase; letter-spacing: 0.5px; display: block;">Total Tareas Analizadas</span>
                <span style="font-size: 24px; font-weight: 800; color: #0f172a; margin-top: 4px; display: block;">{{ aiResult.totalTareas }}</span>
              </div>
            </div>
            
            <div style="background: white; border-radius: 12px; padding: 24px; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.05), 0 2px 4px -1px rgba(0,0,0,0.03); border: 1px solid #e2e8f0; display: flex; align-items: center; gap: 16px;">
              <div style="width: 44px; height: 44px; border-radius: 8px; background: rgba(37, 99, 235, 0.1); color: #2563eb; display: flex; align-items: center; justify-content: center; font-size: 22px; flex-shrink: 0;">
                ⏱️
              </div>
              <div>
                <span style="font-size: 11px; font-weight: 700; color: #64748b; text-transform: uppercase; letter-spacing: 0.5px; display: block;">Promedio de Ejecución</span>
                <span style="font-size: 24px; font-weight: 800; color: #0f172a; margin-top: 4px; display: block;">{{ aiResult.promedioMinutos | number:'1.0-0' }} min</span>
              </div>
            </div>
          </div>

          <!-- RECOMMENDATION CARD -->
          <div style="background: white; border-radius: 12px; padding: 24px; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.05), 0 2px 4px -1px rgba(0,0,0,0.03); border: 1px solid #e2e8f0; border-left: 4px solid #2563eb; margin-bottom: 24px;">
            <div style="display: flex; align-items: center; gap: 8px; margin-bottom: 12px;">
              <span style="font-size: 18px;">✨</span>
              <h3 style="font-size: 16px; font-weight: 700; color: #2563eb; margin: 0;">Recomendación de la IA</h3>
            </div>
            <p style="margin: 0; font-size: 14px; line-height: 1.6; color: #334155;">{{ aiResult.recomendacion }}</p>
          </div>

          <!-- CUELLOS DE BOTELLA CARD -->
          <div style="background: white; border-radius: 12px; padding: 24px; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.05), 0 2px 4px -1px rgba(0,0,0,0.03); border: 1px solid #e2e8f0;">
            <h3 style="font-size: 16px; font-weight: 700; color: #0f172a; margin: 0 0 16px 0;">Nodos y Funcionarios con Cuellos de Botella</h3>
            @if (aiResult.cuellos.length === 0) {
              <div style="padding: 32px; text-align: center; color: #64748b; background: #f8fafc; border-radius: 8px; border: 1px dashed #cbd5e1; font-size: 14px; font-weight: 500;">
                No se detectaron cuellos de botella significativos en este flujo.
              </div>
            } @else {
              <div style="display: flex; flex-direction: column; gap: 16px;">
                @for(c of aiResult.cuellos; track c.nodo) {
                  <div style="display: flex; justify-content: space-between; align-items: center; padding: 16px; background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 12px; transition: all 0.2s;">
                    <div style="display: flex; flex-direction: column; gap: 4px;">
                      <span style="font-weight: 700; color: #0f172a; font-size: 14px;">{{ c.nodo }}</span>
                      <span style="font-size: 12px; color: #64748b;">Exceso detectado en los tiempos de respuesta.</span>
                    </div>
                    <div style="display: flex; align-items: center; gap: 6px; background: rgba(239, 68, 68, 0.08); color: #ef4444; padding: 4px 12px; border-radius: 12px; font-weight: 700; font-size: 13px; border: 1px solid rgba(239, 68, 68, 0.15);">
                      ⚠️ +{{ c.excesoPct | number:'1.0-0' }}% demora
                    </div>
                  </div>
                }
              </div>
            }
          </div>
        }
      </main>
    </div>
  `,
  styles: [`
    .spinner-sm { width: 16px; height: 16px; border: 2px solid rgba(255,255,255,0.3); border-top-color: white; border-radius: 50%; display: inline-block; animation: spin 0.8s linear infinite; }
    @keyframes spin { to { transform: rotate(360deg); } }
  `]
})
export class AdminCuellosComponent implements OnInit {
  politicas: Politica[] = [];
  selectedId = '';
  loading = false;
  error = '';
  aiResult: AIResult | null = null;

  navItems = ADMIN_NAV_ITEMS;

  private politicaService = inject(PoliticaService);
  private aiService = inject(AIService);

  ngOnInit() {
    this.politicaService.getAll().subscribe(p => this.politicas = p);
  }

  onPoliticaSelect(event: any) {
    this.selectedId = event.target.value;
    this.aiResult = null;
    this.error = '';
  }

  analizar() {
    if (!this.selectedId) return;
    this.loading = true;
    this.error = '';
    this.aiResult = null;

    this.aiService.detectarCuellosBottella(this.selectedId).subscribe({
      next: (res: any) => {
        this.loading = false;
        this.aiResult = this.parseAIResult(res);
      },
      error: (err) => {
        this.loading = false;
        this.error = err.error?.error || 'Error al conectar con la IA.';
      }
    });
  }

  private parseAIResult(res: any): AIResult {
    const cuellos: { nodo: string; excesoPct: number }[] = [];
    const cuellosArr = res.cuellosBottella || res.cuellosDetectados || [];
    if (Array.isArray(cuellosArr)) {
      for (const c of cuellosArr) {
        cuellos.push({
          nodo: c.nombreNodo || c.nodo || c.nodeId || 'Nodo desconocido',
          excesoPct: c.excesoPorcentaje ?? c.excesoPct ?? c.excess ?? 0,
        });
      }
    }
    return {
      totalTareas: res.totalTareas ?? res.totalTasks ?? 0,
      promedioMinutos: res.promedioMinutos ?? res.averageMinutes ?? 0,
      cuellos,
      recomendacion: res.recomendacion ?? res.recommendation ?? '',
    };
  }
}
