import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { BankAccountService } from '../bank-accounts/bank-account.service';
import { CompanyService } from '../companies/company.service';
import { TransactionService } from '../transactions/transaction.service';
import { TransactionCategoryService } from '../transactions/transaction-category.service';

import { Reports } from './reports';

describe('Reports', () => {
  let component: Reports;
  let fixture: ComponentFixture<Reports>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: [
        { provide: TransactionCategoryService, useValue: { listCategoriesByCompany: () => of([]) } },
        { provide: CompanyService, useValue: { list: () => of([]) } },
        { provide: BankAccountService, useValue: { listByCompany: () => of([]) } },
        {
          provide: TransactionService,
          useValue: {
            list: () => of([]),
          },
        },
      ],
      imports: [Reports],
    }).compileComponents();

    fixture = TestBed.createComponent(Reports);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
