import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AuthService } from './auth.service';

describe('Autenticacao pelo backend', () => {
  let service: AuthService;
  let http: HttpTestingController;
  const base = 'http://localhost:8080/api/users';
  const user = { id: 1, login: 'aluno@example.com', name: 'Aluno' };

  beforeEach(() => {
    localStorage.setItem('parceiro-auto:users:v1', 'senhas antigas');
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());

  it('envia credenciais sem modificar a senha e remove o armazenamento antigo', () => {
    expect(localStorage.getItem('parceiro-auto:users:v1')).toBeNull();
    service.login(' aluno@example.com ', ' senha ').subscribe();
    const request = http.expectOne(base + '/login');
    expect(request.request.body).toEqual({ login: user.login, password: ' senha ' });
    expect(request.request.withCredentials).toBe(true);
    request.flush({ dados: user });
    expect(service.getCurrentUser()).toEqual(user);
    expect(localStorage.getItem('parceiro-auto:logado:v1')).toBeNull();
  });

  it('cadastra no banco e encerra a sessao no servidor', () => {
    service.register({ email: user.login, name: 'Aluno', password: '123456' }).subscribe();
    const request = http.expectOne(base);
    expect(request.request.body).toEqual({ login: user.login, name: 'Aluno', password: '123456' });
    request.flush({ dados: user });
    service.logout().subscribe();
    const logout = http.expectOne(base + '/logout');
    expect(logout.request.withCredentials).toBe(true);
    logout.flush(null);
    expect(service.getCurrentUser()).toBeNull();
  });

  it('consulta a sessao e rejeita uma sessao expirada', () => {
    service.checkSession().subscribe(value => expect(value).toBe(true));
    http.expectOne(base + '/me').flush({ dados: user });
    service.checkSession().subscribe(value => expect(value).toBe(false));
    http.expectOne(base + '/me').flush({}, { status: 401, statusText: 'Unauthorized' });
    expect(service.getCurrentUser()).toBeNull();
  });

  it('mostra a mensagem de senha invalida devolvida pelo servidor', () => {
    service.login(user.login, 'errada').subscribe({
      next: () => { throw new Error('Login deveria falhar'); },
      error: error => expect(error.message).toBe('Login ou senha invalidos!'),
    });
    http.expectOne(base + '/login').flush({ mensagem: 'Login ou senha invalidos!' }, { status: 401, statusText: 'Unauthorized' });
    expect(service.getCurrentUser()).toBeNull();
  });
});
