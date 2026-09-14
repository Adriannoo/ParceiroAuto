import { Routes } from '@angular/router';

import { LoginComponent } from './components/login/login.component';
import { RegisterComponent } from './components/register/register.component';
import { Layout } from './components/layout/layout';

import { Dashboard } from './pages/dashboard/dashboard';
import { BankAccounts } from './pages/bank-accounts/bank-accounts';
import { Reports } from './pages/reports/reports';
import { AccessManagement } from './pages/access-management/access-management';

import { CompanyList } from './pages/companies/company-list/company-list';
import { CompanyForm } from './pages/companies/company-form/company-form';
import { CompanyTransactions } from './pages/companies/company-transactions/company-transactions';

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

      { path: 'bank-accounts', component: BankAccounts },
      { path: 'reports', component: Reports },
      { path: 'access-management', component: AccessManagement },
    ],
  },
];