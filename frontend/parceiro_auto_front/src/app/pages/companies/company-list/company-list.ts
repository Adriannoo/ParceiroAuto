import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { CompanyService } from '../company.service';
import {
  Company,
  TAX_REGIMES,
  TaxRegime,
  companySizeLabel,
  taxRegimeLabel,
} from '../company.model';

type SortColumn = 'legalName' | 'cnpj' | 'taxRegime' | 'size' | 'city';
type SortDirection = 'asc' | 'desc';

@Component({
  selector: 'app-company-list',
  imports: [FormsModule, RouterLink],
  templateUrl: './company-list.html',
  styleUrl: './company-list.scss',
})
export class CompanyList implements OnInit {
  private companyService = inject(CompanyService);

  taxRegimes = TAX_REGIMES;

  companies = signal<Company[]>([]);
  loading = signal(false);

  searchTerm = signal('');
  taxRegime = signal<TaxRegime | 'all'>('all');
  status = signal<'all' | 'active' | 'inactive'>('all');

  sortColumn = signal<SortColumn>('legalName');
  sortDirection = signal<SortDirection>('asc');

  companyToDelete = signal<Company | null>(null);
  deleting = signal(false);

  filtered = computed(() => {
    const term = this.searchTerm().trim().toLowerCase();
    const taxRegimeFilter = this.taxRegime();
    const statusFilter = this.status();
    const column = this.sortColumn();
    const direction = this.sortDirection() === 'asc' ? 1 : -1;

    const list = this.companies()
      .filter((e) => taxRegimeFilter === 'all' || e.taxRegime === taxRegimeFilter)
      .filter((e) =>
        statusFilter === 'all' ? true : e.active === (statusFilter === 'active'),
      )
      .filter(
        (e) =>
          !term ||
          e.legalName.toLowerCase().includes(term) ||
          e.tradeName.toLowerCase().includes(term) ||
          e.cnpj.includes(term),
      );

    return [...list].sort(
      (a, b) => String(a[column]).localeCompare(String(b[column]), 'pt-BR') * direction,
    );
  });

  totalActive = computed(() => this.companies().filter((e) => e.active).length);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);

    this.companyService.list().subscribe({
      next: (companies) => {
        this.companies.set(companies);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  sortBy(column: SortColumn): void {
    if (this.sortColumn() === column) {
      this.sortDirection.set(this.sortDirection() === 'asc' ? 'desc' : 'asc');
      return;
    }

    this.sortColumn.set(column);
    this.sortDirection.set('asc');
  }

  sortArrow(column: SortColumn): string {
    if (this.sortColumn() !== column) {
      return '';
    }

    return this.sortDirection() === 'asc' ? '▲' : '▼';
  }

  taxRegimeLabel(e: Company): string {
    return taxRegimeLabel(e.taxRegime);
  }

  companySizeLabel(e: Company): string {
    return companySizeLabel(e.size);
  }

  hasFilters(): boolean {
    return this.searchTerm() !== '' || this.taxRegime() !== 'all' || this.status() !== 'all';
  }

  clearFilters(): void {
    this.searchTerm.set('');
    this.taxRegime.set('all');
    this.status.set('all');
  }

  openConfirmation(company: Company): void {
    this.companyToDelete.set(company);
  }

  closeConfirmation(): void {
    if (this.deleting()) {
      return;
    }

    this.companyToDelete.set(null);
  }

  async confirmDeletion(): Promise<void> {
    const company = this.companyToDelete();

    if (!company || this.deleting()) {
      return;
    }

    // Carrega o alerta apenas quando a exclusao for utilizada.
    const { default: Swal } = await import('sweetalert2');
    if (this.deleting() || this.companyToDelete() !== company) {
      return;
    }

    this.deleting.set(true);
    this.companyService.delete(company.id).subscribe({
      next: () => {
        this.deleting.set(false);
        this.closeConfirmation();
        this.load();
        void Swal.fire({ title: 'Empresa excluida', icon: 'success', confirmButtonText: 'OK' });
      },
      error: () => {
        this.deleting.set(false);
        void Swal.fire({
          title: 'Nao foi possivel excluir',
          text: 'Verifique os vinculos da empresa e tente novamente.',
          icon: 'error',
          confirmButtonText: 'OK',
        });
      },
    });
  }
}
