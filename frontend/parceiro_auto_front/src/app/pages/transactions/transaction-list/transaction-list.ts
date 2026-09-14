import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { TransactionService } from '../transaction.service';
import { Transaction, paymentMethodLabel } from '../transaction.model';
import { CompanyService } from '../../companies/company.service';
import { Company } from '../../companies/company.model';

type TypeFilter = 'all' | 'ENTRADA' | 'SAIDA';

@Component({
  selector: 'app-transaction-list',
  imports: [FormsModule, RouterLink],
  templateUrl: './transaction-list.html',
  styleUrl: './transaction-list.scss',
})
export class TransactionList implements OnInit {
  private transactionService = inject(TransactionService);
  private companyService = inject(CompanyService);

  transactions = signal<Transaction[]>([]);
  companies = signal<Company[]>([]);
  loading = signal(false);

  searchTerm = signal('');
  type = signal<TypeFilter>('all');
  companyId = signal<number | 'all'>('all');

  transactionToDelete = signal<Transaction | null>(null);

  filtered = computed(() => {
    const term = this.searchTerm().trim().toLowerCase();
    const typeFilter = this.type();
    const companyFilter = this.companyId();

    return this.transactions()
      .filter((m) => (typeFilter === 'all' ? true : m.type === typeFilter))
      .filter((m) => (companyFilter === 'all' ? true : m.companyId === companyFilter))
      .filter(
        (m) =>
          !term ||
          m.description.toLowerCase().includes(term) ||
          m.category.toLowerCase().includes(term) ||
          m.account.toLowerCase().includes(term),
      )
      .sort((a, b) => b.date.localeCompare(a.date) || b.id - a.id);
  });

  totalIncome = computed(() =>
    this.filtered()
      .filter((m) => m.type === 'ENTRADA')
      .reduce((sum, m) => sum + m.value, 0),
  );

  totalExpenses = computed(() =>
    this.filtered()
      .filter((m) => m.type === 'SAIDA')
      .reduce((sum, m) => sum + m.value, 0),
  );

  balance = computed(() => this.totalIncome() - this.totalExpenses());

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);

    forkJoin({
      transactions: this.transactionService.list(),
      companies: this.companyService.list(),
    }).subscribe({
      next: ({ transactions, companies }) => {
        this.transactions.set(transactions);
        this.companies.set(companies);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  companyName(id: number): string {
    return this.companies().find((e) => e.id === id)?.tradeName ?? 'Empresa removida';
  }

  paymentMethodLabel(transaction: Transaction): string {
    return paymentMethodLabel(transaction.method);
  }

  formatCurrency(value: number): string {
    return value.toLocaleString('pt-BR', {
      style: 'currency',
      currency: 'BRL',
    });
  }

  formatDate(iso: string): string {
    const [year, month, day] = iso.split('-');
    return `${day}/${month}/${year}`;
  }

  clearFilters(): void {
    this.searchTerm.set('');
    this.type.set('all');
    this.companyId.set('all');
  }

  hasFilters(): boolean {
    return this.searchTerm() !== '' || this.type() !== 'all' || this.companyId() !== 'all';
  }

  openConfirmation(transaction: Transaction): void {
    this.transactionToDelete.set(transaction);
  }

  closeConfirmation(): void {
    this.transactionToDelete.set(null);
  }

  confirmDeletion(): void {
    const transaction = this.transactionToDelete();

    if (!transaction) {
      return;
    }

    this.transactionService.delete(transaction.id).subscribe(() => {
      this.closeConfirmation();
      this.load();
    });
  }
}