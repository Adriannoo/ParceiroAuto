import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { ApiResponse } from '../../models/api-response.model';
import {
  RecurringTransaction,
  RecurrenceRuleRequest,
  Transaction,
  TransactionCategory,
  TransactionCategoryRequest,
  TransactionRequest,
} from './transaction.model';

@Injectable({ providedIn: 'root' })
export class TransactionService {
  private http = inject(HttpClient);
  private readonly apiUrl = 'http://localhost:8080/api/transactions';
  private readonly categoryApiUrl = 'http://localhost:8080/api/transaction-categories';
  private readonly recurrenceApiUrl = 'http://localhost:8080/api/recurrence-rules';

  list(): Observable<Transaction[]> {
    return this.http.get<ApiResponse<Transaction[]>>(this.apiUrl).pipe(
      map(response => response.dados)
    );
  }

  listByCompany(companyId: number): Observable<Transaction[]> {
    return this.http.get<ApiResponse<Transaction[]>>(`${this.apiUrl}/company/${companyId}`).pipe(
      map(response => response.dados)
    );
  }

  findById(id: number): Observable<Transaction> {
    return this.http.get<ApiResponse<Transaction>>(`${this.apiUrl}/${id}`).pipe(
      map(response => response.dados)
    );
  }

  create(transaction: TransactionRequest): Observable<Transaction> {
    return this.http.post<ApiResponse<Transaction>>(this.apiUrl, transaction).pipe(
      map(response => response.dados)
    );
  }

  update(id: number, transaction: TransactionRequest): Observable<Transaction> {
    return this.http.put<ApiResponse<Transaction>>(`${this.apiUrl}/${id}`, transaction).pipe(
      map(response => response.dados)
    );
  }

  delete(id: number): Observable<void> {
    return this.http.delete(`${this.apiUrl}/${id}`).pipe(
      map(() => void 0)
    );
  }

  listCategoriesByCompany(companyId: number): Observable<TransactionCategory[]> {
    return this.http.get<ApiResponse<TransactionCategory[]>>(`${this.categoryApiUrl}/company/${companyId}`).pipe(
      map(response => response.dados)
    );
  }

  createCategory(companyId: number, category: TransactionCategoryRequest): Observable<TransactionCategory> {
    return this.http.post<ApiResponse<TransactionCategory>>(`${this.categoryApiUrl}/company/${companyId}`, category).pipe(
      map(response => response.dados)
    );
  }

  updateCategory(companyId: number, categoryId: number, category: TransactionCategoryRequest): Observable<TransactionCategory> {
    return this.http.put<ApiResponse<TransactionCategory>>(`${this.categoryApiUrl}/company/${companyId}/${categoryId}`, category).pipe(
      map(response => response.dados)
    );
  }

  deleteCategory(companyId: number, categoryId: number): Observable<void> {
    return this.http.delete(`${this.categoryApiUrl}/company/${companyId}/${categoryId}`).pipe(
      map(() => void 0)
    );
  }

  listNextRecurringByCompany(companyId: number, limit = 3): Observable<RecurringTransaction[]> {
    return this.http.get<ApiResponse<RecurringTransaction[]>>(`${this.recurrenceApiUrl}/company/${companyId}/next?limit=${limit}`).pipe(
      map(response => response.dados)
    );
  }

  listRecurringByCompany(companyId: number): Observable<RecurringTransaction[]> {
    return this.http.get<ApiResponse<RecurringTransaction[]>>(`${this.recurrenceApiUrl}/company/${companyId}`).pipe(
      map(response => response.dados)
    );
  }

  updateRecurrence(id: number, recurrence: RecurrenceRuleRequest): Observable<RecurringTransaction> {
    return this.http.put<ApiResponse<RecurringTransaction>>(`${this.recurrenceApiUrl}/${id}`, recurrence).pipe(
      map(response => response.dados)
    );
  }

  deleteRecurrence(id: number): Observable<void> {
    return this.http.delete(`${this.recurrenceApiUrl}/${id}`).pipe(
      map(() => void 0)
    );
  }
}
