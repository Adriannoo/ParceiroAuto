import { Component, effect, inject, untracked, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { CurrentCompanyService } from '../../../services/current-company.service';
import { BANK_ACCOUNT_TYPES, BankAccountRequest, BankAccountType } from '../bank-account.model';
import { BankAccountService } from '../bank-account.service';

@Component({
  selector: 'app-bank-account-form',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './bank-account-form.html',
  styleUrl: './bank-account-form.scss',
})
export class BankAccountForm {
  private fb = inject(FormBuilder);
  private bankAccountService = inject(BankAccountService);
  private currentCompanyService = inject(CurrentCompanyService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  bankAccountId = signal<number | null>(null);
  companyId = this.currentCompanyService.currentCompanyId;
  private loadedCompanyId: number | null = null;
  currentDefaultAccount = signal(false);

  accountTypes = BANK_ACCOUNT_TYPES;
  saving = signal(false);
  loading = signal(false);
  error = signal<string | null>(null);

  form = this.fb.group({
    bankName: ['', [Validators.required, Validators.maxLength(50)]],
    branch: ['', [Validators.required, Validators.minLength(4), Validators.maxLength(4), Validators.pattern(/^\d+$/)]],
    accountNumber: ['', [Validators.required, Validators.minLength(4), Validators.maxLength(13), Validators.pattern(/^\d+$/)]],
    accountType: ['CHECKING' as BankAccountType, [Validators.required]],
  });

  get editing(): boolean {
    return this.bankAccountId() !== null;
  }

  constructor() {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) this.bankAccountId.set(Number(id));
    effect((onCleanup) => {
      const companyId = this.companyId();
      untracked(() => {
        this.loadedCompanyId = null;
        this.loading.set(false);
        this.form.disable();
        if (!companyId) {
          this.error.set('Selecione uma empresa na sidebar ou cadastre uma empresa para continuar.');
          return;
        }
        this.error.set(null);
        const accountId = this.bankAccountId();
        if (accountId === null) {
          this.form.enable();
          return;
        }
        if (!Number.isSafeInteger(accountId) || accountId <= 0) {
          this.error.set('Conta bancaria invalida.');
          return;
        }
        this.loading.set(true);
        const subscription = this.bankAccountService.findById(companyId, accountId).subscribe({
          next: account => {
            if (this.companyId() !== companyId) return;
            this.loading.set(false);
            if (account.companyId !== companyId) {
              this.error.set('Esta conta nao pertence a empresa atual. Selecione a empresa correta na sidebar.');
              return;
            }
            this.loadedCompanyId = companyId;
            this.currentDefaultAccount.set(account.defaultAccount);
            this.form.patchValue(account);
            this.form.enable();
          },
          error: () => {
            this.loading.set(false);
            this.error.set('Conta nao encontrada na empresa atual. Selecione a empresa correta na sidebar.');
          },
        });
        onCleanup(() => subscription.unsubscribe());
      });
    });
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

    const companyId = this.companyId();
    if (!companyId) {
      this.error.set('Selecione uma empresa na sidebar antes de salvar.');
      return;
    }
    if (this.saving() || this.loading() || this.form.disabled || (this.editing && this.loadedCompanyId !== companyId)) {
      this.error.set('Aguarde o carregamento de uma conta pertencente a empresa atual.');
      return;
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.error.set('Existem campos obrigatorios pendentes. Revise os campos destacados.');
      return;
    }

    const data = {
      ...this.form.getRawValue(),
      defaultAccount: this.editing ? this.currentDefaultAccount() : false,
    } as BankAccountRequest;
    const id = this.bankAccountId();

    this.saving.set(true);

    const request = id
      ? this.bankAccountService.update(companyId, id, data)
      : this.bankAccountService.create(companyId, data);

    request.subscribe({
      next: () => this.router.navigate(['/bank-accounts']),
      error: () => {
        this.error.set('Nao foi possivel salvar. Tente novamente.');
        this.saving.set(false);
      },
    });
  }

  reset(): void {
    this.form.reset({
      accountType: 'CHECKING',
    });
    this.error.set(null);
  }
}
