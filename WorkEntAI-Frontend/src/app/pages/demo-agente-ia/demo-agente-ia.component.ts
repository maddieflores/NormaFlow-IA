import { Component, OnInit, OnDestroy, ViewChild, ElementRef, AfterViewChecked } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AgenteService, AgenteSession } from '../../services/agente/agente.service';

@Component({
  selector: 'app-demo-agente-ia',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './demo-agente-ia.component.html',
  styleUrls: ['./demo-agente-ia.component.css']
})
export class DemoAgenteIaComponent implements OnInit, OnDestroy, AfterViewChecked {

  @ViewChild('mensajesContainer') private mensajesContainer!: ElementRef;

  sesion: AgenteSession | null = null;
  mensajeInput = '';
  cargando = false;
  error = '';
  enviando = false;
  private shouldScrollDown = false;

  isRecording = false;
  fileAttached: File | null = null;
  sidebarHidden = false;

  // Texto de bienvenida inicial
  readonly mensajeBienvenida = `¡Hola! 👋 Soy el **Asistente de Demo de NormaFlow**.\n\nEstoy aquí para guiarte en tus consultas sobre trámites públicos o empresariales. Cuéntame:\n**¿Qué trámite necesitas realizar o qué consulta tienes sobre los requisitos?**`;

  constructor(
    private agenteService: AgenteService,
    public router: Router
  ) { }

  ngOnInit(): void {
    this.iniciarSesion();
  }

  ngOnDestroy(): void { }

  ngAfterViewChecked(): void {
    if (this.shouldScrollDown) {
      this.scrollToBottom();
      this.shouldScrollDown = false;
    }
  }

  iniciarSesion(): void {
    this.cargando = true;
    this.agenteService.iniciarSesionDemo().subscribe({
      next: (sesion) => {
        this.sesion = sesion;
        this.cargando = false;
        this.shouldScrollDown = true;
      },
      error: (err) => {
        this.error = 'Error al conectar con el asistente de demo. Por favor recarga la página.';
        this.cargando = false;
      }
    });
  }

  onFileSelected(event: any): void {
    const file = event.target.files[0];
    if (file) this.fileAttached = file;
  }

  toggleVoice(): void {
    this.isRecording = !this.isRecording;
    if (this.isRecording) {
      this.mensajeInput = 'Escuchando...';
      setTimeout(() => {
        this.isRecording = false;
        this.mensajeInput = 'Me gustaría realizar un trámite de actualización de datos';
      }, 3000);
    } else {
      this.mensajeInput = '';
    }
  }

  enviarMensaje(): void {
    const texto = this.mensajeInput.trim() + (this.fileAttached ? `\n[Archivo adjunto: ${this.fileAttached.name}]` : '');
    if (!texto || !this.sesion || this.enviando) return;

    if (this.fileAttached) {
      this.agenteService.archivosPendientesDemo.push(this.fileAttached);
    }

    this.mensajeInput = '';
    this.fileAttached = null;
    this.enviando = true;

    this.agenteService.enviarMensajeDemo(this.sesion.id, texto).subscribe({
      next: (sesionActualizada) => {
        this.sesion = sesionActualizada;
        this.enviando = false;
        this.shouldScrollDown = true;
      },
      error: (err) => {
        this.enviando = false;
        this.error = 'Error al procesar tu mensaje. Intenta de nuevo.';
        setTimeout(() => this.error = '', 4000);
      }
    });
  }

  onKeydown(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.enviarMensaje();
    }
  }

  irALanding(): void {
    this.router.navigate(['/']);
  }

  irALogin(): void {
    if (this.sesion) {
      localStorage.setItem('demo_session_id', this.sesion.id);
    }
    this.router.navigate(['/login']);
  }

  confirmarTramite(): void {
    this.mensajeInput = 'Sí, iniciar trámite';
    this.enviarMensaje();
  }

  nuevaSesion(): void {
    this.agenteService.archivosPendientesDemo = [];
    if (this.sesion) {
      this.cargando = true;
      this.agenteService.cerrarSesionDemo(this.sesion.id).subscribe({
        next: () => {
          this.sesion = null;
          this.error = '';
          this.iniciarSesion();
        },
        error: () => {
          this.sesion = null;
          this.error = '';
          this.iniciarSesion();
        }
      });
    } else {
      this.sesion = null;
      this.error = '';
      this.iniciarSesion();
    }
  }

  toggleSidebar(): void {
    this.sidebarHidden = !this.sidebarHidden;
  }

  get todosLosMensajes(): Array<{ rol: string; contenido: string; timestamp: string }> {
    const bienvenida = [{ rol: 'AGENTE', contenido: this.mensajeBienvenida, timestamp: this.sesion?.fechaCreacion || new Date().toISOString() }];
    return [...bienvenida, ...(this.sesion?.mensajes || [])];
  }

  formatearContenido(contenido: string): string {
    return contenido
      .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
      .replace(/\*(.*?)\*/g, '<em>$1</em>')
      .replace(/\n/g, '<br>');
  }

  formatearHora(timestamp: string): string {
    return new Date(timestamp).toLocaleTimeString('es', { hour: '2-digit', minute: '2-digit' });
  }

  get estaCompleta(): boolean { return this.sesion?.fase === 'COMPLETADA'; }
  get tieneError(): boolean { return !!this.error; }

  private scrollToBottom(): void {
    try {
      const el = this.mensajesContainer?.nativeElement;
      if (el) el.scrollTop = el.scrollHeight;
    } catch { }
  }

  faseLegible(fase: string): string {
    return { IDENTIFICACION: 'Identificando trámite', REQUISITOS: 'Verificando requisitos', CONFIRMACION: 'Confirmación', COMPLETADA: 'Demo Completada', ABANDONADA: 'Cancelado' }[fase] || fase;
  }

  getBadgeClass(fase: string): string {
    return { IDENTIFICACION: 'badge-proceso', REQUISITOS: 'badge-warning', CONFIRMACION: 'badge-activa', COMPLETADA: 'badge-completado', ABANDONADA: 'badge-rechazado' }[fase] || 'badge-inactiva';
  }
}
