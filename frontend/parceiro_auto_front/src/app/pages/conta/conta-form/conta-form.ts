import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ContaService } from '../conta.service';
import { ContaBancaria, TIPOS_CONTA } from '../conta.model';

@Component({
  selector: 'app-conta-form',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './conta-form.html',
  styleUrl: './conta-form.scss',
})
export class ContaForm implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly contaService = inject(ContaService);
  private readonly router = inject(Router);
  private readonly rota = inject(ActivatedRoute);

  tipos = TIPOS_CONTA;
  contaId = signal<number | null>(null);
  salvando = signal(false);
  erro = signal<string | null>(null);

  formulario = this.fb.group({
    nome: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(80)]],
    banco: ['', [Validators.required, Validators.maxLength(80)]],
    agencia: ['', [Validators.required, Validators.maxLength(20)]],
    numero: ['', [Validators.required, Validators.maxLength(30)]],
    tipo: ['CORRENTE', [Validators.required]],
    saldo: [0, [Validators.required, Validators.min(0)]],
    ativa: [true],
  });

  get editando(): boolean {
    return this.contaId() !== null;
  }

  ngOnInit(): void {
    const id = this.rota.snapshot.paramMap.get('id');

    if (id) {
      this.contaId.set(Number(id));
      this.contaService.buscarPorId(Number(id)).subscribe({
        next: (conta) => this.formulario.patchValue(conta),
        error: () => {
          this.erro.set('Conta bancária não encontrada.');
          this.formulario.disable();
        },
      });
    }
  }

  invalido(campo: string): boolean {
    const controle = this.formulario.get(campo);
    return !!controle && controle.invalid && (controle.touched || controle.dirty);
  }

  erroDe(campo: string, tipo: string): boolean {
    return !!this.formulario.get(campo)?.errors?.[tipo];
  }

  salvar(): void {
    this.erro.set(null);

    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      this.erro.set('Existem campos obrigatórios pendentes. Revise as seções destacadas.');
      return;
    }

    const dados = this.formulario.getRawValue();
    const id = this.contaId();

    this.salvando.set(true);

    const requisicao = id
      ? this.contaService.atualizar({ id, usuariosId: [], ...dados } as unknown as ContaBancaria)
      : this.contaService.criar(dados as unknown as Omit<ContaBancaria, 'id' | 'usuariosId'>);

    requisicao.subscribe({
      next: () => this.router.navigate(['/conta']),
      error: () => {
        this.erro.set('Não foi possível salvar a conta. Tente novamente.');
        this.salvando.set(false);
      },
    });
  }

  limpar(): void {
    this.formulario.reset({
      tipo: 'CORRENTE',
      saldo: 0,
      ativa: true,
    });
    this.erro.set(null);
  }
}