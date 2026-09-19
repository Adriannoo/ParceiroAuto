import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { CompanyService } from '../company.service';
import { LEGAL_NATURES, COMPANY_SIZES, TAX_REGIMES, STATES } from '../company.model';
import {
  cnpjValidator,
  formatPostalCode,
  formatCnpj,
  formatPhone,
} from '../company.validators';

@Component({
  selector: 'app-company-form',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './company-form.html',
  styleUrl: './company-form.scss',
})
export class CompanyForm implements OnInit {
  private fb = inject(FormBuilder);
  private companyService = inject(CompanyService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  taxRegimes = TAX_REGIMES;
  companySizes = COMPANY_SIZES;
  legalNatures = LEGAL_NATURES;
  states = STATES;

  companyId = signal<number | null>(null);
  saving = signal(false);
  error = signal<string | null>(null);

  form = this.fb.group({
    legalName: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(120)]],
    tradeName: ['', [Validators.required, Validators.maxLength(80)]],
    cnpj: ['', [Validators.required, cnpjValidator]],
    stateRegistration: ['', [Validators.maxLength(20)]],

    legalNature: ['LTDA', [Validators.required]],
    taxRegime: ['SIMPLES_NACIONAL', [Validators.required]],
    size: ['ME', [Validators.required]],

    postalCode: ['', [Validators.required, Validators.pattern(/^\d{5}-\d{3}$/)]],
    street: ['', [Validators.required]],
    streetNumber: ['', [Validators.required]],
    addressComplement: [''],
    neighborhood: ['', [Validators.required]],
    city: ['', [Validators.required]],
    state: ['', [Validators.required]],

    phone: ['', [Validators.required, Validators.pattern(/^\(\d{2}\)\s\d{4,5}-\d{4}$/)]],
    email: ['', [Validators.required, Validators.email]],

    active: [true],
  });

  get editing(): boolean {
    return this.companyId() !== null;
  }

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');

    if (id) {
      this.companyId.set(Number(id));
      this.load(Number(id));
    }
  }

  private load(id: number): void {
    this.companyService.findById(id).subscribe({
      next: (company) => this.form.patchValue(company),
      error: () => {
        this.error.set('Empresa não encontrada.');
        this.form.disable();
      },
    });
  }

  /** MEI is both a tax regime and a company size; selecting one sets the other. */
  onTaxRegimeChange(): void {
    if (this.form.controls.taxRegime.value === 'MEI') {
      this.form.controls.size.setValue('MEI');
    }
  }

  onCnpjInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.form.controls.cnpj.setValue(formatCnpj(input.value));
  }

  onPhoneInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.form.controls.phone.setValue(formatPhone(input.value));
  }

  onPostalCodeInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.form.controls.postalCode.setValue(formatPostalCode(input.value));
  }

  isInvalid(field: string): boolean {
    const control = this.form.get(field);
    return !!control && control.invalid && (control.touched || control.dirty);
  }

  hasError(field: string, type: string): boolean {
    return !!this.form.get(field)?.errors?.[type];
  }

  save(): void {
    this.error.set(null);

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.error.set('Existem campos obrigatórios pendentes. Revise as seções destacadas.');
      return;
    }

    const data = this.form.getRawValue();
    const id = this.companyId();

    this.saving.set(true);

    const request = id
      ? this.companyService.update({ id, ...data } as any)
      : this.companyService.create(data as any);

    request.subscribe({
      next: () => this.router.navigate(['/companies']),
      error: () => {
        this.error.set('Não foi possível salvar. Tente novamente.');
        this.saving.set(false);
      },
    });
  }

  reset(): void {
    this.form.reset({
      legalNature: 'LTDA',
      taxRegime: 'SIMPLES_NACIONAL',
      size: 'ME',
      active: true,
    });
    this.error.set(null);
  }

  searchByCnpj(): void {
    this.error.set(null);

    const cnpjControl = this.form.controls.cnpj;
    cnpjControl.markAsTouched();

    if (cnpjControl.invalid) {
      this.error.set('Informe um CNPJ válido antes de buscar.');
      return;
    }

    const cnpj = cnpjControl.value ?? '';

    this.companyService.findByCnpjInBrasilApi(cnpj).subscribe({
      next: (companyData) => {
        this.form.patchValue({
          legalName: companyData.legalName,
          tradeName: companyData.tradeName,
          postalCode: companyData.postalCode,
          street: companyData.street,
          streetNumber: companyData.streetNumber,
          neighborhood: companyData.neighborhood,
          city: companyData.city,
          state: companyData.state,
          phone: companyData.phone,
          email: companyData.email,
        });
      },
      error: () => {
        this.error.set('Não foi possível buscar os dados desse CNPJ.')
      },
    });
  }
}
