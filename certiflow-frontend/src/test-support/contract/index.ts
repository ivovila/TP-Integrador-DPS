import { describe } from 'vitest';
import { contractContext, type SystemUnderTest } from './fixture';
export type { SystemUnderTest } from './fixture';
import { assetCatalogueContract } from './assetCatalogueContract';
import { inspectionSchemasAndVersionsContract } from './inspectionSchemasAndVersionsContract';
import { inspectionAssignmentContract } from './inspectionAssignmentContract';
import { executionAndEvaluationContract } from './executionAndEvaluationContract';
import { findingsContract } from './findingsContract';
import { rectificationContract } from './rectificationContract';
import { correctiveActionsContract } from './correctiveActionsContract';
import { certificationContract } from './certificationContract';
import { jurisdictionPoliciesContract } from './jurisdictionPoliciesContract';
import { scheduledSchemaVersionsContract } from './scheduledSchemaVersionsContract';
import { partialCertificatesBySubsystemContract } from './partialCertificatesBySubsystemContract';
import { assistedReviewContract } from './assistedReviewContract';
import { availableActionsContract } from './availableActionsContract';
import { reportsContract } from './reportsContract';

export function describeCertiflowContract(adapterName: string, createSystem: () => SystemUnderTest | Promise<SystemUnderTest>): void {
  describe(`Certiflow contract · ${adapterName}`, () => {
    const context = contractContext(createSystem);
    assetCatalogueContract(context);
    inspectionSchemasAndVersionsContract(context);
    inspectionAssignmentContract(context);
    executionAndEvaluationContract(context);
    findingsContract(context);
    rectificationContract(context);
    correctiveActionsContract(context);
    certificationContract(context);
    jurisdictionPoliciesContract(context);
    scheduledSchemaVersionsContract(context);
    partialCertificatesBySubsystemContract(context);
    assistedReviewContract(context);
    availableActionsContract(context);
    reportsContract(context);
  });
}
