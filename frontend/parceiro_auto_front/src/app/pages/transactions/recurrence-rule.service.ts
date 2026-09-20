import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { ApiResponse } from '../../models/api-response.model';
import { RecurringTransaction, RecurrenceRuleRequest } from './transaction.model';

// Centraliza as chamadas das regras de recorrencia.
@Injectable({ providedIn: 'root' })
export class RecurrenceRuleService {
  private http = inject(HttpClient);
  private readonly apiUrl = 'http://localhost:8080/api/recurrence-rules';

  listNextRecurringByCompany(companyId: number, limit = 3): Observable<RecurringTransaction[]> {
    return this.http.get<ApiResponse<RecurringTransaction[]>>(`${this.apiUrl}/company/${companyId}/next?limit=${limit}`).pipe(
      map(response => response.dados)
    );
  }

  listRecurringByCompany(companyId: number): Observable<RecurringTransaction[]> {
    return this.http.get<ApiResponse<RecurringTransaction[]>>(`${this.apiUrl}/company/${companyId}`).pipe(
      map(response => response.dados)
    );
  }

  updateRecurrence(id: number, recurrence: RecurrenceRuleRequest): Observable<RecurringTransaction> {
    return this.http.put<ApiResponse<RecurringTransaction>>(`${this.apiUrl}/${id}`, recurrence).pipe(
      map(response => response.dados)
    );
  }

  deleteRecurrence(id: number): Observable<void> {
    return this.http.delete(`${this.apiUrl}/${id}`).pipe(
      map(() => void 0)
    );
  }
}
