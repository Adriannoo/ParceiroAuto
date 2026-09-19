import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { CompanyService } from '../../companies/company.service';
import { Company } from '../../companies/company.model';
import { BANK_ACCOUNT_TYPES, BankAccountRequest, BankAccountType } from '../bank-account.model';
import { BankAccountService } from '../bank-account.service';

@Component({
  selector: 'app-bank-account-form',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './bank-account-form.html',
  styleUrl: './bank-account-form.scss',
})
export class BankAccountForm implements OnInit {
  private fb = inject(FormBuilder);
  private bankAccountService = inject(BankAccountService);
  private companyService = inject(CompanyService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  bankAccountId = signal<number | null>(null);
  companies = signal<Company[]>([]);
  currentDefaultAccount = signal(false);

  accountTypes = BANK_ACCOUNT_TYPES;
  saving = signal(false);
  loadingCompanies = signal(false);
  error = signal<string | null>(null);

  form = this.fb.group({
    companyId: [null as number | null, [Validators.required, Validators.min(1)]],
    bankName: ['', [Validators.required, Validators.maxLength(50)]],
    branch: ['', [Validators.required, Validators.minLength(4), Validators.maxLength(4), Validators.pattern(/^\d+$/)]],
    accountNumber: ['', [Validators.required, Validators.minLength(4), Validators.maxLength(13), Validators.pattern(/^\d+$/)]],
    accountType: ['CHECKING' as BankAccountType, [Validators.required]],
  });

  get editing(): boolean {
    return this.bankAccountId() !== null;
  }

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');

    if (id) {
      this.bankAccountId.set(Number(id));
    }

    this.loadCompanies();
  }
  private loadCompanies(): void {
    this.loadingCompanies.set(true);

    this.companyService.list().subscribe({
      next: (companies) => {
        this.companies.set(companies);
        this.loadingCompanies.set(false);

        const companyIdFromRoute = Number(this.route.snapshot.queryParamMap.get('companyId'));
        const initialCompanyId = Number.isNaN(companyIdFromRoute) ? companies[0]?.id : companyIdFromRoute;

        if (!this.form.controls.companyId.value && initialCompanyId) {
          this.form.controls.companyId.setValue(initialCompanyId);
        }

        const id = this.bankAccountId();
        if (id) {
          this.load(id);
        }
      },
      error: () => {
        this.error.set('Nao foi possivel carregar as empresas.');
        this.loadingCompanies.set(false);
      },
    });
  }

  private load(id: number): void {
    const companyId = this.form.controls.companyId.value;

    if (!companyId) {
      this.error.set('Selecione uma empresa antes de carregar a conta.');
      return;
    }

    this.bankAccountService.findById(companyId, id).subscribe({
      next: (bankAccount) => {
        this.currentDefaultAccount.set(bankAccount.defaultAccount);
        this.form.patchValue(bankAccount);
      },
      error: () => {
        this.error.set('Conta bancaria nao encontrada.');
        this.form.disable();
      },
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
      ? this.bankAccountService.update(id, data)
      : this.bankAccountService.create(data);

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
      companyId: this.companies()[0]?.id ?? null,
      accountType: 'CHECKING',
    });
    this.error.set(null);
  }
}
