import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { sessionInterceptor } from './session.interceptor';

describe('Cookie de sessao', () => {
  it('envia credenciais apenas para a API do projeto', () => {
    TestBed.configureTestingModule({ providers: [
      provideHttpClient(withInterceptors([sessionInterceptor])), provideHttpClientTesting(),
    ] });
    const client = TestBed.inject(HttpClient);
    const http = TestBed.inject(HttpTestingController);
    client.get('http://localhost:8080/api/companies').subscribe();
    const internal = http.expectOne('http://localhost:8080/api/companies');
    expect(internal.request.withCredentials).toBe(true);
    internal.flush([]);
    client.get('https://example.com/data').subscribe();
    const external = http.expectOne('https://example.com/data');
    expect(external.request.withCredentials).toBe(false);
    external.flush([]);
    http.verify();
  });
});
