import { Injectable, signal } from '@angular/core';

const CURRENT_COMPANY_KEY = 'parceiro-auto:current-company-id';

@Injectable({ providedIn: 'root' })
export class CurrentCompanyService {
  private currentCompanyIdSignal = signal<number | null>(this.readStoredCompanyId());

  currentCompanyId = this.currentCompanyIdSignal.asReadonly();

  setCurrentCompanyId(companyId: number | null): void {
    this.currentCompanyIdSignal.set(companyId);

    if (companyId === null) {
      localStorage.removeItem(CURRENT_COMPANY_KEY);
      return;
    }

    localStorage.setItem(CURRENT_COMPANY_KEY, String(companyId));
  }

  ensureCompany(companyIds: number[]): void {
    const current = this.currentCompanyIdSignal();

    if (current && companyIds.includes(current)) {
      return;
    }

    this.setCurrentCompanyId(companyIds[0] ?? null);
  }

  private readStoredCompanyId(): number | null {
    const value = Number(localStorage.getItem(CURRENT_COMPANY_KEY));
    return Number.isFinite(value) && value > 0 ? value : null;
  }
}
