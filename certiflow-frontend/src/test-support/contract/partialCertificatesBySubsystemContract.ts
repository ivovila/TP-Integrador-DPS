import { describe, expect, test } from 'vitest';
import type { ContractContext } from './fixture';
import { administrator, assetResponsible, certifier, today, lastDayOfSixMonths, lastDayOfThirtyDays, yes, no, measured, chosen } from './fixture';
import type { RawAnswer, Section } from '../../domain/schema';



export function partialCertificatesBySubsystemContract(context: ContractContext): void {
    describe('partial certificates by subsystem', () => {
      const boilerType = () => context.unique('Pressure vessel');
      const boilerSubsystems = [
        { code: 'ELECTRICAL', name: 'Electrical system' },
        { code: 'PRESSURE', name: 'Pressure system' },
      ];
      const compliantBoilerAnswers: Record<string, RawAnswer> = {
        RELIEF: measured('8.0'),
        INSULATION: chosen('Good'),
        GROUNDING: yes,
        PILOT: yes,
      };

      function boilerSections(): Section[] {
        const severityNamed = (name: string) => context.vocabulary.severities.find((severity) => severity.name === name)!;
        return [
          {
            title: 'Pressure',
            subsystemCode: 'PRESSURE',
            criteria: [
              {
                code: 'RELIEF',
                text: 'Relief valve opening pressure',
                severity: severityNamed('CRITICAL'),
                rule: { type: 'numeric-range', unit: 'bar', minimum: '7.5', maximum: '8.5' },
                requiredEvidence: [],
              },
              {
                code: 'INSULATION',
                text: 'Insulation condition',
                severity: severityNamed('MINOR'),
                rule: { type: 'option-in-list', acceptedOptions: ['Good'] },
                requiredEvidence: [],
              },
            ],
          },
          {
            title: 'Electrical',
            subsystemCode: 'ELECTRICAL',
            criteria: [
              {
                code: 'GROUNDING',
                text: 'Burner grounding verified',
                severity: severityNamed('MAJOR'),
                rule: { type: 'yes-no', expected: 'YES' },
                requiredEvidence: [],
              },
              {
                code: 'PILOT',
                text: 'Pressure pilot light works',
                severity: severityNamed('MINOR'),
                rule: { type: 'yes-no', expected: 'YES' },
                requiredEvidence: [],
                subsystemCode: 'PRESSURE',
              },
            ],
          },
        ];
      }

      function registerBoiler(code = context.unique('BLR-0007'), jurisdictionCode = 'DEMO-CONDITIONAL', subsystems = boilerSubsystems) {
        return context.certiflow.catalog.registerAsset({
          code,
          name: 'Boiler',
          type: boilerType(),
          location: 'Basement',
          responsible: assetResponsible,
          characteristics: [],
          jurisdictionCode,
          subsystems,
          registeredBy: administrator,
        });
      }

      function createBoilerSchema(sections = boilerSections(), requiredSubsystems = ['ELECTRICAL', 'PRESSURE']) {
        return context.certiflow.schemas.createInspectionSchema({
          name: 'Boiler inspection',
          assetType: boilerType(),
          sections,
          requiredSubsystems,
          createdBy: administrator,
        });
      }

      async function closedBoilerInspection(overrides: Record<string, RawAnswer> = {}, assetCode = context.unique('BLR-0007'), sectionTitles?: string[]) {
        const inspectionId = await context.assignInspection(assetCode, sectionTitles);
        const inspection = await context.certiflow.inspections.findInspection(inspectionId);
        const answers = { ...compliantBoilerAnswers, ...overrides };
        for (const response of inspection.responses) {
          await context.answer(inspectionId, response.criterion.code, answers[response.criterion.code]!);
        }
        const findingIds = await context.close(inspectionId);
        return { inspectionId, findingIds };
      }

      async function boilerWithClosedInspection(overrides: Record<string, RawAnswer> = {}, jurisdictionCode = 'DEMO-CONDITIONAL') {
        await registerBoiler(context.unique('BLR-0007'), jurisdictionCode);
        await createBoilerSchema();
        return closedBoilerInspection(overrides);
      }

      async function certifyBothSubsystems(inspectionId: string) {
        return {
          electrical: await context.issue(inspectionId, 'ELECTRICAL'),
          pressure: await context.issue(inspectionId, 'PRESSURE'),
        };
      }

      function issueGlobal(partialCertificateNumbers: string[], assetCode = context.unique('BLR-0007')) {
        return context.certiflow.certification.issueGlobalCertificate({
          assetCode,
          schemaVersion: 1,
          partialCertificateNumbers,
          issuedBy: certifier,
        });
      }

      test('every criterion of a schema with subsystems resolves to exactly one of them', async () => {
        const withoutSubsystem = boilerSections().map((section) => ({ ...section, subsystemCode: undefined }));
        await context.expectRejection(createBoilerSchema(withoutSubsystem), 'InvalidSchemaException');
        await context.expectRejection(createBoilerSchema(boilerSections(), ['ELECTRICAL', 'PRESSURE', 'FUEL']), 'InvalidSchemaException');
      });

      test('an asset that lacks a required subsystem cannot be inspected with that schema', async () => {
        await registerBoiler(context.unique('BLR-0007'), 'DEMO-CONDITIONAL', [boilerSubsystems[0]!]);
        await createBoilerSchema();
        await context.expectRejection(context.assignInspection(context.unique('BLR-0007')), 'AssetLacksRequiredSubsystemsException');
      });

      test('a finding belongs to the subsystem of its criterion, which overrides the one of its section', async () => {
        const { findingIds } = await boilerWithClosedInspection({ PILOT: no, GROUNDING: no });
        const findings = await Promise.all(findingIds.map((findingId) => context.certiflow.findings.findFinding(findingId)));
        expect(findings.map((finding) => [finding.criterion.code, finding.subsystemCode])).toEqual([
          ['GROUNDING', 'ELECTRICAL'],
          ['PILOT', 'PRESSURE'],
        ]);
      });

      test('a rejected subsystem does not prevent certifying another one', async () => {
        const { inspectionId } = await boilerWithClosedInspection({ RELIEF: measured('9.5') });
        const electrical = await context.certiflow.certification.evaluateCertification(inspectionId, 'ELECTRICAL');
        const pressure = await context.certiflow.certification.evaluateCertification(inspectionId, 'PRESSURE');
        expect(electrical).toMatchObject({ outcome: 'APPROVED', scope: { kind: 'subsystem', subsystemCode: 'ELECTRICAL' } });
        expect(pressure).toMatchObject({ outcome: 'REJECTED', ineligibilities: [{ reason: 'BLOCKING_FINDINGS' }] });
        const electricalNumber = await context.issue(inspectionId, 'ELECTRICAL');
        await context.expectRejection(context.issue(inspectionId, 'PRESSURE'), 'NotEligibleForCertificationException');
        await expect(issueGlobal([electricalNumber])).rejects.toMatchObject({
          name: 'NotEligibleForCertificationException',
          ineligibilities: [{ reason: 'MISSING_SUBSYSTEM', detail: 'Missing partial for PRESSURE' }],
        });
      });

      test('an inspection of some sections certifies only the subsystems it fully covers', async () => {
        await registerBoiler();
        await createBoilerSchema();
        const { inspectionId } = await closedBoilerInspection({}, context.unique('BLR-0007'), ['Electrical']);
        expect(await context.certiflow.certification.evaluateCertification(inspectionId, 'ELECTRICAL')).toMatchObject({
          outcome: 'APPROVED',
        });
        expect(await context.certiflow.certification.evaluateCertification(inspectionId, 'PRESSURE')).toMatchObject({
          outcome: 'REJECTED',
          ineligibilities: [{ reason: 'PARTIAL_INSPECTION' }],
        });
      });

      test('an asset split into subsystems is certified whole only through its partial certificates', async () => {
        const { inspectionId } = await boilerWithClosedInspection();
        expect(await context.certiflow.certification.evaluateCertification(inspectionId)).toMatchObject({
          eligible: false,
          outcome: 'REJECTED',
          scope: { kind: 'wholeAsset' },
          ineligibilities: [{ reason: 'REQUIRES_PARTIAL_CERTIFICATES' }],
        });
        await context.expectRejection(context.issue(inspectionId), 'NotEligibleForCertificationException');
        await context.expectRejection(context.certiflow.certification.evaluateCertification(inspectionId, 'FUEL'), 'SubsystemNotFoundException');
      });

      test('each subsystem holds its own current certificate', async () => {
        const { inspectionId } = await boilerWithClosedInspection();
        const { electrical, pressure } = await certifyBothSubsystems(inspectionId);
        await context.expectRejection(context.issue(inspectionId, 'ELECTRICAL'), 'AssetAlreadyCertifiedException');
        expect((await context.certiflow.certification.listCertificates({ assetCode: context.unique('BLR-0007'), subsystemCode: 'PRESSURE' })).map((c) => c.number)).toEqual([
          pressure,
        ]);
        expect(await context.certiflow.certification.findCertificate(electrical)).toMatchObject({
          scope: { kind: 'subsystem', subsystemCode: 'ELECTRICAL' },
          status: 'ACTIVE',
        });
      });

      test('the global certificate lasts no longer than its partials and inherits their conditions', async () => {
        const { inspectionId } = await boilerWithClosedInspection({ INSULATION: chosen('Damaged') });
        const { electrical, pressure } = await certifyBothSubsystems(inspectionId);
        const global = await context.certiflow.certification.findCertificate(await issueGlobal([electrical, pressure]));
        expect(await context.certiflow.certification.findCertificate(electrical)).toMatchObject({ modality: 'APPROVED', validUntil: lastDayOfSixMonths });
        expect(await context.certiflow.certification.findCertificate(pressure)).toMatchObject({ modality: 'CONDITIONAL', validUntil: lastDayOfThirtyDays });
        expect(global).toMatchObject({
          scope: { kind: 'wholeAsset' },
          basis: { kind: 'partialCertificates', partialCertificateNumbers: [electrical, pressure] },
          modality: 'CONDITIONAL',
          validFrom: today,
          validUntil: lastDayOfThirtyDays,
          decision: { policyCode: 'ALL-REQUIRED-SUBSYSTEMS' },
        });
        expect(global.decision.conditions.map((condition) => condition.criterionCode)).toEqual(['INSULATION']);
      });

      test('a global certificate is not valid while one of its partials is suspended', async () => {
        const { inspectionId } = await boilerWithClosedInspection();
        const { electrical, pressure } = await certifyBothSubsystems(inspectionId);
        const globalNumber = await issueGlobal([electrical, pressure]);
        await context.certiflow.certification.suspendCertificate({ number: pressure, reason: 'Valve recall', suspendedBy: certifier });
        expect(await context.certiflow.certification.findCertificate(globalNumber)).toMatchObject({
          status: 'DEPENDENCY_INVALID',
          actions: ['renewGlobalCertificate'],
        });
        expect((await context.certiflow.certification.generateCertificateDocument(globalNumber)).status).toBe('DEPENDENCY_INVALID');
      });

      test('a global certificate only accepts active partials of the same asset', async () => {
        const { inspectionId } = await boilerWithClosedInspection();
        const { electrical } = await certifyBothSubsystems(inspectionId);
        await registerBoiler(context.unique('BLR-0099'));
        const { inspectionId: otherInspectionId } = await closedBoilerInspection({}, context.unique('BLR-0099'));
        const otherPressure = await context.issue(otherInspectionId, 'PRESSURE');
        await expect(issueGlobal([electrical, otherPressure])).rejects.toMatchObject({
          name: 'NotEligibleForCertificationException',
          ineligibilities: [
            { reason: 'INVALID_PARTIAL_CERTIFICATE', detail: expect.stringContaining(otherPressure) },
            { reason: 'MISSING_SUBSYSTEM' },
          ],
        });
      });

      test('renewing a partial keeps its subsystem, and the global is renewed from the new partials', async () => {
        const { inspectionId } = await boilerWithClosedInspection();
        const { electrical, pressure } = await certifyBothSubsystems(inspectionId);
        const globalNumber = await issueGlobal([electrical, pressure]);
        const { inspectionId: laterInspectionId } = await closedBoilerInspection();
        const renewedElectrical = await context.certiflow.certification.renewCertificate({
          currentNumber: electrical,
          inspectionId: laterInspectionId,
          renewedBy: certifier,
        });
        expect(await context.certiflow.certification.findCertificate(globalNumber)).toMatchObject({ status: 'DEPENDENCY_INVALID' });
        const renewedGlobal = await context.certiflow.certification.renewGlobalCertificate({
          currentNumber: globalNumber,
          schemaVersion: 1,
          partialCertificateNumbers: [renewedElectrical, pressure],
          renewedBy: certifier,
        });
        expect(await context.certiflow.certification.findCertificate(renewedElectrical)).toMatchObject({
          scope: { kind: 'subsystem', subsystemCode: 'ELECTRICAL' },
        });
        expect((await context.certiflow.certification.findCertificate(globalNumber)).status).toBe('RENEWED');
        expect((await context.certiflow.certification.findCertificate(renewedGlobal)).status).toBe('ACTIVE');
      });

      test('an asset split into subsystems offers a global certificate', async () => {
        await registerBoiler();
        await createBoilerSchema();
        expect((await context.certiflow.catalog.findAsset(context.unique('BLR-0007'))).actions).toEqual(['assignInspection', 'issueGlobalCertificate']);
      });
    });

}
