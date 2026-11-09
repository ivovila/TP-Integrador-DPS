import { describe, expect, test } from 'vitest';
import type { ContractContext } from './fixture';
import { verifier, certifier, today, lastDayOfAYear, no } from './fixture';




export function certificationContract(context: ContractContext): void {
    describe('certification', () => {
      test('an open or partial inspection is rejected before the jurisdiction policy is consulted', async () => {
        await context.registerExtinguisher();
        await context.createExtinguisherSchema();
        await context.closedInspection({ SEAL: no });
        const partialInspectionId = await context.assignInspection(context.extinguisherCode, ['Installation']);
        const decision = await context.certiflow.certification.evaluateCertification(partialInspectionId);
        expect(decision).toMatchObject({ eligible: false, outcome: 'REJECTED', policyCode: 'STANDARD' });
        expect(decision.ineligibilities.map((ineligibility) => ineligibility.reason)).toEqual([
          'INSPECTION_NOT_CLOSED',
          'PARTIAL_INSPECTION',
        ]);
      });

      test('minor findings do not prevent certification under the standard policy', async () => {
        const { inspectionId } = await context.assetWithClosedInspection({ SIGNAGE: no });
        const decision = await context.certiflow.certification.evaluateCertification(inspectionId);
        expect(decision).toMatchObject({ eligible: true, outcome: 'APPROVED', ineligibilities: [] });
      });

      test('an asset becomes certifiable again once its blocking findings are closed', async () => {
        const { inspectionId, findingIds } = await context.assetWithClosedInspection({ SEAL: no });
        await context.expectRejection(context.issue(inspectionId), 'NotEligibleForCertificationException');
        await context.planAction(findingIds[0]!);
        await context.certiflow.findings.verifyCorrectiveAction({
          findingId: findingIds[0]!,
          actionId: 1,
          verifier,
          result: 'ACCEPTED',
          notes: '',
        });
        const number = await context.issue(inspectionId);
        expect(await context.certiflow.certification.findCertificate(number)).toMatchObject({ status: 'ACTIVE' });
      });

      test('a refused certificate explains every reason', async () => {
        const { inspectionId } = await context.assetWithClosedInspection({ SEAL: no });
        await expect(context.issue(inspectionId)).rejects.toMatchObject({
          name: 'NotEligibleForCertificationException',
          ineligibilities: [{ reason: 'BLOCKING_FINDINGS' }],
        });
      });

      test('a certificate is valid from the day it is issued for the term its policy grants, and records who issued it', async () => {
        const { inspectionId } = await context.assetWithClosedInspection();
        const number = await context.issue(inspectionId);
        const certificate = await context.certiflow.certification.findCertificate(number);
        const history = await context.certiflow.audit.auditHistory({ kind: 'certificate', number });
        expect(certificate).toMatchObject({
          assetCode: context.extinguisherCode,
          scope: { kind: 'wholeAsset' },
          basis: { kind: 'inspection', inspectionId },
          validFrom: today,
          validUntil: lastDayOfAYear,
          modality: 'APPROVED',
          decision: { policyCode: 'STANDARD', jurisdictionCode: 'DEMO-STANDARD' },
          issuedBy: certifier,
          status: 'ACTIVE',
        });
        expect(history).toMatchObject([{ action: 'ISSUED', performedBy: certifier }]);
      });

      test('an asset holds at most one current certificate', async () => {
        const { inspectionId } = await context.assetWithClosedInspection();
        await context.issue(inspectionId);
        await context.expectRejection(context.issue(inspectionId), 'AssetAlreadyCertifiedException');
      });

      function suspend(number: string, reason = 'Tampering reported') {
        return context.certiflow.certification.suspendCertificate({ number, reason, suspendedBy: certifier });
      }

      test('a suspended certificate keeps its reason and who suspended it', async () => {
        const { inspectionId } = await context.assetWithClosedInspection();
        const number = await context.issue(inspectionId);
        await suspend(number);
        const certificate = await context.certiflow.certification.findCertificate(number);
        expect(certificate).toMatchObject({
          status: 'SUSPENDED',
          suspensions: [{ reason: 'Tampering reported', suspendedBy: certifier }],
        });
      });

      test('only an active certificate can be suspended', async () => {
        const { inspectionId } = await context.assetWithClosedInspection();
        const number = await context.issue(inspectionId);
        await suspend(number);
        await context.expectRejection(suspend(number), 'CertificateNotActiveException');
      });

      test('a suspended certificate still prevents issuing another one', async () => {
        const { inspectionId } = await context.assetWithClosedInspection();
        const number = await context.issue(inspectionId);
        await suspend(number);
        await context.expectRejection(context.issue(inspectionId), 'AssetAlreadyCertifiedException');
      });

      function renew(currentNumber: string, inspectionId: string) {
        return context.certiflow.certification.renewCertificate({ currentNumber, inspectionId, renewedBy: certifier });
      }

      test('renewing issues a new certificate and retires the previous one', async () => {
        const { inspectionId } = await context.assetWithClosedInspection();
        const number = await context.issue(inspectionId);
        const { inspectionId: laterInspectionId } = await context.closedInspection();
        const renewedNumber = await renew(number, laterInspectionId);
        const previous = await context.certiflow.certification.findCertificate(number);
        const renewed = await context.certiflow.certification.findCertificate(renewedNumber);
        const previousHistory = await context.certiflow.audit.auditHistory({ kind: 'certificate', number });
        expect(previous.status).toBe('RENEWED');
        expect(renewed).toMatchObject({
          status: 'ACTIVE',
          basis: { kind: 'inspection', inspectionId: laterInspectionId },
          validFrom: today,
          validUntil: lastDayOfAYear,
        });
        expect(previousHistory.map((entry) => entry.action)).toEqual(['ISSUED', 'RENEWED']);
      });

      test('a renewal needs an inspection closed after the one behind the current certificate', async () => {
        const { inspectionId } = await context.assetWithClosedInspection();
        const number = await context.issue(inspectionId);
        await context.expectRejection(renew(number, inspectionId), 'RenewalRequiresLaterInspectionException');
      });

      test('a renewal needs an inspection of the same asset', async () => {
        const { inspectionId } = await context.assetWithClosedInspection();
        const number = await context.issue(inspectionId);
        await context.registerExtinguisher(context.unique('EXT-0099'));
        const { inspectionId: otherAssetInspectionId } = await context.closedInspection({}, context.unique('EXT-0099'));
        await context.expectRejection(renew(number, otherAssetInspectionId), 'ReplacementForAnotherAssetException');
      });

      test('a certificate can only be renewed once', async () => {
        const { inspectionId } = await context.assetWithClosedInspection();
        const number = await context.issue(inspectionId);
        const { inspectionId: laterInspectionId } = await context.closedInspection();
        await renew(number, laterInspectionId);
        const { inspectionId: latestInspectionId } = await context.closedInspection();
        await context.expectRejection(renew(number, latestInspectionId), 'CertificateAlreadyRenewedException');
      });

      test('certificates can be listed by asset', async () => {
        const { inspectionId } = await context.assetWithClosedInspection();
        await context.issue(inspectionId);
        expect(await context.certiflow.certification.listCertificates({ assetCode: context.extinguisherCode })).toHaveLength(1);
        expect(await context.certiflow.certification.listCertificates({ assetCode: 'OTHER' })).toEqual([]);
      });
    });

}
