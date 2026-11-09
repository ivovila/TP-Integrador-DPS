import { describe, expect, test } from 'vitest';
import type { ContractContext } from './fixture';
import { verifier, certifier, no } from './fixture';




export function availableActionsContract(context: ContractContext): void {
    describe('available actions', () => {
      test('an asset offers an inspection only when its type has a schema', async () => {
        const assetBeforeSchema = await context.registerExtinguisher();
        await context.createExtinguisherSchema();
        const assetAfterSchema = await context.certiflow.catalog.findAsset(context.extinguisherCode);
        expect(assetBeforeSchema.actions).toEqual([]);
        expect(assetAfterSchema.actions).toEqual(['assignInspection']);
      });

      test('an open inspection offers execution and closure; a closed one offers rectification and its act', async () => {
        const inspectionId = await context.assetWithSchemaAndOpenInspection();
        const openInspection = await context.certiflow.inspections.findInspection(inspectionId);
        await context.answerEverything(inspectionId);
        await context.close(inspectionId);
        const closedOne = await context.certiflow.inspections.findInspection(inspectionId);
        expect(openInspection.actions).toEqual([
          'recordAnswer',
          'attachEvidence',
          'addObservation',
          'closeInspection',
          'requestAssistantReview',
        ]);
        expect(closedOne.actions).toEqual(['rectifyInspection', 'generateInspectionAct']);
      });

      test('issuing a certificate is offered only by a favourable eligibility decision', async () => {
        const { inspectionId } = await context.assetWithClosedInspection({ SEAL: no });
        const refused = await context.certiflow.certification.evaluateCertification(inspectionId);
        const { inspectionId: cleanInspectionId } = await (async () => {
          await context.registerExtinguisher(context.unique('EXT-0099'));
          return context.closedInspection({}, context.unique('EXT-0099'));
        })();
        const favourable = await context.certiflow.certification.evaluateCertification(cleanInspectionId);
        expect(refused.actions).toEqual([]);
        expect(favourable.actions).toEqual(['issueCertificate']);
      });

      test('findings and corrective actions offer their next step only while they are open', async () => {
        const { findingIds } = await context.assetWithClosedInspection({ SEAL: no });
        await context.planAction(findingIds[0]!);
        const openFinding = await context.certiflow.findings.findFinding(findingIds[0]!);
        await context.certiflow.findings.verifyCorrectiveAction({
          findingId: findingIds[0]!,
          actionId: 1,
          verifier,
          result: 'ACCEPTED',
          notes: '',
        });
        const closedFinding = await context.certiflow.findings.findFinding(findingIds[0]!);
        expect(openFinding.actions).toEqual(['planCorrectiveAction']);
        expect(openFinding.correctiveActions[0]!.actions).toEqual(['verifyCorrectiveAction']);
        expect(closedFinding.actions).toEqual([]);
        expect(closedFinding.correctiveActions[0]!.actions).toEqual([]);
      });

      test('a certificate offers suspension while active and renewal until it is renewed', async () => {
        const { inspectionId } = await context.assetWithClosedInspection();
        const number = await context.issue(inspectionId);
        const active = await context.certiflow.certification.findCertificate(number);
        await context.certiflow.certification.suspendCertificate({ number, reason: 'Tampering', suspendedBy: certifier });
        const suspended = await context.certiflow.certification.findCertificate(number);
        const { inspectionId: laterInspectionId } = await context.closedInspection();
        await context.certiflow.certification.renewCertificate({
          currentNumber: number,
          inspectionId: laterInspectionId,
          renewedBy: certifier,
        });
        const renewed = await context.certiflow.certification.findCertificate(number);
        expect(active.actions).toEqual(['suspendCertificate', 'renewCertificate']);
        expect(suspended.actions).toEqual(['renewCertificate']);
        expect(renewed.actions).toEqual([]);
      });
    });

}
