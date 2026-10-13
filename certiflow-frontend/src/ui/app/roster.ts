import type { Person } from '../../domain/shared';
import type { Role } from './roles';

export interface RosterMember {
  person: Person;
  role: Role;
}

export const demoPeople = {
  administrator: { id: 'EMP-006', name: 'Ana Torres' },
  coordinator: { id: 'EMP-001', name: 'Paula Rios' },
  inspector: { id: 'EMP-002', name: 'Ines Duarte' },
  assetResponsible: { id: 'EMP-003', name: 'Raul Mendez' },
  verifier: { id: 'EMP-004', name: 'Vera Soto' },
  certifier: { id: 'EMP-005', name: 'Carlos Vidal' },
} satisfies Record<string, Person>;

export const roster: RosterMember[] = [
  { person: demoPeople.administrator, role: 'administrator' },
  { person: demoPeople.coordinator, role: 'inspectionCoordinator' },
  { person: demoPeople.inspector, role: 'inspector' },
  { person: demoPeople.assetResponsible, role: 'assetResponsible' },
  { person: demoPeople.verifier, role: 'verifier' },
  { person: demoPeople.certifier, role: 'certificationOfficer' },
];
