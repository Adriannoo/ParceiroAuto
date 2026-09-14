import { Component, inject, computed } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet, Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-layout',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './layout.html',
  styleUrl: './layout.scss',
})
export class Layout {
  private authService = inject(AuthService);
  private router = inject(Router);

  user = computed(() => this.authService.getCurrentUser());

  userInitials = computed(() => {
    const u = this.user();
    if (!u) return '';
    const parts = u.name.split(' ');
    const firstLetter = parts[0]?.[0] ?? '';
    const secondLetter = parts[1]?.[0] ?? '';
    return (firstLetter + secondLetter).toUpperCase();
  });

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}