import { CurrencyPipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
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
  selectedCompanyId = signal<number | null>(null);
  bankAccounts = signal<BankAccount[]>([]);
  loading = signal(false);
  loadingCompanies = signal(false);

  searchTerm = signal('');
  accountType = signal<AccountFilter>('all');

  bankAccountToDelete = signal<BankAccount | null>(null);

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

  ngOnInit(): void {
    this.loadCompanies();
  }

  loadCompanies(): void {
    this.loadingCompanies.set(true);

    this.companyService.list().subscribe({
      next: (companies) => {
        this.companies.set(companies);
        this.loadingCompanies.set(false);

        const firstCompanyId = companies[0]?.id ?? null;
        this.selectedCompanyId.set(firstCompanyId);

        if (firstCompanyId) {
          this.loadBankAccounts();
        }
      },
      error: () => {
        this.loadingCompanies.set(false);
      },
    });
  }

  onCompanyChange(companyId: number | string): void {
    const selectedId = Number(companyId);
    this.selectedCompanyId.set(Number.isNaN(selectedId) ? null : selectedId);
    this.loadBankAccounts();
  }

  loadBankAccounts(): void {
    const companyId = this.selectedCompanyId();

    if (!companyId) {
      this.bankAccounts.set([]);
      return;
    }

    this.loading.set(true);

    this.bankAccountService.listByCompany(companyId).subscribe({
      next: (bankAccounts) => {
        this.bankAccounts.set(bankAccounts);
        this.loading.set(false);
      },
      error: () => {
        this.bankAccounts.set([]);
        this.loading.set(false);
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

    if (!bankAccount) {
      return;
    }

    this.bankAccountService.delete(bankAccount.id, bankAccount.companyId).subscribe({
      next: () => {
        this.closeConfirmation();
        this.loadBankAccounts();
      },
    });
  }

  setDefault(bankAccount: BankAccount): void {
    if (bankAccount.defaultAccount) {
      return;
    }

    this.bankAccountService.setDefault(bankAccount.id, bankAccount.companyId).subscribe({
      next: () => this.loadBankAccounts(),
    });
  }
}
