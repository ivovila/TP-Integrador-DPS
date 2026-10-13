import type { AuditPort } from './AuditPort';
import type { CatalogPort } from './CatalogPort';
import type { CertificationPort } from './CertificationPort';
import type { FindingPort } from './FindingPort';
import type { InspectionPort } from './InspectionPort';
import type { SchemaPort } from './SchemaPort';

export interface Certiflow {
  catalog: CatalogPort;
  schemas: SchemaPort;
  inspections: InspectionPort;
  findings: FindingPort;
  certification: CertificationPort;
  audit: AuditPort;
}
