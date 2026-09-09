import { Injectable, inject } from '@angular/core';
import { Observable, of, throwError } from 'rxjs';
import { delay } from 'rxjs/operators';
import { AuthService } from '../../services/auth.service';
import { ContaBancaria } from './conta.model';

@Injectable({ providedIn: 'root' })
export class ContaService {
  private readonly chave = 'parceiro-auto:contas-bancarias:v1';
  private readonly latencia = 250;
  private readonly authService = inject(AuthService);

  private readonly sementes: ContaBancaria[] = [
    {
      id: 1,
      usuariosId: [1, 2],
      nome: 'Conta principal',
      banco: 'Banco do Brasil',
      agencia: '1234-5',
      numero: '98765-0',
      tipo: 'CORRENTE',
      saldo: 18500.75,
      ativa: true,
    },
    {
      id: 2,
      usuariosId: [1],
      nome: 'Reserva financeira',
      banco: 'Caixa Econômica Federal',
      agencia: '0420',
      numero: '12345-6',
      tipo: 'POUPANCA',
      saldo: 7200,
      ativa: true,
    },
    {
      id: 3,
      usuariosId: [1],
      nome: 'Caixa da oficina',
      banco: 'Caixa interno',
      agencia: '-',
      numero: '-',
      tipo: 'CAIXA',
      saldo: 950,
      ativa: true,
    },
  ];

  constructor() {
    if (localStorage.getItem(this.chave) === null) {
      this.gravar(this.sementes);
    }
  }

  private ler(): ContaBancaria[] {
    try {
      const bruto = localStorage.getItem(this.chave);
      return bruto ? (JSON.parse(bruto) as ContaBancaria[]) : [];
    } catch {
      this.gravar(this.sementes);
      return [...this.sementes];
    }
  }

  private gravar(contas: ContaBancaria[]): void {
    localStorage.setItem(this.chave, JSON.stringify(contas));
  }

  private usuarioId(): number | null {
    return this.authService.getUsuarioLogado()?.id ?? null;
  }

  private acessivel(conta: ContaBancaria): boolean {
    const usuarioId = this.usuarioId();
    return usuarioId !== null && (this.authService.temPapel('dono') || conta.usuariosId.includes(usuarioId));
  }

  private proximoId(contas: ContaBancaria[]): number {
    return contas.reduce((maior, conta) => Math.max(maior, conta.id), 0) + 1;
  }

  listar(): Observable<ContaBancaria[]> {
    return of(this.ler().filter((conta) => this.acessivel(conta))).pipe(delay(this.latencia));
  }

  buscarPorId(id: number): Observable<ContaBancaria> {
    const conta = this.ler().find((item) => item.id === id);

    if (!conta || !this.acessivel(conta)) {
      return throwError(() => new Error(`Conta ${id} não encontrada.`));
    }

    return of(conta).pipe(delay(this.latencia));
  }

  criar(dados: Omit<ContaBancaria, 'id' | 'usuariosId'>): Observable<ContaBancaria> {
    const usuarioId = this.usuarioId();
    if (usuarioId === null) {
      return throwError(() => new Error('Usuário não autenticado.'));
    }

    const contas = this.ler();
    const novaConta: ContaBancaria = { ...dados, id: this.proximoId(contas), usuariosId: [usuarioId] };
    contas.push(novaConta);
    this.gravar(contas);

    return of(novaConta).pipe(delay(this.latencia));
  }

  atualizar(conta: ContaBancaria): Observable<ContaBancaria> {
    const contas = this.ler();
    const indice = contas.findIndex((item) => item.id === conta.id);

    if (indice === -1 || !this.acessivel(contas[indice])) {
      return throwError(() => new Error(`Conta ${conta.id} não encontrada.`));
    }

    contas[indice] = { ...conta, usuariosId: contas[indice].usuariosId };
    this.gravar(contas);
    return of(conta).pipe(delay(this.latencia));
  }

  ajustarSaldo(contaId: number, variacao: number): boolean {
    const contas = this.ler();
    const indice = contas.findIndex((conta) => conta.id === contaId);

    if (indice === -1 || !this.acessivel(contas[indice])) {
      return false;
    }

    contas[indice] = {
      ...contas[indice],
      saldo: Math.round((contas[indice].saldo + variacao) * 100) / 100,
    };
    this.gravar(contas);
    return true;
  }

  excluir(id: number): Observable<void> {
    const contas = this.ler();
    const conta = contas.find((item) => item.id === id);

    if (!conta || !this.acessivel(conta)) {
      return throwError(() => new Error(`Conta ${id} não encontrada.`));
    }

    this.gravar(contas.filter((item) => item.id !== id));
    return of(void 0).pipe(delay(this.latencia));
  }
}
