export type TransactionType = 'ENTRADA' | 'SAIDA';

export type PaymentMethod = 'PIX' | 'CARTAO' | 'DINHEIRO';

export type RecurrenceFrequency = 'DAILY' | 'WEEKLY' | 'MONTHLY' | 'YEARLY';

export interface Transaction {
  id: number;
  companyId: number;
  bankAccountId: number;
  transactionCategoryId: number;
  type: TransactionType;
  description: string;
  value: number;
  date: string;
  method: PaymentMethod;
  recurrenceFrequency?: RecurrenceFrequency | null;
  recurrenceEndDate?: string | null;
}

export interface TransactionRequest {
  companyId: number;
  bankAccountId: number;
  transactionCategoryId: number;
  type: TransactionType;
  description: string;
  value: number;
  date: string;
  method: PaymentMethod;
  recurrenceFrequency?: RecurrenceFrequency | null;
  recurrenceEndDate?: string | null;
}

export interface TransactionCategory {
  id: number;
  companyId: number;
  name: string;
  type: TransactionType;
  active: boolean;
}

export interface TransactionCategoryRequest {
  name: string;
  type: TransactionType;
}

export interface RecurringTransaction {
  recurrenceRuleId: number;
  transactionId: number;
  companyId: number;
  description: string;
  value: number;
  type: TransactionType;
  frequency: RecurrenceFrequency;
  nextDate: string;
  endDate: string | null;
}

export interface RecurrenceRuleRequest {
  frequency: RecurrenceFrequency;
  endDate: string | null;
}

export const TRANSACTION_TYPES: { value: TransactionType; label: string }[] = [
  { value: 'ENTRADA', label: 'Entrada' },
  { value: 'SAIDA', label: 'Saída' },
];

export const PAYMENT_METHODS: { value: PaymentMethod; label: string }[] = [
  { value: 'PIX', label: 'PIX' },
  { value: 'CARTAO', label: 'Cartão' },
  { value: 'DINHEIRO', label: 'Dinheiro' },
];

export const RECURRENCE_FREQUENCIES: { value: RecurrenceFrequency; label: string }[] = [
  { value: 'DAILY', label: 'Diária' },
  { value: 'WEEKLY', label: 'Semanal' },
  { value: 'MONTHLY', label: 'Mensal' },
  { value: 'YEARLY', label: 'Anual' },
];

export function paymentMethodLabel(method: PaymentMethod): string {
  return PAYMENT_METHODS.find((f) => f.value === method)?.label ?? method;
}
