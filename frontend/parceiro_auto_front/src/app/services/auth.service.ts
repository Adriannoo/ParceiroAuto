import { Injectable } from '@angular/core';
import { Observable, of, throwError } from 'rxjs';
import { delay } from 'rxjs/operators';
import { Papel, Usuario, papelNormalizado } from '../models/usuario.model';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly CHAVE_USUARIOS = 'parceiro-auto:usuarios:v1';
  private readonly CHAVE_LOGADO = 'parceiro-auto:logado:v1';
  private readonly LATENCIA = 300;

  private readonly USUARIOS_SEMENTE: Usuario[] = [
    {
      id: 1,
      nome: 'Gustavo Mendes',
      email: 'gustavo@empresa.com',
      senha: '123456',
      papel: 'dono',
      empresasId: [1, 2],
      ativo: true,
    },
    {
      id: 2,
      nome: 'Maria Silva',
      email: 'maria@empresa.com',
      senha: '123456',
      papel: 'gerente',
      empresasId: [1],
      ativo: true,
    },
    {
      id: 3,
      nome: 'João Santos',
      email: 'joao@empresa.com',
      senha: '123456',
      papel: 'visualizador',
      empresasId: [2, 3],
      ativo: true,
    },
  ];

  constructor() {
    if (localStorage.getItem(this.CHAVE_USUARIOS) === null) {
      this.gravarUsuarios(this.USUARIOS_SEMENTE);
    }
  }

  private lerUsuarios(): Usuario[] {
    try {
      const bruto = localStorage.getItem(this.CHAVE_USUARIOS);
      return bruto ? (JSON.parse(bruto) as Usuario[]) : [];
    } catch {
      this.gravarUsuarios(this.USUARIOS_SEMENTE);
      return [...this.USUARIOS_SEMENTE];
    }
  }

  private gravarUsuarios(usuarios: Usuario[]): void {
    localStorage.setItem(this.CHAVE_USUARIOS, JSON.stringify(usuarios));
  }

  private lerLogado(): Usuario | null {
    try {
      const bruto = localStorage.getItem(this.CHAVE_LOGADO);
      return bruto ? (JSON.parse(bruto) as Usuario) : null;
    } catch {
      return null;
    }
  }

  private gravarLogado(usuario: Usuario | null): void {
    if (usuario === null) {
      localStorage.removeItem(this.CHAVE_LOGADO);
    } else {
      localStorage.setItem(this.CHAVE_LOGADO, JSON.stringify(usuario));
    }
  }

  login(email: string, senha: string): Observable<Usuario> {
    const usuario = this.lerUsuarios().find(
      (u) => u.email === email && u.senha === senha && u.ativo
    );

    if (!usuario) {
      return throwError(() => new Error('Email ou senha inválidos'));
    }

    this.gravarLogado(usuario);

    return of(usuario).pipe(delay(this.LATENCIA));
  }

  logout(): void {
    this.gravarLogado(null);
  }

  estaAutenticado(): boolean {
    return this.lerLogado() !== null;
  }

  getUsuarioLogado(): Usuario | null {
    return this.lerLogado();
  }

  papelAtual(): Exclude<Papel, 'admin' | 'usuario'> | null {
    const usuario = this.getUsuarioLogado();
    return usuario ? papelNormalizado(usuario.papel) : null;
  }

  temPapel(...papeis: Array<Exclude<Papel, 'admin' | 'usuario'>>): boolean {
    const papel = this.papelAtual();
    return papel !== null && papeis.includes(papel);
  }

  podeGerenciarFuncionarios(): boolean { return this.temPapel('dono'); }
  podeGerenciarEmpresas(): boolean { return this.temPapel('dono'); }
  podeGerenciarLancamentos(): boolean { return this.temPapel('dono', 'gerente'); }

  listarUsuarios(): Usuario[] {
    return this.lerUsuarios();
  }

  criarFuncionario(dados: Omit<Usuario, 'id' | 'empresasId'>, empresasId: number[]): Observable<Usuario> {
    if (!this.podeGerenciarFuncionarios()) {
      return throwError(() => new Error('Apenas o dono pode gerenciar funcionários.'));
    }

    const usuarios = this.lerUsuarios();
    if (usuarios.some((usuario) => usuario.email.toLowerCase() === dados.email.toLowerCase())) {
      return throwError(() => new Error('Email já cadastrado'));
    }

    const novoUsuario: Usuario = {
      ...dados,
      email: dados.email.trim().toLowerCase(),
      id: usuarios.length > 0 ? Math.max(...usuarios.map((usuario) => usuario.id)) + 1 : 1,
      empresasId: [...empresasId],
    };

    usuarios.push(novoUsuario);
    this.gravarUsuarios(usuarios);
    return of(novoUsuario).pipe(delay(this.LATENCIA));
  }

  atualizarFuncionario(usuario: Usuario, empresasId: number[]): Observable<Usuario> {
    if (!this.podeGerenciarFuncionarios()) {
      return throwError(() => new Error('Apenas o dono pode gerenciar funcionários.'));
    }

    const usuarios = this.lerUsuarios();
    const indice = usuarios.findIndex((item) => item.id === usuario.id);
    if (indice === -1) return throwError(() => new Error('Funcionário não encontrado.'));

    const atualizado = { ...usuario, empresasId: [...empresasId] };
    usuarios[indice] = atualizado;
    this.gravarUsuarios(usuarios);
    return of(atualizado).pipe(delay(this.LATENCIA));
  }

  excluirFuncionario(id: number): Observable<void> {
    if (!this.podeGerenciarFuncionarios()) {
      return throwError(() => new Error('Apenas o dono pode gerenciar funcionários.'));
    }
    if (this.getUsuarioLogado()?.id === id) {
      return throwError(() => new Error('O dono não pode excluir o próprio acesso.'));
    }

    this.gravarUsuarios(this.lerUsuarios().filter((usuario) => usuario.id !== id));
    return of(void 0).pipe(delay(this.LATENCIA));
  }

  registrar(dados: Omit<Usuario, 'id' | 'empresasId'>): Observable<Usuario> {
    const usuarios = this.lerUsuarios();
    
    // Validar se email já existe
    if (usuarios.some((u) => u.email === dados.email)) {
      return throwError(() => new Error('Email já cadastrado'));
    }

    const novoUsuario: Usuario = {
      ...dados,
      id: usuarios.length > 0 ? Math.max(...usuarios.map((u) => u.id)) + 1 : 1,
      empresasId: [],
    };

    usuarios.push(novoUsuario);
    this.gravarUsuarios(usuarios);
    this.gravarLogado(novoUsuario);

    return of(novoUsuario).pipe(delay(this.LATENCIA));
  }
}
