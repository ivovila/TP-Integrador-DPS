import type { AssetAction } from '../../domain/catalog';
import type { CertificateAction, CertificationDecisionAction } from '../../domain/certification';
import type { CorrectiveActionAction, FindingAction } from '../../domain/finding';
import type { InspectionAction } from '../../domain/inspection';
import type { SchemaAction } from '../../domain/schema';
import type { StaffRole } from '../../ports/AuthenticationPort';

export type Role =
  | 'administrator'
  | 'inspectionCoordinator'
  | 'inspector'
  | 'assetResponsible'
  | 'verifier'
  | 'certificationOfficer';

export type Section = 'assets' | 'schemas' | 'inspections' | 'findings' | 'certificates';

export type SystemAction =
  | AssetAction
  | SchemaAction
  | InspectionAction
  | FindingAction
  | CorrectiveActionAction
  | CertificationDecisionAction
  | CertificateAction
  | 'registerAsset'
  | 'createInspectionSchema';

export type ReadingAction = 'generateInspectionAct';
export type RoleAction = Exclude<SystemAction, ReadingAction>;

interface RoleDefinition {
  label: string;
  sections: Section[];
}

export const roles: Record<Role, RoleDefinition> = {
  administrator: { label: 'Administrator', sections: ['assets', 'schemas'] },
  inspectionCoordinator: { label: 'Inspection coordinator', sections: ['inspections', 'assets'] },
  inspector: { label: 'Inspector', sections: ['inspections'] },
  assetResponsible: { label: 'Asset responsible', sections: ['findings', 'assets'] },
  verifier: { label: 'Verifier', sections: ['findings'] },
  certificationOfficer: { label: 'Certification officer', sections: ['certificates', 'inspections', 'assets'] },
};

export const roleThatMayDo: Record<RoleAction, Role> = {
  registerAsset: 'administrator',
  createInspectionSchema: 'administrator',
  publishSchemaVersion: 'administrator',
  assignInspection: 'inspectionCoordinator',
  recordAnswer: 'inspector',
  attachEvidence: 'inspector',
  addObservation: 'inspector',
  closeInspection: 'inspector',
  rectifyInspection: 'inspector',
  planCorrectiveAction: 'assetResponsible',
  verifyCorrectiveAction: 'verifier',
  issueCertificate: 'certificationOfficer',
  issueGlobalCertificate: 'certificationOfficer',
  suspendCertificate: 'certificationOfficer',
  renewCertificate: 'certificationOfficer',
  renewGlobalCertificate: 'certificationOfficer',
};

export const sections: Record<Section, { label: string; path: string }> = {
  assets: { label: 'Assets', path: '/assets' },
  schemas: { label: 'Schemas', path: '/schemas' },
  inspections: { label: 'Inspections', path: '/inspections' },
  findings: { label: 'Findings', path: '/findings' },
  certificates: { label: 'Certificates', path: '/certificates' },
};

export const everyRole = Object.keys(roles) as Role[];

export const staffRoleOf: Record<Role, StaffRole> = {
  administrator: 'ADMINISTRATOR',
  inspectionCoordinator: 'INSPECTION_COORDINATOR',
  inspector: 'INSPECTOR',
  assetResponsible: 'ASSET_RESPONSIBLE',
  verifier: 'VERIFIER',
  certificationOfficer: 'CERTIFICATION_OFFICER',
};

export function roleOf(staffRole: StaffRole): Role {
  return everyRole.find((role) => staffRoleOf[role] === staffRole)!;
}

function isRoleAction(action: SystemAction): action is RoleAction {
  return action in roleThatMayDo;
}

export function roleMayDo(role: Role, action: SystemAction): boolean {
  return !isRoleAction(action) || roleThatMayDo[action] === role;
}

export function actionsOf(role: Role): RoleAction[] {
  return (Object.keys(roleThatMayDo) as RoleAction[]).filter((action) => roleThatMayDo[action] === role);
}

export function actionsReservedToARole(actions: SystemAction[]): RoleAction[] {
  return actions.filter(isRoleAction);
}

export function homeOf(role: Role): string {
  return sections[roles[role].sections[0]!].path;
}
