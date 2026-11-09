import { describe, expect, test } from 'vitest';
import type { ContractContext } from './fixture';
import { inspector, yes, no, measured } from './fixture';
import type { RawAnswer } from '../../domain/schema';



export function rectificationContract(context: ContractContext): void {
    describe('rectification', () => {
      function rectify(inspectionId: string, criterionCode: string, rawAnswer: RawAnswer, reason = 'Typing mistake') {
        return context.certiflow.inspections.rectifyInspection({
          inspectionId,
          criterionCode,
          answer: rawAnswer,
          reason,
          rectifiedBy: inspector,
        });
      }

      test('a rectification adds a revision and leaves the original one as it was signed', async () => {
        const { inspectionId } = await context.assetWithClosedInspection({ SIGNAGE: no });
        await rectify(inspectionId, 'SIGNAGE', yes);
        const inspection = await context.certiflow.inspections.findInspection(inspectionId);
        const signageOutcomes = inspection.revisions.map(
          (revision) => revision.outcomes.find((outcome) => outcome.criterionCode === 'SIGNAGE')!.outcome,
        );
        expect(inspection.revisions.map((revision) => revision.number)).toEqual([1, 2]);
        expect(inspection.revisions[1]).toMatchObject({ reason: 'Typing mistake', sealedBy: inspector });
        expect(signageOutcomes).toEqual(['OBSERVED', 'APPROVED']);
      });

      test('a rectification without a reason is rejected and changes nothing', async () => {
        const { inspectionId } = await context.assetWithClosedInspection();
        await context.expectRejection(rectify(inspectionId, 'SEAL', no, '  '), 'RectificationReasonRequiredException');
        const inspection = await context.certiflow.inspections.findInspection(inspectionId);
        expect(inspection.revisions).toHaveLength(1);
        expect(await context.outcomeOf(inspectionId, 'SEAL')).toBe('APPROVED');
      });

      test('an open inspection is corrected by answering again, not by rectifying', async () => {
        const inspectionId = await context.assetWithSchemaAndOpenInspection();
        await context.expectRejection(rectify(inspectionId, 'SEAL', no), 'InspectionNotClosedException');
      });

      test('a rectification that introduces a non-conformity raises its finding', async () => {
        const { inspectionId } = await context.assetWithClosedInspection();
        const raised = await rectify(inspectionId, 'SEAL', no);
        const findings = await context.certiflow.findings.listFindings({ assetCode: context.extinguisherCode });
        expect(raised).toHaveLength(1);
        expect(findings).toMatchObject([{ criterion: { code: 'SEAL' }, open: true }]);
      });

      test('a rectification does not duplicate a finding that is still open for the same criterion', async () => {
        const { inspectionId } = await context.assetWithClosedInspection({ PRESSURE: measured('20') });
        const raised = await rectify(inspectionId, 'PRESSURE', measured('21'));
        expect(raised).toEqual([]);
        expect(await context.certiflow.findings.listFindings({ assetCode: context.extinguisherCode })).toHaveLength(1);
      });
    });

}
