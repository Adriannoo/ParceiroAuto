import { Component, OnInit, inject, signal } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ContaBancaria } from '../conta.model';
import { ContaService } from '../conta.service';
import { Movimentacao } from '../../movimentacao/movimentacao.model';
import { MovimentacaoService } from '../../movimentacao/movimentacao.service';

@Component({
  selector: 'app-conta-lancamentos',
  imports: [CurrencyPipe, DatePipe, RouterLink],
  templateUrl: './conta-lancamentos.html',
  styleUrl: './conta-lancamentos.scss',
})
export class ContaLancamentos implements OnInit {
  private readonly rota = inject(ActivatedRoute);
  private readonly contaService = inject(ContaService);
  private readonly movimentacaoService = inject(MovimentacaoService);

  conta = signal<ContaBancaria | null>(null);
  lancamentos = signal<Movimentacao[]>([]);
  carregando = signal(true);

  ngOnInit(): void {
    const id = this.rota.snapshot.paramMap.get('id');

    if (id) {
      this.contaService.buscarPorId(Number(id)).subscribe({
        next: (conta) => {
          this.conta.set(conta);
          this.movimentacaoService.listar().subscribe({
            next: (lancamentos) => {
              this.lancamentos.set(lancamentos.filter((item) => item.contaId === conta.id));
              this.carregando.set(false);
            },
            error: () => this.carregando.set(false),
          });
        },
        error: () => this.carregando.set(false),
      });
    }
  }
  totalEntradas(): number {
    return this.lancamentos().filter((item) => item.tipo === 'ENTRADA').reduce((total, item) => total + item.valor, 0);
  }

  totalSaidas(): number {
    return this.lancamentos().filter((item) => item.tipo === 'SAIDA').reduce((total, item) => total + item.valor, 0);
  }
}