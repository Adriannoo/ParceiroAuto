import { Routes } from '@angular/router';

import { LoginComponent } from './components/login/login.component';
import { RegisterComponent } from './components/register/register.component';
import { Layout } from './components/layout/layout';

import { Dashboard } from './pages/dashboard/dashboard';
import { Reports } from './pages/reports/reports';

import { CompanyList } from './pages/companies/company-list/company-list';
import { CompanyForm } from './pages/companies/company-form/company-form';
import { CompanyCategories } from './pages/companies/company-categories/company-categories';
import { CompanyTransactions } from './pages/companies/company-transactions/company-transactions';
import { BankAccountList } from './pages/bank-accounts/bank-account-list/bank-account-list';
import { BankAccountForm } from './pages/bank-accounts/bank-account-form/bank-account-form';

import { TransactionList } from './pages/transactions/transaction-list/transaction-list';
import { TransactionForm } from './pages/transactions/transaction-form/transaction-form';
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
        path: 'companies',
        children: [
          { path: '', component: CompanyList, title: 'Empresas' },
          { path: 'new', component: CompanyForm, title: 'Nova empresa' },
          { path: ':id/edit', component: CompanyForm, title: 'Editar empresa' },
          { path: ':id/categories', component: CompanyCategories, title: 'Categorias da empresa' },
          { path: ':id/transactions', component: CompanyTransactions, title: 'Lançamentos da empresa' },
          { path: '**', redirectTo: '' },
        ],
      },

      {
        path: 'transactions',
        children: [
          { path: '', component: TransactionList, title: 'Lançamentos' },
          { path: 'new', component: TransactionForm, title: 'Novo lançamento' },
          { path: ':id/edit', component: TransactionForm, title: 'Editar lançamento' },
          { path: '**', redirectTo: '' },
        ],
      },

      {
        path: 'bank-accounts',
        children: [
          { path: '', component: BankAccountList, title: 'Contas bancárias' },
          { path: 'new', component: BankAccountForm, title: 'Nova conta bancária' },
          { path: ':id/edit', component: BankAccountForm, title: 'Editar conta bancária' },
          { path: '**', redirectTo: '' },
        ],
      },
      { path: 'reports', component: Reports },
    ],
  },
];
