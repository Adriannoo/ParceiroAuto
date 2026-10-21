import { Type } from '@angular/core';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter, Router } from '@angular/router';
import { CurrentCompanyService } from './current-company.service';
import { BankAccountList } from '../pages/bank-accounts/bank-account-list/bank-account-list';
import { CompanyForm } from '../pages/companies/company-form/company-form';
import { BankAccountForm } from '../pages/bank-accounts/bank-account-form/bank-account-form';
import { TransactionForm } from '../pages/transactions/transaction-form/transaction-form';
import { Dashboard } from '../pages/dashboard/dashboard';
import { TransactionList } from '../pages/transactions/transaction-list/transaction-list';
import { CompanyCategories } from '../pages/companies/company-categories/company-categories';
import { Reports } from '../pages/reports/reports';
import { CompanyService } from '../pages/companies/company.service';
import { CompanyRequest } from '../pages/companies/company.model';
import { Layout } from '../components/layout/layout';

const api = 'http://localhost:8080/api';
const account = (companyId: number) => ({ id: companyId * 10, companyId, bankName: 'Banco', branch: '1234', accountNumber: '12345', accountType: 'CHECKING' as const, defaultAccount: true, balance: 0 });
const category = (companyId: number) => ({ id: companyId * 100, companyId, name: 'Vendas', type: 'ENTRADA', active: true });
const transaction = (companyId: number) => ({ id: 7, companyId, bankAccountId: companyId * 10, transactionCategoryId: companyId * 100, type: 'ENTRADA', description: 'Servico', value: 150, date: '2026-10-21', method: 'PIX', recurrenceFrequency: 'MONTHLY', recurrenceEndDate: null });
const recurrence = (companyId: number) => ({ recurrenceRuleId: 9, transactionId: 7, companyId, description: 'Servico', value: 150, type: 'ENTRADA', frequency: 'MONTHLY', nextDate: '2026-11-21', endDate: null });

describe('Contexto operacional e contratos frontend-backend', () => {
  let http: HttpTestingController;
  let context: CurrentCompanyService;
  let routeId: string | null;

  beforeEach(() => {
    localStorage.clear();
    routeId = null;
    TestBed.configureTestingModule({ providers: [
      provideHttpClient(), provideHttpClientTesting(), provideRouter([]),
      { provide: ActivatedRoute, useValue: { snapshot: { get paramMap() { return convertToParamMap(routeId ? { id: routeId } : {}); } } } },
    ] });
    http = TestBed.inject(HttpTestingController);
    context = TestBed.inject(CurrentCompanyService);
    vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
  });

  afterEach(() => { http.verify(); localStorage.clear(); });

  function open<T>(component: Type<T>, companyId: number | null = 1) {
    context.setCurrentCompanyId(companyId);
    const fixture = TestBed.createComponent(component);
    fixture.detectChanges();
    return fixture;
  }
  function flush(path: string, dados: unknown) { http.expectOne(api + path).flush({ dados }); }
  function options(companyId: number, includeInactive = false) {
    flush(`/bank-accounts/company/${companyId}`, [account(companyId)]);
    flush(`/transaction-categories/company/${companyId}${includeInactive ? "?includeInactive=true" : ""}`, [category(companyId)]);
  }
  function list(companyId: number) {
    flush(`/transactions/company/${companyId}`, [transaction(companyId)]);
    options(companyId, true);
    flush(`/recurrence-rules/company/${companyId}`, [recurrence(companyId)]);
  }

  it('normaliza IDs invalidos e preserva apenas empresas disponiveis', () => {
    for (const id of [0, -1, NaN, Infinity, 1.5]) {
      context.setCurrentCompanyId(id);
      expect(context.currentCompanyId()).toBeNull();
    }
    context.ensureCompany([NaN, 0, 2]);
    expect(context.currentCompanyId()).toBe(2);
    context.ensureCompany([]);
    expect(localStorage.getItem('parceiro-auto:current-company-id')).toBeNull();
  });

  it('cria conta com empresa somente na URL e preserva o payload de conta padrao', () => {
    const fixture = open(BankAccountForm, 2);
    const form = fixture.componentInstance;
    expect(fixture.nativeElement.querySelector('[formControlName="companyId"]')).toBeNull();
    form.form.patchValue(account(2));
    form.save();
    const request = http.expectOne(api + '/bank-accounts/company/2');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ bankName: 'Banco', branch: '1234', accountNumber: '12345', accountType: 'CHECKING', defaultAccount: false });
    request.flush({ dados: account(2) });
  });

  it('edita conta padrao sem enviar campos de response e bloqueia outra empresa', () => {
    routeId = '10';
    const fixture = open(BankAccountForm);
    flush('/bank-accounts/company/1/accounts/10', account(1));
    fixture.componentInstance.save();
    const update = http.expectOne(api + '/bank-accounts/company/1/accounts/10');
    expect(update.request.method).toBe('PUT');
    expect(update.request.body.defaultAccount).toBe(true);
    expect(update.request.body.companyId).toBeUndefined();
    expect(update.request.body.id).toBeUndefined();
    update.flush({ dados: account(1) });
    context.setCurrentCompanyId(2);
    fixture.detectChanges();
    http.expectOne(api + '/bank-accounts/company/2/accounts/10').flush({}, { status: 400, statusText: 'Wrong company' });
    fixture.componentInstance.save();
    expect(fixture.componentInstance.form.disabled).toBe(true);
    http.expectNone(request => request.method === 'PUT');
  });

  it('nao envia requests de formularios sem empresa atual', () => {
    const bank = open(BankAccountForm, null);
    bank.componentInstance.save();
    expect(bank.componentInstance.error()).toContain('sidebar');
    bank.destroy();
    const movement = open(TransactionForm, null);
    movement.componentInstance.save();
    movement.componentInstance.saveCategory();
    expect(movement.componentInstance.error()).toContain('sidebar');
    http.expectNone(() => true);
  });

  it('cria movimentacao recorrente com conta e categoria apenas da empresa atual', () => {
    const fixture = open(TransactionForm, 2);
    options(2);
    const form = fixture.componentInstance;
    form.form.patchValue({ description: 'Servico', value: 150, recurring: true });
    form.save();
    const request = http.expectOne(api + '/transactions');
    expect(request.request.method).toBe('POST');
    expect(request.request.body.companyId).toBe(2);
    expect(request.request.body.bankAccountId).toBe(20);
    expect(request.request.body.transactionCategoryId).toBe(200);
    expect(request.request.body.recurrenceFrequency).toBe('MONTHLY');
    expect(request.request.body.recurring).toBeUndefined();
    expect(request.request.body.id).toBeUndefined();
    request.flush({ dados: transaction(2) });
  });

  it('cancela opcoes antigas ao trocar empresa e impede salvar referencias antigas', () => {
    const fixture = open(TransactionForm);
    const oldAccounts = http.expectOne(api + '/bank-accounts/company/1');
    const oldCategories = http.expectOne(api + '/transaction-categories/company/1');
    context.setCurrentCompanyId(2);
    fixture.detectChanges();
    expect(oldAccounts.cancelled).toBe(true);
    expect(oldCategories.cancelled).toBe(true);
    options(2);
    const form = fixture.componentInstance;
    form.form.patchValue({ description: 'Servico', value: 150, bankAccountId: 10, transactionCategoryId: 100 });
    form.save();
    http.expectNone(api + '/transactions');
    expect(form.error()).toContain('empresa atual');
    expect(form.bankAccounts().map(item => item.companyId)).toEqual([2]);
  });

  it('edita movimentacao mantendo empresa e recorrencia; bloqueia edicao cruzada', () => {
    routeId = '7';
    const fixture = open(TransactionForm);
    flush('/transactions/7', transaction(1));
    options(1);
    expect(fixture.componentInstance.form.controls.bankAccountId.value).toBe(10);
    fixture.componentInstance.save();
    const update = http.expectOne(api + '/transactions/7');
    expect(update.request.method).toBe('PUT');
    expect(update.request.body.companyId).toBe(1);
    expect(update.request.body.recurrenceFrequency).toBe('MONTHLY');
    update.flush({ dados: transaction(1) });
    context.setCurrentCompanyId(2);
    fixture.detectChanges();
    flush('/transactions/7', transaction(1));
    expect(fixture.componentInstance.form.disabled).toBe(true);
    fixture.componentInstance.save();
    http.expectNone(request => request.method === 'PUT');
    http.expectNone(api + '/bank-accounts/company/2');
  });

  it('dashboard recarrega metricas, categorias e proximas recorrencias ao trocar empresa', () => {
    const fixture = open(Dashboard);
    flush('/companies', [{ id: 1, active: true }, { id: 2, active: true }]);
    const old = http.expectOne(api + '/transactions/company/1');
    const oldCategories = http.expectOne(api + '/transaction-categories/company/1?includeInactive=true');
    const oldRecurring = http.expectOne(api + '/recurrence-rules/company/1/next?limit=3');
    context.setCurrentCompanyId(2);
    fixture.detectChanges();
    expect(old.cancelled && oldCategories.cancelled && oldRecurring.cancelled).toBe(true);
    flush('/transactions/company/2', [transaction(2)]);
    flush('/transaction-categories/company/2?includeInactive=true', [category(2)]);
    flush('/recurrence-rules/company/2/next?limit=3', [recurrence(2)]);
    expect(fixture.componentInstance.accumulatedBalance()).toBe(150);
    expect(fixture.componentInstance.recent()[0].companyId).toBe(2);
    context.setCurrentCompanyId(null);
    fixture.detectChanges();
    expect(fixture.componentInstance.accumulatedBalance()).toBe(0);
    expect(fixture.componentInstance.recurring()).toEqual([]);
  });

  it('lista operacional segue sidebar e permite editar/encerrar recorrencia', () => {
    const fixture = open(TransactionList);
    flush('/companies', []);
    list(1);
    const component = fixture.componentInstance;
    component.openRecurrenceEdit(component.recurringTransactions()[0]);
    component.recurrenceForm.patchValue({ frequency: 'WEEKLY' });
    component.saveRecurrence();
    const update = http.expectOne(api + '/recurrence-rules/9');
    expect(update.request.body).toEqual({ frequency: 'WEEKLY', endDate: null });
    update.flush({ dados: recurrence(1) });
    list(1);
    component.openRecurrenceDelete(component.recurringTransactions()[0]);
    component.confirmRecurrenceDelete();
    const remove = http.expectOne(api + '/recurrence-rules/9');
    expect(remove.request.method).toBe('DELETE');
    remove.flush(null);
    list(1);
    context.setCurrentCompanyId(2);
    fixture.detectChanges();
    list(2);
    component.clearFilters();
    expect(component.companyId()).toBe(2);
    expect(component.filtered()[0].companyId).toBe(2);
  });

  it('categorias criam, editam e inativam usando empresa atual na URL', () => {
    routeId = '99';
    const fixture = open(CompanyCategories, 2);
    const load = () => { flush('/companies/2', { id: 2, tradeName: 'Atual' }); flush('/transaction-categories/company/2', [category(2)]); };
    load();
    expect(TestBed.inject(Router).navigate).toHaveBeenCalledWith(['/companies', 2, 'categories'], { replaceUrl: true });
    const component = fixture.componentInstance;
    component.openCreateModal('ENTRADA');
    component.form.patchValue({ name: 'Vendas' });
    component.saveCategory();
    const create = http.expectOne(api + '/transaction-categories/company/2');
    expect(create.request.body).toEqual({ name: 'Vendas', type: 'ENTRADA' });
    create.flush({ dados: category(2) }); load();
    component.openEditModal(component.categories()[0]);
    component.form.patchValue({ name: 'Servicos' });
    component.saveCategory();
    const update = http.expectOne(api + '/transaction-categories/company/2/200');
    expect(update.request.method).toBe('PUT');
    update.flush({ dados: category(2) }); load();
    component.openConfirmation(component.categories()[0]);
    component.confirmDeletion();
    const remove = http.expectOne(api + '/transaction-categories/company/2/200');
    expect(remove.request.method).toBe('DELETE');
    remove.flush(null); load();
    context.setCurrentCompanyId(null);
    fixture.detectChanges();
    component.saveCategory();
    http.expectNone(() => true);
  });

  it('relatorios iniciam na empresa atual e preservam escolha consolidada explicita', () => {
    const fixture = open(Reports, 2);
    flush('/transactions', [transaction(1), transaction(2)]);
    flush('/companies', [{ id: 1 }, { id: 2 }]);
    options(1, true); options(2, true);
    expect(fixture.componentInstance.filteredTransactions().length).toBe(1);
    fixture.componentInstance.onCompanyChange('all');
    context.setCurrentCompanyId(1);
    fixture.detectChanges();
    expect(fixture.componentInstance.companyId()).toBe('all');
    expect(fixture.componentInstance.filteredTransactions().length).toBe(2);
  });

  it('company update usa id na URL e notifica atualizacao da lista', () => {
    const service = TestBed.inject(CompanyService);
    const changed = vi.fn();
    service.companiesChanged.subscribe(changed);
    const body = { legalName: 'Empresa', active: true } as CompanyRequest;
    service.update(2, body).subscribe();
    const update = http.expectOne(api + '/companies/2');
    expect(update.request.body).toEqual(body);
    expect(update.request.body.id).toBeUndefined();
    expect(update.request.body.userIds).toBeUndefined();
    update.flush({ dados: { ...body, id: 2 } });
    expect(changed).toHaveBeenCalledOnce();
  });

  it('sidebar seleciona empresa disponivel e atualiza apos cadastro', () => {
    const fixture = open(Layout, null);
    flush('/companies', [{ id: 1, active: true, tradeName: 'Primeira' }]);
    expect(context.currentCompanyId()).toBe(1);
    TestBed.inject(CompanyService).create({ active: true } as CompanyRequest).subscribe();
    http.expectOne(api + '/companies').flush({ dados: { id: 2 } });
    flush('/companies', [{ id: 2, active: true, tradeName: 'Segunda' }]);
    expect(context.currentCompanyId()).toBe(2);
    expect(fixture.componentInstance.companies()[0].id).toBe(2);
    fixture.componentInstance.logout();
    http.expectOne(api + "/users/logout").flush(null);
    expect(context.currentCompanyId()).toBeNull();
  });

  it('resposta de conta padrao da empresa anterior nao altera a nova lista', () => {
    const fixture = open(BankAccountList);
    flush('/companies', []);
    flush('/bank-accounts/company/1', [{ ...account(1), defaultAccount: false }]);
    fixture.componentInstance.setDefault(fixture.componentInstance.bankAccounts()[0]);
    const patch = http.expectOne(api + '/bank-accounts/company/1/accounts/10/default');
    expect(patch.request.method).toBe('PATCH');
    expect(patch.request.body).toEqual({});
    context.setCurrentCompanyId(2);
    fixture.detectChanges();
    flush('/bank-accounts/company/2', [{ ...account(2), defaultAccount: true }]);
    patch.flush({ dados: account(1) });
    expect(fixture.componentInstance.bankAccounts()[0].companyId).toBe(2);
    expect(fixture.componentInstance.bankAccounts()[0].defaultAccount).toBe(true);
  });

  it('sem empresa nao consulta endpoints operacionais nem permite categorias', () => {
    const dashboard = open(Dashboard, null);
    flush('/companies', []);
    expect(dashboard.componentInstance.transactions()).toEqual([]);
    dashboard.destroy();
    const movements = open(TransactionList, null);
    flush('/companies', []);
    expect(movements.componentInstance.filtered()).toEqual([]);
    movements.destroy();
    const categories = open(CompanyCategories, null);
    categories.componentInstance.openCreateModal('ENTRADA');
    categories.componentInstance.form.patchValue({ name: 'Vendas' });
    categories.componentInstance.saveCategory();
    expect(categories.componentInstance.error()).toContain('sidebar');
    http.expectNone(() => true);
  });

  it('trocar empresa fecha modais antigos e categorias passam a usar novo contexto', () => {
    const fixture = open(CompanyCategories);
    flush('/companies/1', { id: 1 });
    flush('/transaction-categories/company/1', [category(1)]);
    const component = fixture.componentInstance;
    component.openEditModal(component.categories()[0]);
    component.openConfirmation(component.categories()[0]);
    context.setCurrentCompanyId(2);
    // Mesmo antes do effect, a acao antiga nao pode enviar DELETE.
    component.confirmDeletion();
    fixture.detectChanges();
    flush('/companies/2', { id: 2 });
    flush('/transaction-categories/company/2', [category(2)]);
    expect(component.categoryModalOpen()).toBe(false);
    expect(component.categoryToDelete()).toBeNull();
    expect(component.categories()[0].companyId).toBe(2);
    http.expectNone(request => request.method === 'DELETE');
  });

  it('relatorio recebe empresa inicial carregada depois da abertura', () => {
    const fixture = open(Reports, null);
    flush('/transactions', []);
    flush('/companies', []);
    context.setCurrentCompanyId(2);
    fixture.detectChanges();
    expect(fixture.componentInstance.companyId()).toBe(2);
  });

  it('rota invalida de empresa nao consulta nem salva IDs invalidos', () => {
    routeId = 'NaN';
    const fixture = open(CompanyForm);
    fixture.componentInstance.save();
    expect(fixture.componentInstance.form.disabled).toBe(true);
    http.expectNone(() => true);
  });

});
