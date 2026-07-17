import { Component, ElementRef, OnInit, OnDestroy, AfterViewInit, ViewChild, inject, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { EditorStateService, EditorNodeData } from '../../editor-state.service';
import { PoliticaService } from '../../../../services/politica/politica.service';
import { WebSocketService } from '../../../../services/websocket/websocket.service';
import { AuthService } from '../../../../services/auth/auth.service';
import { Politica, Nodo } from '../../../../models/models';
import { AIService } from '../../../../services/ai/ai.service';
import { DepartamentoService, Departamento } from '../../../../services/departamento/departamento.service';
import { FormsModule } from '@angular/forms';

declare const go: any;

@Component({
  selector: 'app-editor-canvas',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="canvas-wrapper">
      <div class="left-palette" [class.collapsed]="!state.elementsOpen()">
        <div class="ptitle">Elementos</div>
        <div #paletteDiv class="palette-div"></div>
      </div>
      <div #diagramDiv class="diagram-div"></div>
      
      @if (showDeptModal) {
        <div class="modal-overlay">
          <div class="modal-content">
            <h3>Seleccionar Departamento</h3>
            <p>Elige el departamento para la nueva calle:</p>
            <select class="form-select" [(ngModel)]="selectedDeptId">
              <option value="" disabled selected>-- Selecciona --</option>
              @for (d of departamentosList; track d.id) {
                <option [value]="d.id">{{ d.nombre }}</option>
              }
            </select>
            <div class="modal-actions">
              <button class="btn-outline" (click)="closeDeptModal()">Cancelar</button>
              <button class="btn-primary" [disabled]="!selectedDeptId" (click)="confirmDeptModal()">Agregar</button>
            </div>
          </div>
        </div>
      }
    </div>
  `,
  styles: [`
    :host { flex: 1; display: flex; flex-direction: column; overflow: hidden; height: 100%; }
    .canvas-wrapper { display: flex; flex: 1; height: 100%; overflow: hidden; background: #fff; }
    .left-palette { width: 140px; min-width: 80px; max-width: 300px; resize: horizontal; flex-shrink: 0; background: var(--bg-2); border-right: 1px solid var(--border); display: flex; flex-direction: column; overflow: hidden; transition: width 0.3s ease; }
    .left-palette.collapsed { width: 0 !important; min-width: 0 !important; resize: none; border-right: none; }
    .ptitle { font-size: 10px; font-weight: 700; text-transform: uppercase; color: var(--text-muted); padding: 10px; text-align: center; letter-spacing: 0.05em; border-bottom: 1px solid var(--border); white-space: nowrap; }
    .palette-div { flex: 1; position: relative; }
    .palette-div::after {
      content: '';
      position: absolute;
      top: 0; left: 0; right: 0; height: 85px;
      background: var(--bg-2);
      z-index: 999;
      pointer-events: none;
    }
    .diagram-div { flex: 1; width: 100%; height: 100%; outline: none; }
    
    .modal-overlay { position: absolute; inset: 0; background: rgba(0,0,0,0.4); display: flex; align-items: center; justify-content: center; z-index: 100; backdrop-filter: blur(2px); }
    .modal-content { background: white; padding: 24px; border-radius: 12px; width: 360px; box-shadow: 0 10px 25px rgba(0,0,0,0.1); }
    .modal-content h3 { margin: 0 0 8px 0; font-size: 16px; font-family: 'Space Grotesk', sans-serif; color: var(--text); }
    .modal-content p { margin: 0 0 16px 0; font-size: 13px; color: var(--text-muted); }
    .form-select { width: 100%; padding: 10px; border: 1px solid var(--border); border-radius: 8px; font-size: 14px; outline: none; transition: border-color 0.2s; }
    .form-select:focus { border-color: var(--primary); }
    .modal-actions { display: flex; justify-content: flex-end; gap: 8px; margin-top: 20px; }
    
    /* Ocultar marca de agua de evaluación de GoJS */
    ::ng-deep a[href*="gojs.net"] { display: none !important; }
    ::ng-deep .palette-div > div[style*="position: absolute"],
    ::ng-deep .diagram-div > div[style*="position: absolute"] {
      display: none !important;
    }
    ::ng-deep .palette-div > canvas,
    ::ng-deep .diagram-div > canvas {
      display: block !important;
    }
  `]
})
export class EditorCanvasComponent implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild('diagramDiv') diagramDiv!: ElementRef;
  @ViewChild('paletteDiv') paletteDiv!: ElementRef;

  state = inject(EditorStateService);
  politicaService = inject(PoliticaService);
  wsService = inject(WebSocketService);
  authService = inject(AuthService);
  aiService = inject(AIService);
  deptService = inject(DepartamentoService);

  diagram: any;
  palette: any;
  getDeptColor!: (dept: string) => string;
  private changeTimer: any = null;
  private pendingPolitica: Politica | null = null;
  private isApplyingRemoteChange = false;
  private lastSentModelJson = '';

  showDeptModal = false;
  departamentosList: Departamento[] = [];
  selectedDeptId = '';

  constructor() {
    effect(() => {
      const zoomFit = this.state.triggerZoomFit();
      if (zoomFit > 0 && this.diagram) this.diagram.zoomToFit();
    });
    effect(() => {
      const zoomIn = this.state.triggerZoomIn();
      if (zoomIn > 0 && this.diagram) this.diagram.commandHandler.increaseZoom();
    });
    effect(() => {
      const zoomOut = this.state.triggerZoomOut();
      if (zoomOut > 0 && this.diagram) this.diagram.commandHandler.decreaseZoom();
    });
    effect(() => {
      const del = this.state.triggerDeleteNode();
      if (del > 0 && this.diagram) this.diagram.commandHandler.deleteSelection();
    });
    effect(() => {
      const addLane = this.state.triggerAddLane();
      if (addLane > 0) this.addNewLane();
    });
    effect(() => {
      const save = this.state.triggerSave();
      if (save > 0) this.savePolitica();
    });
    effect(() => {
      const act = this.state.triggerActivate();
      if (act > 0) this.activatePolitica();
    });
    effect(() => {
      const propUpdate = this.state.triggerApplyProps();
      if (propUpdate > 0) this.applyNodeProps();
    });
    effect(() => {
      const targetLane = this.state.triggerMoveToLane();
      if (targetLane) {
        this.moveNodeToLane(targetLane);
        this.state.triggerMoveToLane.set(null);
      }
    });
    effect(() => {
      const aiData = this.state.triggerAiDiagramUpdate();
      if (aiData) {
        this.applyAiData(aiData);
        this.state.triggerAiDiagramUpdate.set(null);
      }
    });
    effect(() => {
      const uml = this.state.triggerUmlExport();
      if (uml > 0) this.exportarUML();
    });
  }

  ngOnInit() {
    const pId = this.state.politicaId();
    if (pId) {
      const user = this.authService.getUser();
      if (user) {
        this.wsService.conectar(user.id, user.rol, user.departamento);
      }
      this.politicaService.getById(pId).subscribe({
        next: (p) => {
          if (this.diagram) this.loadPolitica(p);
          else this.pendingPolitica = p;
        },
        error: () => console.error('No se pudo cargar la política')
      });
      if (user) {
        this.wsService.unirsePolitica(pId, user.id, user.nombre || 'Administrador', (c: any) => this.applyRemoteChange(c));
      } else {
        this.wsService.suscribirPolitica(pId, (c: any) => this.applyRemoteChange(c));
      }
    }

    this.deptService.getAll().subscribe({
      next: (depts) => this.departamentosList = depts
    });
  }

  ngAfterViewInit() {
    this.initDiagram();
    this.initPalette();
    if (this.pendingPolitica) {
      this.loadPolitica(this.pendingPolitica);
      this.pendingPolitica = null;
    }
  }

  ngOnDestroy() {
    const pId = this.state.politicaId();
    const user = this.authService.getUser();
    if (pId && user) {
      this.wsService.salirPolitica(pId, user.id);
    }
    if (this.diagram) this.diagram.div = null;
    if (this.palette) this.palette.div = null;
    if (this.changeTimer) clearTimeout(this.changeTimer);
  }

  private initDiagram() {
    const $ = go.GraphObject.make;

    this.diagram = $(go.Diagram, this.diagramDiv.nativeElement, {
      'undoManager.isEnabled': true,
      'animationManager.isEnabled': true,
      layout: $(go.GridLayout, { wrappingColumn: 1, cellSize: new go.Size(1, 1) }),
      'draggingTool.dragsTree': false,
      'draggingTool.isGridSnapEnabled': true,
      'draggingTool.gridSnapCellSize': new go.Size(10, 10),
      'linkingTool.portGravity': 20,
      'relinkingTool.portGravity': 20,
      allowDrop: true,
    });

    this.diagram.addModelChangedListener((e: any) => {
      if (e.isTransactionFinished) {
        if (!this.isApplyingRemoteChange) {
          this.onDiagramChanged();
        }
      }
    });

    // POOL
    this.diagram.groupTemplateMap.add('Pool',
      $(go.Group, 'Auto',
        {
          layout: $(go.GridLayout, { wrappingColumn: Infinity, cellSize: new go.Size(1, 1), spacing: new go.Size(0, 0) }),
          isSubGraphExpanded: true, computesBoundsAfterDrag: true, computesBoundsIncludingLinks: false,
          handlesDragDropForMembers: true, mouseDrop: (e: any, grp: any) => this.finishDrop(e, grp),
        },
        $(go.Shape, 'Rectangle', { fill: '#ffffff', stroke: 'var(--border-2)', strokeWidth: 1 }),
        $(go.Panel, 'Table', { defaultRowSeparatorStroke: 'var(--border)' },
          $(go.Panel, 'Horizontal',
            { row: 0, stretch: go.GraphObject.Horizontal, background: 'var(--sidebar)', defaultAlignment: go.Spot.Left },
            $(go.TextBlock, {
              font: 'bold 14px Space Grotesk, sans-serif', stroke: 'white',
              margin: new go.Margin(12, 16), editable: true,
            }, new go.Binding('text').makeTwoWay())
          ),
          $(go.Placeholder, { row: 1, padding: new go.Margin(0, 0) })
        )
      )
    );

    this.getDeptColor = (dept: string) => {
      const colors = ['#3b82f6', '#10b981', '#f59e0b', '#8b5cf6', '#ec4899', '#06b6d4', '#f43f5e', '#14b8a6', '#f97316'];
      let hash = 0;
      const str = dept || 'General';
      for (let i = 0; i < str.length; i++) hash = ((hash << 5) - hash) + str.charCodeAt(i);
      return colors[Math.abs(hash) % colors.length];
    };

    function makePort(name: string, spot: any, output: boolean, input: boolean) {
      return $(go.Shape, 'Circle', {
        fill: 'transparent', stroke: null, desiredSize: new go.Size(8, 8),
        alignment: spot, alignmentFocus: spot, portId: name,
        fromSpot: spot, toSpot: spot, fromLinkable: output, toLinkable: input, cursor: 'pointer'
      });
    }

    function showSmallPorts(node: any, show: boolean) {
      node.ports.each((port: any) => {
        if (port.portId !== '') {
          port.fill = show ? 'rgba(59, 130, 246, 0.8)' : 'transparent';
        }
      });
    }

    // LANE: Configurado en Vertical para flujo arriba-abajo
    this.diagram.groupTemplateMap.add('Lane',
      $(go.Group, 'Vertical',
        {
          selectionObjectName: 'SHAPE', resizable: true, resizeObjectName: 'SHAPE',
          layout: $(go.LayeredDigraphLayout, { isOngoing: false, direction: 90, layerSpacing: 95, columnSpacing: 60, setsPortSpots: false }),
          computesBoundsAfterDrag: true, computesBoundsIncludingLinks: false,
          handlesDragDropForMembers: true, mouseDrop: (e: any, grp: any) => this.finishDrop(e, grp),
          memberAdded: () => this.onDiagramChanged(), memberRemoved: () => this.onDiagramChanged(),
        },
        $(go.Panel, 'Auto', // Lane Header arriba
          $(go.Shape, 'Rectangle', { height: 40, fill: '#3b82f6', stroke: null, stretch: go.GraphObject.Fill },
            new go.Binding('fill', 'text', (t: string) => this.getDeptColor(t))
          ),
          $(go.TextBlock, {
            name: 'LABEL', font: 'bold 13px Space Grotesk, sans-serif', stroke: '#ffffff',
            editable: true, alignment: go.Spot.Center, margin: new go.Margin(0, 12)
          }, new go.Binding('text').makeTwoWay())
        ),
        $(go.Panel, 'Auto',
          { name: 'SHAPE', minSize: new go.Size(240, 800), stretch: go.GraphObject.Fill },
          new go.Binding('desiredSize', 'size', go.Size.parse).makeTwoWay(go.Size.stringify),
          $(go.Shape, 'Rectangle', { fill: 'rgba(248, 250, 252, 0.7)', strokeWidth: 1, stretch: go.GraphObject.Fill },
            new go.Binding('stroke', 'text', (t: string) => this.getDeptColor(t)),
            new go.Binding('fill', 'text', (t: string) => this.getDeptColor(t) + '0A')
          ),
          $(go.Placeholder, { padding: new go.Margin(30, 20), alignment: go.Spot.TopLeft })
        )
      )
    );

    // START
    this.diagram.nodeTemplateMap.add('Start',
      $(go.Node, 'Vertical',
        {
          cursor: 'pointer',
          mouseEnter: (e: any, node: any) => showSmallPorts(node, true), mouseLeave: (e: any, node: any) => showSmallPorts(node, false)
        },
        new go.Binding('location', 'loc', go.Point.parse).makeTwoWay(go.Point.stringify),
        $(go.Panel, 'Spot',
          $(go.Shape, 'Circle', { fill: '#10b981', stroke: '#047857', strokeWidth: 2, width: 34, height: 34, portId: '', toLinkable: true, toSpot: go.Spot.AllSides }),
          makePort('T', go.Spot.Top, true, true),
          makePort('L', go.Spot.Left, true, true),
          makePort('R', go.Spot.Right, true, true),
          makePort('B', go.Spot.Bottom, true, true)
        ),
        $(go.TextBlock, { font: 'bold 10px Space Grotesk, sans-serif', stroke: '#0f172a', margin: new go.Margin(6, 0, 0, 0), editable: true, textAlign: 'center', width: 120 }, new go.Binding('text').makeTwoWay())
      )
    );

    // END
    this.diagram.nodeTemplateMap.add('End',
      $(go.Node, 'Vertical',
        {
          cursor: 'pointer',
          mouseEnter: (e: any, node: any) => showSmallPorts(node, true), mouseLeave: (e: any, node: any) => showSmallPorts(node, false)
        },
        new go.Binding('location', 'loc', go.Point.parse).makeTwoWay(go.Point.stringify),
        $(go.Panel, 'Spot',
          $(go.Shape, 'Circle', { fill: '#ef4444', stroke: '#b91c1c', strokeWidth: 3, width: 34, height: 34, portId: '', toLinkable: true, toSpot: go.Spot.AllSides }),
          makePort('T', go.Spot.Top, true, true),
          makePort('L', go.Spot.Left, true, true),
          makePort('R', go.Spot.Right, true, true),
          makePort('B', go.Spot.Bottom, true, true)
        ),
        $(go.TextBlock, { font: 'bold 10px Space Grotesk, sans-serif', stroke: '#0f172a', margin: new go.Margin(6, 0, 0, 0), editable: true, textAlign: 'center', width: 120 }, new go.Binding('text').makeTwoWay())
      )
    );

    // TASK
    this.diagram.nodeTemplateMap.add('',
      $(go.Node, 'Spot',
        {
          cursor: 'pointer',
          shadowColor: 'rgba(0,0,0,0.12)', shadowOffset: new go.Point(0, 6), shadowBlur: 14, isShadowed: true,
          mouseEnter: (e: any, node: any) => showSmallPorts(node, true), mouseLeave: (e: any, node: any) => showSmallPorts(node, false)
        },
        new go.Binding('location', 'loc', go.Point.parse).makeTwoWay(go.Point.stringify),
        $(go.Panel, 'Auto',
          $(go.Shape, 'RoundedRectangle', { fill: '#ffffff', stroke: '#3b82f6', strokeWidth: 3, portId: '', minSize: new go.Size(140, 60), parameter1: 10, toLinkable: true, toSpot: go.Spot.AllSides },
            new go.Binding('stroke', 'departamento', (d: string) => this.getDeptColor(d))
          ),
          $(go.Panel, 'Vertical', { margin: new go.Margin(10, 14) },
            $(go.TextBlock, { font: 'bold 14px Space Grotesk, sans-serif', stroke: '#1e293b', editable: true, textAlign: 'center', maxLines: 2, overflow: go.TextBlock.OverflowEllipsis, width: 120 }, new go.Binding('text').makeTwoWay()),
            $(go.TextBlock, { font: '11px Space Grotesk, sans-serif', stroke: '#64748b', textAlign: 'center', maxLines: 1, overflow: go.TextBlock.OverflowEllipsis, width: 120, margin: new go.Margin(4, 0, 0, 0) }, new go.Binding('text', 'departamento', (d: string) => d ? d : '')),
            $(go.TextBlock, { font: '10px Space Grotesk, sans-serif', stroke: '#3b82f6', margin: new go.Margin(4, 0, 0, 0) },
              new go.Binding('text', 'camposFormulario', (arr: any[]) => arr && arr.length > 0 ? '📝 ' + arr.length + ' campos a llenar' : '')),
            $(go.TextBlock, { font: '11px Space Grotesk, sans-serif', stroke: '#8b5cf6', margin: new go.Margin(2, 0, 0, 0) },
              new go.Binding('text', 'departamento', (d: string) => (d && (d.toLowerCase().includes('ia') || d.toLowerCase().includes('agente') || d.toLowerCase().includes('inteligente'))) ? '🤖 Agente IA' : ''))
          )
        ),
        makePort('T', go.Spot.Top, true, true),
        makePort('L', go.Spot.Left, true, true),
        makePort('R', go.Spot.Right, true, true),
        makePort('B', go.Spot.Bottom, true, true)
      )
    );

    // DECISION
    this.diagram.nodeTemplateMap.add('Decision',
      $(go.Node, 'Spot',
        {
          cursor: 'pointer', isShadowed: true, shadowColor: 'rgba(0,0,0,0.12)', shadowOffset: new go.Point(0, 6), shadowBlur: 14,
          mouseEnter: (e: any, node: any) => showSmallPorts(node, true), mouseLeave: (e: any, node: any) => showSmallPorts(node, false)
        },
        new go.Binding('location', 'loc', go.Point.parse).makeTwoWay(go.Point.stringify),
        $(go.Panel, 'Auto',
          $(go.Shape, 'Diamond', { fill: '#ffffff', stroke: '#f97316', strokeWidth: 3, portId: '', minSize: new go.Size(120, 80), toLinkable: true, toSpot: go.Spot.AllSides },
            new go.Binding('stroke', 'departamento', (d: string) => this.getDeptColor(d))
          ),
          $(go.TextBlock, { font: 'bold 13px Space Grotesk, sans-serif', stroke: '#1e293b', editable: true, textAlign: 'center', maxLines: 2, overflow: go.TextBlock.OverflowEllipsis, width: 100, margin: new go.Margin(14) }, new go.Binding('text').makeTwoWay())
        ),
        makePort('T', go.Spot.Top, true, true),
        makePort('L', go.Spot.Left, true, true),
        makePort('R', go.Spot.Right, true, true),
        makePort('B', go.Spot.Bottom, true, true)
      )
    );

    // PARALLEL
    this.diagram.nodeTemplateMap.add('Parallel',
      $(go.Node, 'Spot',
        {
          cursor: 'pointer', isShadowed: true, shadowColor: 'rgba(0,0,0,0.12)', shadowOffset: new go.Point(0, 6), shadowBlur: 14,
          mouseEnter: (e: any, node: any) => showSmallPorts(node, true), mouseLeave: (e: any, node: any) => showSmallPorts(node, false)
        },
        new go.Binding('location', 'loc', go.Point.parse).makeTwoWay(go.Point.stringify),
        $(go.Panel, 'Auto',
          $(go.Shape, 'Rectangle', { fill: '#ffffff', stroke: '#a855f7', strokeWidth: 3, portId: '', minSize: new go.Size(130, 60), toLinkable: true, toSpot: go.Spot.AllSides },
            new go.Binding('stroke', 'departamento', (d: string) => this.getDeptColor(d))
          ),
          $(go.Panel, 'Vertical', { margin: new go.Margin(8, 14) },
            $(go.TextBlock, '||', { font: 'bold 18px monospace', stroke: '#1e293b' },
              new go.Binding('stroke', 'departamento', (d: string) => this.getDeptColor(d))
            ),
            $(go.TextBlock, { font: 'bold 13px Space Grotesk, sans-serif', stroke: '#1e293b', editable: true, textAlign: 'center', maxLines: 2, overflow: go.TextBlock.OverflowEllipsis, width: 100 }, new go.Binding('text').makeTwoWay())
          )
        ),
        makePort('T', go.Spot.Top, true, true),
        makePort('L', go.Spot.Left, true, true),
        makePort('R', go.Spot.Right, true, true),
        makePort('B', go.Spot.Bottom, true, true)
      )
    );

    // LINK
    this.diagram.linkTemplate = $(go.Link,
      { routing: go.Routing.AvoidsNodes, corner: 12, reshapable: true, relinkableFrom: true, relinkableTo: true, toShortLength: 4 },
      $(go.Shape, { strokeWidth: 2, stroke: '#94a3b8' }),
      $(go.Shape, { toArrow: 'Standard', fill: '#94a3b8', stroke: null, scale: 1.3 }),
      $(go.TextBlock, { segmentOffset: new go.Point(0, -14), font: 'bold 12px Space Grotesk, sans-serif', stroke: '#475569', editable: true, background: 'rgba(255,255,255,0.9)', margin: new go.Margin(2, 4) }, new go.Binding('text').makeTwoWay())
    );

    // Listeners
    this.diagram.addDiagramListener('ChangedSelection', () => {
      const sel = this.diagram.selection.first();
      if (sel instanceof go.Node && sel.data.category !== 'Lane' && sel.data.category !== 'Pool') {
        const d = sel.data;
        this.state.selectedNode.set({
          key: d.key, text: d.text || '', category: d.category || '', group: d.group,
          departamento: d.departamento || '', descripcion: d.descripcion || '',
          tiempoLimiteHoras: d.tiempoLimiteHoras, camposFormulario: d.camposFormulario ? JSON.parse(JSON.stringify(d.camposFormulario)) : [],
        });
      } else {
        this.state.selectedNode.set(null);
      }
      this.syncLanes();
    });

    this.diagram.addDiagramListener('Modified', () => this.onDiagramChanged());

    if (!this.state.politicaId()) {
      this.buildInitialModel();
    }
  }

  private initPalette() {
    const $ = go.GraphObject.make;
    this.palette = $(go.Palette, this.paletteDiv.nativeElement, {
      layout: $(go.GridLayout, { wrappingColumn: 1, cellSize: new go.Size(1, 1), spacing: new go.Size(0, 12), alignment: go.GridLayout.Position }),
      contentAlignment: go.Spot.TopCenter,
      padding: new go.Margin(85, 12, 12, 12)
    });

    this.palette.nodeTemplate = $(go.Node, 'Horizontal',
      { cursor: 'pointer', background: 'transparent' },
      $(go.Panel, 'Auto',
        { width: 36, height: 36 },
        $(go.Shape, 'Rectangle',
          { fill: '#ffffff', strokeWidth: 2, stretch: go.GraphObject.Fill, portId: '' },
          new go.Binding('figure', 'category', (c: string) => {
            if (c === 'Start' || c === 'End') return 'Circle';
            if (c === 'Decision') return 'Diamond';
            return 'RoundedRectangle';
          }),
          new go.Binding('stroke', 'category', (c: string) => {
            if (c === 'Start') return '#10b981';
            if (c === 'End') return '#ef4444';
            if (c === 'Decision') return '#f97316';
            if (c === 'Parallel') return '#a855f7';
            return '#3b82f6';
          })
        )
      ),
      $(go.TextBlock, { font: '600 13px Space Grotesk, sans-serif', stroke: '#1e293b', margin: new go.Margin(0, 0, 0, 10), width: 70, overflow: go.TextBlock.OverflowEllipsis },
        new go.Binding('text')
      )
    );

    this.palette.model = new go.GraphLinksModel([
      { key: 'ps', text: 'Inicio', category: 'Start' },
      { key: 'pe', text: 'Fin', category: 'End' },
      { key: 'pt', text: 'Tarea', category: '' },
      { key: 'pd', text: 'Decisión', category: 'Decision' },
      { key: 'pp', text: 'Paralelo', category: 'Parallel' },
    ]);
  }

  private buildInitialModel() {
    const nodeDataArray: any[] = [
      { key: 'pool1', text: 'Nuevo Proceso', isGroup: true, category: 'Pool' },
      { key: 'lane1', text: 'General', isGroup: true, category: 'Lane', group: 'pool1', size: '240 800' },
      { key: 1, text: 'Inicio', category: 'Start', group: 'lane1', loc: '80 60' },
      { key: 2, text: 'Nueva Tarea', category: '', group: 'lane1', loc: '300 60', departamento: 'General' },
      { key: 3, text: 'Fin', category: 'End', group: 'lane1', loc: '520 60' },
    ];
    this.diagram.model = new go.GraphLinksModel(nodeDataArray, [{ from: 1, to: 2 }, { from: 2, to: 3 }]);
    this.diagram.model.nodeGroupKeyProperty = 'group';
    this.diagram.model.linkFromPortIdProperty = 'fromPort';
    this.diagram.model.linkToPortIdProperty = 'toPort';
    this.syncLanes();
  }

  private finishDrop(e: any, grp: any) {
    if (!(e.diagram.currentTool instanceof go.DraggingTool)) return;
    const ok = grp !== null ? grp.addMembers(grp.diagram.selection, true) : e.diagram.commandHandler.addTopLevelParts(e.diagram.selection, true);
    if (!ok) e.diagram.currentTool.doCancel();
    else {
      e.diagram.selection.each((part: any) => {
        if (part instanceof go.Node && grp && grp.data.category === 'Lane') {
          e.diagram.model.setDataProperty(part.data, 'departamento', grp.data.text);
        }
      });
      this.syncLanes();
    }
  }

  private addNewLane() {
    this.selectedDeptId = '';
    this.showDeptModal = true;
  }

  closeDeptModal() {
    this.showDeptModal = false;
  }

  confirmDeptModal() {
    this.showDeptModal = false;
    let poolKey: any = null;
    this.diagram.nodes.each((n: any) => { if (n.data.category === 'Pool') poolKey = n.data.key; });
    if (!poolKey) return;

    const selectedDept = this.departamentosList.find(d => d.id === this.selectedDeptId);
    if (!selectedDept) return;

    this.diagram.startTransaction('add lane');
    (this.diagram.model as any).addNodeData({
      key: 'lane_' + Date.now(), text: selectedDept.nombre, isGroup: true, category: 'Lane', group: poolKey, size: '240 800',
    });
    this.diagram.commitTransaction('add lane');
    this.syncLanes();
  }

  private moveNodeToLane(laneName: string) {
    const sel = this.state.selectedNode();
    if (!sel) return;
    let targetLane: any = null;
    this.diagram.nodes.each((n: any) => { if (n.data.category === 'Lane' && n.data.text === laneName) targetLane = n; });
    if (!targetLane) return;
    const node = this.diagram.findNodeForKey(sel.key);
    if (!node) return;
    this.diagram.startTransaction('move to lane');
    this.diagram.model.setDataProperty(node.data, 'group', targetLane.data.key);
    this.diagram.model.setDataProperty(node.data, 'departamento', laneName);
    this.diagram.commitTransaction('move to lane');
  }

  private applyNodeProps() {
    const sel = this.state.selectedNode();
    if (!sel || !this.diagram) return;
    this.diagram.startTransaction('update props');
    const node = this.diagram.findNodeForKey(sel.key);
    if (node) {
      this.diagram.model.setDataProperty(node.data, 'text', sel.text);
      this.diagram.model.setDataProperty(node.data, 'departamento', sel.departamento);
      this.diagram.model.setDataProperty(node.data, 'descripcion', sel.descripcion);
      this.diagram.model.setDataProperty(node.data, 'tiempoLimiteHoras', sel.tiempoLimiteHoras);
      this.diagram.model.setDataProperty(node.data, 'camposFormulario', sel.camposFormulario);
      this.diagram.model.setDataProperty(node.data, 'permisosDocumentales', sel.permisosDocumentales);
    }
    this.diagram.commitTransaction('update props');
  }

  private applyAiData(parsed: any) {
    const nodos: any[] = parsed.nodos || parsed.nodes || [];
    const links: any[] = parsed.links || parsed.enlaces || [];
    if (nodos.length > 0 || links.length > 0) {
      let poolKey = 'pool1';
      const isMultiple = parsed.accion === 'AGREGAR_MULTIPLES' || parsed.action === 'AGREGAR_MULTIPLES';

      if (isMultiple) {
        // Limpiar el lienzo para crear el flujo desde cero en un único Pool limpio
        const pNombre = this.state.politicaNombre() || 'Nueva Política';
        const initialNodes = [
          { key: poolKey, text: pNombre, isGroup: true, category: 'Pool' }
        ];
        this.diagram.model = new go.GraphLinksModel(initialNodes, []);
        this.diagram.model.nodeGroupKeyProperty = 'group';
        this.diagram.model.linkFromPortIdProperty = 'fromPort';
        this.diagram.model.linkToPortIdProperty = 'toPort';
      } else {
        // Buscar el Pool existente para insertar elementos en él
        this.diagram.nodes.each((n: any) => {
          if (n.data.category === 'Pool') poolKey = n.data.key;
        });
        if (!poolKey) {
          poolKey = 'pool1';
          this.diagram.startTransaction('add pool');
          (this.diagram.model as any).addNodeData({ key: poolKey, text: this.state.politicaNombre() || 'Proceso', isGroup: true, category: 'Pool' });
          this.diagram.commitTransaction('add pool');
        }
      }

      this.diagram.startTransaction('ai update');

      // Preprocesar nodos para separar el nodo de inicio si tiene texto personalizado
      const processedNodos: any[] = [];
      const processedLinks: any[] = [...links];

      nodos.forEach((n: any) => {
        const cat = n.category !== undefined ? n.category : (({ START: 'Start', END: 'End', TASK: '', DECISION: 'Decision', PARALLEL: 'Parallel' } as any)[n.tipo || 'TASK'] ?? '');

        if (cat === 'Start') {
          const textUpper = (n.text || n.nombre || '').toUpperCase();
          if (textUpper !== 'INICIO' && textUpper !== 'START' && textUpper !== '') {
            // 1. Creamos un nodo de inicio limpio de tipo 'Start' con texto "INICIO"
            const startKey = 'start_' + Date.now() + '_' + Math.floor(Math.random() * 1000);
            processedNodos.push({
              key: startKey,
              text: 'INICIO',
              category: 'Start',
              departamento: n.departamento || ''
            });
            // 2. Convertimos el nodo original en un nodo de tarea normal ('')
            processedNodos.push({
              ...n,
              category: '',
              tipo: 'TASK',
              camposFormulario: n.camposFormulario || [
                { nombre: 'Observación', tipo: 'TEXT', requerido: true },
                { nombre: 'Aprobado', tipo: 'CHECKBOX', requerido: true }
              ]
            });
            // 3. Añadimos un enlace desde el nuevo nodo de inicio al nodo de tarea original
            processedLinks.push({
              from: startKey,
              to: n.key ?? n.id
            });
          } else {
            processedNodos.push({
              ...n,
              text: 'INICIO',
              category: 'Start'
            });
          }
        } else if (cat === 'End') {
          processedNodos.push({
            ...n,
            text: 'FIN',
            category: 'End'
          });
        } else {
          // Si es una tarea, poblar campos por defecto para mostrar el badge azul del layout de referencia
          if (cat === '' || cat === 'TASK') {
            if (!n.camposFormulario || n.camposFormulario.length === 0) {
              const numCampos = 2 + Math.floor(Math.random() * 2); // 2 o 3 campos dinámicos
              n.camposFormulario = Array.from({ length: numCampos }, (_, idx) => ({
                nombre: 'Campo ' + (idx + 1),
                tipo: 'TEXT',
                requerido: true
              }));
            }
          }
          processedNodos.push(n);
        }
      });

      processedNodos.forEach((n: any) => {
        const key = n.key ?? n.id;
        const existing = this.diagram.findNodeForKey(key);
        if (existing) {
          this.diagram.model.setDataProperty(existing.data, 'text', n.text || n.nombre || existing.data.text);
          if (n.departamento) this.diagram.model.setDataProperty(existing.data, 'departamento', n.departamento);
          if (n.camposFormulario) this.diagram.model.setDataProperty(existing.data, 'camposFormulario', n.camposFormulario);
        } else {
          let laneKey: any = null;
          const deptName = n.departamento || 'General';

          // Buscar si ya existe la calle para este departamento
          this.diagram.nodes.each((ln: any) => {
            if (ln.data.category === 'Lane' && ln.data.text === deptName) {
              laneKey = ln.data.key;
            }
          });

          // Si no existe, crear la calle enlazada al Pool principal
          if (!laneKey) {
            laneKey = 'lane_' + deptName.toLowerCase().replace(/[^a-z0-9]/g, '_') + '_' + Math.floor(Math.random() * 1000);
            (this.diagram.model as any).addNodeData({
              key: laneKey, text: deptName, isGroup: true, category: 'Lane', group: poolKey, size: '240 800'
            });
          }

          const newKey = key ?? (Date.now() + Math.floor(Math.random() * 1000));
          const cat = n.category !== undefined ? n.category : this.tipoToCategory(n.tipo || 'TASK');
          (this.diagram.model as any).addNodeData({
            key: newKey, text: n.text || n.nombre || 'Nodo', category: cat,
            group: laneKey, departamento: deptName,
            loc: '100 100', // Evitar NaN en el binding de coordenadas
            camposFormulario: n.camposFormulario || []
          });
        }
      });

      processedLinks.forEach((l: any) => {
        (this.diagram.model as any).addLinkData({ from: l.from || l.origen, to: l.to || l.destino });
      });

      // Forzar la invalidación y ejecución de layouts de las calles
      this.diagram.nodes.each((n: any) => {
        if (n instanceof go.Group && n.data.category === 'Lane') {
          n.layout.invalidateLayout();
        }
      });
      this.diagram.layout.invalidateLayout();

      this.diagram.commitTransaction('ai update');
      this.diagram.layoutDiagram(true);
      this.syncLanes();
    }
  }

  private syncLanes() {
    const lanes: any[] = [];
    let nodes = 0, links = 0;
    this.diagram.nodes.each((n: any) => {
      if (n.data.category === 'Lane') lanes.push({ key: n.data.key, text: n.data.text });
      else if (n.data.category !== 'Pool') nodes++;
    });
    this.diagram.links.each(() => links++);

    this.state.lanes.set(lanes);
    this.state.laneCount.set(lanes.length);
    this.state.nodeCount.set(nodes);
    this.state.linkCount.set(links);
  }

  private onDiagramChanged() {
    this.syncLanes();
    if (!this.diagram) return;
    const newJson = this.diagram.model.toJson();
    if (newJson === this.lastSentModelJson) {
      return;
    }
    if (this.changeTimer) clearTimeout(this.changeTimer);
    this.changeTimer = setTimeout(() => {
      const pId = this.state.politicaId();
      if (pId) {
        const finalJson = this.diagram.model.toJson();
        if (finalJson !== this.lastSentModelJson) {
          this.lastSentModelJson = finalJson;
          this.wsService.enviarCambioDiagrama(pId, { modelo: finalJson });
        }
      }
    }, 800);
  }

  private loadPolitica(p: Politica) {
    console.log("Loading Politica in Canvas:", p);
    this.state.politicaNombre.set(p.nombre);
    this.state.politicaCategoria.set(p.categoria || '');
    this.state.politicaEstado.set(p.estado);
    if (p.nodos && p.nodos.length > 0) {
      console.log("Nodos present:", p.nodos.length);
      const depts = [...new Set(p.nodos.filter(n => n.departamento).map(n => n.departamento))];
      const poolKey = 'pool1';
      const nodeDataArray: any[] = [{ key: poolKey, text: p.nombre, isGroup: true, category: 'Pool' }];
      const laneMap: Record<string, string> = {};
      depts.forEach((dept, i) => { const lk = 'lane_' + i; laneMap[dept] = lk; nodeDataArray.push({ key: lk, text: dept, isGroup: true, category: 'Lane', group: poolKey, size: '240 800' }); });
      const defaultLaneKey = 'lane_default';
      if (p.nodos.some(n => !n.departamento)) { nodeDataArray.push({ key: defaultLaneKey, text: 'General', isGroup: true, category: 'Lane', group: poolKey, size: '240 800' }); }

      p.nodos.forEach(n => {
        const laneKey = n.departamento && laneMap[n.departamento] ? laneMap[n.departamento] : defaultLaneKey;
        nodeDataArray.push({
          key: n.id, text: n.nombre, category: this.tipoToCategory(n.tipo), group: laneKey, loc: n.posX + ' ' + n.posY,
          departamento: n.departamento || '', descripcion: n.descripcion || '', tiempoLimiteHoras: n.tiempoLimiteHoras, camposFormulario: n.camposFormulario || [],
          permisosDocumentales: n.permisosDocumentales || {}
        });
      });
      const linkDataArray: any[] = [];
      p.nodos.forEach(n => { (n.conexiones || []).forEach(targetId => { linkDataArray.push({ from: n.id, to: targetId }); }); });

      console.log("NodeDataArray:", nodeDataArray);
      console.log("LinkDataArray:", linkDataArray);

      try {
        this.diagram.model = new go.GraphLinksModel(nodeDataArray, linkDataArray);
        this.diagram.model.nodeGroupKeyProperty = 'group';
        this.diagram.model.linkFromPortIdProperty = 'fromPort';
        this.diagram.model.linkToPortIdProperty = 'toPort';
        this.syncLanes();
        setTimeout(() => this.diagram.zoomToFit(), 100);
      } catch (e) {
        console.error("GoJS Error rendering diagram:", e);
      }
    } else {
      console.log("No nodos in politica. Inicializando GraphLinksModel vacío.");
      const poolKey = 'pool1';
      const nodeDataArray: any[] = [
        { key: poolKey, text: p.nombre || 'Nueva Política', isGroup: true, category: 'Pool' },
        { key: 'lane_default', text: 'General', isGroup: true, category: 'Lane', group: poolKey, size: '240 800' }
      ];
      this.diagram.model = new go.GraphLinksModel(nodeDataArray, []);
      this.diagram.model.nodeGroupKeyProperty = 'group';
      this.diagram.model.linkFromPortIdProperty = 'fromPort';
      this.diagram.model.linkToPortIdProperty = 'toPort';
      this.syncLanes();
    }
  }

  private savePolitica() {
    this.state.saving.set(true);
    const nodos = this.extractNodos();
    const payload: Partial<Politica> = {
      nombre: this.state.politicaNombre(), categoria: this.state.politicaCategoria(), nodos, estado: this.state.politicaEstado(), creadoPorId: this.authService.getUser()?.id || '',
    };
    const pId = this.state.politicaId();
    if (pId) {
      this.politicaService.update(pId, payload).subscribe({
        next: () => this.state.saving.set(false),
        error: () => this.state.saving.set(false)
      });
    } else {
      this.politicaService.create(payload).subscribe({
        next: (p) => { this.state.saving.set(false); this.state.politicaId.set(p.id); },
        error: () => this.state.saving.set(false)
      });
    }
  }

  private activatePolitica() {
    const pId = this.state.politicaId();
    if (!pId) return;
    this.politicaService.activar(pId).subscribe({
      next: (p) => this.state.politicaEstado.set(p.estado)
    });
  }

  private exportarUML() {
    const pId = this.state.politicaId();
    if (!pId) return;
    this.aiService.generarPlantUML(pId).subscribe({
      next: (resp) => {
        const blob = new Blob([resp.plantuml], { type: 'text/plain;charset=utf-8' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a'); a.href = url; a.download = this.state.politicaNombre().replace(/\s+/g, '_') + '_UML.puml'; a.click(); URL.revokeObjectURL(url);
      }
    });
  }

  private applyRemoteChange(cambio: any) {
    if (cambio.editoresActivos !== undefined) this.state.editoresActivos.set(cambio.editoresActivos);
    if (cambio.nombresEditores !== undefined) this.state.nombresEditores.set(cambio.nombresEditores);
    if (cambio.modelo && this.diagram) {
      if (cambio.modelo === this.diagram.model.toJson()) {
        return;
      }
      try {
        this.isApplyingRemoteChange = true;
        this.lastSentModelJson = cambio.modelo;
        this.diagram.model = go.Model.fromJson(cambio.modelo);
        this.syncLanes();
      } catch (e) {
        console.error("Error al aplicar cambio remoto:", e);
      } finally {
        this.isApplyingRemoteChange = false;
      }
    }
  }

  private extractNodos(): Nodo[] {
    const nodos: Nodo[] = [];
    this.diagram.nodes.each((node: any) => {
      const d = node.data;
      if (d.category === 'Lane' || d.category === 'Pool') return;
      const loc = go.Point.parse(d.loc || '0 0');
      nodos.push({
        id: String(d.key), nombre: d.text || '', descripcion: d.descripcion || '', tipo: this.categoryToTipo(d.category || ''),
        departamento: d.departamento || '', responsableId: '', tiempoLimiteHoras: d.tiempoLimiteHoras,
        posX: loc.x, posY: loc.y, conexiones: [], condiciones: {}, camposFormulario: d.camposFormulario || [],
        permisosDocumentales: d.permisosDocumentales || {}
      });
    });
    this.diagram.links.each((link: any) => {
      const fromKey = String(link.data.from); const toKey = String(link.data.to);
      const nodo = nodos.find(n => n.id === fromKey);
      if (nodo && !nodo.conexiones.includes(toKey)) nodo.conexiones.push(toKey);
    });
    return nodos;
  }

  private tipoToCategory(tipo: string): string { return ({ START: 'Start', END: 'End', TASK: '', DECISION: 'Decision', PARALLEL: 'Parallel' } as any)[tipo] ?? ''; }
  private categoryToTipo(cat: string): Nodo['tipo'] { return ({ Start: 'START', End: 'END', '': 'TASK', Decision: 'DECISION', Parallel: 'PARALLEL' } as any)[cat] ?? 'TASK'; }
}
