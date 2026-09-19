import { provideRouter } from '@angular/router';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { CompanyService } from '../companies/company.service';
import { TransactionService } from '../transactions/transaction.service';

import { Dashboard } from './dashboard';

describe('Dashboard', () => {
  let component: Dashboard;
  let fixture: ComponentFixture<Dashboard>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: CompanyService, useValue: { list: () => of([]) } },
        {
          provide: TransactionService,
          useValue: {
            list: () => of([]),
            listCategoriesByCompany: () => of([]),
            listNextRecurringByCompany: () => of([]),
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
