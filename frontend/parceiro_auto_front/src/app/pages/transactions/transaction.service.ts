import { Injectable } from '@angular/core';
import { Observable, of, throwError } from 'rxjs';
import { delay } from 'rxjs/operators';
import { Transaction } from './transaction.model';

@Injectable({ providedIn: 'root' })
export class TransactionService {
  private readonly STORAGE_KEY = 'parceiro-auto:transactions';
  private readonly LATENCY = 300;

  private readonly SEED: Transaction[] = [
    {
      id: 1,
      companyId: 1,
      account: 'Conta Corrente',
      category: 'Vendas',
      type: 'ENTRADA',
      description: 'Venda de peças ao cliente Souza',
      value: 4850,
      date: '2026-08-03',
      method: 'PIX',
    },
    {
      id: 2,
      companyId: 1,
      account: 'Conta Corrente',
      category: 'Fornecedores',
      type: 'SAIDA',
      description: 'Compra de estoque - distribuidora Bosch',
      value: 2310.5,
      date: '2026-08-05',
      method: 'BOLETO',
    },
    {
      id: 3,
      companyId: 1,
      account: 'Conta Corrente',
      category: 'Salários',
      type: 'SAIDA',
      description: 'Folha de pagamento de julho',
      value: 8200,
      date: '2026-08-05',
      method: 'TRANSFERENCIA',
    },
    {
      id: 4,
      companyId: 2,
      account: 'Caixa',
      category: 'Serviços',
      type: 'ENTRADA',
      description: 'Revisão completa - frota Martins',
      value: 3120,
      date: '2026-08-08',
      method: 'CARTAO_CREDITO',
    },
    {
      id: 5,
      companyId: 1,
      account: 'Conta Corrente',
      category: 'Aluguel',
      type: 'SAIDA',
      description: 'Aluguel do galpão',
      value: 4500,
      date: '2026-08-10',
      method: 'PIX',
    },
    {
      id: 6,
      companyId: 2,
      account: 'Conta Corrente',
      category: 'Vendas',
      type: 'ENTRADA',
      description: 'Venda balcão - lote de filtros',
      value: 1980.75,
      date: '2026-08-12',
      method: 'DINHEIRO',
    },
  ];

  constructor() {
    if (localStorage.getItem(this.STORAGE_KEY) === null) {
      this.write(this.SEED);
    }
  }

  private read(): Transaction[] {
    try {
      const raw = localStorage.getItem(this.STORAGE_KEY);
      return raw ? (JSON.parse(raw) as Transaction[]) : [];
    } catch {
      this.write(this.SEED);
      return [...this.SEED];
    }
  }

  private write(transactions: Transaction[]): void {
    localStorage.setItem(this.STORAGE_KEY, JSON.stringify(transactions));
  }

  private generateId(transactions: Transaction[]): number {
    return transactions.reduce((largest, m) => Math.max(largest, m.id), 0) + 1;
  }

  list(): Observable<Transaction[]> {
    return of(this.read()).pipe(delay(this.LATENCY));
  }

  findById(id: number): Observable<Transaction> {
    const transaction = this.read().find((m) => m.id === id);

    if (!transaction) {
      return throwError(() => new Error(`Movimentação ${id} não encontrada.`));
    }

    return of(transaction).pipe(delay(this.LATENCY));
  }

  create(data: Omit<Transaction, 'id'>): Observable<Transaction> {
    const transactions = this.read();
    const created: Transaction = { ...data, id: this.generateId(transactions) };

    transactions.push(created);
    this.write(transactions);

    return of(created).pipe(delay(this.LATENCY));
  }

  update(transaction: Transaction): Observable<Transaction> {
    const transactions = this.read();
    const index = transactions.findIndex((m) => m.id === transaction.id);

    if (index === -1) {
      return throwError(() => new Error(`Movimentação ${transaction.id} não encontrada.`));
    }

    transactions[index] = { ...transaction };
    this.write(transactions);

    return of(transaction).pipe(delay(this.LATENCY));
  }

  delete(id: number): Observable<void> {
    this.write(this.read().filter((m) => m.id !== id));

    return of(void 0).pipe(delay(this.LATENCY));
  }

  /** Checks for transactions linked to a company before deletion. */
  hasTransactions(companyId: number): boolean {
    return this.read().some((m) => m.companyId === companyId);
  }

  restoreExamples(): void {
    this.write(this.SEED);
  }
}