import { provideRouter } from '@angular/router';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { CompanyService } from '../companies/company.service';
import { TransactionService } from '../transactions/transaction.service';
import { RecurrenceRuleService } from '../transactions/recurrence-rule.service';
import { TransactionCategoryService } from '../transactions/transaction-category.service';

import { Dashboard } from './dashboard';

describe('Dashboard', () => {
  let component: Dashboard;
  let fixture: ComponentFixture<Dashboard>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: RecurrenceRuleService, useValue: { listNextRecurringByCompany: () => of([]) } },
        { provide: TransactionCategoryService, useValue: { listCategoriesByCompany: () => of([]) } },
        { provide: CompanyService, useValue: { list: () => of([]) } },
        {
          provide: TransactionService,
          useValue: {
            list: () => of([]),
          },
        },
      ],
      imports: [Dashboard],
    }).compileComponents();

    fixture = TestBed.createComponent(Dashboard);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
