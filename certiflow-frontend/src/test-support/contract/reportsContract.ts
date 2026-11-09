import { describe, expect, test } from 'vitest';
import type { ContractContext } from './fixture';
import { inspector, verifier, today, lastDayOfAYear, no } from './fixture';




export function reportsContract(context: ContractContext): void {
    describe('reports', () => {
      test('the inspection act lists every answer with its outcome, the revisions and the history', async () => {
        const { inspectionId } = await context.assetWithClosedInspection({ SIGNAGE: no });
        const act = await context.certiflow.inspections.generateInspectionAct(inspectionId);
        expect(act).toMatchObject({ assetCode: context.extinguisherCode, assetName: 'Lobby extinguisher', schemaVersion: 1 });
        expect(act.lines.map((line) => [line.criterionCode, line.outcome])).toEqual([
          ['PRESSURE', 'APPROVED'],
          ['SEAL', 'APPROVED'],
          ['SIGNAGE', 'OBSERVED'],
          ['HOSE', 'APPROVED'],
        ]);
        expect(act.lines[0]).toMatchObject({ evidenceCount: 1, answeredBy: inspector });
        expect(act.revisions).toHaveLength(1);
        expect(act.history.at(0)!.action).toBe('ASSIGNED');
        expect(act.history.at(-1)!.action).toBe('CLOSED');
      });

      test('an act is only issued for a closed inspection', async () => {
        const inspectionId = await context.assetWithSchemaAndOpenInspection();
        await context.expectRejection(context.certiflow.inspections.generateInspectionAct(inspectionId), 'InspectionNotClosedException');
      });

      test('the findings summary of an asset lists each finding with its severity and counts the open ones', async () => {
        const { findingIds } = await context.assetWithClosedInspection({ SEAL: no, SIGNAGE: no });
        await context.planAction(findingIds[0]!);
        const summary = await context.certiflow.findings.generateFindingsSummary(context.extinguisherCode);
        expect(summary).toMatchObject({ assetCode: context.extinguisherCode, openFindings: 2 });
        expect(summary.findings).toMatchObject([
          { findingId: findingIds[0], criterionCode: 'SEAL', severity: 'Major', overdueActions: 0 },
          { findingId: findingIds[1], criterionCode: 'SIGNAGE', severity: 'Minor', overdueActions: 0 },
        ]);
      });

      test('the certificate document states the status on the day it is consulted', async () => {
        const { inspectionId } = await context.assetWithClosedInspection();
        const number = await context.issue(inspectionId);
        const document = await context.certiflow.certification.generateCertificateDocument(number);
        expect(document).toMatchObject({
          number,
          assetName: 'Lobby extinguisher',
          validFrom: today,
          validUntil: lastDayOfAYear,
          modality: 'APPROVED',
          decision: { policyCode: 'STANDARD' },
          statusDate: today,
          status: 'ACTIVE',
        });
      });
    });

    test('the whole flow: asset, schema, inspection, closure, finding, corrective action and certificate', async () => {
      await context.registerExtinguisher();
      await context.createExtinguisherSchema();
      const inspectionId = await context.assignInspection();
      await context.answerEverything(inspectionId, { SEAL: no });
      const [findingId] = await context.close(inspectionId);
      const refusal = await context.certiflow.certification.evaluateCertification(inspectionId);
      const actionId = await context.planAction(findingId!);
      await context.certiflow.findings.verifyCorrectiveAction({ findingId: findingId!, actionId, verifier, result: 'ACCEPTED', notes: '' });
      const approval = await context.certiflow.certification.evaluateCertification(inspectionId);
      const number = await context.issue(inspectionId);
      const inspectionHistory = await context.certiflow.audit.auditHistory({ kind: 'inspection', id: inspectionId });

      expect(refusal.eligible).toBe(false);
      expect(approval.eligible).toBe(true);
      expect((await context.certiflow.certification.findCertificate(number)).status).toBe('ACTIVE');
      expect(inspectionHistory.map((entry) => entry.action)).toEqual([
        'ASSIGNED',
        'ANSWER_RECORDED',
        'ANSWER_RECORDED',
        'ANSWER_RECORDED',
        'ANSWER_RECORDED',
        'EVIDENCE_ATTACHED',
        'CLOSED',
      ]);
    });
}
