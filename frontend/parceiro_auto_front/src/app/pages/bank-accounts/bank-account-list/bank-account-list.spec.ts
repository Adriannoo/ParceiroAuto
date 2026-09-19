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
    expect(TestBed.inject(Router).url).toBe('/2/edit?companyId=1');
  });

  it('updates the default account in the list after setting a new default', async () => {
    const bankAccountService = {
      listByCompany: () => of([
        {
          id: 1,
          companyId: 1,
          bankName: 'Banco atual',
          branch: '1234',
          accountNumber: '1111',
          accountType: 'CHECKING',
          balance: 0,
          defaultAccount: true,
        },
        {
          id: 2,
          companyId: 1,
          bankName: 'Banco novo',
          branch: '5678',
          accountNumber: '2222',
          accountType: 'SAVINGS',
          balance: 0,
          defaultAccount: false,
        },
      ]),
      setDefault: () => of({
        id: 2,
        companyId: 1,
        bankName: 'Banco novo',
        branch: '5678',
        accountNumber: '2222',
        accountType: 'SAVINGS',
        balance: 0,
        defaultAccount: true,
      }),
    };

    await TestBed.configureTestingModule({
      imports: [BankAccountList],
      providers: [
        ...appConfig.providers,
        provideRouter([]),
        { provide: CompanyService, useValue: { list: () => of([{ id: 1, tradeName: 'Empresa teste' }]) } },
        { provide: BankAccountService, useValue: bankAccountService },
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(BankAccountList);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    const beforeRows = Array.from((fixture.nativeElement as HTMLElement).querySelectorAll('tbody tr'));
    expect(beforeRows[0].textContent).toContain('Conta padrão');
    expect(beforeRows[1].textContent).toContain('Não padrão');

    beforeRows[1].querySelector<HTMLButtonElement>('button')!.click();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    const afterRows = Array.from((fixture.nativeElement as HTMLElement).querySelectorAll('tbody tr'));
    expect(afterRows[0].textContent).toContain('Não padrão');
    expect(afterRows[1].textContent).toContain('Conta padrão');
  });
});
