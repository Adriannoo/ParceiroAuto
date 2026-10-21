import { Injectable, inject, signal } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, catchError, map, of, tap, throwError } from 'rxjs';
import { User, UserRegistration } from '../models/user.model';
import { ApiResponse } from '../models/api-response.model';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = 'http://localhost:8080/api/users';
  private readonly user = signal<User | null>(null);

  constructor() {
    localStorage.removeItem('parceiro-auto:users:v1');
    localStorage.removeItem('parceiro-auto:logado:v1');
  }

  login(email: string, password: string): Observable<User> {
    return this.authenticate('/login', { login: email.trim(), password });
  }

  register(data: UserRegistration): Observable<User> {
    return this.authenticate('', { login: data.email.trim(), password: data.password, name: data.name.trim() });
  }

  private authenticate(path: string, body: object): Observable<User> {
    return this.http.post<ApiResponse<User>>(this.apiUrl + path, body, { withCredentials: true }).pipe(
      map(response => response.dados),
      tap(user => this.user.set(user)),
      catchError((error: HttpErrorResponse) => {
        this.user.set(null);
        const validation = error.status === 400 && error.error?.dados;
        const message = validation ? Object.values(validation).join(' ') : error.error?.mensagem;
        return throwError(() => new Error(message || 'Nao foi possivel acessar o servidor. Tente novamente.'));
      }),
    );
  }

  checkSession(): Observable<boolean> {
    return this.http.get<ApiResponse<User>>(this.apiUrl + '/me', { withCredentials: true }).pipe(
      tap(response => this.user.set(response.dados)),
      map(() => true),
      catchError(() => { this.user.set(null); return of(false); }),
    );
  }

  logout(): Observable<void> {
    return this.http.post<void>(this.apiUrl + '/logout', {}, { withCredentials: true }).pipe(
      tap(() => this.user.set(null)),
    );
  }

  getCurrentUser(): User | null { return this.user(); }
}
