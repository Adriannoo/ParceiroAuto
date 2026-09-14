import { Injectable, inject } from '@angular/core';
import { Observable, of, throwError } from 'rxjs';
import { delay } from 'rxjs/operators';
import { Company } from './company.model';
import { AuthService } from '../../services/auth.service';

@Injectable({ providedIn: 'root' })
export class CompanyService {
  private readonly STORAGE_KEY = 'parceiro-auto:companies:v3';
  private readonly LATENCY = 300;

  private authService = inject(AuthService);

  private readonly SEED: Company[] = [
    {
      id: 1,
      userIds: [1, 2],  // Gustavo (admin) and Maria have access
      legalName: 'Auto Peças Iguaçu LTDA',
      tradeName: 'Iguaçu Peças',
      cnpj: '12.345.678/0001-95',
      stateRegistration: '9012345678',
      legalNature: 'LTDA',
      taxRegime: 'SIMPLES_NACIONAL',
      size: 'EPP',
      postalCode: '85851-000',
      street: 'Avenida Brasil',
      streetNumber: '1420',
      addressComplement: 'Galpão 2',
      neighborhood: 'Centro',
      city: 'Foz do Iguaçu',
      state: 'PR',
      phone: '(45) 3521-1000',
      email: 'contato@iguacupecas.com.br',
      active: true,
    },
    {
      id: 2,
      userIds: [1, 3],  // Gustavo (admin) and Joao have access
      legalName: 'Marcia Ferreira Confecções',
      tradeName: 'Ateliê Marcia',
      cnpj: '04.252.011/0001-10',
      stateRegistration: 'ISENTO',
      legalNature: 'EI',
      taxRegime: 'MEI',
      size: 'MEI',
      postalCode: '89201-100',
      street: 'Rua Blumenau',
      streetNumber: '210',
      addressComplement: 'Sala 3',
      neighborhood: 'Atiradores',
      city: 'Joinville',
      state: 'SC',
      phone: '(47) 99812-4477',
      email: 'marcia@atelie.com.br',
      active: true,
    },
    {
      id: 3,
      userIds: [3],  // Only Joao has access
      legalName: 'Oficina Mecânica Central S/A',
      tradeName: 'Central Motors',
      cnpj: '11.222.333/0001-81',
      stateRegistration: '9087654321',
      legalNature: 'SA',
      taxRegime: 'LUCRO_PRESUMIDO',
      size: 'DEMAIS',
      postalCode: '80010-010',
      street: 'Rua XV de Novembro',
      streetNumber: '870',
      addressComplement: '',
      neighborhood: 'Centro',
      city: 'Curitiba',
      state: 'PR',
      phone: '(41) 3030-2200',
      email: 'sac@centralmotors.com.br',
      active: false,
    },
  ];

  constructor() {
    if (localStorage.getItem(this.STORAGE_KEY) === null) {
      this.write(this.SEED);
    }
  }

  private read(): Company[] {
    try {
      const raw = localStorage.getItem(this.STORAGE_KEY);
      return raw ? (JSON.parse(raw) as Company[]) : [];
    } catch {
      this.write(this.SEED);
      return [...this.SEED];
    }
  }

  private write(companies: Company[]): void {
    localStorage.setItem(this.STORAGE_KEY, JSON.stringify(companies));
  }

  private generateId(companies: Company[]): number {
    return companies.reduce((largest, e) => Math.max(largest, e.id), 0) + 1;
  }

  private getCurrentUserId(): number | null {
    return this.authService.getCurrentUser()?.id ?? null;
  }

  private canAccess(userIds: number[]): boolean {
    const userId = this.getCurrentUserId();
    if (!userId) return false;
    return userIds.includes(userId);
  }

  list(): Observable<Company[]> {
    const all = this.read();
    // Include only companies accessible to the current user
    const filtered = all.filter((e) => this.canAccess(e.userIds));
    return of(filtered).pipe(delay(this.LATENCY));
  }

  findById(id: number): Observable<Company> {
    const company = this.read().find((e) => e.id === id);

    if (!company) {
      return throwError(() => new Error(`Empresa ${id} não encontrada.`));
    }

    // Check whether the user has access
    if (!this.canAccess(company.userIds)) {
      return throwError(() => new Error(`Acesso negado à empresa ${id}.`));
    }

    return of(company).pipe(delay(this.LATENCY));
  }

  create(data: Omit<Company, 'id' | 'userIds'>): Observable<Company> {
    const companies = this.read();
    const userId = this.getCurrentUserId();

    if (!userId) {
      return throwError(() => new Error('Usuário não autenticado'));
    }

    const created: Company = {
      ...data,
      id: this.generateId(companies),
      userIds: [userId],  // Grant access to the current user
    };

    companies.push(created);
    this.write(companies);

    return of(created).pipe(delay(this.LATENCY));
  }

  update(company: Company): Observable<Company> {
    const companies = this.read();
    const index = companies.findIndex((e) => e.id === company.id);

    if (index === -1) {
      return throwError(() => new Error(`Empresa ${company.id} não encontrada.`));
    }

    // Check whether the user has access
    if (!this.canAccess(companies[index].userIds)) {
      return throwError(() => new Error(`Acesso negado à empresa ${company.id}.`));
    }

    companies[index] = { ...company };
    this.write(companies);

    return of(company).pipe(delay(this.LATENCY));
  }

  delete(id: number): Observable<void> {
    const companies = this.read();
    const company = companies.find((e) => e.id === id);

    if (!company) {
      return throwError(() => new Error(`Empresa ${id} não encontrada.`));
    }

    // Check whether the user has access
    if (!this.canAccess(company.userIds)) {
      return throwError(() => new Error(`Acesso negado à empresa ${id}.`));
    }

    this.write(companies.filter((e) => e.id !== id));

    return of(void 0).pipe(delay(this.LATENCY));
  }

  isCnpjRegistered(cnpj: string, ignoredId?: number): boolean {
    const normalized = cnpj.replace(/\D/g, '');
    const userId = this.getCurrentUserId();
    const all = this.read();

    // Include only companies accessible to the user
    const accessibleCompanies = userId
      ? all.filter((e) => e.userIds.includes(userId))
      : [];

    return accessibleCompanies.some(
      (e) => e.cnpj.replace(/\D/g, '') === normalized && e.id !== ignoredId,
    );
  }

  restoreExamples(): void {
    this.write(this.SEED);
  }
}