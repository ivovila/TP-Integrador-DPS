import { describe, expect, test } from 'vitest';
import type { ContractContext } from './fixture';
import { inspector, yes, no, measured, chosen } from './fixture';
import type { Outcome, RawAnswer } from '../../domain/schema';



export function executionAndEvaluationContract(context: ContractContext): void {
    describe('execution and evaluation', () => {
      const evaluations: [string, string, RawAnswer, boolean, Outcome][] = [
        ['a measurement inside the range with its photo is approved', 'PRESSURE', measured('12.5'), true, 'APPROVED'],
        ['the lower limit of the range is approved', 'PRESSURE', measured('10'), true, 'APPROVED'],
        ['the upper limit of the range is approved', 'PRESSURE', measured('16'), true, 'APPROVED'],
        ['a critical criterion that is not met is rejected', 'PRESSURE', measured('16.01'), true, 'REJECTED'],
        ['an answer without its mandatory photo stays pending', 'PRESSURE', measured('12.5'), false, 'PENDING'],
        ['a major criterion that is not met is rejected', 'SEAL', no, false, 'REJECTED'],
        ['a minor criterion that is not met is observed', 'SIGNAGE', no, false, 'OBSERVED'],
        ['an accepted option is approved', 'HOSE', chosen('Acceptable'), false, 'APPROVED'],
        ['an option outside the list is observed when the criterion is minor', 'HOSE', chosen('Cracked'), false, 'OBSERVED'],
      ];

      test.each(evaluations)('%s', async (_description, criterionCode, rawAnswer, withPhoto, expectedOutcome) => {
        const inspectionId = await context.assetWithSchemaAndOpenInspection();
        await context.answer(inspectionId, criterionCode, rawAnswer);
        if (withPhoto) {
          await context.attachPhoto(inspectionId, criterionCode);
        }
        expect(await context.outcomeOf(inspectionId, criterionCode)).toBe(expectedOutcome);
      });

      test('a criterion without an answer is pending', async () => {
        const inspectionId = await context.assetWithSchemaAndOpenInspection();
        expect(await context.outcomeOf(inspectionId, 'SEAL')).toBe('PENDING');
      });

      test('an answer of the wrong type for the criterion is rejected', async () => {
        const inspectionId = await context.assetWithSchemaAndOpenInspection();
        await context.expectRejection(context.answer(inspectionId, 'PRESSURE', chosen('high')), 'AnswerTypeMismatchException');
        await context.expectRejection(context.answer(inspectionId, 'SEAL', measured('5')), 'AnswerTypeMismatchException');
      });

      test('a criterion that is not in the captured version cannot be answered', async () => {
        const inspectionId = await context.assetWithSchemaAndOpenInspection();
        await context.expectRejection(context.answer(inspectionId, 'UNKNOWN', yes), 'CriterionNotInSchemaException');
      });

      test('the same file cannot back a criterion twice', async () => {
        const inspectionId = await context.assetWithSchemaAndOpenInspection();
        await context.attachPhoto(inspectionId, 'PRESSURE', 'gauge.jpg');
        await context.expectRejection(context.attachPhoto(inspectionId, 'PRESSURE', 'gauge.jpg'), 'DuplicateEvidenceException');
      });

      test('answers, evidence and observations keep who registered them', async () => {
        const inspectionId = await context.assetWithSchemaAndOpenInspection();
        await context.answer(inspectionId, 'SEAL', yes);
        await context.attachPhoto(inspectionId, 'SEAL', 'seal.jpg');
        await context.certiflow.inspections.addObservation({
          inspectionId,
          criterionCode: 'SEAL',
          text: 'Seal replaced last month',
          observedBy: inspector,
        });
        const inspection = await context.certiflow.inspections.findInspection(inspectionId);
        const sealResponse = inspection.responses.find((response) => response.criterion.code === 'SEAL')!;
        expect(sealResponse.given).toMatchObject({ value: yes, answeredBy: inspector });
        expect(sealResponse.attachments).toMatchObject([{ evidence: { reference: 'seal.jpg' }, attachedBy: inspector }]);
        expect(sealResponse.observations).toMatchObject([{ text: 'Seal replaced last month', observedBy: inspector }]);
      });

      test('an inspection with pending criteria cannot be closed and stays untouched', async () => {
        const inspectionId = await context.assetWithSchemaAndOpenInspection();
        await context.answer(inspectionId, 'SEAL', yes);
        const historyBefore = await context.certiflow.audit.auditHistory({ kind: 'inspection', id: inspectionId });
        await context.expectRejection(context.close(inspectionId), 'InspectionIncompleteException');
        const inspection = await context.certiflow.inspections.findInspection(inspectionId);
        const historyAfter = await context.certiflow.audit.auditHistory({ kind: 'inspection', id: inspectionId });
        expect(inspection.closed).toBe(false);
        expect(historyAfter).toEqual(historyBefore);
      });

      test('a closed inspection cannot be altered freely', async () => {
        const { inspectionId } = await context.assetWithClosedInspection();
        await context.expectRejection(context.answer(inspectionId, 'SEAL', no), 'InspectionAlreadyClosedException');
        await context.expectRejection(context.attachPhoto(inspectionId, 'SEAL', 'late.jpg'), 'InspectionAlreadyClosedException');
        await context.expectRejection(context.close(inspectionId), 'InspectionAlreadyClosedException');
      });
    });

}
