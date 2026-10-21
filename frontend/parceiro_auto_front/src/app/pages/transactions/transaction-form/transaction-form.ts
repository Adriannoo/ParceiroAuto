import { Component, OnInit, computed, effect, untracked, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { BankAccount } from '../../bank-accounts/bank-account.model';
import { BankAccountService } from '../../bank-accounts/bank-account.service';
import { forkJoin, Subscription } from 'rxjs';
import { CurrentCompanyService } from '../../../services/current-company.service';
import {
  PAYMENT_METHODS,
  RECURRENCE_FREQUENCIES,
  TRANSACTION_TYPES,
  TransactionCategory,
  TransactionRequest,
  TransactionType,
} from '../transaction.model';
import { TransactionService } from '../transaction.service';
import { TransactionCategoryService } from '../transaction-category.service';

@Component({
  selector: 'app-transaction-form',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './transaction-form.html',
  styleUrl: './transaction-form.scss',
})
export class TransactionForm implements OnInit {
  private fb = inject(FormBuilder);
  private transactionService = inject(TransactionService);
  private categoryService = inject(TransactionCategoryService);
  private bankAccountService = inject(BankAccountService);
  private currentCompanyService = inject(CurrentCompanyService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  transactionTypes = TRANSACTION_TYPES;
  paymentMethods = PAYMENT_METHODS;
  recurrenceFrequencies = RECURRENCE_FREQUENCIES;

  companyId = this.currentCompanyService.currentCompanyId;
  private loadedCompanyId: number | null = null;
  private optionsSubscription?: Subscription;
  bankAccounts = signal<BankAccount[]>([]);
  categories = signal<TransactionCategory[]>([]);
  transactionId = signal<number | null>(null);
  selectedType = signal<TransactionType>('ENTRADA');
  loadingOptions = signal(false);
  saving = signal(false);
  savingCategory = signal(false);
  categoryModalOpen = signal(false);
  error = signal<string | null>(null);
  categoryError = signal<string | null>(null);

  form = this.fb.group({
    type: ['ENTRADA' as TransactionType, [Validators.required]],
    description: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(120)]],
    value: [null as number | null, [Validators.required, Validators.min(0.01)]],
    date: [this.todayIso(), [Validators.required]],
    transactionCategoryId: [null as number | null, [Validators.required]],
    bankAccountId: [null as number | null, [Validators.required]],
    method: ['PIX', [Validators.required]],
    recurring: [false],
    recurrenceFrequency: ['MONTHLY' as TransactionRequest['recurrenceFrequency']],
    recurrenceEndDate: [null as string | null],
  });

  categoryForm = this.fb.group({
    name: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(50)]],
  });

  filteredCategories = computed(() => {
    const type = this.selectedType();
    return this.categories().filter((category) => category.type === type);
  });

  get editing(): boolean {
    return this.transactionId() !== null;
  }

  constructor() {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) this.transactionId.set(Number(id));
    effect((onCleanup) => {
      const companyId = this.companyId();
      untracked(() => {
        this.optionsSubscription?.unsubscribe();
        this.loadedCompanyId = null;
        this.bankAccounts.set([]);
        this.categories.set([]);
        this.categoryModalOpen.set(false);
        this.loadingOptions.set(false);
        this.form.disable({ emitEvent: false });
        if (!companyId) {
          this.error.set('Selecione uma empresa na sidebar ou cadastre uma empresa para continuar.');
          return;
        }
        this.error.set(null);
        const transactionId = this.transactionId();
        if (transactionId === null) {
          this.loadOptions(companyId);
        } else if (!Number.isSafeInteger(transactionId) || transactionId <= 0) {
          this.error.set('Movimentacao invalida.');
        } else {
          this.loadingOptions.set(true);
          const subscription = this.transactionService.findById(transactionId).subscribe({
            next: transaction => {
              if (this.companyId() !== companyId) return;
              if (transaction.companyId !== companyId) {
                this.loadingOptions.set(false);
                this.error.set('Esta movimentacao nao pertence a empresa atual. Selecione a empresa correta na sidebar.');
                return;
              }
              this.loadedCompanyId = companyId;
              this.form.patchValue(transaction, { emitEvent: false });
              this.form.controls.recurring.setValue(!!transaction.recurrenceFrequency, { emitEvent: false });
              this.selectedType.set(transaction.type);
              this.loadOptions(companyId, transaction.bankAccountId, transaction.transactionCategoryId);
            },
            error: () => {
              this.loadingOptions.set(false);
              this.error.set('Movimentacao nao encontrada.');
            },
          });
          onCleanup(() => subscription.unsubscribe());
        }
        onCleanup(() => this.optionsSubscription?.unsubscribe());
      });
    });
  }

  ngOnInit(): void {
    this.form.controls.type.valueChanges.subscribe((type) => {
      this.selectedType.set(type as TransactionType);
      this.selectFirstCategoryByType();
    });
  }

  private loadOptions(
    companyId: number,
    selectedBankAccountId: number | null = null,
    selectedCategoryId: number | null = null,
  ): void {
    this.optionsSubscription?.unsubscribe();
    this.form.controls.bankAccountId.setValue(null, { emitEvent: false });
    this.form.controls.transactionCategoryId.setValue(null, { emitEvent: false });
    this.loadingOptions.set(true);
    this.form.disable({ emitEvent: false });
    this.optionsSubscription = forkJoin({
      bankAccounts: this.bankAccountService.listByCompany(companyId),
      categories: this.categoryService.listCategoriesByCompany(companyId),
    }).subscribe({
      next: ({ bankAccounts, categories }) => {
        if (this.companyId() !== companyId) return;
        this.bankAccounts.set(bankAccounts.filter(account => account.companyId === companyId));
        this.categories.set(categories.filter(category => category.companyId === companyId && category.active));
        const account = selectedBankAccountId
          ? this.bankAccounts().find(item => item.id === selectedBankAccountId)
          : this.bankAccounts().find(item => item.defaultAccount) ?? this.bankAccounts()[0];
        this.form.controls.bankAccountId.setValue(account?.id ?? null, { emitEvent: false });
        if (selectedCategoryId) {
          const category = this.filteredCategories().find(item => item.id === selectedCategoryId);
          this.form.controls.transactionCategoryId.setValue(category?.id ?? null, { emitEvent: false });
        } else {
          this.selectFirstCategoryByType();
        }
        this.loadingOptions.set(false);
        this.form.enable({ emitEvent: false });
      },
      error: () => {
        this.loadingOptions.set(false);
        this.error.set('Nao foi possivel carregar contas e categorias da empresa atual. Reabra o formulario para tentar novamente.');
      },
    });
  }

  private selectFirstCategoryByType(): void {
    const category = this.filteredCategories()[0];
    this.form.controls.transactionCategoryId.setValue(category?.id ?? null, { emitEvent: false });
  }

  openCategoryModal(): void {
    this.categoryError.set(null);
    this.categoryForm.reset();
    if (!this.companyId() || this.form.disabled) return;
    this.categoryModalOpen.set(true);
  }

  closeCategoryModal(): void {
    if (this.savingCategory()) {
      return;
    }

    this.categoryModalOpen.set(false);
    this.categoryError.set(null);
    this.categoryForm.reset();
  }

  saveCategory(): void {
    this.categoryError.set(null);

    if (!this.companyId() || this.form.disabled || this.loadingOptions() || this.savingCategory()) {
      this.categoryError.set('Selecione uma empresa antes de criar a categoria.');
      return;
    }

    if (this.categoryForm.invalid) {
      this.categoryForm.markAllAsTouched();
      return;
    }

    const companyId = this.companyId()!;
    const name = this.categoryForm.controls.name.value?.trim() ?? '';
    const type = this.selectedType();

    this.savingCategory.set(true);

    this.categoryService.createCategory(companyId, { name, type }).subscribe({
      next: (category) => {
        this.savingCategory.set(false);
        if (this.companyId() !== companyId) return;
        const categories = [
          ...this.categories().filter((item) => item.id !== category.id),
          category,
        ].sort((a, b) => a.name.localeCompare(b.name));

        this.categories.set(categories);
        this.form.controls.transactionCategoryId.setValue(category.id, { emitEvent: false });
        this.categoryModalOpen.set(false);
        this.categoryForm.reset();
        this.savingCategory.set(false);
      },
      error: () => {
        this.categoryError.set('Não foi possível criar a categoria. Confira o nome e tente novamente.');
        this.savingCategory.set(false);
      },
    });
  }

  accountLabel(account: BankAccount): string {
    return `${account.bankName} - Ag. ${account.branch} - Conta ${account.accountNumber}`;
  }

  private todayIso(): string {
    return new Date().toISOString().slice(0, 10);
  }

  isInvalid(field: string): boolean {
    const control = this.form.get(field);
    return !!control && control.invalid && (control.touched || control.dirty);
  }

  hasError(field: string, type: string): boolean {
    return !!this.form.get(field)?.errors?.[type];
  }

  isCategoryInvalid(field: string): boolean {
    const control = this.categoryForm.get(field);
    return !!control && control.invalid && (control.touched || control.dirty);
  }

  save(): void {
    this.error.set(null);

    const companyId = this.companyId();
    if (!companyId) {
      this.error.set('Selecione uma empresa na sidebar antes de salvar.');
      return;
    }
    if (this.saving() || this.loadingOptions() || this.form.disabled || (this.editing && this.loadedCompanyId !== companyId)) {
      this.error.set('Aguarde o carregamento dos dados da empresa atual.');
      return;
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const data = this.form.getRawValue();
    if (!this.bankAccounts().some(account => account.id === data.bankAccountId && account.companyId === companyId)
      || !this.filteredCategories().some(category => category.id === data.transactionCategoryId && category.companyId === companyId)) {
      this.error.set('Selecione uma conta e uma categoria validas da empresa atual.');
      return;
    }
    if (data.recurring && !data.recurrenceFrequency) {
      this.error.set('Informe a frequencia da recorrencia.');
      return;
    }
    const id = this.transactionId();
    const transactionDate = data.date ?? this.todayIso();
    const recurrenceEndDate = data.recurring && data.recurrenceEndDate
      ? data.recurrenceEndDate
      : null;

    if (data.recurring && recurrenceEndDate && recurrenceEndDate < transactionDate) {
      this.error.set('A data final da recorrência não pode ser anterior à transação.');
      return;
    }

    const request: TransactionRequest = {
      companyId,
      bankAccountId: Number(data.bankAccountId),
      transactionCategoryId: Number(data.transactionCategoryId),
      type: data.type as TransactionType,
      description: data.description ?? '',
      value: Math.round(Number(data.value) * 100) / 100,
      date: transactionDate,
      method: data.method as TransactionRequest['method'],
      recurrenceFrequency: data.recurring ? data.recurrenceFrequency : null,
      recurrenceEndDate,
    };

    this.saving.set(true);

    const saveRequest = id
      ? this.transactionService.update(id, request)
      : this.transactionService.create(request);

    saveRequest.subscribe({
      next: () => this.router.navigate(['/transactions']),
      error: () => {
        this.error.set('Não foi possível salvar. Confira os dados e tente novamente.');
        this.saving.set(false);
      },
    });
  }

  reset(): void {
    this.form.reset({
      type: 'ENTRADA',
      method: 'PIX',
      date: this.todayIso(),
      recurring: false,
      recurrenceFrequency: 'MONTHLY',
      recurrenceEndDate: null,
    });
    this.selectedType.set('ENTRADA');
    const companyId = this.companyId();
    if (companyId && !this.form.disabled) this.loadOptions(companyId);
    this.error.set(null);
  }
}
