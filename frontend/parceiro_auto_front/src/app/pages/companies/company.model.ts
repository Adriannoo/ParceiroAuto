export type TaxRegime =
  | 'MEI'
  | 'SIMPLES_NACIONAL'
  | 'LUCRO_PRESUMIDO'
  | 'LUCRO_REAL';

export type CompanySize = 'MEI' | 'ME' | 'EPP' | 'DEMAIS';

export type LegalNature = 'EI' | 'SLU' | 'LTDA' | 'SA' | 'SS';

export interface Company {
  id: number;
  userIds: number[];  // Users with access to this company

  legalName: string;
  tradeName: string;
  cnpj: string;
  stateRegistration: string;

  legalNature: LegalNature;
  taxRegime: TaxRegime;
  size: CompanySize;

  postalCode: string;
  street: string;
  streetNumber: string;
  addressComplement: string;
  neighborhood: string;
  city: string;
  state: string;

  phone: string;
  email: string;

  active: boolean;
}

interface Option<T> {
  value: T;
  label: string;
}

export const TAX_REGIMES: Option<TaxRegime>[] = [
  { value: 'MEI', label: 'MEI' },
  { value: 'SIMPLES_NACIONAL', label: 'Simples Nacional' },
  { value: 'LUCRO_PRESUMIDO', label: 'Lucro Presumido' },
  { value: 'LUCRO_REAL', label: 'Lucro Real' },
];

export const COMPANY_SIZES: Option<CompanySize>[] = [
  { value: 'MEI', label: 'MEI' },
  { value: 'ME', label: 'Microempresa (ME)' },
  { value: 'EPP', label: 'Empresa de Pequeno Porte (EPP)' },
  { value: 'DEMAIS', label: 'Demais' },
];

export const LEGAL_NATURES: Option<LegalNature>[] = [
  { value: 'EI', label: 'Empresário Individual' },
  { value: 'SLU', label: 'Sociedade Limitada Unipessoal' },
  { value: 'LTDA', label: 'Sociedade Empresária Limitada' },
  { value: 'SA', label: 'Sociedade Anônima' },
  { value: 'SS', label: 'Sociedade Simples' },
];

export const STATES: string[] = [
  'AC', 'AL', 'AP', 'AM', 'BA', 'CE', 'DF', 'ES', 'GO', 'MA',
  'MT', 'MS', 'MG', 'PA', 'PB', 'PR', 'PE', 'PI', 'RJ', 'RN',
  'RS', 'RO', 'RR', 'SC', 'SP', 'SE', 'TO',
];

function optionLabel<T>(list: Option<T>[], value: T): string {
  return list.find((o) => o.value === value)?.label ?? String(value);
}

export const taxRegimeLabel = (v: TaxRegime) => optionLabel(TAX_REGIMES, v);
export const companySizeLabel = (v: CompanySize) => optionLabel(COMPANY_SIZES, v);
export const legalNatureLabel = (v: LegalNature) => optionLabel(LEGAL_NATURES, v);

/** Single-line address for tables and detail views. */
export function formatAddress(e: Company): string {
  const addressComplement = e.addressComplement ? `, ${e.addressComplement}` : '';
  return `${e.street}, ${e.streetNumber}${addressComplement} — ${e.neighborhood}, ${e.city}/${e.state}`;
}