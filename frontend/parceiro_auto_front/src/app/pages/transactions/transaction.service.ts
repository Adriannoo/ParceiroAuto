import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { ApiResponse } from '../../models/api-response.model';
import {
  Transaction,
  TransactionRequest,
} from './transaction.model';

@Injectable({ providedIn: 'root' })
export class TransactionService {
  private http = inject(HttpClient);
  private readonly apiUrl = 'http://localhost:8080/api/transactions';

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
}
