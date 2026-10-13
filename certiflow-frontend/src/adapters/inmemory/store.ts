import type { Characteristic } from '../../domain/catalog';
import type { Suspension } from '../../domain/certification';
import type { Verification } from '../../domain/finding';
import type { AttachedEvidence, GivenAnswer, InspectionAssignment, Observation, Revision } from '../../domain/inspection';
import type { Criterion, SchemaVersion } from '../../domain/schema';
import type { Instant, LocalDate, Person } from '../../domain/shared';
import { InMemoryAuditLog } from './auditLog';
import type { ControllableClock } from './clock';

export interface StoredAsset {
  code: string;
  name: string;
  type: string;
  location: string;
  responsible: Person;
  characteristics: Characteristic[];
}

export interface StoredSchema {
  name: string;
  assetType: string;
  versions: SchemaVersion[];
}

export interface StoredResponse {
  sectionTitle: string;
  criterion: Criterion;
  given?: GivenAnswer;
  attachments: AttachedEvidence[];
  observations: Observation[];
}

export interface StoredInspection {
  id: string;
  assetCode: string;
  assetResponsible: Person;
  assignment: InspectionAssignment;
  schemaVersion: SchemaVersion;
  responses: StoredResponse[];
  revisions: Revision[];
}

export interface StoredCancellation {
  cancelledBy: Person;
  cancelledAt: Instant;
}

export interface StoredCorrectiveAction {
  id: number;
  description: string;
  responsible: Person;
  dueDate: LocalDate;
  plannedBy: Person;
  plannedAt: Instant;
  verifications: Verification[];
  cancellation?: StoredCancellation;
}

export interface StoredFinding {
  id: string;
  inspectionId: string;
  assetCode: string;
  criterion: Criterion;
  evidence: AttachedEvidence[];
  responsible: Person;
  correctiveActions: StoredCorrectiveAction[];
}

export type CertificateLifecycle = 'ISSUED' | 'SUSPENDED' | 'RENEWED';

export interface StoredCertificate {
  number: string;
  assetCode: string;
  inspectionId: string;
  inspectionClosedAt: Instant;
  validFrom: LocalDate;
  validUntil: LocalDate;
  issuedBy: Person;
  issuedAt: Instant;
  suspensions: Suspension[];
  lifecycle: CertificateLifecycle;
}

export interface InMemorySystem {
  clock: ControllableClock;
  auditLog: InMemoryAuditLog;
  assets: Map<string, StoredAsset>;
  schemas: Map<string, StoredSchema>;
  inspections: Map<string, StoredInspection>;
  findings: Map<string, StoredFinding>;
  certificates: Map<string, StoredCertificate>;
}

export function createInMemorySystem(clock: ControllableClock): InMemorySystem {
  return {
    clock,
    auditLog: new InMemoryAuditLog(),
    assets: new Map(),
    schemas: new Map(),
    inspections: new Map(),
    findings: new Map(),
    certificates: new Map(),
  };
}
