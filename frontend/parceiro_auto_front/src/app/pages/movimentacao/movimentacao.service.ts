import { Injectable, inject } from '@angular/core';
import { Observable, of, throwError } from 'rxjs';
import { delay } from 'rxjs/operators';
import { Movimentacao } from './movimentacao.model';
import { ContaService } from '../conta/conta.service';
import { AuthService } from '../../services/auth.service';
import { EmpresaService } from '../empresa/empresa.service';

@Injectable({ providedIn: 'root' })
export class MovimentacaoService {
  private readonly CHAVE = 'parceiro-auto:movimentacoes:v2';
  private readonly CHAVE_LEGADA = 'parceiro-auto:movimentacoes';
  private readonly LATENCIA = 300;
  private readonly contaService = inject(ContaService);
  private readonly authService = inject(AuthService);
  private readonly empresaService = inject(EmpresaService);

  private readonly SEMENTE: Movimentacao[] = [
    {
      id: 1,
      empresaId: 1,
      contaId: 1,
      categoria: 'Vendas',
      tipo: 'ENTRADA',
      descricao: 'Venda de peças ao cliente Souza',
      valor: 4850,
      data: '2026-08-03',
      forma: 'PIX',
    },
    {
      id: 2,
      empresaId: 1,
      contaId: 1,
      categoria: 'Fornecedores',
      tipo: 'SAIDA',
      descricao: 'Compra de estoque - distribuidora Bosch',
      valor: 2310.5,
      data: '2026-08-05',
      forma: 'BOLETO',
    },
    {
      id: 3,
      empresaId: 1,
      contaId: 1,
      categoria: 'Salários',
      tipo: 'SAIDA',
      descricao: 'Folha de pagamento de julho',
      valor: 8200,
      data: '2026-08-05',
      forma: 'TRANSFERENCIA',
    },
    {
      id: 4,
      empresaId: 2,
      contaId: 3,
      categoria: 'Serviços',
      tipo: 'ENTRADA',
      descricao: 'Revisão completa - frota Martins',
      valor: 3120,
      data: '2026-08-08',
      forma: 'CARTAO_CREDITO',
    },
    {
      id: 5,
      empresaId: 1,
      contaId: 1,
      categoria: 'Aluguel',
      tipo: 'SAIDA',
      descricao: 'Aluguel do galpão',
      valor: 4500,
      data: '2026-08-10',
      forma: 'PIX',
    },
    {
      id: 6,
      empresaId: 2,
      contaId: 1,
      categoria: 'Vendas',
      tipo: 'ENTRADA',
      descricao: 'Venda balcão - lote de filtros',
      valor: 1980.75,
      data: '2026-08-12',
      forma: 'DINHEIRO',
    },
  ];

  constructor() {
    if (localStorage.getItem(this.CHAVE) === null) {
      this.migrarOuInicializar();
    }
  }

  private migrarOuInicializar(): void {
    const legado = localStorage.getItem(this.CHAVE_LEGADA);

    if (legado) {
      try {
        const movimentacoesLegadas = JSON.parse(legado) as Array<Omit<Movimentacao, 'contaId'> & { conta: string }>;
        const mapaContas: Record<string, number> = {
          'Conta Corrente': 1,
          'Conta Poupança': 2,
          Caixa: 3,
          Aplicação: 2,
        };
        const migradas = movimentacoesLegadas.map(({ conta, ...movimentacao }) => ({
          ...movimentacao,
          contaId: mapaContas[conta] ?? 1,
        }));
        this.gravar(migradas);
        return;
      } catch {
        // Inicializa com os exemplos quando o armazenamento legado estiver inválido.
      }
    }

    this.gravar(this.SEMENTE);
  }

  private ler(): Movimentacao[] {
    try {
      const bruto = localStorage.getItem(this.CHAVE);
      return bruto ? (JSON.parse(bruto) as Movimentacao[]) : [];
    } catch {
      this.gravar(this.SEMENTE);
      return [...this.SEMENTE];
    }
  }

  private gravar(movimentacoes: Movimentacao[]): void {
    localStorage.setItem(this.CHAVE, JSON.stringify(movimentacoes));
  }

  private gerarId(movimentacoes: Movimentacao[]): number {
    return movimentacoes.reduce((maior, m) => Math.max(maior, m.id), 0) + 1;
  }

  listar(): Observable<Movimentacao[]> {
    return of(this.ler().filter((movimentacao) => this.empresaService.temAcessoEmpresa(movimentacao.empresaId))).pipe(delay(this.LATENCIA));
  }

  buscarPorId(id: number): Observable<Movimentacao> {
    const movimentacao = this.ler().find((m) => m.id === id);

    if (!movimentacao || !this.empresaService.temAcessoEmpresa(movimentacao.empresaId)) {
      return throwError(() => new Error(`Movimentação ${id} não encontrada.`));
    }

    return of(movimentacao).pipe(delay(this.LATENCIA));
  }

  criar(dados: Omit<Movimentacao, 'id'>): Observable<Movimentacao> {
    if (!this.authService.podeGerenciarLancamentos()) {
      return throwError(() => new Error('Seu perfil não pode alterar lançamentos.'));
    }
    if (!this.empresaService.temAcessoEmpresa(dados.empresaId)) {
      return throwError(() => new Error('Acesso negado à empresa do lançamento.'));
    }
    const impacto = this.impacto(dados);
    if (!this.contaService.ajustarSaldo(dados.contaId, impacto)) {
      return throwError(() => new Error(`Conta ${dados.contaId} não encontrada.`));
    }

    const movimentacoes = this.ler();
    const nova: Movimentacao = { ...dados, id: this.gerarId(movimentacoes) };

    movimentacoes.push(nova);
    this.gravar(movimentacoes);

    return of(nova).pipe(delay(this.LATENCIA));
  }

  atualizar(movimentacao: Movimentacao): Observable<Movimentacao> {
    if (!this.authService.podeGerenciarLancamentos()) {
      return throwError(() => new Error('Seu perfil não pode alterar lançamentos.'));
    }
    if (!this.empresaService.temAcessoEmpresa(movimentacao.empresaId)) {
      return throwError(() => new Error('Acesso negado à empresa do lançamento.'));
    }
    const movimentacoes = this.ler();
    const indice = movimentacoes.findIndex((m) => m.id === movimentacao.id);

    if (indice === -1) {
      return throwError(() => new Error(`Movimentação ${movimentacao.id} não encontrada.`));
    }

    const anterior = movimentacoes[indice];
    const ajusteAnterior = this.contaService.ajustarSaldo(anterior.contaId, -this.impacto(anterior));
    const ajusteAtual = this.contaService.ajustarSaldo(movimentacao.contaId, this.impacto(movimentacao));

    if (!ajusteAnterior || !ajusteAtual) {
      if (ajusteAnterior) {
        this.contaService.ajustarSaldo(anterior.contaId, this.impacto(anterior));
      }
      if (ajusteAtual) {
        this.contaService.ajustarSaldo(movimentacao.contaId, -this.impacto(movimentacao));
      }
      return throwError(() => new Error('Não foi possível atualizar o saldo da conta.'));
    }

    movimentacoes[indice] = { ...movimentacao };
    this.gravar(movimentacoes);

    return of(movimentacao).pipe(delay(this.LATENCIA));
  }

  excluir(id: number): Observable<void> {
    if (!this.authService.podeGerenciarLancamentos()) {
      return throwError(() => new Error('Seu perfil não pode alterar lançamentos.'));
    }
    const movimentacoes = this.ler();
    const movimentacao = movimentacoes.find((item) => item.id === id);

    if (!movimentacao || !this.empresaService.temAcessoEmpresa(movimentacao.empresaId)) {
      return throwError(() => new Error(`Movimentação ${id} não encontrada.`));
    }
    if (!this.contaService.ajustarSaldo(movimentacao.contaId, -this.impacto(movimentacao))) {
      return throwError(() => new Error(`Conta ${movimentacao.contaId} não encontrada.`));
    }

    this.gravar(movimentacoes.filter((m) => m.id !== id));

    return of(void 0).pipe(delay(this.LATENCIA));
  }

  /** Usado antes de excluir uma empresa, para não deixar movimentação órfã. */
  possuiMovimentacoes(empresaId: number): boolean {
    return this.ler().some((m) => m.empresaId === empresaId);
  }

  restaurarExemplos(): void {
    this.gravar(this.SEMENTE);
  }

  private impacto(movimentacao: Pick<Movimentacao, 'tipo' | 'valor'>): number {
    const valor = Math.round(movimentacao.valor * 100) / 100;
    return movimentacao.tipo === 'ENTRADA' ? valor : -valor;
  }
}