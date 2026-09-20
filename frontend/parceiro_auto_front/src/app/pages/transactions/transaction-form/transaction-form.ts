import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { BankAccount } from '../../bank-accounts/bank-account.model';
import { BankAccountService } from '../../bank-accounts/bank-account.service';
import { Company } from '../../companies/company.model';
import { CompanyService } from '../../companies/company.service';
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
  private companyService = inject(CompanyService);
  private bankAccountService = inject(BankAccountService);
  private currentCompanyService = inject(CurrentCompanyService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  transactionTypes = TRANSACTION_TYPES;
  paymentMethods = PAYMENT_METHODS;
  recurrenceFrequencies = RECURRENCE_FREQUENCIES;

  companies = signal<Company[]>([]);
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
    companyId: [null as number | null, [Validators.required]],
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

  ngOnInit(): void {
    this.loadCompanies();

    this.form.controls.companyId.valueChanges.subscribe((companyId) => {
      this.loadOptions(companyId);
    });

    this.form.controls.type.valueChanges.subscribe((type) => {
      this.selectedType.set(type as TransactionType);
      this.selectFirstCategoryByType();
    });

    const id = this.route.snapshot.paramMap.get('id');

    if (id) {
      this.transactionId.set(Number(id));
      this.load(Number(id));
    }
  }

  private loadCompanies(): void {
    this.companyService.list().subscribe({
      next: (companies) => {
        const activeCompanies = companies.filter((company) => company.active);
        this.companies.set(activeCompanies);

        if (activeCompanies.length === 0) {
          this.error.set('Cadastre uma empresa ativa antes de lançar movimentações.');
          this.form.disable();
          return;
        }

        this.currentCompanyService.ensureCompany(activeCompanies.map((company) => company.id));

        if (!this.editing) {
          this.form.controls.companyId.setValue(this.currentCompanyService.currentCompanyId() ?? activeCompanies[0].id);
        }
      },
      error: () => {
        this.error.set('Não foi possível carregar as empresas.');
        this.form.disable();
      },
    });
  }

  private load(id: number): void {
    this.transactionService.findById(id).subscribe({
      next: (transaction) => {
        this.form.patchValue(transaction, { emitEvent: false });
        this.form.controls.recurring.setValue(!!transaction.recurrenceFrequency, { emitEvent: false });
        this.selectedType.set(transaction.type);
        this.loadOptions(transaction.companyId, transaction.bankAccountId, transaction.transactionCategoryId);
      },
      error: () => {
        this.error.set('Movimentação não encontrada.');
        this.form.disable();
      },
    });
  }

  private loadOptions(
    companyId: number | null,
    selectedBankAccountId: number | null = null,
    selectedCategoryId: number | null = null,
  ): void {
    this.bankAccounts.set([]);
    this.categories.set([]);
    this.form.controls.bankAccountId.setValue(null, { emitEvent: false });
    this.form.controls.transactionCategoryId.setValue(null, { emitEvent: false });

    if (!companyId) {
      return;
    }

    this.loadingOptions.set(true);

    this.bankAccountService.listByCompany(companyId).subscribe({
      next: (bankAccounts) => {
        this.bankAccounts.set(bankAccounts);

        const bankAccount = selectedBankAccountId
          ? bankAccounts.find((account) => account.id === selectedBankAccountId)
          : bankAccounts.find((account) => account.defaultAccount) ?? bankAccounts[0];

        this.form.controls.bankAccountId.setValue(bankAccount?.id ?? null, { emitEvent: false });
      },
      error: () => {
        this.bankAccounts.set([]);
      },
    });

    this.categoryService.listCategoriesByCompany(companyId).subscribe({
      next: (categories) => {
        this.categories.set(categories);

        if (selectedCategoryId && categories.some((category) => category.id === selectedCategoryId)) {
          this.form.controls.transactionCategoryId.setValue(selectedCategoryId, { emitEvent: false });
        } else {
          this.selectFirstCategoryByType();
        }

        this.loadingOptions.set(false);
      },
      error: () => {
        this.categories.set([]);
        this.loadingOptions.set(false);
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

    if (!this.form.controls.companyId.value) {
      this.categoryError.set('Selecione uma empresa antes de criar a categoria.');
      return;
    }

    if (this.categoryForm.invalid) {
      this.categoryForm.markAllAsTouched();
      return;
    }

    const companyId = Number(this.form.controls.companyId.value);
    const name = this.categoryForm.controls.name.value?.trim() ?? '';
    const type = this.selectedType();

    this.savingCategory.set(true);

    this.categoryService.createCategory(companyId, { name, type }).subscribe({
      next: (category) => {
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

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const data = this.form.getRawValue();
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
      companyId: Number(data.companyId),
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
      companyId: this.companies()[0]?.id ?? null,
      type: 'ENTRADA',
      method: 'PIX',
      date: this.todayIso(),
      recurring: false,
      recurrenceFrequency: 'MONTHLY',
      recurrenceEndDate: null,
    });
    this.selectedType.set('ENTRADA');
    this.error.set(null);
  }
}
