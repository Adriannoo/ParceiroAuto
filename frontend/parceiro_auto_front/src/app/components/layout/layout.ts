import { Component, inject, computed } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet, Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { rotuloPapel } from '../../models/usuario.model';

@Component({
  selector: 'app-layout',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './layout.html',
  styleUrl: './layout.scss',
})
export class Layout {
  private authService = inject(AuthService);
  private router = inject(Router);

  usuario = computed(() => this.authService.getUsuarioLogado());
  podeVerEmpresas = computed(() => this.authService.temPapel('dono'));
  podeVerLancamentos = computed(() => this.authService.temPapel('dono', 'gerente'));
  podeVerRelatorios = computed(() => this.authService.temPapel('dono', 'gerente', 'visualizador'));
  podeVerGestao = computed(() => this.authService.temPapel('dono'));

  initialsUsuario = computed(() => {
    const u = this.usuario();
    if (!u) return '';
    const partes = u.nome.split(' ');
    const firstLetter = partes[0]?.[0] ?? '';
    const secondLetter = partes[1]?.[0] ?? '';
    return (firstLetter + secondLetter).toUpperCase();
  });

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  papelLegivel(): string {
    const papel = this.usuario()?.papel;
    return papel ? rotuloPapel(papel) : '';
  }
}