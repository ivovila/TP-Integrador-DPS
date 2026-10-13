import type { Person } from './shared';

export type AssetAction = 'assignInspection' | 'issueGlobalCertificate';

export interface Characteristic {
  name: string;
  value: string;
}

export interface Subsystem {
  code: string;
  name: string;
}

export interface Asset {
  code: string;
  name: string;
  type: string;
  location: string;
  responsible: Person;
  characteristics: Characteristic[];
  jurisdictionCode: string;
  subsystems: Subsystem[];
  actions: AssetAction[];
}
