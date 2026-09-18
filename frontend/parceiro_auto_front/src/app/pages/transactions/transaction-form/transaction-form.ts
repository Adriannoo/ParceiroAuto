import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { TransactionService } from '../transaction.service';
import { CATEGORIES, PAYMENT_METHODS, TRANSACTION_TYPES } from '../transaction.model';
import { CompanyService } from '../../companies/company.service';
import { Company } from '../../companies/company.model';
import { BankAccount } from '../../bank-accounts/bank-account.model';
import { BankAccountService } from '../../bank-accounts/bank-account.service';

@Component({
  selector: 'app-transaction-form',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './transaction-form.html',
  styleUrl: './transaction-form.scss',
})
export class TransactionForm implements OnInit {
  private fb = inject(FormBuilder);
  private transactionService = inject(TransactionService);
  private companyService = inject(CompanyService);
  private bankAccountService = inject(BankAccountService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  transactionTypes = TRANSACTION_TYPES;
  paymentMethods = PAYMENT_METHODS;
  categories = CATEGORIES;

  companies = signal<Company[]>([]);
  bankAccounts = signal<BankAccount[]>([]);
  transactionId = signal<number | null>(null);
  loadingBankAccounts = signal(false);
  saving = signal(false);
  error = signal<string | null>(null);

  form = this.fb.group({
    companyId: [null as number | null, [Validators.required]],
    type: ['ENTRADA', [Validators.required]],
    description: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(120)]],
    value: [null as number | null, [Validators.required, Validators.min(0.01)]],
    date: [this.todayIso(), [Validators.required]],
    category: ['', [Validators.required]],
    account: ['', [Validators.required]],
    method: ['PIX', [Validators.required]],
  });

  get editing(): boolean {
    return this.transactionId() !== null;
  }

  ngOnInit(): void {
    this.companyService.list().subscribe((companies) => {
      this.companies.set(companies.filter((e) => e.active));

      if (this.companies().length === 0) {
        this.error.set('Cadastre uma empresa ativa antes de lançar movimentações.');
        this.form.disable();
      }
    });

    this.form.controls.companyId.valueChanges.subscribe((companyId) => {
      this.loadBankAccounts(companyId);
    });

    const id = this.route.snapshot.paramMap.get('id');

    if (id) {
      this.transactionId.set(Number(id));
      this.load(Number(id));
    }
  }

  private load(id: number): void {
    this.transactionService.findById(id).subscribe({
      next: (transaction) => {
        this.form.patchValue(transaction, { emitEvent: false });
        this.loadBankAccounts(transaction.companyId, transaction.account);
      },
      error: () => {
        this.error.set('Movimentação não encontrada.');
        this.form.disable();
      },
    });
  }

  private loadBankAccounts(companyId: number | null, selectedAccount = ''): void {
    this.bankAccounts.set([]);

    if (!companyId) {
      this.form.controls.account.setValue('');
      return;
    }

    this.loadingBankAccounts.set(true);

    this.bankAccountService.listByCompany(companyId).subscribe({
      next: (bankAccounts) => {
        this.bankAccounts.set(bankAccounts);
        this.loadingBankAccounts.set(false);

        const currentAccount = selectedAccount || this.form.controls.account.value;
        const accountExists = bankAccounts.some((account) => this.accountOptionValue(account) === currentAccount);

        if (accountExists) {
          this.form.controls.account.setValue(currentAccount);
          return;
        }

        const defaultAccount = bankAccounts.find((account) => account.defaultAccount);
        const firstAccount = defaultAccount ?? bankAccounts[0];

        this.form.controls.account.setValue(firstAccount ? this.accountOptionValue(firstAccount) : '');
      },
      error: () => {
        this.bankAccounts.set([]);
        this.loadingBankAccounts.set(false);
        this.form.controls.account.setValue('');
      },
    });
  }

  accountOptionValue(account: BankAccount): string {
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

  save(): void {
    this.error.set(null);

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const data = this.form.getRawValue();
    const id = this.transactionId();

    /** Round to two decimal places to avoid fractions of a cent. */
    const payload = {
      ...data,
      value: Math.round(Number(data.value) * 100) / 100,
    };

    this.saving.set(true);

    const request = id
      ? this.transactionService.update({ id, ...payload } as any)
      : this.transactionService.create(payload as any);

    request.subscribe({
      next: () => this.router.navigate(['/transactions']),
      error: () => {
        this.error.set('Não foi possível salvar. Tente novamente.');
        this.saving.set(false);
      },
    });
  }

  reset(): void {
    this.form.reset({
      type: 'ENTRADA',
      method: 'PIX',
      date: this.todayIso(),
    });
    this.error.set(null);
  }
}
