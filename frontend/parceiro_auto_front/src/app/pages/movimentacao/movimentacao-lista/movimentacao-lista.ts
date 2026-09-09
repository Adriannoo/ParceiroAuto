import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { MovimentacaoService } from '../movimentacao.service';
import { Movimentacao, rotuloForma } from '../movimentacao.model';
import { EmpresaService } from '../../empresa/empresa.service';
import { Empresa } from '../../empresa/empresa.model';
import { ContaBancaria } from '../../conta/conta.model';
import { ContaService } from '../../conta/conta.service';

type FiltroTipo = 'todas' | 'ENTRADA' | 'SAIDA';

@Component({
  selector: 'app-movimentacao-lista',
  imports: [FormsModule, RouterLink],
  templateUrl: './movimentacao-lista.html',
  styleUrl: './movimentacao-lista.scss',
})
export class MovimentacaoLista implements OnInit {
  private movimentacaoService = inject(MovimentacaoService);
  private empresaService = inject(EmpresaService);
  private contaService = inject(ContaService);

  movimentacoes = signal<Movimentacao[]>([]);
  empresas = signal<Empresa[]>([]);
  contas = signal<ContaBancaria[]>([]);
  carregando = signal(false);

  termoBusca = signal('');
  tipo = signal<FiltroTipo>('todas');
  empresaId = signal<number | 'todas'>('todas');

  movimentacaoParaExcluir = signal<Movimentacao | null>(null);

  filtradas = computed(() => {
    const termo = this.termoBusca().trim().toLowerCase();
    const filtroTipo = this.tipo();
    const filtroEmpresa = this.empresaId();

    return this.movimentacoes()
      .filter((m) => (filtroTipo === 'todas' ? true : m.tipo === filtroTipo))
      .filter((m) => (filtroEmpresa === 'todas' ? true : m.empresaId === filtroEmpresa))
      .filter(
        (m) =>
          !termo ||
          m.descricao.toLowerCase().includes(termo) ||
          m.categoria.toLowerCase().includes(termo) ||
          this.nomeConta(m.contaId).toLowerCase().includes(termo),
      )
      .sort((a, b) => b.data.localeCompare(a.data) || b.id - a.id);
  });

  totalEntradas = computed(() =>
    this.filtradas()
      .filter((m) => m.tipo === 'ENTRADA')
      .reduce((soma, m) => soma + m.valor, 0),
  );

  totalSaidas = computed(() =>
    this.filtradas()
      .filter((m) => m.tipo === 'SAIDA')
      .reduce((soma, m) => soma + m.valor, 0),
  );

  saldo = computed(() => this.totalEntradas() - this.totalSaidas());

  ngOnInit(): void {
    this.carregar();
  }

  carregar(): void {
    this.carregando.set(true);

    forkJoin({
      movimentacoes: this.movimentacaoService.listar(),
      empresas: this.empresaService.listar(),
      contas: this.contaService.listar(),
    }).subscribe({
      next: ({ movimentacoes, empresas, contas }) => {
        this.movimentacoes.set(movimentacoes);
        this.empresas.set(empresas);
        this.contas.set(contas);
        this.carregando.set(false);
      },
      error: () => this.carregando.set(false),
    });
  }

  nomeEmpresa(id: number): string {
    return this.empresas().find((e) => e.id === id)?.nomeFantasia ?? 'Empresa removida';
  }

  nomeConta(id: number): string {
    return this.contas().find((conta) => conta.id === id)?.nome ?? 'Conta removida';
  }

  formaLegivel(movimentacao: Movimentacao): string {
    return rotuloForma(movimentacao.forma);
  }

  moeda(valor: number): string {
    return valor.toLocaleString('pt-BR', {
      style: 'currency',
      currency: 'BRL',
    });
  }

  dataLegivel(iso: string): string {
    const [ano, mes, dia] = iso.split('-');
    return `${dia}/${mes}/${ano}`;
  }

  limparFiltros(): void {
    this.termoBusca.set('');
    this.tipo.set('todas');
    this.empresaId.set('todas');
  }

  temFiltro(): boolean {
    return this.termoBusca() !== '' || this.tipo() !== 'todas' || this.empresaId() !== 'todas';
  }

  abrirConfirmacao(movimentacao: Movimentacao): void {
    this.movimentacaoParaExcluir.set(movimentacao);
  }

  fecharConfirmacao(): void {
    this.movimentacaoParaExcluir.set(null);
  }

  confirmarExclusao(): void {
    const movimentacao = this.movimentacaoParaExcluir();

    if (!movimentacao) {
      return;
    }

    this.movimentacaoService.excluir(movimentacao.id).subscribe(() => {
      this.fecharConfirmacao();
      this.carregar();
    });
  }
}