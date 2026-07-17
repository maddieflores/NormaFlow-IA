import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { SidebarComponent, NavItem, ADMIN_NAV_ITEMS } from '../../../components/sidebar/sidebar.component';
import { TramiteService } from '../../../services/tramite/tramite.service';
import { PoliticaService } from '../../../services/politica/politica.service';
import { UsuarioService } from '../../../services/usuario/usuario.service';
import { Tramite, Politica, Usuario } from '../../../models/models';

@Component({
  selector: 'app-admin-tramites',
  standalone: true,
  imports: [CommonModule, RouterModule, SidebarComponent, FormsModule],
  templateUrl: './admin-tramites.component.html',
  styleUrls: ['../admin.component.css'] // Reutilizamos los estilos premium
})
export class AdminTramitesComponent implements OnInit {
  tramites: Tramite[] = [];
  filteredTramites: Tramite[] = [];
  loading = false;

  searchTerm = '';
  statusFilter = '';

  // Crear nuevo trámite modal
  showCreateModal = false;
  activePolicies: Politica[] = [];
  clients: Usuario[] = [];
  selectedPoliticaId = '';
  descripcion = '';
  prioridad = 'MEDIA';
  selectedClienteId = '';
  creando = false;
  modalError = '';

  private tramiteService = inject(TramiteService);
  private politicaService = inject(PoliticaService);
  private usuarioService = inject(UsuarioService);
  public router = inject(Router);

  navItems = ADMIN_NAV_ITEMS;

  ngOnInit(): void {
    this.loadTramites();
  }

  abrirModalCrear(): void {
    this.selectedPoliticaId = '';
    this.descripcion = '';
    this.prioridad = 'MEDIA';
    this.selectedClienteId = '';
    this.modalError = '';
    this.creando = false;
    this.showCreateModal = true;
    this.cargarPoliticasYClientes();
  }

  cargarPoliticasYClientes(): void {
    this.politicaService.getActivas().subscribe({
      next: (policies) => {
        this.activePolicies = policies;
      }
    });
    this.usuarioService.getAll().subscribe({
      next: (users) => {
        this.clients = users.filter(u => u.rol === 'CLIENTE');
      }
    });
  }

  crearTramite(): void {
    if (!this.selectedPoliticaId) {
      this.modalError = 'Debes seleccionar una política';
      return;
    }
    if (!this.selectedClienteId) {
      this.modalError = 'Debes seleccionar un cliente solicitante';
      return;
    }

    this.creando = true;
    this.modalError = '';

    this.tramiteService.iniciar(this.selectedPoliticaId, this.selectedClienteId, this.descripcion, this.prioridad).subscribe({
      next: () => {
        this.creando = false;
        this.showCreateModal = false;
        this.loadTramites(); // Recargar tabla
      },
      error: (err) => {
        this.modalError = err.error?.error || err.message || 'Error al iniciar el trámite';
        this.creando = false;
      }
    });
  }


  loadTramites(): void {
    this.loading = true;
    this.tramiteService.getAll().subscribe({
      next: (data: Tramite[]) => {
        this.tramites = data;
        this.filteredTramites = data;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  filter(event?: Event): void {
    if (event) {
      this.searchTerm = (event.target as HTMLInputElement).value.toLowerCase();
    }

    this.filteredTramites = this.tramites.filter(t => {
      // Búsqueda en todas las columnas
      const term = this.searchTerm;
      const matchSearch =
        t.id.toLowerCase().includes(term) ||
        (t.numeroReferencia || '').toLowerCase().includes(term) ||
        (t.clienteId || '').toLowerCase().includes(term) ||
        (t.nombreCliente || '').toLowerCase().includes(term) ||
        (t.politicaId || '').toLowerCase().includes(term) ||
        (t.nombrePolitica || '').toLowerCase().includes(term) ||
        (t.descripcion || '').toLowerCase().includes(term);

      // Filtro por estado
      let matchStatus = true;
      if (this.statusFilter === 'DEMORA') {
        matchStatus = this.isDelayed(t);
      } else if (this.statusFilter) {
        matchStatus = t.estado === this.statusFilter;
      }

      return matchSearch && matchStatus;
    });
  }

  isDelayed(t: Tramite): boolean {
    if (t.estado === 'COMPLETADO' || t.estado === 'RECHAZADO') return false;
    // Si la prioridad es ALTA o lleva mucho tiempo, se considera en demora
    if (t.prioridad === 'ALTA') return true;

    // Simulación: más de 3 días = demora
    const created = new Date(t.fechaInicio).getTime();
    const now = new Date().getTime();
    const diffDays = (now - created) / (1000 * 3600 * 24);
    return diffDays > 3;
  }

  setStatus(status: string): void {
    this.statusFilter = status;
    this.filter();
  }

  badgeClass(estado: string): string {
    switch (estado) {
      case 'NUEVO': return 'badge-borrador'; // Amarillo/Naranja
      case 'EN_PROCESO': return 'badge-activa'; // Verde
      case 'COMPLETADO': return 'badge-inactiva'; // Gris oscuro / o Azul
      case 'RECHAZADO': return 'badge-inactiva'; // Rojo
      default: return 'badge-inactiva';
    }
  }
}
