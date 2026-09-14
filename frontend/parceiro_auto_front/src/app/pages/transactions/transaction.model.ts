export type TransactionType = 'ENTRADA' | 'SAIDA';

export type PaymentMethod =
  | 'PIX'
  | 'DINHEIRO'
  | 'CARTAO_CREDITO'
  | 'CARTAO_DEBITO'
  | 'BOLETO'
  | 'TRANSFERENCIA';

export interface Transaction {
  id: number;
  companyId: number;
  account: string;
  category: string;
  type: TransactionType;
  description: string;
  value: number;
  date: string;
  method: PaymentMethod;
}

export const TRANSACTION_TYPES: { value: TransactionType; label: string }[] = [
  { value: 'ENTRADA', label: 'Entrada' },
  { value: 'SAIDA', label: 'Saída' },
];

export const PAYMENT_METHODS: { value: PaymentMethod; label: string }[] = [
  { value: 'PIX', label: 'PIX' },
  { value: 'DINHEIRO', label: 'Dinheiro' },
  { value: 'CARTAO_CREDITO', label: 'Cartão de crédito' },
  { value: 'CARTAO_DEBITO', label: 'Cartão de débito' },
  { value: 'BOLETO', label: 'Boleto' },
  { value: 'TRANSFERENCIA', label: 'Transferência' },
];

export const ACCOUNTS: string[] = [
  'Conta Corrente',
  'Conta Poupança',
  'Caixa',
  'Aplicação',
];

export const CATEGORIES: string[] = [
  'Vendas',
  'Serviços',
  'Peças',
  'Fornecedores',
  'Salários',
  'Impostos',
  'Aluguel',
  'Energia',
  'Manutenção',
  'Outros',
];

export function paymentMethodLabel(method: PaymentMethod): string {
  return PAYMENT_METHODS.find((f) => f.value === method)?.label ?? method;
}