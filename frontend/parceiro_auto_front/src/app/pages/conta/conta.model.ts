export type TipoConta = 'CORRENTE' | 'POUPANCA' | 'PAGAMENTO' | 'CAIXA';

export interface ContaBancaria {
  id: number;
  usuariosId: number[];
  nome: string;
  banco: string;
  agencia: string;
  numero: string;
  tipo: TipoConta;
  saldo: number;
  ativa: boolean;
}

export const TIPOS_CONTA: { valor: TipoConta; rotulo: string }[] = [
  { valor: 'CORRENTE', rotulo: 'Conta corrente' },
  { valor: 'POUPANCA', rotulo: 'Conta poupança' },
  { valor: 'PAGAMENTO', rotulo: 'Conta de pagamento' },
  { valor: 'CAIXA', rotulo: 'Caixa interno' },
];

export function rotuloTipoConta(tipo: TipoConta): string {
  return TIPOS_CONTA.find((opcao) => opcao.valor === tipo)?.rotulo ?? tipo;
}
