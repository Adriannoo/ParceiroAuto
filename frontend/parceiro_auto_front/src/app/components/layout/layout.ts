import { Component, OnInit, inject, computed, signal } from '@angular/core';
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

  user = computed(() => this.authService.getCurrentUser());
  companies = signal<Company[]>([]);
  currentCompanyId = this.currentCompanyService.currentCompanyId;

  userInitials = computed(() => {
    const u = this.user();
    if (!u) return '';
    const parts = u.name.split(' ');
    const firstLetter = parts[0]?.[0] ?? '';
    const secondLetter = parts[1]?.[0] ?? '';
    return (firstLetter + secondLetter).toUpperCase();
  });

  ngOnInit(): void {
    this.companyService.list().subscribe((companies) => {
      const activeCompanies = companies.filter((company) => company.active);
      this.companies.set(activeCompanies);
      this.currentCompanyService.ensureCompany(activeCompanies.map((company) => company.id));
    });
  }

  setCurrentCompany(companyId: number | string): void {
    this.currentCompanyService.setCurrentCompanyId(Number(companyId));
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
