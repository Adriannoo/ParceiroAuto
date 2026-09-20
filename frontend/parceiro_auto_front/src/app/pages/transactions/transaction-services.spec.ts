import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { TransactionCategoryService } from './transaction-category.service';
import { RecurrenceRuleService } from './recurrence-rule.service';
import { TransactionService } from './transaction.service';

describe('Contratos HTTP dos services financeiros', () => {
  let http: HttpTestingController;
  const base = 'http://localhost:8080/api';

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('cria, consulta, altera e exclui categorias no endpoint dedicado', () => {
    const service = TestBed.inject(TransactionCategoryService);
    const payload = { name: 'Vendas', type: 'ENTRADA' as const };
    const category = { ...payload, id: 8, companyId: 2, active: true };
    service.createCategory(2, payload).subscribe(value => expect(value).toEqual(category));
    const create = http.expectOne(`${base}/transaction-categories/company/2`);
    expect(create.request.method).toBe('POST');
    expect(create.request.body).toEqual(payload);
    create.flush({ dados: category });
    service.listCategoriesByCompany(2).subscribe(value => expect(value).toEqual([category]));
    http.expectOne(`${base}/transaction-categories/company/2`).flush({ dados: [category] });
    service.updateCategory(2, 8, { ...payload, name: 'Servicos' }).subscribe();
    const update = http.expectOne(`${base}/transaction-categories/company/2/8`);
    expect(update.request.method).toBe('PUT');
    expect(update.request.body.name).toBe('Servicos');
    update.flush({ dados: category });
    service.deleteCategory(2, 8).subscribe(value => expect(value).toBeUndefined());
    const remove = http.expectOne(`${base}/transaction-categories/company/2/8`);
    expect(remove.request.method).toBe('DELETE');
    remove.flush(null, { status: 204, statusText: 'No Content' });
  });

  it('consulta e modifica recorrencias sem usar o endpoint de transacoes', () => {
    const service = TestBed.inject(RecurrenceRuleService);
    service.listNextRecurringByCompany(2, 3).subscribe(value => expect(value).toEqual([]));
    http.expectOne(`${base}/recurrence-rules/company/2/next?limit=3`).flush({ dados: [] });
    service.listRecurringByCompany(2).subscribe();
    http.expectOne(`${base}/recurrence-rules/company/2`).flush({ dados: [] });
    const payload = { frequency: 'MONTHLY' as const, endDate: null };
    service.updateRecurrence(9, payload).subscribe();
    const update = http.expectOne(`${base}/recurrence-rules/9`);
    expect(update.request.method).toBe('PUT');
    expect(update.request.body).toEqual(payload);
    update.flush({ dados: { recurrenceRuleId: 9, ...payload } });
    service.deleteRecurrence(9).subscribe();
    const remove = http.expectOne(`${base}/recurrence-rules/9`);
    expect(remove.request.method).toBe('DELETE');
    remove.flush(null, { status: 204, statusText: 'No Content' });
  });

  it('mantem os lancamentos separados e propaga falhas para o componente', () => {
    const service = TestBed.inject(TransactionService);
    service.listByCompany(2).subscribe(value => expect(value).toEqual([]));
    http.expectOne(`${base}/transactions/company/2`).flush({ dados: [] });
    let status: number | undefined;
    service.delete(7).subscribe({ error: error => status = error.status });
    const remove = http.expectOne(`${base}/transactions/7`);
    expect(remove.request.method).toBe('DELETE');
    remove.flush({}, { status: 409, statusText: 'Conflict' });
    expect(status).toBe(409);
  });
});
