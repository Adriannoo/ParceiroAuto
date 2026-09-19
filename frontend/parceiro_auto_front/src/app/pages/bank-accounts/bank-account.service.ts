import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { ApiResponse } from '../../models/api-response.model';
import { BankAccount, BankAccountRequest } from './bank-account.model';

@Injectable({ providedIn: 'root' })
export class BankAccountService {
  private http = inject(HttpClient);
  private readonly apiUrl = 'http://localhost:8080/api/bank-accounts';
  listByCompany(companyId: number): Observable<BankAccount[]> {
    return this.http.get<ApiResponse<BankAccount[]>>(`${this.apiUrl}/company/${companyId}`).pipe(
      map(response => response.dados)
    );
  }

  findById(companyId: number, id: number): Observable<BankAccount> {
    return this.http.get<ApiResponse<BankAccount>>(`${this.apiUrl}/company/${companyId}/accounts/${id}`).pipe(
      map(response => response.dados)
    );
  }

  create(bankAccount: BankAccountRequest): Observable<BankAccount> {
    return this.http.post<ApiResponse<BankAccount>>(`${this.apiUrl}/company/${bankAccount.companyId}`, bankAccount).pipe(
      map(response => response.dados)
    );
  }

  update(id: number, bankAccount: BankAccountRequest): Observable<BankAccount> {
    return this.http.put<ApiResponse<BankAccount>>(`${this.apiUrl}/company/${bankAccount.companyId}/accounts/${id}`, bankAccount).pipe(
      map(response => response.dados)
    );
  }

  delete(id: number, companyId: number): Observable<void> {
    return this.http.delete(`${this.apiUrl}/company/${companyId}/accounts/${id}`).pipe(
      map(() => void 0)
    );
  }

  setDefault(id: number, companyId: number): Observable<BankAccount> {
    return this.http.patch<ApiResponse<BankAccount>>(`${this.apiUrl}/company/${companyId}/accounts/${id}/default`, {}).pipe(
      map(response => response.dados)
    );
  }
}
