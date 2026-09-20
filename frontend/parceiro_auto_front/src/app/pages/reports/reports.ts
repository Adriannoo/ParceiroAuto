import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { forkJoin, of, switchMap } from 'rxjs';
import { BankAccount } from '../bank-accounts/bank-account.model';
import { BankAccountService } from '../bank-accounts/bank-account.service';
import { Company } from '../companies/company.model';
import { CompanyService } from '../companies/company.service';
import { CurrentCompanyService } from '../../services/current-company.service';
import {
  PAYMENT_METHODS,
  Transaction,
  TransactionCategory,
  TransactionType,
  paymentMethodLabel,
} from '../transactions/transaction.model';
import { TransactionService } from '../transactions/transaction.service';
import { TransactionCategoryService } from '../transactions/transaction-category.service';

type TypeFilter = 'all' | TransactionType;
type NumberFilter = number | 'all';

interface ReportColumn {
  key: string;
  label: string;
  selected: boolean;
  value: (transaction: Transaction) => string;
}

@Component({
  selector: 'app-reports',
  imports: [FormsModule],
  templateUrl: './reports.html',
  styleUrl: './reports.scss',
})
export class Reports implements OnInit {
  private transactionService = inject(TransactionService);
  private categoryService = inject(TransactionCategoryService);
  private companyService = inject(CompanyService);
  private bankAccountService = inject(BankAccountService);
  private currentCompanyService = inject(CurrentCompanyService);

  transactions = signal<Transaction[]>([]);
  companies = signal<Company[]>([]);
  bankAccounts = signal<BankAccount[]>([]);
  categories = signal<TransactionCategory[]>([]);
  loading = signal(false);
  exportModalOpen = signal(false);

  companyId = signal<NumberFilter>('all');
  type = signal<TypeFilter>('all');
  categoryId = signal<NumberFilter>('all');
  bankAccountId = signal<NumberFilter>('all');
  startDate = signal('');
  endDate = signal('');
  searchTerm = signal('');

  columns: ReportColumn[] = [
    { key: 'date', label: 'Data', selected: true, value: (t) => this.formatDate(t.date) },
    { key: 'description', label: 'Descrição', selected: true, value: (t) => t.description },
    { key: 'company', label: 'Empresa', selected: true, value: (t) => this.companyName(t.companyId) },
    { key: 'category', label: 'Categoria', selected: true, value: (t) => this.categoryName(t.transactionCategoryId) },
    { key: 'account', label: 'Conta', selected: true, value: (t) => this.accountName(t.bankAccountId) },
    { key: 'type', label: 'Tipo', selected: true, value: (t) => this.transactionTypeLabel(t.type) },
    { key: 'method', label: 'Forma', selected: true, value: (t) => paymentMethodLabel(t.method) },
    { key: 'value', label: 'Valor', selected: true, value: (t) => this.formatNumber(t.type === 'ENTRADA' ? t.value : -t.value) },
    { key: 'recurrence', label: 'Recorrência', selected: false, value: (t) => this.recurrenceLabel(t) },
  ];

  filteredTransactions = computed(() => {
    const companyId = this.companyId();
    const type = this.type();
    const categoryId = this.categoryId();
    const bankAccountId = this.bankAccountId();
    const startDate = this.startDate();
    const endDate = this.endDate();
    const term = this.searchTerm().trim().toLowerCase();

    return this.transactions()
      .filter((transaction) => companyId === 'all' || transaction.companyId === companyId)
      .filter((transaction) => type === 'all' || transaction.type === type)
      .filter((transaction) => categoryId === 'all' || transaction.transactionCategoryId === categoryId)
      .filter((transaction) => bankAccountId === 'all' || transaction.bankAccountId === bankAccountId)
      .filter((transaction) => !startDate || transaction.date >= startDate)
      .filter((transaction) => !endDate || transaction.date <= endDate)
      .filter((transaction) => {
        if (!term) {
          return true;
        }

        return [
          transaction.description,
          this.companyName(transaction.companyId),
          this.categoryName(transaction.transactionCategoryId),
          this.accountName(transaction.bankAccountId),
          paymentMethodLabel(transaction.method),
        ].some((value) => value.toLowerCase().includes(term));
      })
      .sort((a, b) => b.date.localeCompare(a.date) || b.id - a.id);
  });

  filteredCategories = computed(() => {
    const companyId = this.companyId();
    const type = this.type();

    return this.categories()
      .filter((category) => companyId === 'all' || category.companyId === companyId)
      .filter((category) => type === 'all' || category.type === type);
  });

  filteredBankAccounts = computed(() => {
    const companyId = this.companyId();
    return this.bankAccounts().filter((account) => companyId === 'all' || account.companyId === companyId);
  });

  totalIncome = computed(() => this.filteredTransactions()
    .filter((transaction) => transaction.type === 'ENTRADA')
    .reduce((sum, transaction) => sum + transaction.value, 0));

  totalExpense = computed(() => this.filteredTransactions()
    .filter((transaction) => transaction.type === 'SAIDA')
    .reduce((sum, transaction) => sum + transaction.value, 0));

  balance = computed(() => this.totalIncome() - this.totalExpense());

  ngOnInit(): void {
    const currentCompanyId = this.currentCompanyService.currentCompanyId();

    if (currentCompanyId) {
      this.companyId.set(currentCompanyId);
    }

    this.loadReportData();
  }

  loadReportData(): void {
    this.loading.set(true);

    forkJoin({
      transactions: this.transactionService.list(),
      companies: this.companyService.list(),
    }).pipe(
      switchMap(({ transactions, companies }) => {
        this.transactions.set(transactions);
        this.companies.set(companies);

        if (companies.length === 0) {
          return of({ bankAccounts: [], categories: [] });
        }

        return forkJoin({
          bankAccounts: forkJoin(companies.map((company) => this.bankAccountService.listByCompany(company.id))),
          categories: forkJoin(companies.map((company) => this.categoryService.listCategoriesByCompany(company.id))),
        });
      }),
    ).subscribe({
      next: ({ bankAccounts, categories }) => {
        this.bankAccounts.set(bankAccounts.flat());
        this.categories.set(categories.flat());
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  onCompanyChange(companyId: NumberFilter): void {
    this.companyId.set(companyId);
    this.categoryId.set('all');
    this.bankAccountId.set('all');
  }

  onTypeChange(type: TypeFilter): void {
    this.type.set(type);
    this.categoryId.set('all');
  }

  clearFilters(): void {
    this.companyId.set(this.currentCompanyService.currentCompanyId() ?? 'all');
    this.type.set('all');
    this.categoryId.set('all');
    this.bankAccountId.set('all');
    this.startDate.set('');
    this.endDate.set('');
    this.searchTerm.set('');
  }

  openExportModal(): void {
    this.exportModalOpen.set(true);
  }

  closeExportModal(): void {
    this.exportModalOpen.set(false);
  }

  selectedColumns(): ReportColumn[] {
    return this.columns.filter((column) => column.selected);
  }

  toggleAllColumns(selected: boolean): void {
    this.columns.forEach((column) => column.selected = selected);
  }

  exportXls(): void {
    const selectedColumns = this.selectedColumns();

    if (selectedColumns.length === 0 || this.filteredTransactions().length === 0) {
      return;
    }

    const header = selectedColumns.map((column) => `<th>${this.escapeHtml(column.label)}</th>`).join('');
    const rows = this.filteredTransactions().map((transaction) => {
      const cells = selectedColumns
        .map((column) => `<td>${this.escapeHtml(column.value(transaction))}</td>`)
        .join('');

      return `<tr>${cells}</tr>`;
    }).join('');

    const html = `
      <html>
        <head>
          <meta charset="UTF-8" />
          <style>
            table { border-collapse: collapse; }
            th, td { border: 1px solid #999; padding: 6px; }
            th { background: #e9eef7; font-weight: bold; }
          </style>
        </head>
        <body>
          <table>
            <thead><tr>${header}</tr></thead>
            <tbody>${rows}</tbody>
          </table>
        </body>
      </html>`;

    const blob = new Blob([html], { type: 'application/vnd.ms-excel;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `relatorio-movimentacoes-${this.todayIso()}.xls`;
    link.click();
    URL.revokeObjectURL(url);
    this.closeExportModal();
  }

  companyName(id: number): string {
    return this.companies().find((company) => company.id === id)?.tradeName ?? 'Empresa removida';
  }

  categoryName(id: number): string {
    return this.categories().find((category) => category.id === id)?.name ?? 'Categoria removida';
  }

  accountName(id: number): string {
    const account = this.bankAccounts().find((item) => item.id === id);
    return account ? `${account.bankName} - ${account.accountNumber}` : 'Conta removida';
  }

  transactionTypeLabel(type: TransactionType): string {
    return type === 'ENTRADA' ? 'Entrada' : 'Saída';
  }

  recurrenceLabel(transaction: Transaction): string {
    if (!transaction.recurrenceFrequency) {
      return 'Não recorrente';
    }

    const frequency = {
      DAILY: 'Diária',
      WEEKLY: 'Semanal',
      MONTHLY: 'Mensal',
      YEARLY: 'Anual',
    }[transaction.recurrenceFrequency];

    return transaction.recurrenceEndDate
      ? `${frequency} até ${this.formatDate(transaction.recurrenceEndDate)}`
      : `${frequency} sem data final`;
  }

  formatCurrency(value: number): string {
    return value.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
  }

  formatNumber(value: number): string {
    return value.toLocaleString('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }

  formatDate(iso: string): string {
    const [year, month, day] = iso.split('-');
    return `${day}/${month}/${year}`;
  }

  paymentMethodLabel(transaction: Transaction): string {
    return paymentMethodLabel(transaction.method);
  }

  private todayIso(): string {
    return new Date().toISOString().slice(0, 10);
  }

  private escapeHtml(value: string): string {
    return value
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#039;');
  }
}
