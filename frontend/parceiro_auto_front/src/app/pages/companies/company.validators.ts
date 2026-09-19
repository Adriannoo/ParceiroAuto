import { AbstractControl, ValidationErrors } from '@angular/forms';

export function cnpjValidator(control: AbstractControl): ValidationErrors | null {
  const value: string = (control.value ?? '').replace(/\D/g, '');

  if (!value) {
    return null;
  }

  if (value.length !== 14 || /^(\d)\1{13}$/.test(value)) {
    return { invalidCnpj: true };
  }

  const calculateDigit = (base: string): number => {
    let weight = base.length - 7;
    let sum = 0;

    for (const digit of base) {
      sum += Number(digit) * weight--;
      if (weight < 2) {
        weight = 9;
      }
    }

    const remainder = sum % 11;
    return remainder < 2 ? 0 : 11 - remainder;
  };

  const first = calculateDigit(value.substring(0, 12));
  const second = calculateDigit(value.substring(0, 13));

  const valid = first === Number(value[12]) && second === Number(value[13]);

  return valid ? null : { invalidCnpj: true };
}

export function formatCnpj(value: string): string {
  return (value ?? '')
    .replace(/\D/g, '')
    .slice(0, 14)
    .replace(/^(\d{2})(\d)/, '$1.$2')
    .replace(/^(\d{2})\.(\d{3})(\d)/, '$1.$2.$3')
    .replace(/\.(\d{3})(\d)/, '.$1/$2')
    .replace(/(\d{4})(\d)/, '$1-$2');
}

export function formatPhone(value: string): string {
  return (value ?? '')
    .replace(/\D/g, '')
    .slice(0, 11)
    .replace(/^(\d{2})(\d)/, '($1) $2')
    .replace(/(\d{4,5})(\d{4})$/, '$1-$2');
}

export function formatPostalCode(value: string): string {
  return (value ?? '')
    .replace(/\D/g, '')
    .slice(0, 8)
    .replace(/^(\d{5})(\d)/, '$1-$2');
}