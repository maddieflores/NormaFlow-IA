import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth/auth.service';
import { AgenteService } from '../../services/agente/agente.service';
import { DocumentoService } from '../../services/documento/documento.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="login-page">

      <!-- Animated mesh background -->
      <div class="bg-mesh">
        <div class="mesh-blob blob-1"></div>
        <div class="mesh-blob blob-2"></div>
        <div class="mesh-blob blob-3"></div>
      </div>

      <!-- Split layout -->
      <div class="login-split">

        <!-- LEFT: Branding & copy -->
        <div class="login-left">
          <div class="logo">
            <svg width="36" height="36" viewBox="0 0 40 40" fill="none" xmlns="http://www.w3.org/2000/svg">
              <rect width="40" height="40" rx="12" fill="url(#brandGradL)" />
              <path d="M12 20L20 12L28 20L20 28L12 20Z" fill="white" fill-opacity="0.2" />
              <circle cx="20" cy="20" r="4" fill="white" />
              <path d="M8 20H12M28 20H32M20 8V12M20 28V32" stroke="white" stroke-width="2" stroke-linecap="round" />
              <defs>
                <linearGradient id="brandGradL" x1="0" y1="0" x2="40" y2="40" gradientUnits="userSpaceOnUse">
                  <stop stop-color="#3b82f6" />
                  <stop offset="1" stop-color="#8b5cf6" />
                </linearGradient>
              </defs>
            </svg>
            <span class="logo-text">NormaFlow</span>
          </div>

          <div class="left-copy">
            <div class="badge-pill">
              <span class="pulse-dot"></span>
              Plataforma Workflow impulsada por IA
            </div>
            <h1 class="left-title">
              Orquesta tus <br/>procesos con
              <span class="gradient-text"><br/>Inteligencia Artificial</span>
            </h1>
            <p class="left-desc">
              NormaFlow automatiza trámites, facilita la colaboración documental en tiempo real y usa Agentes IA para predecir cuellos de botella antes de que ocurran.
            </p>

            <div class="features-pills">
              <div class="pill"><span class="pill-icon">🤖</span> Agentes IA</div>
              <div class="pill"><span class="pill-icon">⚡</span> Tiempo Real</div>
              <div class="pill"><span class="pill-icon">📊</span> Analítica</div>
            </div>
          </div>
        </div>

        <!-- RIGHT: Glass mockup with login form -->
        <div class="login-right">
          <div class="glass-mockup">
            <div class="mockup-header">
              <div class="dots">
                <span></span><span></span><span></span>
              </div>
              <span class="mockup-url">app.normaflow.io — Acceso Seguro</span>
            </div>

            <div class="mockup-body">
              <div class="login-form-area">

                <div class="form-brand">
                  <svg width="40" height="40" viewBox="0 0 40 40" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <rect width="40" height="40" rx="12" fill="url(#brandGradR)" />
                    <path d="M12 20L20 12L28 20L20 28L12 20Z" fill="white" fill-opacity="0.2" />
                    <circle cx="20" cy="20" r="4" fill="white" />
                    <path d="M8 20H12M28 20H32M20 8V12M20 28V32" stroke="white" stroke-width="2" stroke-linecap="round" />
                    <defs>
                      <linearGradient id="brandGradR" x1="0" y1="0" x2="40" y2="40" gradientUnits="userSpaceOnUse">
                        <stop stop-color="#3b82f6" />
                        <stop offset="1" stop-color="#8b5cf6" />
                      </linearGradient>
                    </defs>
                  </svg>
                  <div>
                    <div class="form-brand-name">NormaFlow</div>
                    <div class="form-brand-sub">✦ Gestión Inteligente</div>
                  </div>
                </div>

                <h2 class="form-title">{{ isRegisterMode ? 'Crear Cuenta' : 'Iniciar Sesión' }}</h2>
                <p class="form-subtitle">{{ isRegisterMode ? 'Regístrate para simular e iniciar tus trámites' : 'Ingresa tus credenciales para continuar' }}</p>

                <!-- Nombre (Solo en modo registro) -->
                @if (isRegisterMode) {
                  <div class="field">
                    <label>Nombre Completo</label>
                    <div class="input-wrap">
                      <svg class="input-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
                      <input id="register-name" type="text" [(ngModel)]="nombre" placeholder="Juan Pérez" class="input-field" (keydown.enter)="onSubmit()" />
                    </div>
                  </div>
                }

                <!-- Email -->
                <div class="field">
                  <label>Correo Electrónico</label>
                  <div class="input-wrap">
                    <svg class="input-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="2" y="4" width="20" height="16" rx="2"/><path d="m22 7-8.97 5.7a1.94 1.94 0 0 1-2.06 0L2 7"/></svg>
                    <input id="login-email" type="email" [(ngModel)]="email" placeholder="usuario@cotasenergy.com" class="input-field" (keydown.enter)="onSubmit()" />
                  </div>
                </div>

                <!-- Password -->
                <div class="field">
                  <label>Contraseña</label>
                  <div class="input-wrap">
                    <svg class="input-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="11" width="18" height="11" rx="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
                    <input id="login-password" type="password" [(ngModel)]="password" placeholder="••••••••" class="input-field" (keydown.enter)="onSubmit()" />
                  </div>
                </div>

                <!-- Error -->
                @if (error) {
                  <p class="error-msg">⚠️ {{ error }}</p>
                }

                <!-- Submit -->
                <button id="login-submit" class="btn-login" (click)="onSubmit()" [disabled]="loading">
                  @if (loading) {
                    <span class="spinner"></span> {{ isRegisterMode ? 'Registrando...' : 'Iniciando...' }}
                  } @else {
                    {{ isRegisterMode ? 'Crear Cuenta' : 'Ingresar' }}
                  }
                </button>

                <p class="forgot">
                  {{ isRegisterMode ? '¿Ya tienes una cuenta?' : '¿No tienes una cuenta?' }}
                  <span class="forgot-link" (click)="toggleMode()">{{ isRegisterMode ? 'Iniciar Sesión' : 'Registrarse' }}</span>
                </p>

                <div class="back-link" (click)="router.navigate(['/'])">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M19 12H5M12 19l-7-7 7-7"/></svg>
                  Volver al inicio
                </div>

              </div>
            </div>

            <!-- Floating badge -->
            <div class="mockup-float">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#a78bfa" stroke-width="2"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>
              Conexión Segura
            </div>
          </div>
        </div>

      </div>
    </div>
  `,
  styles: [`
    @import url('https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;500;600;700;800&display=swap');

    .login-page {
      min-height: 100vh;
      background: #0b0f19;
      color: #f8fafc;
      font-family: 'Outfit', sans-serif;
      position: relative;
      overflow: hidden;
    }

    /* ── Mesh Background ── */
    .bg-mesh { position: absolute; inset: 0; overflow: hidden; z-index: 0; pointer-events: none; }
    .mesh-blob {
      position: absolute; border-radius: 50%;
      filter: blur(80px); opacity: 0.5;
      animation: blobFloat 10s infinite alternate ease-in-out;
    }
    .blob-1 {
      top: -10%; left: -10%; width: 600px; height: 600px;
      background: radial-gradient(circle, rgba(59,130,246,0.4), transparent 60%);
    }
    .blob-2 {
      top: 40%; right: -20%; width: 700px; height: 700px;
      background: radial-gradient(circle, rgba(139,92,246,0.3), transparent 60%);
      animation-delay: -3s;
    }
    .blob-3 {
      bottom: -20%; left: 20%; width: 500px; height: 500px;
      background: radial-gradient(circle, rgba(16,185,129,0.2), transparent 60%);
      animation-delay: -6s;
    }
    @keyframes blobFloat {
      from { transform: translate(0,0) scale(1); }
      to   { transform: translate(40px,-40px) scale(1.08); }
    }

    /* ── Split layout ── */
    .login-split {
      position: relative; z-index: 10;
      min-height: 100vh;
      display: grid;
      grid-template-columns: 1fr 1fr;
      align-items: center;
      padding: 40px 64px;
      max-width: 1300px;
      margin: 0 auto;
      box-sizing: border-box;
      gap: 48px;
    }

    /* ── LEFT ── */
    .login-left {
      padding-right: 40px;
      animation: slideInLeft 0.5s ease-out;
    }
    @keyframes slideInLeft {
      from { opacity: 0; transform: translateX(-30px); }
      to   { opacity: 1; transform: translateX(0); }
    }

    .logo {
      display: flex; align-items: center; gap: 12px;
      margin-bottom: 48px;
    }
    .logo-text {
      font-size: 22px; font-weight: 800;
      background: linear-gradient(to right, #f8fafc, #94a3b8);
      -webkit-background-clip: text; background-clip: text;
      -webkit-text-fill-color: transparent;
    }

    .badge-pill {
      display: inline-flex; align-items: center; gap: 8px;
      background: rgba(59,130,246,0.1);
      border: 1px solid rgba(59,130,246,0.25);
      border-radius: 100px;
      padding: 6px 16px;
      font-size: 13px; font-weight: 500;
      color: #93c5fd;
      margin-bottom: 28px;
    }
    .pulse-dot {
      width: 8px; height: 8px; border-radius: 50%;
      background: #3b82f6;
      box-shadow: 0 0 0 0 rgba(59,130,246,0.6);
      animation: pulseAnim 2s infinite;
      flex-shrink: 0;
    }
    @keyframes pulseAnim {
      0%   { box-shadow: 0 0 0 0 rgba(59,130,246,0.6); }
      70%  { box-shadow: 0 0 0 8px rgba(59,130,246,0); }
      100% { box-shadow: 0 0 0 0 rgba(59,130,246,0); }
    }

    .left-title {
      font-size: 46px; font-weight: 800; line-height: 1.12;
      margin: 0 0 20px; color: #f8fafc;
    }
    .gradient-text {
      background: linear-gradient(135deg, #60a5fa, #c084fc, #f472b6);
      -webkit-background-clip: text; background-clip: text;
      -webkit-text-fill-color: transparent;
    }
    .left-desc {
      font-size: 15px; color: #94a3b8; line-height: 1.7;
      max-width: 440px; margin: 0 0 32px;
    }
    .features-pills { display: flex; gap: 12px; flex-wrap: wrap; }
    .pill {
      display: flex; align-items: center; gap: 8px;
      background: rgba(255,255,255,0.06);
      border: 1px solid rgba(255,255,255,0.1);
      border-radius: 100px; padding: 8px 18px;
      font-size: 13px; font-weight: 500; color: #cbd5e1;
      transition: background 0.2s;
    }
    .pill:hover { background: rgba(255,255,255,0.1); }
    .pill-icon { font-size: 16px; }

    /* ── RIGHT: Glass Mockup ── */
    .login-right {
      display: flex; justify-content: center; align-items: center;
      animation: slideInRight 0.5s ease-out;
    }
    @keyframes slideInRight {
      from { opacity: 0; transform: translateX(30px); }
      to   { opacity: 1; transform: translateX(0); }
    }

    .glass-mockup {
      background: rgba(15,23,42,0.8);
      backdrop-filter: blur(24px);
      border: 1px solid rgba(255,255,255,0.1);
      border-radius: 24px;
      padding: 28px;
      width: 100%; max-width: 480px;
      box-shadow: 0 25px 80px rgba(0,0,0,0.6), 0 0 0 1px rgba(255,255,255,0.05);
      position: relative;
      transition: transform 0.3s;
    }
    .glass-mockup:hover { transform: translateY(-4px); }
    .glass-mockup::before {
      content: '';
      position: absolute; inset: 0;
      border-radius: 24px; padding: 1px;
      background: linear-gradient(135deg, rgba(59,130,246,0.5), rgba(139,92,246,0.5));
      -webkit-mask: linear-gradient(#fff 0 0) content-box, linear-gradient(#fff 0 0);
      -webkit-mask-composite: xor; mask-composite: exclude;
      pointer-events: none;
    }

    .mockup-header {
      display: flex; align-items: center; gap: 12px;
      margin-bottom: 28px;
    }
    .dots { display: flex; gap: 6px; }
    .dots span { width: 10px; height: 10px; border-radius: 50%; }
    .dots span:nth-child(1) { background: #ef4444; }
    .dots span:nth-child(2) { background: #f59e0b; }
    .dots span:nth-child(3) { background: #10b981; }
    .mockup-url {
      font-size: 11px; color: #475569;
      background: rgba(255,255,255,0.04);
      border: 1px solid rgba(255,255,255,0.07);
      border-radius: 6px; padding: 3px 10px; flex: 1;
    }

    .form-brand {
      display: flex; align-items: center; gap: 14px;
      margin-bottom: 24px;
    }
    .form-brand-name {
      font-size: 18px; font-weight: 700;
      background: linear-gradient(to right, #f8fafc, #94a3b8);
      -webkit-background-clip: text; background-clip: text;
      -webkit-text-fill-color: transparent;
    }
    .form-brand-sub { font-size: 11px; color: #64748b; margin-top: 2px; }

    .form-title {
      font-size: 22px; font-weight: 700;
      color: #f8fafc; margin: 0 0 4px;
    }
    .form-subtitle { font-size: 13px; color: #64748b; margin: 0 0 22px; }

    .field { margin-bottom: 16px; }
    .field label {
      display: block; font-size: 11px; font-weight: 500;
      color: #94a3b8; margin-bottom: 6px; letter-spacing: 0.03em;
    }
    .input-wrap { position: relative; }
    .input-icon {
      position: absolute; left: 12px; top: 50%;
      transform: translateY(-50%);
      color: #475569; pointer-events: none;
    }
    .input-field {
      width: 100%; padding: 11px 14px 11px 38px;
      background: rgba(255,255,255,0.05);
      border: 1px solid rgba(255,255,255,0.1);
      border-radius: 10px; color: #f8fafc;
      font-size: 13px; font-family: 'Outfit', sans-serif;
      outline: none; box-sizing: border-box;
      transition: border-color 0.2s, background 0.2s;
    }
    .input-field::placeholder { color: #334155; }
    .input-field:focus {
      border-color: rgba(59,130,246,0.6);
      background: rgba(59,130,246,0.06);
    }

    .error-msg {
      font-size: 12px; color: #f87171;
      background: rgba(239,68,68,0.1);
      border: 1px solid rgba(239,68,68,0.25);
      border-radius: 8px; padding: 8px 12px;
      margin: 0 0 14px;
    }

    .btn-login {
      width: 100%; padding: 13px;
      background: linear-gradient(135deg, #3b82f6, #8b5cf6);
      color: white; border: none; border-radius: 10px;
      font-size: 14px; font-weight: 600; font-family: 'Outfit', sans-serif;
      cursor: pointer; margin-top: 4px;
      box-shadow: 0 4px 24px rgba(59,130,246,0.35);
      transition: opacity 0.2s, transform 0.15s;
      display: flex; align-items: center; justify-content: center; gap: 8px;
    }
    .btn-login:hover:not(:disabled) { opacity: 0.9; transform: translateY(-1px); }
    .btn-login:active:not(:disabled) { transform: translateY(0); }
    .btn-login:disabled { opacity: 0.5; cursor: not-allowed; }

    .spinner {
      width: 14px; height: 14px; border-radius: 50%;
      border: 2px solid rgba(255,255,255,0.3);
      border-top-color: white;
      animation: spin 0.7s linear infinite;
    }
    @keyframes spin { to { transform: rotate(360deg); } }

    .forgot {
      font-size: 11px; color: #475569;
      text-align: center; margin: 14px 0 0;
    }
    .forgot-link { color: #60a5fa; cursor: pointer; transition: color 0.2s; }
    .forgot-link:hover { color: #93c5fd; text-decoration: underline; }

    .back-link {
      display: flex; align-items: center; justify-content: center; gap: 6px;
      font-size: 12px; color: #475569;
      margin-top: 16px; cursor: pointer;
      transition: color 0.2s;
    }
    .back-link:hover { color: #94a3b8; }

    .mockup-float {
      position: absolute; right: -20px; bottom: -20px;
      background: rgba(15,23,42,0.95);
      border: 1px solid rgba(139,92,246,0.4);
      padding: 10px 18px; border-radius: 14px;
      display: flex; align-items: center; gap: 8px;
      font-size: 13px; font-weight: 600; color: #c4b5fd;
      box-shadow: 0 8px 24px rgba(0,0,0,0.5);
      animation: floatUpDown 4s infinite ease-in-out;
    }
    @keyframes floatUpDown {
      0%, 100% { transform: translateY(0); }
      50%       { transform: translateY(-8px); }
    }

    /* ── Responsive ── */
    @media (max-width: 900px) {
      .login-split {
        grid-template-columns: 1fr;
        padding: 32px 24px;
        gap: 40px;
        min-height: auto;
      }
      .login-left { padding-right: 0; text-align: center; }
      .left-title { font-size: 32px; }
      .left-desc { max-width: 100%; }
      .badge-pill, .features-pills { justify-content: center; }
      .logo { justify-content: center; margin-bottom: 28px; }
      .mockup-float { right: 0; bottom: -24px; font-size: 11px; }
    }
  `]
})
export class LoginComponent {
  email = '';
  password = '';
  nombre = '';
  loading = false;
  error = '';
  isRegisterMode = false;

  constructor(
    private authService: AuthService,
    private agenteService: AgenteService,
    private documentoService: DocumentoService,
    public router: Router
  ) { }

  toggleMode() {
    this.isRegisterMode = !this.isRegisterMode;
    this.error = '';
    this.email = '';
    this.password = '';
    this.nombre = '';
  }

  onSubmit() {
    if (this.isRegisterMode) {
      this.doRegister();
    } else {
      this.doLogin();
    }
  }

  doLogin() {
    this.loading = true;
    this.error = '';

    this.authService.login(this.email, this.password).subscribe({
      next: (res) => {
        this.loading = false;
        this.handleLoginSuccess(res);
      },
      error: (err) => {
        this.error = err.error?.message || 'Email o contraseña incorrectos';
        this.loading = false;
      }
    });
  }

  doRegister() {
    if (!this.nombre.trim() || !this.email.trim() || !this.password.trim()) {
      this.error = 'Todos los campos son obligatorios';
      return;
    }

    this.loading = true;
    this.error = '';

    const registerData = {
      nombre: this.nombre,
      email: this.email,
      password: this.password,
      rol: 'CLIENTE',
      departamento: 'Externo'
    };

    this.authService.register(registerData).subscribe({
      next: (res) => {
        // Al registrarse exitosamente, el backend retorna AuthResponse (con token, etc.)
        // Guardamos las credenciales y el token en localStorage como un login normal
        localStorage.setItem('token', res.token);
        localStorage.setItem('user', JSON.stringify(res));

        this.loading = false;
        this.handleLoginSuccess(res);
      },
      error: (err) => {
        this.error = err.error?.message || 'Error al crear la cuenta';
        this.loading = false;
      }
    });
  }

  private handleLoginSuccess(res: any) {
    if (res.rol === 'ADMIN') {
      this.router.navigate(['/admin']);
    } else if (res.rol === 'FUNCIONARIO') {
      this.router.navigate(['/dashboard']);
    } else {
      // Si es un cliente y tiene una sesión demo guardada, la reclamamos
      const demoSessionId = localStorage.getItem('demo_session_id');
      if (demoSessionId) {
        this.agenteService.reclamarSesion(demoSessionId).subscribe({
          next: (sesionActualizada) => {
            localStorage.removeItem('demo_session_id');
            if (sesionActualizada.tramiteId) {
              // Subir los archivos acumulados durante la demo
              const archivos = this.agenteService.archivosPendientesDemo;
              if (archivos && archivos.length > 0) {
                archivos.forEach(archivo => {
                  this.documentoService.subirDocumento(
                    sesionActualizada.tramiteId!,
                    archivo,
                    undefined,
                    `Subido por IA (Demo Reclamada): ${archivo.name}`
                  ).subscribe({
                    next: () => console.log('Documento demo subido con éxito:', archivo.name),
                    error: (err) => console.error('Error al subir documento demo:', archivo.name, err)
                  });
                });
                this.agenteService.archivosPendientesDemo = []; // Limpiar
              }

              // Redirigir directamente al nuevo trámite creado
              this.router.navigate(['/tramite', sesionActualizada.tramiteId]);
            } else {
              this.router.navigate(['/cliente']);
            }
          },
          error: (err) => {
            console.error('Error al reclamar la sesión demo:', err);
            localStorage.removeItem('demo_session_id');
            this.router.navigate(['/cliente']);
          }
        });
      } else {
        this.router.navigate(['/cliente']);
      }
    }
  }

  // Alias para mantener compatibilidad si es necesario
  login() {
    this.onSubmit();
  }
}