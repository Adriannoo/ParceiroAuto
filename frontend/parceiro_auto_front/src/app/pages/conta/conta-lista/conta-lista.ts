import { CurrencyPipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ContaBancaria, rotuloTipoConta } from '../conta.model';
import { ContaService } from '../conta.service';

@Component({
	selector: 'app-conta-lista',
	imports: [CurrencyPipe, RouterLink],
	templateUrl: './conta-lista.html',
	styleUrl: './conta-lista.scss',
})
export class ContaLista implements OnInit {
	private readonly contaService = inject(ContaService);

	contas = signal<ContaBancaria[]>([]);
	carregando = signal(false);
	contaParaExcluir = signal<ContaBancaria | null>(null);

	contasAtivas = computed(() => this.contas().filter((conta) => conta.ativa).length);
	saldo = computed(() => this.contas().reduce((total, conta) => total + conta.saldo, 0));

	ngOnInit(): void {
		this.carregar();
	}

	carregar(): void {
		this.carregando.set(true);
		this.contaService.listar().subscribe({
			next: (contas) => {
				this.contas.set(contas);
				this.carregando.set(false);
			},
			error: () => this.carregando.set(false),
		});
	}

	tipoLegivel(conta: ContaBancaria): string {
		return rotuloTipoConta(conta.tipo);
	}

	abrirExclusao(conta: ContaBancaria): void {
		this.contaParaExcluir.set(conta);
	}

	fecharExclusao(): void {
		this.contaParaExcluir.set(null);
	}

	excluir(): void {
		const conta = this.contaParaExcluir();
		if (!conta) return;

		this.contaService.excluir(conta.id).subscribe({
			next: () => {
				this.fecharExclusao();
				this.carregar();
			},
		});
	}
}
