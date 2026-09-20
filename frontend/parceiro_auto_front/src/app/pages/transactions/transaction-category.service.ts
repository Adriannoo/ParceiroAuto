import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { ApiResponse } from '../../models/api-response.model';
import { TransactionCategory, TransactionCategoryRequest } from './transaction.model';

// Centraliza as chamadas de categorias de transacoes.
@Injectable({ providedIn: 'root' })
export class TransactionCategoryService {
  private http = inject(HttpClient);
  private readonly apiUrl = 'http://localhost:8080/api/transaction-categories';

  listCategoriesByCompany(companyId: number): Observable<TransactionCategory[]> {
    return this.http.get<ApiResponse<TransactionCategory[]>>(`${this.apiUrl}/company/${companyId}`).pipe(
      map(response => response.dados)
    );
  }

  createCategory(companyId: number, category: TransactionCategoryRequest): Observable<TransactionCategory> {
    return this.http.post<ApiResponse<TransactionCategory>>(`${this.apiUrl}/company/${companyId}`, category).pipe(
      map(response => response.dados)
    );
  }

  updateCategory(companyId: number, categoryId: number, category: TransactionCategoryRequest): Observable<TransactionCategory> {
    return this.http.put<ApiResponse<TransactionCategory>>(`${this.apiUrl}/company/${companyId}/${categoryId}`, category).pipe(
      map(response => response.dados)
    );
  }

  deleteCategory(companyId: number, categoryId: number): Observable<void> {
    return this.http.delete(`${this.apiUrl}/company/${companyId}/${categoryId}`).pipe(
      map(() => void 0)
    );
  }

}
