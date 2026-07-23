import { Component, OnInit } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { catchError, of } from 'rxjs';
import { AIService } from '../../services/ai/ai.service';
import { PoliticaService } from '../../services/politica/politica.service';
import { DepartamentoService } from '../../services/departamento/departamento.service';
import { SidebarComponent, NavItem, ADMIN_NAV_ITEMS } from '../../components/sidebar/sidebar.component';
import { Politica } from '../../models/models';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-analytics',
  standalone: true,
  imports: [CommonModule, DecimalPipe, FormsModule, SidebarComponent],
  template: `
    <div class="app-layout">
      <app-sidebar activeRoute="/analytics" [navItems]="navItems" />
      <main class="main-content" style="background-color: #f8fafc; padding: 40px; color: #1e293b; overflow-y: auto; flex: 1;">

        <!-- HEADER -->
        <div style="margin-bottom: 24px;">
          <h1 style="font-size: 24px; font-weight: 700; color: #0f172a; margin-bottom: 8px;">Analytics & IA</h1>
          <p style="color: #64748b; font-size: 14px; margin: 0;">Panel de control, métricas, cuellos de botella y rendimiento de la organización.</p>
        </div>

        <!-- TABS NAV -->
        <div style="display: flex; gap: 12px; margin-bottom: 32px; flex-wrap: wrap;">
          <button 
            [ngStyle]="activeTab === 'general' ? {'background': '#2563eb', 'color': 'white'} : {'background': 'white', 'color': '#1e293b'}"
            style="border: none; padding: 8px 16px; border-radius: 20px; font-weight: 600; font-size: 14px; cursor: pointer; box-shadow: 0 1px 3px rgba(0,0,0,0.1);" 
            (click)="activeTab = 'general'">
            🌐 General y Anomalías
          </button>
          <button 
            [ngStyle]="activeTab === 'politicas' ? {'background': '#2563eb', 'color': 'white'} : {'background': 'white', 'color': '#1e293b'}"
            style="border: none; padding: 8px 16px; border-radius: 20px; font-weight: 600; font-size: 14px; cursor: pointer; box-shadow: 0 1px 3px rgba(0,0,0,0.1);" 
            (click)="activeTab = 'politicas'">
            🔀 Análisis por Política
          </button>
          <button 
            [ngStyle]="activeTab === 'departamentos' ? {'background': '#2563eb', 'color': 'white'} : {'background': 'white', 'color': '#1e293b'}"
            style="border: none; padding: 8px 16px; border-radius: 20px; font-weight: 600; font-size: 14px; cursor: pointer; box-shadow: 0 1px 3px rgba(0,0,0,0.1);" 
            (click)="activeTab = 'departamentos'">
            🏢 Por Departamento/Funcionario
          </button>
        </div>

        <!-- TAB: GENERAL -->
        @if (activeTab === 'general') {
          <div style="animation: fadeIn 0.3s ease;">
            <!-- Anomalías IA (CU-29) -->
            <div style="background: white; border-radius: 12px; padding: 24px; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.05), 0 2px 4px -1px rgba(0,0,0,0.03); border: 1px solid #e2e8f0; border-left: 4px solid #ef4444; margin-bottom: 24px;">
              <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; flex-wrap: wrap; gap: 10px;">
                <h3 style="font-size: 16px; font-weight: 700; margin: 0; color: #ef4444;">🚨 Anomalías Detectadas (IA LSTM)</h3>
                <button 
                  style="background: white; border: 1px solid #cbd5e1; color: #475569; padding: 6px 12px; border-radius: 8px; font-size: 13px; font-weight: 600; cursor: pointer;" 
                  (click)="cargarAnomalias()">
                  Actualizar
                </button>
              </div>
              
              @if (loadingAnomalias) {
                <div style="padding: 20px; text-align: center; color: #64748b;"><div class="spinner" style="margin: 0 auto;"></div></div>
              } @else if (anomalias.length > 0) {
                <div style="display: flex; flex-direction: column; gap: 12px;">
                  @for (a of anomalias; track a.id) {
                    <div style="padding: 16px; background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 12px; display: flex; justify-content: space-between; align-items: center; transition: all 0.2s; flex-wrap: wrap; gap: 10px;">
                      <div>
                        <span 
                          [style.background]="a.nivelGravedad === 'CRITICO' ? 'rgba(239, 68, 68, 0.1)' : 'rgba(245, 158, 11, 0.1)'"
                          [style.color]="a.nivelGravedad === 'CRITICO' ? '#ef4444' : '#f59e0b'"
                          [style.border]="a.nivelGravedad === 'CRITICO' ? '1px solid rgba(239, 68, 68, 0.2)' : '1px solid rgba(245, 158, 11, 0.2)'"
                          style="padding: 4px 10px; border-radius: 12px; font-size: 12px; font-weight: 700; display: inline-block;">
                          {{ a.nivelGravedad }}
                        </span>
                        <strong style="margin-left: 8px; color: #0f172a; font-size: 14px;">Trámite: {{ a.tramiteId.substring(0,8) }}... (Nodo: {{ a.nodoId }})</strong>
                        <p style="margin: 4px 0 0; font-size: 13px; color: #64748b;">{{ a.descripcion }}</p>
                      </div>
                      <div>
                        <button class="btn-action-row" 
                          (click)="resolverAnomalia(a.id)">
                          Marcar Resuelta
                        </button>
                      </div>
                    </div>
                  }
                </div>
              } @else {
                 <div style="padding: 40px; text-align: center; color: #64748b; background: #f8fafc; border-radius: 8px; border: 1px dashed #cbd5e1; font-size: 14px;">
                   No se han detectado anomalías graves recientes. ¡Todo marcha bien!
                 </div>
              }
            </div>

            <!-- KPIs Globales -->
            @if (kpis) {
              <div style="background: white; border-radius: 12px; padding: 24px; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.05), 0 2px 4px -1px rgba(0,0,0,0.03); border: 1px solid #e2e8f0; margin-bottom: 24px;">
                <h3 style="font-size: 16px; font-weight: 700; color: #0f172a; margin: 0 0 16px 0;">📈 KPIs Globales de la Empresa</h3>
                <div style="display: grid; grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); gap: 16px;">
                  <div style="background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 12px; padding: 20px; text-align: center;">
                    <p style="font-size: 11px; font-weight: 700; color: #64748b; text-transform: uppercase; letter-spacing: 0.5px; margin: 0 0 8px;">TOTAL TRÁMITES</p>
                    <p style="font-size: 28px; font-weight: 800; margin: 0; color: #2563eb;">{{ kpis.totalTramites }}</p>
                  </div>
                  <div style="background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 12px; padding: 20px; text-align: center;">
                    <p style="font-size: 11px; font-weight: 700; color: #64748b; text-transform: uppercase; letter-spacing: 0.5px; margin: 0 0 8px;">TASA COMPLETADO</p>
                    <p style="font-size: 28px; font-weight: 800; margin: 0; color: #10b981;">{{ kpis.tasaCompletadoPct }}%</p>
                  </div>
                  <div style="background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 12px; padding: 20px; text-align: center;">
                    <p style="font-size: 11px; font-weight: 700; color: #64748b; text-transform: uppercase; letter-spacing: 0.5px; margin: 0 0 8px;">ACTIVOS AHORA</p>
                    <p style="font-size: 28px; font-weight: 800; margin: 0; color: #f59e0b;">{{ kpis.tramitesActivos }}</p>
                  </div>
                  <div style="background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 12px; padding: 20px; text-align: center;">
                    <p style="font-size: 11px; font-weight: 700; color: #64748b; text-transform: uppercase; letter-spacing: 0.5px; margin: 0 0 8px;">DURACIÓN PROM.</p>
                    <p style="font-size: 28px; font-weight: 800; margin: 0; color: #0f172a;">{{ kpis.duracionPromedioMinutos }} min</p>
                  </div>
                  <div style="background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 12px; padding: 20px; text-align: center;">
                    <p style="font-size: 11px; font-weight: 700; color: #64748b; text-transform: uppercase; letter-spacing: 0.5px; margin: 0 0 8px;">TAREAS PENDIENTES</p>
                    <p style="font-size: 28px; font-weight: 800; margin: 0; color: #ef4444;">{{ kpis.tareasPendientes }}</p>
                  </div>
                  <div style="background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 12px; padding: 20px; text-align: center;">
                    <p style="font-size: 11px; font-weight: 700; color: #64748b; text-transform: uppercase; letter-spacing: 0.5px; margin: 0 0 8px;">RECHAZADOS</p>
                    <p style="font-size: 28px; font-weight: 800; margin: 0; color: #ef4444;">{{ kpis.tramitesRechazados }}</p>
                  </div>
                </div>
              </div>
            }
          </div>
        }

        <!-- TAB: POR POLÍTICA -->
        @if (activeTab === 'politicas') {
          <div style="animation: fadeIn 0.3s ease;">
            <div style="background: white; border-radius: 12px; padding: 24px; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.05), 0 2px 4px -1px rgba(0,0,0,0.03); border: 1px solid #e2e8f0; margin-bottom: 24px;">
              <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; flex-wrap: wrap; gap: 16px;">
                <h3 style="font-size: 16px; font-weight: 700; color: #0f172a; margin: 0;">🤖 Análisis de Cuellos de Botella en Políticas</h3>
                <div style="display: flex; gap: 12px; align-items: center;">
                  <select 
                    style="width: 250px; padding: 10px 14px; border: 1px solid #cbd5e1; border-radius: 8px; font-size: 14px; outline: none; background: white;" 
                    [(ngModel)]="politicaSeleccionada" 
                    (change)="analizarPolitica()">
                    <option value="">Seleccionar política a analizar...</option>
                    @for (p of politicas; track p.id) {
                      <option [value]="p.id">{{ p.nombre }}</option>
                    }
                  </select>
                  <button 
                    style="background: #2563eb; border: none; color: white; padding: 10px 18px; border-radius: 8px; font-weight: 600; cursor: pointer; font-size: 14px;" 
                    (click)="analizarPolitica()" 
                    [disabled]="!politicaSeleccionada || loadingAnalisis">
                    {{ loadingAnalisis ? '⏳ Analizando...' : 'Analizar' }}
                  </button>
                </div>
              </div>

              @if (loadingAnalisis) {
                <div style="padding: 40px; text-align: center; color: #64748b;"><div class="spinner" style="margin: 0 auto 10px;"></div><p style="margin: 0;">La IA está analizando los datos históricos de la política seleccionada...</p></div>
              } @else if (analisisIA) {
                <div style="display: flex; gap: 24px; margin-bottom: 24px; flex-wrap: wrap; background: #f8fafc; padding: 20px; border-radius: 12px; border: 1px solid #e2e8f0;">
                  <div style="flex: 1; min-width: 140px; text-align: center; border-right: 1px solid #e2e8f0; padding: 0 16px;">
                    <p style="font-size: 10px; color: #64748b; font-weight: 700; text-transform: uppercase; letter-spacing: 0.5px; margin: 0;">TAREAS ANALIZADAS</p>
                    <p style="font-size: 28px; font-weight: 800; color: #2563eb; margin: 4px 0 0 0;">{{ analisisIA.totalTareas || 0 }}</p>
                  </div>
                  <div style="flex: 1; min-width: 140px; text-align: center; border-right: 1px solid #e2e8f0; padding: 0 16px;">
                    <p style="font-size: 10px; color: #64748b; font-weight: 700; text-transform: uppercase; letter-spacing: 0.5px; margin: 0;">PROMEDIO GENERAL</p>
                    <p style="font-size: 28px; font-weight: 800; color: #0f172a; margin: 4px 0 0 0;">{{ analisisIA.promedioMinutos | number:'1.0-0' }} min</p>
                  </div>
                  <div style="flex: 1; min-width: 140px; text-align: center; padding: 0 16px;">
                    <p style="font-size: 10px; color: #64748b; font-weight: 700; text-transform: uppercase; letter-spacing: 0.5px; margin: 0;">CUELLS DETECTADOS</p>
                    <p 
                      [style.color]="(analisisIA.cuellosDetectados?.length || 0) > 0 ? '#ef4444' : '#10b981'"
                      style="font-size: 28px; font-weight: 800; margin: 4px 0 0 0;">
                      {{ analisisIA.cuellosDetectados?.length || 0 }}
                    </p>
                  </div>
                </div>

                @for (c of analisisIA.cuellosDetectados || []; track c.nodoId) {
                  <div style="padding: 16px; margin-bottom: 12px; background: rgba(239, 68, 68, 0.08); border: 1px solid rgba(239, 68, 68, 0.15); border-radius: 12px; font-size: 13px; line-height: 1.5; color: #334155;">
                    ⚠️ El paso <strong>{{ c.nombreNodo || c.nodoId }}</strong> está
                    <strong style="color: #ef4444;">{{ c.excesoPct | number:'1.0-0' }}% por encima</strong>
                    del promedio de tiempo de todo el trámite (Toma {{ c.promedioMinutos | number:'1.0-0' }} min)
                  </div>
                }

                @if (analisisIA.recomendacion) {
                  <div style="background: white; border-radius: 12px; padding: 20px; border: 1px solid #e2e8f0; border-left: 4px solid #8b5cf6; margin-top: 16px;">
                    <p style="font-size: 13px; font-weight: 700; color: #8b5cf6; margin: 0 0 8px 0; display: flex; align-items: center; gap: 6px;">💡 Recomendaciones Estratégicas de la IA</p>
                    <p style="font-size: 14px; color: #334155; margin: 0; line-height: 1.6;">{{ analisisIA.recomendacion }}</p>
                  </div>
                }
              } @else {
                <div style="padding: 40px; text-align: center; color: #64748b; background: #f8fafc; border-radius: 8px; border: 1px dashed #cbd5e1; font-size: 14px; margin-top: 30px;">
                  Selecciona una política en el menú superior para identificar cuellos de botella.
                </div>
              }
            </div>
          </div>
        }

        <!-- TAB: DEPARTAMENTOS -->
        @if (activeTab === 'departamentos') {
          <div style="animation: fadeIn 0.3s ease;">
            <!-- Promedios por departamento -->
            <div style="background: white; border-radius: 12px; padding: 24px; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.05), 0 2px 4px -1px rgba(0,0,0,0.03); border: 1px solid #e2e8f0; margin-bottom: 24px;">
              <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;">
                <h3 style="font-size: 16px; font-weight: 700; color: #0f172a; margin: 0;">⏱️ Tiempo Promedio por Departamento</h3>
                <button 
                  style="background: white; border: 1px solid #cbd5e1; color: #475569; padding: 6px 12px; border-radius: 8px; font-size: 13px; font-weight: 600; cursor: pointer;" 
                  (click)="cargarPromediosDept()">
                  Actualizar
                </button>
              </div>
              @if (loadingDept) {
                <div style="padding: 30px; text-align: center; color: #64748b;"><div class="spinner" style="margin: 0 auto;"></div></div>
              } @else if (promediosDept && objectKeys(promediosDept).length > 0) {
                <div style="display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap: 16px;">
                  @for (key of objectKeys(promediosDept); track key) {
                    <div style="background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 12px; padding: 18px;">
                      <div style="font-size: 13px; color: #64748b; margin-bottom: 6px; font-weight: 600;">{{ key }}</div>
                      <div style="font-size: 24px; font-weight: 800; color: #0f172a; margin-bottom: 10px;">{{ promediosDept[key] | number:'1.0-0' }} min</div>
                      <div style="height: 6px; background: #e2e8f0; border-radius: 3px; overflow: hidden;">
                        <div style="height: 100%; border-radius: 3px; transition: width 0.8s ease;"
                          [style.width]="getBarWidth(promediosDept[key]) + '%'"
                          [style.background]="getBarColor(promediosDept[key])"></div>
                      </div>
                    </div>
                  }
                </div>
              } @else {
                <div style="padding: 40px; text-align: center; color: #64748b; background: #f8fafc; border-radius: 8px; border: 1px dashed #cbd5e1; font-size: 14px;">
                  No hay datos suficientes de tareas para calcular el promedio por departamento.
                </div>
              }
            </div>

            <!-- Eficiencia de funcionarios -->
            <div style="background: white; border-radius: 12px; padding: 24px; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.05), 0 2px 4px -1px rgba(0,0,0,0.03); border: 1px solid #e2e8f0;">
              <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; flex-wrap: wrap; gap: 10px;">
                <h3 style="font-size: 16px; font-weight: 700; color: #0f172a; margin: 0;">👥 Eficiencia y Rendimiento por Funcionario</h3>
                <select 
                  style="width: 250px; padding: 10px 14px; border: 1px solid #cbd5e1; border-radius: 8px; font-size: 14px; outline: none; background: white;" 
                  [(ngModel)]="deptSeleccionado" 
                  (change)="cargarEficiencia()">
                  <option value="">Seleccionar departamento a analizar...</option>
                  @for (dept of departamentos; track dept.id) {
                    <option [value]="dept.nombre">{{ dept.nombre }}</option>
                  }
                </select>
              </div>
              @if (deptSeleccionado) {
                @if (eficiencia.length > 0) {
                  <div style="overflow-x: auto; border: 1px solid #e2e8f0; border-radius: 12px; margin-top: 10px;">
                    <table style="width: 100%; border-collapse: collapse; text-align: left;">
                      <thead>
                        <tr style="border-bottom: 2px solid #e2e8f0;">
                          <th style="padding: 12px 16px; font-size: 12px; font-weight: 700; color: #0f172a;"># RKG</th>
                          <th style="padding: 12px 16px; font-size: 12px; font-weight: 700; color: #0f172a;">FUNCIONARIO</th>
                          <th style="padding: 12px 16px; font-size: 12px; font-weight: 700; color: #0f172a;">PROMEDIO TAREA (min)</th>
                          <th style="padding: 12px 16px; font-size: 12px; font-weight: 700; color: #0f172a;">ESTADO DE EFICIENCIA</th>
                        </tr>
                      </thead>
                      <tbody>
                        @for (f of eficiencia; track f.funcionarioId; let i = $index) {
                          <tr style="border-bottom: 1px solid #f1f5f9;">
                            <td style="padding: 16px; font-family: monospace; font-size: 14px; font-weight: bold; color: #64748b;">{{ i + 1 }}</td>
                            <td style="padding: 16px; font-size: 14px; color: #334155; font-weight: 500;">{{ f.nombreFuncionario || f.funcionarioId }}</td>
                            <td style="padding: 16px; font-family: monospace; font-size: 14px; color: #334155;">{{ f.promedioDuracion | number:'1.0-0' }}</td>
                            <td style="padding: 16px;">
                              <span 
                                [style.background]="i === 0 ? 'rgba(16, 185, 129, 0.1)' : (i === eficiencia.length - 1 && eficiencia.length > 1) ? 'rgba(239, 68, 68, 0.1)' : 'rgba(37, 99, 235, 0.1)'"
                                [style.color]="i === 0 ? '#10b981' : (i === eficiencia.length - 1 && eficiencia.length > 1) ? '#ef4444' : '#2563eb'"
                                [style.border]="i === 0 ? '1px solid rgba(16, 185, 129, 0.2)' : (i === eficiencia.length - 1 && eficiencia.length > 1) ? '1px solid rgba(239, 68, 68, 0.2)' : '1px solid rgba(37, 99, 235, 0.2)'"
                                style="padding: 4px 10px; border-radius: 12px; font-size: 12px; font-weight: 700; display: inline-block;">
                                {{ i === 0 ? '🏆 Sobresaliente' : (i === eficiencia.length - 1 && eficiencia.length > 1) ? '🐢 Requiere Mejora' : '⚡ Estable' }}
                              </span>
                            </td>
                          </tr>
                        }
                      </tbody>
                    </table>
                  </div>
                } @else {
                  <div style="padding: 40px; text-align: center; color: #64748b; background: #f8fafc; border-radius: 8px; border: 1px dashed #cbd5e1; font-size: 14px;">
                    No hay datos de funcionarios para el departamento seleccionado.
                  </div>
                }
              } @else {
                <div style="padding: 40px; text-align: center; color: #64748b; background: #f8fafc; border-radius: 8px; border: 1px dashed #cbd5e1; font-size: 14px;">
                  Selecciona un departamento en el menú de la derecha para ver la eficiencia de sus funcionarios.
                </div>
              }
            </div>
          </div>
        }
      </main>
    </div>
  `,
  styles: [`
    @keyframes fadeIn { from { opacity: 0; transform: translateY(5px); } to { opacity: 1; transform: translateY(0); } }
  `]
})
export class AnalyticsComponent implements OnInit {
  navItems = ADMIN_NAV_ITEMS;

  politicas: Politica[] = [];
  politicaSeleccionada = '';
  deptSeleccionado = '';
  activeTab = 'general';
  analisisIA: any = null;
  promediosDept: any = {};
  eficiencia: any[] = [];
  kpis: any = null;
  anomalias: any[] = [];
  departamentos: any[] = [];
  loadingAnalisis = false;
  loadingDept = false;
  loadingAnomalias = false;

  objectKeys = Object.keys;

  constructor(
    private aiService: AIService,
    private politicaService: PoliticaService,
    private departamentoService: DepartamentoService,
    private http: HttpClient,
    public router: Router
  ) { }

  ngOnInit(): void {
    this.politicaService.getAll().pipe(catchError(() => of([]))).subscribe(p => {
      this.politicas = p;
    });
    this.departamentoService.getAll().pipe(catchError(() => of([]))).subscribe(depts => {
      this.departamentos = depts;
    });
    this.cargarPromediosDept();
    this.cargarKpis();
    this.cargarAnomalias();
  }

  cargarAnomalias(): void {
    this.loadingAnomalias = true;
    this.http.get<any[]>(`${environment.apiUrl}/predictor/anomalias`)
      .pipe(catchError(() => of([])))
      .subscribe(data => {
        this.anomalias = data;
        this.loadingAnomalias = false;
      });
  }

  resolverAnomalia(id: string): void {
    this.http.post(`${environment.apiUrl}/predictor/anomalias/${id}/resolver`, {})
      .subscribe(() => {
        this.anomalias = this.anomalias.filter(a => a.id !== id);
      });
  }

  cargarKpis(): void {
    this.http.get<any>(`${environment.apiUrl}/analytics/kpis`)
      .pipe(catchError(() => of(null)))
      .subscribe(data => { this.kpis = data; });
  }

  analizarPolitica(): void {
    if (!this.politicaSeleccionada) return;
    this.loadingAnalisis = true;
    this.aiService.detectarCuellosBottella(this.politicaSeleccionada)
      .pipe(catchError(() => of(null)))
      .subscribe(data => {
        this.analisisIA = data;
        this.loadingAnalisis = false;
      });
  }

  cargarPromediosDept(): void {
    this.loadingDept = true;
    this.http.get<any>(`${environment.apiUrl}/analytics/departamentos`)
      .pipe(catchError(() => of({})))
      .subscribe(data => {
        this.promediosDept = data;
        this.loadingDept = false;
      });
  }

  cargarEficiencia(): void {
    if (!this.deptSeleccionado) return;
    this.http.get<any[]>(`${environment.apiUrl}/analytics/funcionarios/${encodeURIComponent(this.deptSeleccionado)}`)
      .pipe(catchError(() => of([])))
      .subscribe(data => { this.eficiencia = data; });
  }

  getBarWidth(val: number): number {
    const max = Math.max(...Object.values(this.promediosDept) as number[]);
    return max > 0 ? (val / max) * 100 : 0;
  }

  getBarColor(val: number): string {
    const max = Math.max(...Object.values(this.promediosDept) as number[]);
    const pct = max > 0 ? val / max : 0;
    if (pct > 0.7) return 'var(--danger)';
    if (pct > 0.4) return 'var(--warning)';
    return 'var(--success)';
  }
}
