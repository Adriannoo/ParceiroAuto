import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, Subject } from 'rxjs';
import { vi } from 'vitest';
import Swal from 'sweetalert2';
import { Company } from '../company.model';
import { CompanyService } from '../company.service';
import { CompanyList } from './company-list';

vi.mock('sweetalert2', () => ({ default: { fire: vi.fn() } }));

describe('Exclusao de empresas', () => {
  const company = { id: 1, legalName: 'Empresa teste', tradeName: 'Teste', cnpj: '11222333000181' } as Company;
  let deletion: Subject<void>;
  let service: { list: ReturnType<typeof vi.fn>; delete: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    vi.clearAllMocks();
    deletion = new Subject<void>();
    service = { list: vi.fn(() => of([company])), delete: vi.fn(() => deletion) };
    await TestBed.configureTestingModule({
      imports: [CompanyList],
      providers: [provideRouter([]), { provide: CompanyService, useValue: service }],
    }).compileComponents();
  });

  it('mantem o modal e permite cancelar sem excluir', () => {
    const fixture = TestBed.createComponent(CompanyList);
    fixture.componentInstance.openConfirmation(company);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.modal')).toBeTruthy();
    fixture.componentInstance.closeConfirmation();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.modal')).toBeNull();
    expect(service.delete).not.toHaveBeenCalled();
    expect(Swal.fire).not.toHaveBeenCalled();
  });

  it('evita exclusao duplicada e atualiza a lista no sucesso', async () => {
    const fixture = TestBed.createComponent(CompanyList);
    const component = fixture.componentInstance;
    component.openConfirmation(company);
    await Promise.all([component.confirmDeletion(), component.confirmDeletion()]);
    expect(service.delete).toHaveBeenCalledTimes(1);
    component.closeConfirmation();
    expect(component.companyToDelete()).toBe(company);
    deletion.next();
    deletion.complete();
    expect(component.companyToDelete()).toBeNull();
    expect(component.deleting()).toBe(false);
    expect(service.list).toHaveBeenCalled();
    expect(Swal.fire).toHaveBeenCalledWith(expect.objectContaining({ icon: 'success' }));
  });

  it('mantem o modal e permite nova tentativa quando a API recusa', async () => {
    const component = TestBed.createComponent(CompanyList).componentInstance;
    component.openConfirmation(company);
    await component.confirmDeletion();
    deletion.error({ status: 409 });
    expect(component.companyToDelete()).toBe(company);
    expect(component.deleting()).toBe(false);
    expect(Swal.fire).toHaveBeenCalledWith(expect.objectContaining({ icon: 'error' }));
    deletion = new Subject<void>();
    await component.confirmDeletion();
    expect(service.delete).toHaveBeenCalledTimes(2);
    deletion.complete();
  });
});
