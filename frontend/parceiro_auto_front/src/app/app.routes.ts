import { Routes } from '@angular/router';

import { LoginComponent } from './components/login/login.component';
import { RegisterComponent } from './components/register/register.component';
import { Layout } from './components/layout/layout';

import { Dashboard } from './pages/dashboard/dashboard';
import { ContaLista } from './pages/conta/conta-lista/conta-lista';
import { ContaForm } from './pages/conta/conta-form/conta-form';
import { ContaLancamentos } from './pages/conta/conta-lancamentos/conta-lancamentos';
import { Relatorios } from './pages/relatorios/relatorios';
import { GestaoAcessos } from './pages/gestao-acessos/gestao-acessos';

import { EmpresaLista } from './pages/empresa/empresa-lista/empresa-lista';
import { EmpresaForm } from './pages/empresa/empresa-form/empresa-form';
import { EmpresaLancamentos } from './pages/empresa/empresa-lancamentos/empresa-lancamentos';

import { MovimentacaoLista } from './pages/movimentacao/movimentacao-lista/movimentacao-lista';
import { MovimentacaoForm } from './pages/movimentacao/movimentacao-form/movimentacao-form';
import { AuthGuard } from './guards/auth.guard';

export const routes: Routes = [
  { path: '', redirectTo: 'login', pathMatch: 'full' },
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },

  {
    path: '',
    component: Layout,
    canActivate: [AuthGuard],
    children: [
      { path: 'dashboard', component: Dashboard, title: 'Visão geral' },

      {
        path: 'empresas',
        children: [
          { path: '', component: EmpresaLista, title: 'Empresas' },
          { path: 'nova', component: EmpresaForm, title: 'Nova empresa' },
          { path: ':id/editar', component: EmpresaForm, title: 'Editar empresa' },
          { path: ':id/lancamentos', component: EmpresaLancamentos, title: 'Lançamentos da empresa' },
          { path: '**', redirectTo: '' },
        ],
      },

      {
        path: 'lancamentos',
        children: [
          { path: '', component: MovimentacaoLista, title: 'Lançamentos' },
          { path: 'novo', component: MovimentacaoForm, title: 'Novo lançamento' },
          { path: ':id/editar', component: MovimentacaoForm, title: 'Editar lançamento' },
          { path: '**', redirectTo: '' },
        ],
      },

      {
        path: 'conta',
        children: [
          { path: '', component: ContaLista, title: 'Contas bancárias' },
          { path: 'nova', component: ContaForm, title: 'Nova conta bancária' },
          { path: ':id/editar', component: ContaForm, title: 'Editar conta bancária' },
          { path: ':id/lancamentos', component: ContaLancamentos, title: 'Lançamentos da conta' },
          { path: '**', redirectTo: '' },
        ],
      },
      { path: 'relatorios', component: Relatorios },
      { path: 'gestao-acessos', component: GestaoAcessos },
    ],
  },
];