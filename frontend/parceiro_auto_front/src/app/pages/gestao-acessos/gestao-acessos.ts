import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../services/auth.service';
import { Papel, Usuario, rotuloPapel } from '../../models/usuario.model';
import { Empresa } from '../empresa/empresa.model';
import { EmpresaService } from '../empresa/empresa.service';

@Component({
  selector: 'app-gestao-acessos',
  imports: [FormsModule],
  templateUrl: './gestao-acessos.html',
  styleUrl: './gestao-acessos.scss',
})
export class GestaoAcessos implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly empresaService = inject(EmpresaService);

  usuarios = signal<Usuario[]>([]);
  empresas = signal<Empresa[]>([]);
  selecionado = signal<Usuario | null>(null);
  carregando = signal(false);
  mensagem = signal<string | null>(null);
  erro = signal<string | null>(null);

  nome = '';
  email = '';
  senha = '';
  papel: Exclude<Papel, 'dono' | 'admin' | 'usuario'> = 'gerente';
  ativo = true;
  empresasId: number[] = [];

  funcionarios = computed(() => {
    const logado = this.authService.getUsuarioLogado()?.id;
    return this.usuarios().filter((usuario) => usuario.id !== logado);
  });

  ngOnInit(): void {
    this.carregar();
  }

  carregar(): void {
    this.usuarios.set(this.authService.listarUsuarios());
    this.empresaService.listar().subscribe({
      next: (empresas) => this.empresas.set(empresas),
      error: (err: Error) => this.erro.set(err.message),
    });
  }

  novo(): void {
    this.selecionado.set(null);
    this.nome = '';
    this.email = '';
    this.senha = '';
    this.papel = 'gerente';
    this.ativo = true;
    this.empresasId = [];
    this.limparMensagens();
  }

  editar(usuario: Usuario): void {
    this.selecionado.set(usuario);
    this.nome = usuario.nome;
    this.email = usuario.email;
    this.senha = '';
    this.papel = usuario.papel === 'gerente' ? 'gerente' : 'visualizador';
    this.ativo = usuario.ativo;
    this.empresasId = [...usuario.empresasId];
    this.limparMensagens();
  }

  empresaSelecionada(id: number): boolean {
    return this.empresasId.includes(id);
  }

  alternarEmpresa(id: number): void {
    this.empresasId = this.empresaSelecionada(id)
      ? this.empresasId.filter((empresaId) => empresaId !== id)
      : [...this.empresasId, id];
  }

  salvar(): void {
    this.limparMensagens();
    if (!this.nome.trim() || !this.email.trim() || (!this.selecionado() && !this.senha.trim())) {
      this.erro.set('Preencha nome, email e senha para criar um funcionário.');
      return;
    }

    this.carregando.set(true);
    const usuario = this.selecionado();
    const dados = {
      nome: this.nome.trim(),
      email: this.email.trim(),
      senha: this.senha.trim() || usuario?.senha || '',
      papel: this.papel,
      ativo: this.ativo,
    } as const;

    const operacao = usuario
      ? this.authService.atualizarFuncionario({ ...usuario, ...dados }, this.empresasId)
      : this.authService.criarFuncionario(dados, this.empresasId);

    operacao.subscribe({
      next: () => {
        this.mensagem.set(usuario ? 'Funcionário atualizado.' : 'Funcionário criado.');
        this.carregando.set(false);
        this.carregar();
        if (!usuario) {
          this.selecionado.set(null);
          this.nome = '';
          this.email = '';
          this.senha = '';
          this.empresasId = [];
        }
      },
      error: (err: Error) => {
        this.erro.set(err.message);
        this.carregando.set(false);
      },
    });
  }

  excluir(usuario: Usuario): void {
    if (!confirm(`Excluir o acesso de ${usuario.nome}?`)) return;

    this.authService.excluirFuncionario(usuario.id).subscribe({
      next: () => {
        this.mensagem.set('Acesso excluído.');
        this.carregar();
      },
      error: (err: Error) => this.erro.set(err.message),
    });
  }

  papelLegivel(papel: Papel): string {
    return rotuloPapel(papel);
  }

  empresasLegiveis(usuario: Usuario): string {
    return usuario.empresasId
      .map((id) => this.empresas().find((empresa) => empresa.id === id)?.nomeFantasia)
      .filter((nome): nome is string => Boolean(nome))
      .join(', ') || 'Nenhuma empresa selecionada';
  }

  private limparMensagens(): void {
    this.mensagem.set(null);
    this.erro.set(null);
  }
}
