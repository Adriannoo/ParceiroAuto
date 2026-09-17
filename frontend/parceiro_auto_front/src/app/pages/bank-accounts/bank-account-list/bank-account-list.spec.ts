import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of } from 'rxjs';
import { appConfig } from '../../../app.config';
import { CompanyService } from '../../companies/company.service';
import { BankAccountService } from '../bank-account.service';
import { BankAccountList } from './bank-account-list';

describe('BankAccountList', () => {
  it('renders the balance and opens editing with a single click', async () => {
    await TestBed.configureTestingModule({
      imports: [BankAccountList],
      providers: [
        ...appConfig.providers,
        provideRouter([{ path: ':id/edit', component: BankAccountList }]),
        { provide: CompanyService, useValue: { list: () => of([{ id: 1, tradeName: 'Empresa teste' }]) } },
        { provide: BankAccountService, useValue: {
          listByCompany: () => of([{
            id: 2, companyId: 1, bankName: 'Banco teste', branch: '1234',
            accountNumber: '12345', accountType: 'CHECKING', balance: 1234.56,
            defaultAccount: false,
          }]),
        } },
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(BankAccountList);
    fixture.detectChanges();
    await fixture.whenStable();

    const row = (fixture.nativeElement as HTMLElement).querySelector('tbody tr')!;
    expect(row.textContent).toContain('1.234,56');
    expect(row.querySelector('button')?.disabled).toBe(false);
    row.querySelector<HTMLAnchorElement>('a')!.click();
    await fixture.whenStable();
    expect(TestBed.inject(Router).url).toBe('/2/edit');
  });
});
