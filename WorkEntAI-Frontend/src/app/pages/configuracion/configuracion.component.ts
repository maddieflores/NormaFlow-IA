import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SidebarComponent, NavItem, ADMIN_NAV_ITEMS } from '../../components/sidebar/sidebar.component';
import { ThemeService } from '../../services/theme/theme.service';
import { AuthService } from '../../services/auth/auth.service';

@Component({
  selector: 'app-configuracion',
  standalone: true,
  imports: [CommonModule, FormsModule, SidebarComponent],
  template: `
    <div class="app-layout">
      <app-sidebar activeRoute="/configuracion" [navItems]="navItems"></app-sidebar>
      <main class="main-content" style="background-color: var(--bg); padding: 40px; color: var(--text);">

        <!-- HEADER -->
        <div style="margin-bottom: 32px;">
          <h1 style="font-size: 24px; font-weight: 700; color: var(--text); margin-bottom: 8px;">Configuración</h1>
          <p style="color: var(--text-muted); font-size: 14px; margin: 0;">Preferencias locales del navegador (se guardan automáticamente).</p>
        </div>

        <!-- SETTINGS CARD -->
        <div style="background: var(--card); border-radius: 12px; padding: 24px 32px; border: 1px solid var(--border); box-shadow: var(--shadow); max-width: 800px;">
          
          <!-- ITEM: TEMA -->
          <div style="display: flex; justify-content: space-between; align-items: center; padding: 20px 0; border-bottom: 1px solid var(--border);">
            <div>
              <h3 style="font-size: 16px; font-weight: 600; color: var(--text); margin: 0 0 4px 0;">Tema</h3>
              <p style="font-size: 13px; color: var(--text-muted); margin: 0;">Cambiar entre claro y oscuro.</p>
            </div>
            <div>
              <button 
                style="background: var(--bg-2); border: 1px solid var(--border-2); color: var(--text); padding: 10px 24px; border-radius: 8px; font-size: 14px; font-weight: 600; cursor: pointer; min-width: 100px; text-align: center; transition: all 0.2s;"
                (click)="toggleTheme()">
                {{ isDark() ? 'Oscuro' : 'Claro' }}
              </button>
            </div>
          </div>

          <!-- ITEM: NOTIFICATIONS APP -->
          <div style="display: flex; justify-content: space-between; align-items: center; padding: 20px 0; border-bottom: 1px solid var(--border);">
            <div>
              <h3 style="font-size: 16px; font-weight: 600; color: var(--text); margin: 0 0 4px 0;">Notificaciones en la app</h3>
              <p style="font-size: 13px; color: var(--text-muted); margin: 0;">Muestra avisos dentro del sistema.</p>
            </div>
            <div>
              <label class="switch">
                <input type="checkbox" [checked]="notifApp()" (change)="toggleNotifApp()">
                <span class="slider round"></span>
              </label>
            </div>
          </div>

          <!-- ITEM: NOTIFICATIONS EMAIL -->
          <div style="display: flex; justify-content: space-between; align-items: center; padding: 20px 0;">
            <div>
              <h3 style="font-size: 16px; font-weight: 600; color: var(--text); margin: 0 0 4px 0;">Notificaciones por email</h3>
              <p style="font-size: 13px; color: var(--text-muted); margin: 0;">Preferencia local (no envía emails en MVP).</p>
            </div>
            <div>
              <label class="switch">
                <input type="checkbox" [checked]="notifEmail()" (change)="toggleNotifEmail()">
                <span class="slider round"></span>
              </label>
            </div>
          </div>

        </div>

      </main>
    </div>
  `,
  styles: [`
    .switch {
      position: relative;
      display: inline-block;
      width: 48px;
      height: 24px;
    }
    .switch input { 
      opacity: 0;
      width: 0;
      height: 0;
    }
    .slider {
      position: absolute;
      cursor: pointer;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background-color: var(--border-2);
      transition: .3s;
      border-radius: 24px;
    }
    .slider:before {
      position: absolute;
      content: "";
      height: 18px;
      width: 18px;
      left: 3px;
      bottom: 3px;
      background-color: white;
      transition: .3s;
      border-radius: 50%;
    }
    input:checked + .slider {
      background-color: #2563eb;
    }
    input:checked + .slider:before {
      transform: translateX(24px);
    }
  `]
})
export class ConfiguracionComponent implements OnInit {
  navItems: NavItem[] = [];

  notifApp = signal<boolean>(true);
  notifEmail = signal<boolean>(false);

  private readonly APP_KEY = 'wf-notif-app';
  private readonly EMAIL_KEY = 'wf-notif-email';

  constructor(
    private themeService: ThemeService,
    private authService: AuthService
  ) { }

  ngOnInit(): void {
    this.initNavItems();
    this.loadSettings();
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

  private loadSettings() {
    const appVal = localStorage.getItem(this.APP_KEY);
    this.notifApp.set(appVal !== null ? appVal === 'true' : true);

    const emailVal = localStorage.getItem(this.EMAIL_KEY);
    this.notifEmail.set(emailVal !== null ? emailVal === 'true' : false);
  }

  isDark(): boolean {
    return this.themeService.isDark();
  }

  toggleTheme(): void {
    this.themeService.toggle();
  }

  toggleNotifApp(): void {
    this.notifApp.update(v => !v);
    localStorage.setItem(this.APP_KEY, String(this.notifApp()));
  }

  toggleNotifEmail(): void {
    this.notifEmail.update(v => !v);
    localStorage.setItem(this.EMAIL_KEY, String(this.notifEmail()));
  }
}
