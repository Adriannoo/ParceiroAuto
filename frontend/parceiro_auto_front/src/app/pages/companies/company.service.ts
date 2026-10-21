import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, Subject, map, tap } from 'rxjs';
import { ApiResponse } from '../../models/api-response.model';
import {Company, CompanyLookup, CompanyRequest} from './company.model';

@Injectable({ providedIn: 'root' })
export class CompanyService {
  private http = inject(HttpClient);
  private changes = new Subject<void>();
  readonly companiesChanged = this.changes.asObservable();
  private readonly apiUrl = 'http://localhost:8080/api/companies';

  list(): Observable<Company[]> {
    return this.http.get<ApiResponse<Company[]>>(this.apiUrl).pipe(
      map(response => response.dados)
    );
  }

  findById(id: number): Observable<Company> {
    return this.http.get<ApiResponse<Company>>(`${this.apiUrl}/${id}`).pipe(
      map(response => response.dados)
    );
  }

  create(company: CompanyRequest): Observable<Company> {
    return this.http.post<ApiResponse<Company>>(this.apiUrl, company).pipe(
      map(response => response.dados),
      tap(() => this.changes.next())
    );
  }

  update(id: number, company: CompanyRequest): Observable<Company> {
    return this.http.put<ApiResponse<Company>>(`${this.apiUrl}/${id}`, company).pipe(
      map(response => response.dados),
      tap(() => this.changes.next())
    );
  }

  delete(id: number): Observable<void> {
    return this.http.delete(`${this.apiUrl}/${id}`, { responseType: 'text' }).pipe(
      map(() => void 0),
      tap(() => this.changes.next())
    );
  }

  findByCnpjInBrasilApi(cnpj: string): Observable<CompanyLookup> {
    const cleanedCnpj = cnpj.replace(/\D/g, '');

    return this.http.get<ApiResponse<CompanyLookup>>(`${this.apiUrl}/lookup/${cleanedCnpj}`).pipe(
      map(response => response.dados)
    );
  }
}
