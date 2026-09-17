export interface BankAccount {
  id: number;
  bankName: string;
  branch: string;
  accountNumber: string;
  accountType: BankAccountType;
  balance: number;
  defaultAccount: boolean;
  companyId: number;
}

export type BankAccountType = 'CHECKING' | 'SAVINGS' | 'CASH' | 'INVESTMENT';

export interface BankAccountRequest {
  companyId: number;
  bankName: string;
  branch: string;
  accountNumber: string;
  accountType: BankAccountType;
  defaultAccount: boolean;
}

export const BANK_ACCOUNT_TYPES = [
  { value: 'CHECKING', label: 'Conta Corrente' },
  { value: 'SAVINGS', label: 'Conta Poupanca' },
  { value: 'CASH', label: 'Caixa' },
  { value: 'INVESTMENT', label: 'Investimento' },
];

