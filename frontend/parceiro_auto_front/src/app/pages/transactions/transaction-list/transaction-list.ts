import { Component, OnInit, computed, effect, untracked, inject, signal } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { forkJoin, Subscription } from 'rxjs';
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
import { RecurrenceRuleService } from '../recurrence-rule.service';
import { TransactionCategoryService } from '../transaction-category.service';

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
  private recurrenceService = inject(RecurrenceRuleService);
  private categoryService = inject(TransactionCategoryService);
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
  companyId = this.currentCompanyService.currentCompanyId;
  private dataSubscription?: Subscription;
  private recurringSubscription?: Subscription;
  error = signal<string | null>(null);

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
      .filter((transaction) => transaction.companyId === companyFilter)
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

  constructor() {
    effect(onCleanup => {
      this.companyId();
      untracked(() => {
        this.transactionToDelete.set(null);
        this.recurrenceToEdit.set(null);
        this.recurrenceToDelete.set(null);
        this.error.set(null);
        this.load();
      });
      onCleanup(() => {
        this.dataSubscription?.unsubscribe();
        this.recurringSubscription?.unsubscribe();
      });
    });
  }

  ngOnInit(): void {
    this.companyService.list().subscribe({ next: companies => this.companies.set(companies) });
  }

  load(): void {
    this.dataSubscription?.unsubscribe();
    const companyId = this.companyId();
    this.transactions.set([]);
    this.bankAccounts.set([]);
    this.categories.set([]);
    this.loading.set(false);
    this.loadRecurring();
    if (!companyId) return;
    this.loading.set(true);
    this.dataSubscription = forkJoin({
      transactions: this.transactionService.listByCompany(companyId),
      bankAccounts: this.bankAccountService.listByCompany(companyId),
      categories: this.categoryService.listCategoriesByCompany(companyId, true),
    }).subscribe({
      next: ({ transactions, bankAccounts, categories }) => {
        if (this.companyId() !== companyId) return;
        this.transactions.set(transactions);
        this.bankAccounts.set(bankAccounts);
        this.categories.set(categories);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.error.set('Nao foi possivel carregar as movimentacoes da empresa atual.');
      },
    });
  }

  loadRecurring(): void {
    this.recurringSubscription?.unsubscribe();
    const companyId = this.companyId();
    this.recurringTransactions.set([]);
    this.loadingRecurring.set(false);
    if (!companyId) return;
    this.loadingRecurring.set(true);
    this.recurringSubscription = this.recurrenceService.listRecurringByCompany(companyId).subscribe({
      next: items => {
        if (this.companyId() !== companyId) return;
        this.recurringTransactions.set(items);
        this.loadingRecurring.set(false);
      },
      error: () => {
        this.loadingRecurring.set(false);
        this.error.set('Nao foi possivel carregar as recorrencias.');
      },
    });
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
  }

  hasFilters(): boolean {
    return this.searchTerm() !== '' || this.type() !== 'all';
  }

  openConfirmation(transaction: Transaction): void {
    this.transactionToDelete.set(transaction);
  }

  closeConfirmation(): void {
    this.transactionToDelete.set(null);
  }

  confirmDeletion(): void {
    const transaction = this.transactionToDelete();

    if (!transaction || transaction.companyId !== this.companyId()) {
      return;
    }

    this.transactionService.delete(transaction.id).subscribe({
      next: () => {
        this.closeConfirmation();
        this.load();
      },
      error: () => this.error.set('Nao foi possivel excluir a movimentacao. Tente novamente.'),
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

    if (!recurrence || this.savingRecurrence() || recurrence.companyId !== this.companyId()) {
      return;
    }

    if (this.recurrenceForm.invalid) {
      this.recurrenceForm.markAllAsTouched();
      return;
    }

    const data = this.recurrenceForm.getRawValue();
    this.savingRecurrence.set(true);

    this.recurrenceService.updateRecurrence(recurrence.recurrenceRuleId, {
      frequency: data.frequency ?? 'MONTHLY',
      endDate: data.endDate || null,
    }).subscribe({
      next: () => {
        this.savingRecurrence.set(false);
        this.closeRecurrenceEdit();
        this.load();
      },
      error: () => {
        this.savingRecurrence.set(false);
        this.error.set('Nao foi possivel atualizar a recorrencia. Confira a data final.');
      },
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

    if (!recurrence || this.savingRecurrence() || recurrence.companyId !== this.companyId()) {
      return;
    }

    this.savingRecurrence.set(true);

    this.recurrenceService.deleteRecurrence(recurrence.recurrenceRuleId).subscribe({
      next: () => {
        this.recurrenceToDelete.set(null);
        this.savingRecurrence.set(false);
        this.load();
      },
      error: () => {
        this.savingRecurrence.set(false);
        this.error.set('Nao foi possivel encerrar a recorrencia.');
      },
    });
  }
}
