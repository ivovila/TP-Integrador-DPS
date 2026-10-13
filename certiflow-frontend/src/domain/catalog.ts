import type { Person } from './shared';

export type AssetAction = 'assignInspection';

export interface Characteristic {
  name: string;
  value: string;
}

export interface Asset {
  code: string;
  name: string;
  type: string;
  location: string;
  responsible: Person;
  characteristics: Characteristic[];
  actions: AssetAction[];
}
