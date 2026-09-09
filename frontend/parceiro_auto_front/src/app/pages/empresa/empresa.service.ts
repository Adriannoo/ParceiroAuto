import { Injectable, inject } from '@angular/core';
import { Observable, of, throwError } from 'rxjs';
import { delay } from 'rxjs/operators';
import { Empresa } from './empresa.model';
import { AuthService } from '../../services/auth.service';

@Injectable({ providedIn: 'root' })
export class EmpresaService {
  private readonly CHAVE = 'parceiro-auto:empresas:v3';
  private readonly LATENCIA = 300;

  private authService = inject(AuthService);

  private readonly SEMENTE: Empresa[] = [
    {
      id: 1,
      usuariosId: [1, 2],  // Gustavo (admin) e Maria têm acesso
      razaoSocial: 'Auto Peças Iguaçu LTDA',
      nomeFantasia: 'Iguaçu Peças',
      cnpj: '12.345.678/0001-95',
      inscricaoEstadual: '9012345678',
      naturezaJuridica: 'LTDA',
      regime: 'SIMPLES_NACIONAL',
      porte: 'EPP',
      cep: '85851-000',
      logradouro: 'Avenida Brasil',
      numero: '1420',
      complemento: 'Galpão 2',
      bairro: 'Centro',
      cidade: 'Foz do Iguaçu',
      uf: 'PR',
      telefone: '(45) 3521-1000',
      email: 'contato@iguacupecas.com.br',
      ativa: true,
    },
    {
      id: 2,
      usuariosId: [1, 3],  // Gustavo (admin) e João têm acesso
      razaoSocial: 'Marcia Ferreira Confecções',
      nomeFantasia: 'Ateliê Marcia',
      cnpj: '04.252.011/0001-10',
      inscricaoEstadual: 'ISENTO',
      naturezaJuridica: 'EI',
      regime: 'MEI',
      porte: 'MEI',
      cep: '89201-100',
      logradouro: 'Rua Blumenau',
      numero: '210',
      complemento: 'Sala 3',
      bairro: 'Atiradores',
      cidade: 'Joinville',
      uf: 'SC',
      telefone: '(47) 99812-4477',
      email: 'marcia@atelie.com.br',
      ativa: true,
    },
    {
      id: 3,
      usuariosId: [3],  // Só João tem acesso
      razaoSocial: 'Oficina Mecânica Central S/A',
      nomeFantasia: 'Central Motors',
      cnpj: '11.222.333/0001-81',
      inscricaoEstadual: '9087654321',
      naturezaJuridica: 'SA',
      regime: 'LUCRO_PRESUMIDO',
      porte: 'DEMAIS',
      cep: '80010-010',
      logradouro: 'Rua XV de Novembro',
      numero: '870',
      complemento: '',
      bairro: 'Centro',
      cidade: 'Curitiba',
      uf: 'PR',
      telefone: '(41) 3030-2200',
      email: 'sac@centralmotors.com.br',
      ativa: false,
    },
  ];

  constructor() {
    if (localStorage.getItem(this.CHAVE) === null) {
      this.gravar(this.SEMENTE);
    }
  }

  private ler(): Empresa[] {
    try {
      const bruto = localStorage.getItem(this.CHAVE);
      return bruto ? (JSON.parse(bruto) as Empresa[]) : [];
    } catch {
      this.gravar(this.SEMENTE);
      return [...this.SEMENTE];
    }
  }

  private gravar(empresas: Empresa[]): void {
    localStorage.setItem(this.CHAVE, JSON.stringify(empresas));
  }

  private gerarId(empresas: Empresa[]): number {
    return empresas.reduce((maior, e) => Math.max(maior, e.id), 0) + 1;
  }

  private getUsuarioLogadoId(): number | null {
    return this.authService.getUsuarioLogado()?.id ?? null;
  }

  private podeAcessar(usuariosId: number[]): boolean {
    const usuarioId = this.getUsuarioLogadoId();
    if (!usuarioId) return false;
    if (this.authService.temPapel('dono')) return true;
    return usuariosId.includes(usuarioId);
  }

  temAcessoEmpresa(id: number): boolean {
    const empresa = this.ler().find((item) => item.id === id);
    return empresa !== undefined && this.podeAcessar(empresa.usuariosId);
  }

  listar(): Observable<Empresa[]> {
    const todas = this.ler();
    // Filtrar apenas empresas que o usuário logado tem acesso
    const filtradas = todas.filter((e) => this.podeAcessar(e.usuariosId));
    return of(filtradas).pipe(delay(this.LATENCIA));
  }

  buscarPorId(id: number): Observable<Empresa> {
    const empresa = this.ler().find((e) => e.id === id);

    if (!empresa) {
      return throwError(() => new Error(`Empresa ${id} não encontrada.`));
    }

    // Verificar se o usuário tem acesso
    if (!this.podeAcessar(empresa.usuariosId)) {
      return throwError(() => new Error(`Acesso negado à empresa ${id}.`));
    }

    return of(empresa).pipe(delay(this.LATENCIA));
  }

  criar(dados: Omit<Empresa, 'id' | 'usuariosId'>): Observable<Empresa> {
    if (!this.authService.podeGerenciarEmpresas()) {
      return throwError(() => new Error('Apenas o dono pode gerenciar empresas.'));
    }
    const empresas = this.ler();
    const usuarioId = this.getUsuarioLogadoId();

    if (!usuarioId) {
      return throwError(() => new Error('Usuário não autenticado'));
    }

    const nova: Empresa = {
      ...dados,
      id: this.gerarId(empresas),
      usuariosId: [usuarioId],  // Adiciona o usuário logado como único acesso
    };

    empresas.push(nova);
    this.gravar(empresas);

    return of(nova).pipe(delay(this.LATENCIA));
  }

  atualizar(empresa: Empresa): Observable<Empresa> {
    if (!this.authService.podeGerenciarEmpresas()) {
      return throwError(() => new Error('Apenas o dono pode gerenciar empresas.'));
    }
    const empresas = this.ler();
    const indice = empresas.findIndex((e) => e.id === empresa.id);

    if (indice === -1) {
      return throwError(() => new Error(`Empresa ${empresa.id} não encontrada.`));
    }

    // Verificar se o usuário tem acesso
    if (!this.podeAcessar(empresas[indice].usuariosId)) {
      return throwError(() => new Error(`Acesso negado à empresa ${empresa.id}.`));
    }

    empresas[indice] = { ...empresa };
    this.gravar(empresas);

    return of(empresa).pipe(delay(this.LATENCIA));
  }

  excluir(id: number): Observable<void> {
    if (!this.authService.podeGerenciarEmpresas()) {
      return throwError(() => new Error('Apenas o dono pode gerenciar empresas.'));
    }
    const empresas = this.ler();
    const empresa = empresas.find((e) => e.id === id);

    if (!empresa) {
      return throwError(() => new Error(`Empresa ${id} não encontrada.`));
    }

    // Verificar se o usuário tem acesso
    if (!this.podeAcessar(empresa.usuariosId)) {
      return throwError(() => new Error(`Acesso negado à empresa ${id}.`));
    }

    this.gravar(empresas.filter((e) => e.id !== id));

    return of(void 0).pipe(delay(this.LATENCIA));
  }

  cnpjJaCadastrado(cnpj: string, idIgnorado?: number): boolean {
    const limpo = cnpj.replace(/\D/g, '');
    const usuarioId = this.getUsuarioLogadoId();
    const todas = this.ler();

    // Filtrar apenas empresas que o usuário tem acesso
    const empresasAcesso = usuarioId
      ? todas.filter((e) => e.usuariosId.includes(usuarioId))
      : [];

    return empresasAcesso.some(
      (e) => e.cnpj.replace(/\D/g, '') === limpo && e.id !== idIgnorado,
    );
  }

  restaurarExemplos(): void {
    this.gravar(this.SEMENTE);
  }
}