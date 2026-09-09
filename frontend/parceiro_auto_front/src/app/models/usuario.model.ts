export type Papel = 'dono' | 'gerente' | 'visualizador' | 'admin' | 'usuario';

export interface Usuario {
  id: number;
  nome: string;
  email: string;
  senha: string;
  papel: Papel;
  empresasId: number[];
  ativo: boolean;
}

export function papelNormalizado(papel: Papel): Exclude<Papel, 'admin' | 'usuario'> {
  if (papel === 'admin') return 'dono';
  if (papel === 'usuario') return 'visualizador';
  return papel;
}

export function rotuloPapel(papel: Papel): string {
  const rotulos: Record<Exclude<Papel, 'admin' | 'usuario'>, string> = {
    dono: 'Dono',
    gerente: 'Gerente',
    visualizador: 'Visualizador',
  };

  return rotulos[papelNormalizado(papel)];
}
