import type { Certiflow } from '../../ports/Certiflow';
import { type FetchFunction, HalClient } from './HalClient';
import { createHttpAuditPort } from './HttpAuditPort';
import { createHttpCatalogPort } from './HttpCatalogPort';
import { createHttpCertificationPort } from './HttpCertificationPort';
import { createHttpFindingPort } from './HttpFindingPort';
import { createHttpInspectionPort } from './HttpInspectionPort';
import { createHttpSchemaPort } from './HttpSchemaPort';

export function createHttpCertiflow(baseUrl: string, fetchFunction?: FetchFunction): Certiflow {
  const client = new HalClient(baseUrl, fetchFunction);
  const schemas = createHttpSchemaPort(client);
  return {
    catalog: createHttpCatalogPort(client, schemas),
    schemas,
    inspections: createHttpInspectionPort(client, schemas),
    findings: createHttpFindingPort(client, schemas),
    certification: createHttpCertificationPort(client, schemas),
    audit: createHttpAuditPort(client),
  };
}
