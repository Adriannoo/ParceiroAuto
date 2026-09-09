import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { EmpresaService } from '../empresa.service';
import {
  Empresa,
  enderecoResumido,
  rotuloNatureza,
  rotuloPorte,
  rotuloRegime,
} from '../empresa.model';
import { MovimentacaoService } from '../../movimentacao/movimentacao.service';
import { Movimentacao, rotuloForma } from '../../movimentacao/movimentacao.model';
import { ContaBancaria } from '../../conta/conta.model';
import { ContaService } from '../../conta/conta.service';

@Component({
  selector: 'app-empresa-lancamentos',
  imports: [RouterLink],
  templateUrl: './empresa-lancamentos.html',
  styleUrl: './empresa-lancamentos.scss',
})
export class EmpresaLancamentos implements OnInit {
  private empresaService = inject(EmpresaService);
  private movimentacaoService = inject(MovimentacaoService);
  private rota = inject(ActivatedRoute);
  private contaService = inject(ContaService);

  empresa = signal<Empresa | null>(null);
  movimentacoes = signal<Movimentacao[]>([]);
  contas = signal<ContaBancaria[]>([]);
  carregando = signal(false);
  erro = signal<string | null>(null);

  ordenadas = computed(() =>
    [...this.movimentacoes()].sort((a, b) => b.data.localeCompare(a.data) || b.id - a.id),
  );

  entradas = computed(() =>
    this.movimentacoes()
      .filter((m) => m.tipo === 'ENTRADA')
      .reduce((soma, m) => soma + m.valor, 0),
  );

  saidas = computed(() =>
    this.movimentacoes()
      .filter((m) => m.tipo === 'SAIDA')
      .reduce((soma, m) => soma + m.valor, 0),
  );

  saldo = computed(() => this.entradas() - this.saidas());

  ngOnInit(): void {
    const id = Number(this.rota.snapshot.paramMap.get('id'));
    this.carregando.set(true);

    forkJoin({
      empresa: this.empresaService.buscarPorId(id),
      movimentacoes: this.movimentacaoService.listar(),
      contas: this.contaService.listar(),
    }).subscribe({
      next: ({ empresa, movimentacoes, contas }) => {
        this.empresa.set(empresa);
        this.contas.set(contas);
        this.movimentacoes.set(movimentacoes.filter((m) => m.empresaId === id));
        this.carregando.set(false);
      },
      error: () => {
        this.erro.set('Empresa não encontrada.');
        this.carregando.set(false);
      },
    });
  }

  endereco(e: Empresa): string {
    return enderecoResumido(e);
  }

  regime(e: Empresa): string {
    return rotuloRegime(e.regime);
  }

  porte(e: Empresa): string {
    return rotuloPorte(e.porte);
  }

  natureza(e: Empresa): string {
    return rotuloNatureza(e.naturezaJuridica);
  }

  forma(m: Movimentacao): string {
    return rotuloForma(m.forma);
  }

  conta(m: Movimentacao): string {
    return this.contas().find((conta) => conta.id === m.contaId)?.nome ?? 'Conta removida';
  }

  moeda(valor: number): string {
    return valor.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
  }

  data(iso: string): string {
    const [ano, mes, dia] = iso.split('-');
    return `${dia}/${mes}/${ano}`;
  }
}