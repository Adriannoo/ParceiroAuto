import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { ApiResponse } from '../../models/api-response.model';
import { Company } from './company.model';

@Injectable({ providedIn: 'root' })
export class CompanyService {
  private http = inject(HttpClient);
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

  create(company: Omit<Company, 'id' | 'userIds'>): Observable<Company> {
    return this.http.post<ApiResponse<Company>>(this.apiUrl, company).pipe(
      map(response => response.dados)
    );
  }

  update(company: Company): Observable<Company> {
    return this.http.put<ApiResponse<Company>>(`${this.apiUrl}/${company.id}`, company).pipe(
      map(response => response.dados)
    );
  }

  delete(id: number): Observable<void> {
    return this.http.delete(`${this.apiUrl}/${id}`, { responseType: 'text' }).pipe(
      map(() => void 0)
    );
  }
}
