import { CurrencyPipe } from '@angular/common';
import { Component, OnInit, computed, effect, untracked, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';
import { CurrentCompanyService } from '../../../services/current-company.service';
import { Company } from '../../companies/company.model';
import { CompanyService } from '../../companies/company.service';
import { BankAccount, BANK_ACCOUNT_TYPES, BankAccountType } from '../bank-account.model';
import { BankAccountService } from '../bank-account.service';

type AccountFilter = BankAccountType | 'all';

@Component({
  selector: 'app-bank-account-list',
  imports: [CurrencyPipe, FormsModule, RouterLink],
  templateUrl: './bank-account-list.html',
  styleUrl: './bank-account-list.scss',
})
export class BankAccountList implements OnInit {
  accountTypes = BANK_ACCOUNT_TYPES;
  private bankAccountService = inject(BankAccountService);
  private companyService = inject(CompanyService);

  companies = signal<Company[]>([]);
  private currentCompanyService = inject(CurrentCompanyService);
  selectedCompanyId = this.currentCompanyService.currentCompanyId;
  private accountsSubscription?: Subscription;
  bankAccounts = signal<BankAccount[]>([]);
  loading = signal(false);
  error = signal<string | null>(null);
  loadingCompanies = signal(false);

  searchTerm = signal('');
  accountType = signal<AccountFilter>('all');

  bankAccountToDelete = signal<BankAccount | null>(null);
  settingDefaultId = signal<number | null>(null);

  filtered = computed(() => {
    const term = this.searchTerm().trim().toLowerCase();
    const accountTypeFilter = this.accountType();

    return this.bankAccounts()
      .filter((account) => accountTypeFilter === 'all' || account.accountType === accountTypeFilter)
      .filter(
        (account) =>
          !term ||
          account.bankName.toLowerCase().includes(term) ||
          account.branch.includes(term) ||
          account.accountNumber.includes(term),
      );
  });

  totalDefault = computed(() => this.bankAccounts().filter((account) => account.defaultAccount).length);

  constructor() {
    effect(onCleanup => {
      this.selectedCompanyId();
      untracked(() => {
        this.closeConfirmation();
        this.settingDefaultId.set(null);
        this.loadBankAccounts();
      });
      onCleanup(() => this.accountsSubscription?.unsubscribe());
    });
  }

  ngOnInit(): void {
    this.loadCompanies();
  }

  loadCompanies(): void {
    this.loadingCompanies.set(true);

    this.companyService.list().subscribe({
      next: (companies) => {
        this.companies.set(companies);
        this.loadingCompanies.set(false);

      },
      error: () => {
        this.loadingCompanies.set(false);
      },
    });
  }

  loadBankAccounts(): void {
    this.error.set(null);
    this.accountsSubscription?.unsubscribe();
    this.bankAccounts.set([]);
    this.loading.set(false);
    const companyId = this.selectedCompanyId();

    if (!companyId) {
      this.bankAccounts.set([]);
      return;
    }

    this.loading.set(true);

    this.accountsSubscription = this.bankAccountService.listByCompany(companyId).subscribe({
      next: (bankAccounts) => {
        if (this.selectedCompanyId() !== companyId) return;
        this.bankAccounts.set(bankAccounts);
        this.loading.set(false);
      },
      error: () => {
        this.bankAccounts.set([]);
        this.loading.set(false);
        this.error.set('Nao foi possivel carregar as contas da empresa atual.');
      },
    });
  }

  accountTypeLabel(account: BankAccount): string {
    return this.accountTypes.find((type) => type.value === account.accountType)?.label ?? account.accountType;
  }

  companyName(account: BankAccount): string {
    const company = this.companies().find((item) => item.id === account.companyId);

    return company?.tradeName || company?.legalName || `Empresa #${account.companyId}`;
  }

  hasFilters(): boolean {
    return this.searchTerm() !== '' || this.accountType() !== 'all';
  }

  clearFilters(): void {
    this.searchTerm.set('');
    this.accountType.set('all');
  }

  openConfirmation(bankAccount: BankAccount): void {
    this.bankAccountToDelete.set(bankAccount);
  }

  closeConfirmation(): void {
    this.bankAccountToDelete.set(null);
  }

  confirmDeletion(): void {
    const bankAccount = this.bankAccountToDelete();

    if (!bankAccount || bankAccount.companyId !== this.selectedCompanyId()) {
      return;
    }

    this.bankAccountService.delete(bankAccount.id, bankAccount.companyId).subscribe({
      next: () => {
        this.closeConfirmation();
        this.loadBankAccounts();
      },
      error: () => this.error.set('Nao foi possivel excluir a conta bancaria. Verifique os vinculos da conta.'),
    });
  }

  setDefault(bankAccount: BankAccount): void {
    if (this.settingDefaultId() || bankAccount.defaultAccount || bankAccount.companyId !== this.selectedCompanyId()) {
      return;
    }

    this.settingDefaultId.set(bankAccount.id);

    this.bankAccountService.setDefault(bankAccount.id, bankAccount.companyId).subscribe({
      next: (updatedBankAccount) => {
        if (this.selectedCompanyId() !== bankAccount.companyId) return;
        this.bankAccounts.update((accounts) =>
          accounts.map((account) => ({
            ...account,
            defaultAccount: account.id === updatedBankAccount.id,
          })),
        );
        this.settingDefaultId.set(null);
      },
      error: () => {
        this.settingDefaultId.set(null);
        if (this.selectedCompanyId() === bankAccount.companyId) this.error.set('Nao foi possivel definir a conta padrao.');
      },
    });
  }
}
