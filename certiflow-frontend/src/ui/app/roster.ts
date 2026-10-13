import type { Person } from '../../domain/shared';

export interface RosterMember {
  person: Person;
  role: string;
}

export const demoPeople = {
  coordinator: { id: 'EMP-001', name: 'Paula Rios' },
  inspector: { id: 'EMP-002', name: 'Ines Duarte' },
  assetResponsible: { id: 'EMP-003', name: 'Raul Mendez' },
  verifier: { id: 'EMP-004', name: 'Vera Soto' },
  certifier: { id: 'EMP-005', name: 'Carlos Vidal' },
} satisfies Record<string, Person>;

export const roster: RosterMember[] = [
  { person: demoPeople.coordinator, role: 'Inspection coordinator' },
  { person: demoPeople.inspector, role: 'Inspector' },
  { person: demoPeople.assetResponsible, role: 'Asset responsible' },
  { person: demoPeople.verifier, role: 'Verifier' },
  { person: demoPeople.certifier, role: 'Certification officer' },
];
