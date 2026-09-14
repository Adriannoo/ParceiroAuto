import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AccessManagement } from './access-management';

describe('AccessManagement', () => {
  let component: AccessManagement;
  let fixture: ComponentFixture<AccessManagement>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AccessManagement],
    }).compileComponents();

    fixture = TestBed.createComponent(AccessManagement);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});