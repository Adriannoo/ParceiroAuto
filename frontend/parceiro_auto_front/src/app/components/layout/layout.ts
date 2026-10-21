import { Component, OnInit, inject, computed, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, of, startWith, switchMap } from 'rxjs';
import { DestroyRef } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink, RouterLinkActive, RouterOutlet, Router } from '@angular/router';
import { Company } from '../../pages/companies/company.model';
import { CompanyService } from '../../pages/companies/company.service';
import { AuthService } from '../../services/auth.service';
import { CurrentCompanyService } from '../../services/current-company.service';

@Component({
  selector: 'app-layout',
  imports: [FormsModule, RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './layout.html',
  styleUrl: './layout.scss',
})
export class Layout implements OnInit {
  private authService = inject(AuthService);
  private companyService = inject(CompanyService);
  private currentCompanyService = inject(CurrentCompanyService);
  private router = inject(Router);
  private destroyRef = inject(DestroyRef);

  user = computed(() => this.authService.getCurrentUser());
  companies = signal<Company[]>([]);
  currentCompanyId = this.currentCompanyService.currentCompanyId;

  userInitials = computed(() => {
    const u = this.user();
    if (!u) return '';
    const parts = (u.name || u.login).split(' ');
    const firstLetter = parts[0]?.[0] ?? '';
    const secondLetter = parts[1]?.[0] ?? '';
    return (firstLetter + secondLetter).toUpperCase();
  });

  ngOnInit(): void {
    this.companyService.companiesChanged.pipe(
      startWith(undefined),
      switchMap(() => this.companyService.list().pipe(catchError(() => of([])))),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe((companies) => {
      const activeCompanies = companies.filter((company) => company.active);
      this.companies.set(activeCompanies);
      this.currentCompanyService.ensureCompany(activeCompanies.map((company) => company.id));
    });
  }

  setCurrentCompany(companyId: number | string): void {
    this.currentCompanyService.setCurrentCompanyId(Number(companyId));
  }

  logout(): void {
    this.authService.logout().subscribe({
      next: () => {
        this.currentCompanyService.setCurrentCompanyId(null);
        this.router.navigate(['/login']);
      },
      error: () => window.alert('Nao foi possivel encerrar a sessao. Tente novamente.'),
    });
  }
}
