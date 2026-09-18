import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { forkJoin, of, switchMap } from 'rxjs';
import { BankAccount } from '../../bank-accounts/bank-account.model';
import { BankAccountService } from '../../bank-accounts/bank-account.service';
import { Company } from '../../companies/company.model';
import { CompanyService } from '../../companies/company.service';
import { CurrentCompanyService } from '../../../services/current-company.service';
import {
  RECURRENCE_FREQUENCIES,
  RecurringTransaction,
  Transaction,
  TransactionCategory,
  TransactionType,
  paymentMethodLabel,
} from '../transaction.model';
import { TransactionService } from '../transaction.service';

type TypeFilter = 'all' | TransactionType;

@Component({
  selector: 'app-transaction-list',
  imports: [FormsModule, ReactiveFormsModule, RouterLink],
  templateUrl: './transaction-list.html',
  styleUrl: './transaction-list.scss',
})
export class TransactionList implements OnInit {
  private fb = inject(FormBuilder);
  private transactionService = inject(TransactionService);
  private companyService = inject(CompanyService);
  private bankAccountService = inject(BankAccountService);
  private currentCompanyService = inject(CurrentCompanyService);

  recurrenceFrequencies = RECURRENCE_FREQUENCIES;
  transactions = signal<Transaction[]>([]);
  companies = signal<Company[]>([]);
  bankAccounts = signal<BankAccount[]>([]);
  categories = signal<TransactionCategory[]>([]);
  recurringTransactions = signal<RecurringTransaction[]>([]);
  loading = signal(false);
  loadingRecurring = signal(false);
  savingRecurrence = signal(false);

  searchTerm = signal('');
  type = signal<TypeFilter>('all');
  companyId = signal<number | 'all'>('all');

  transactionToDelete = signal<Transaction | null>(null);
  recurrenceToEdit = signal<RecurringTransaction | null>(null);
  recurrenceToDelete = signal<RecurringTransaction | null>(null);

  recurrenceForm = this.fb.group({
    frequency: ['MONTHLY' as RecurringTransaction['frequency'], [Validators.required]],
    endDate: [null as string | null],
  });

  filtered = computed(() => {
    const term = this.searchTerm().trim().toLowerCase();
    const typeFilter = this.type();
    const companyFilter = this.companyId();

    return this.transactions()
      .filter((transaction) => typeFilter === 'all' || transaction.type === typeFilter)
      .filter((transaction) => companyFilter === 'all' || transaction.companyId === companyFilter)
      .filter(
        (transaction) =>
          !term ||
          transaction.description.toLowerCase().includes(term) ||
          this.categoryName(transaction).toLowerCase().includes(term) ||
          this.accountName(transaction).toLowerCase().includes(term) ||
          this.companyName(transaction.companyId).toLowerCase().includes(term),
      )
      .sort((a, b) => b.date.localeCompare(a.date) || b.id - a.id);
  });

  recentTransactions = computed(() => this.filtered().slice(0, 10));

  ngOnInit(): void {
    const currentCompanyId = this.currentCompanyService.currentCompanyId();

    if (currentCompanyId) {
      this.companyId.set(currentCompanyId);
    }

    this.load();
  }

  load(): void {
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
          categories: forkJoin(companies.map((company) => this.transactionService.listCategoriesByCompany(company.id))),
        });
      }),
    ).subscribe({
      next: ({ bankAccounts, categories }) => {
        this.bankAccounts.set(bankAccounts.flat());
        this.categories.set(categories.flat());
        this.loading.set(false);
        this.loadRecurring();
      },
      error: () => this.loading.set(false),
    });
  }

  loadRecurring(): void {
    const companyId = this.companyId();

    if (companyId === 'all') {
      this.recurringTransactions.set([]);
      return;
    }

    this.loadingRecurring.set(true);

    this.transactionService.listRecurringByCompany(companyId).subscribe({
      next: (items) => {
        this.recurringTransactions.set(items);
        this.loadingRecurring.set(false);
      },
      error: () => {
        this.recurringTransactions.set([]);
        this.loadingRecurring.set(false);
      },
    });
  }

  onCompanyFilterChange(companyId: number | 'all'): void {
    this.companyId.set(companyId);
    this.loadRecurring();
  }

  companyName(id: number): string {
    return this.companies().find((company) => company.id === id)?.tradeName ?? 'Empresa removida';
  }

  accountName(transaction: Transaction): string {
    const account = this.bankAccounts().find((item) => item.id === transaction.bankAccountId);

    return account ? `${account.bankName} - ${account.accountNumber}` : 'Conta removida';
  }

  categoryName(transaction: Transaction): string {
    return this.categories().find((category) => category.id === transaction.transactionCategoryId)?.name ?? 'Categoria removida';
  }

  paymentMethodLabel(transaction: Transaction): string {
    return paymentMethodLabel(transaction.method);
  }

  transactionTypeLabel(transaction: Transaction): string {
    return transaction.type === 'ENTRADA' ? 'Entrada' : 'Saída';
  }

  recurrenceTypeLabel(transaction: RecurringTransaction): string {
    return transaction.type === 'ENTRADA' ? 'Entrada' : 'Saída';
  }

  recurrenceFrequencyLabel(frequency: RecurringTransaction['frequency']): string {
    return this.recurrenceFrequencies.find((item) => item.value === frequency)?.label ?? frequency;
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
    this.recurringTransactions.set([]);
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

  openRecurrenceEdit(recurrence: RecurringTransaction): void {
    this.recurrenceForm.reset({
      frequency: recurrence.frequency,
      endDate: recurrence.endDate,
    });
    this.recurrenceToEdit.set(recurrence);
  }

  closeRecurrenceEdit(): void {
    if (this.savingRecurrence()) {
      return;
    }

    this.recurrenceToEdit.set(null);
    this.recurrenceForm.reset({
      frequency: 'MONTHLY',
      endDate: null,
    });
  }

  saveRecurrence(): void {
    const recurrence = this.recurrenceToEdit();

    if (!recurrence) {
      return;
    }

    if (this.recurrenceForm.invalid) {
      this.recurrenceForm.markAllAsTouched();
      return;
    }

    const data = this.recurrenceForm.getRawValue();
    this.savingRecurrence.set(true);

    this.transactionService.updateRecurrence(recurrence.recurrenceRuleId, {
      frequency: data.frequency ?? 'MONTHLY',
      endDate: data.endDate || null,
    }).subscribe({
      next: () => {
        this.savingRecurrence.set(false);
        this.closeRecurrenceEdit();
        this.loadRecurring();
      },
      error: () => this.savingRecurrence.set(false),
    });
  }

  openRecurrenceDelete(recurrence: RecurringTransaction): void {
    this.recurrenceToDelete.set(recurrence);
  }

  closeRecurrenceDelete(): void {
    if (this.savingRecurrence()) {
      return;
    }

    this.recurrenceToDelete.set(null);
  }

  confirmRecurrenceDelete(): void {
    const recurrence = this.recurrenceToDelete();

    if (!recurrence) {
      return;
    }

    this.savingRecurrence.set(true);

    this.transactionService.deleteRecurrence(recurrence.recurrenceRuleId).subscribe({
      next: () => {
        this.recurrenceToDelete.set(null);
        this.savingRecurrence.set(false);
        this.loadRecurring();
        this.load();
      },
      error: () => this.savingRecurrence.set(false),
    });
  }
}
