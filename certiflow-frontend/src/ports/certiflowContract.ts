import { beforeEach, describe, expect, test } from 'vitest';
import type { Outcome, RawAnswer, Section, Vocabulary } from '../domain/schema';
import type { LocalDate, Person } from '../domain/shared';
import type { Certiflow } from './Certiflow';
import type { DomainErrorName } from './DomainError';

export interface SystemUnderTest {
  certiflow: Certiflow;
}

const administrator: Person = { id: 'EMP-006', name: 'Ana Torres' };
const coordinator: Person = { id: 'EMP-001', name: 'Paula Rios' };
const inspector: Person = { id: 'EMP-002', name: 'Ines Duarte' };
const assetResponsible: Person = { id: 'EMP-003', name: 'Raul Mendez' };
const verifier: Person = { id: 'EMP-004', name: 'Vera Soto' };
const certifier: Person = { id: 'EMP-005', name: 'Carlos Vidal' };

function plusDays(date: LocalDate, days: number): LocalDate {
  const shifted = new Date(`${date}T00:00:00Z`);
  shifted.setUTCDate(shifted.getUTCDate() + days);
  return shifted.toISOString().slice(0, 10);
}

function plusMonths(date: LocalDate, months: number): LocalDate {
  const [year, month, day] = date.split('-').map(Number) as [number, number, number];
  const monthsSinceYearZero = year * 12 + (month - 1) + months;
  const targetYear = Math.floor(monthsSinceYearZero / 12);
  const targetMonth = monthsSinceYearZero % 12;
  const lastDayOfTargetMonth = new Date(Date.UTC(targetYear, targetMonth + 1, 0)).getUTCDate();
  return new Date(Date.UTC(targetYear, targetMonth, Math.min(day, lastDayOfTargetMonth))).toISOString().slice(0, 10);
}

function startOfDayIn(days: number): string {
  return `${plusDays(today, days)}T00:00:00.000Z`;
}

function sameInstant(instant: string): string {
  return new Date(instant).toISOString();
}

const today: LocalDate = new Date().toISOString().slice(0, 10);
const yesterday = plusDays(today, -1);
const inEighteenDays = plusDays(today, 18);
const lastDayOfAYear = plusDays(plusMonths(today, 12), -1);
const lastDayOfSixMonths = plusDays(plusMonths(today, 6), -1);
const lastDayOfThirtyDays = plusDays(today, 29);

const yes: RawAnswer = { kind: 'yes-no', value: true };
const no: RawAnswer = { kind: 'yes-no', value: false };

function measured(value: string): RawAnswer {
  return { kind: 'number', value };
}

function chosen(value: string): RawAnswer {
  return { kind: 'option', value };
}

function extinguisherSections(vocabulary: Vocabulary): Section[] {
  const severityNamed = (name: string) => vocabulary.severities.find((severity) => severity.name === name)!;
  const photo = vocabulary.evidenceKinds.find((kind) => kind.name === 'PHOTO')!;
  return [
    {
      title: 'Physical condition',
      criteria: [
        {
          code: 'PRESSURE',
          text: 'Gauge pressure',
          severity: severityNamed('CRITICAL'),
          rule: { type: 'numeric-range', unit: 'bar', minimum: '10', maximum: '16' },
          requiredEvidence: [{ kind: photo, minimum: 1 }],
        },
        {
          code: 'SEAL',
          text: 'Safety seal intact',
          severity: severityNamed('MAJOR'),
          rule: { type: 'yes-no', expected: 'YES' },
          requiredEvidence: [],
        },
      ],
    },
    {
      title: 'Installation',
      criteria: [
        {
          code: 'SIGNAGE',
          text: 'Signage visible',
          severity: severityNamed('MINOR'),
          rule: { type: 'yes-no', expected: 'YES' },
          requiredEvidence: [],
        },
        {
          code: 'HOSE',
          text: 'Hose condition',
          severity: severityNamed('MINOR'),
          rule: { type: 'option-in-list', acceptedOptions: ['Good', 'Acceptable'] },
          requiredEvidence: [],
        },
      ],
    },
  ];
}

const compliantAnswers: Record<string, RawAnswer> = {
  PRESSURE: measured('12.5'),
  SEAL: yes,
  SIGNAGE: yes,
  HOSE: chosen('Good'),
};

export function describeCertiflowContract(
  adapterName: string,
  createSystem: () => SystemUnderTest | Promise<SystemUnderTest>,
): void {
  describe(`Certiflow contract · ${adapterName}`, () => {
    let certiflow: Certiflow;
    let vocabulary: Vocabulary;
    let suffix: string;
    let extinguisherType: string;
    let extinguisherCode: string;

    const unique = (base: string) => `${base}-${suffix}`;

    beforeEach(async () => {
      const system = await createSystem();
      certiflow = system.certiflow;
      vocabulary = await certiflow.schemas.vocabulary();
      suffix = `${Date.now().toString(36)}${Math.random().toString(36).slice(2, 7)}`;
      extinguisherType = unique('Fire extinguisher');
      extinguisherCode = unique('EXT-0042');
    });

    async function expectRejection(operation: Promise<unknown>, expectedName: DomainErrorName): Promise<void> {
      await expect(operation).rejects.toMatchObject({ name: expectedName });
    }

    function registerExtinguisher(code = extinguisherCode, jurisdictionCode = 'DEMO-STANDARD', responsible = assetResponsible) {
      return certiflow.catalog.registerAsset({
        code,
        name: 'Lobby extinguisher',
        type: extinguisherType,
        location: 'Lobby',
        responsible,
        characteristics: [{ name: 'Agent', value: 'ABC powder' }],
        jurisdictionCode,
        subsystems: [],
        registeredBy: administrator,
      });
    }

    function createExtinguisherSchema() {
      return certiflow.schemas.createInspectionSchema({
        name: 'Extinguisher inspection',
        assetType: extinguisherType,
        sections: extinguisherSections(vocabulary),
        requiredSubsystems: [],
        createdBy: administrator,
      });
    }

    function assignInspection(assetCode = extinguisherCode, sectionTitles?: string[]) {
      return certiflow.inspections.assignInspection({
        assetCode,
        inspector,
        plannedDate: today,
        scope: sectionTitles === undefined ? { kind: 'full' } : { kind: 'sections', titles: sectionTitles },
        assignedBy: coordinator,
      });
    }

    async function assetWithSchemaAndOpenInspection(): Promise<string> {
      await registerExtinguisher();
      await createExtinguisherSchema();
      return assignInspection();
    }

    function attachPhoto(inspectionId: string, criterionCode: string, reference = 'gauge.jpg') {
      return certiflow.inspections.attachEvidence({
        inspectionId,
        criterionCode,
        evidence: { kind: vocabulary.evidenceKinds.find((kind) => kind.name === 'PHOTO')!, reference, description: '' },
        attachedBy: inspector,
      });
    }

    function answer(inspectionId: string, criterionCode: string, rawAnswer: RawAnswer) {
      return certiflow.inspections.recordAnswer({ inspectionId, criterionCode, answer: rawAnswer, answeredBy: inspector });
    }

    async function answerEverything(inspectionId: string, overrides: Record<string, RawAnswer> = {}): Promise<void> {
      const inspection = await certiflow.inspections.findInspection(inspectionId);
      const answers = { ...compliantAnswers, ...overrides };
      for (const response of inspection.responses) {
        await answer(inspectionId, response.criterion.code, answers[response.criterion.code]!);
      }
      if (inspection.responses.some((response) => response.criterion.code === 'PRESSURE')) {
        await attachPhoto(inspectionId, 'PRESSURE');
      }
    }

    function close(inspectionId: string) {
      return certiflow.inspections.closeInspection({ inspectionId, closedBy: inspector });
    }

    async function closedInspection(overrides: Record<string, RawAnswer> = {}, assetCode = extinguisherCode) {
      const inspectionId = await assignInspection(assetCode);
      await answerEverything(inspectionId, overrides);
      const findingIds = await close(inspectionId);
      return { inspectionId, findingIds };
    }

    async function assetWithClosedInspection(overrides: Record<string, RawAnswer> = {}) {
      await registerExtinguisher();
      await createExtinguisherSchema();
      return closedInspection(overrides);
    }

    function planAction(findingId: string, dueDate: LocalDate = inEighteenDays, responsible: Person = assetResponsible) {
      return certiflow.findings.planCorrectiveAction({
        findingId,
        description: 'Replace the safety seal',
        responsible,
        dueDate,
        plannedBy: assetResponsible,
      });
    }

    function issue(inspectionId: string, subsystemCode?: string) {
      return certiflow.certification.issueCertificate({ inspectionId, subsystemCode, issuedBy: certifier });
    }

    async function outcomeOf(inspectionId: string, criterionCode: string): Promise<Outcome> {
      const inspection = await certiflow.inspections.findInspection(inspectionId);
      return inspection.responses.find((response) => response.criterion.code === criterionCode)!.outcome;
    }

    describe('asset catalogue', () => {
      test('a registered asset can be consulted with its type, location, responsible and characteristics', async () => {
        await registerExtinguisher();
        const asset = await certiflow.catalog.findAsset(extinguisherCode);
        expect(asset).toMatchObject({
          name: 'Lobby extinguisher',
          type: extinguisherType,
          location: 'Lobby',
          responsible: assetResponsible,
          characteristics: [{ name: 'Agent', value: 'ABC powder' }],
        });
      });

      test('registering an asset is recorded in its history with who did it', async () => {
        await registerExtinguisher();
        const history = await certiflow.audit.auditHistory({ kind: 'asset', code: extinguisherCode });
        expect(history).toMatchObject([{ action: 'REGISTERED', performedBy: administrator }]);
      });

      test('the same asset code cannot be registered twice', async () => {
        await registerExtinguisher();
        await expectRejection(registerExtinguisher(), 'AssetAlreadyRegisteredException');
      });

      test('an asset cannot have two characteristics with the same name', async () => {
        const registration = certiflow.catalog.registerAsset({
          code: unique('EXT-0001'),
          name: 'Hall extinguisher',
          type: extinguisherType,
          location: 'Hall',
          responsible: assetResponsible,
          characteristics: [
            { name: 'Agent', value: 'ABC powder' },
            { name: 'Agent', value: 'CO2' },
          ],
          jurisdictionCode: 'DEMO-STANDARD',
          subsystems: [],
          registeredBy: administrator,
        });
        await expectRejection(registration, 'DuplicateCharacteristicException');
      });

      test('assets can be listed by type', async () => {
        await registerExtinguisher();
        await certiflow.catalog.registerAsset({
          code: unique('BLR-0007'),
          name: 'Boiler',
          type: unique('Pressure vessel'),
          location: 'Basement',
          responsible: assetResponsible,
          characteristics: [],
          jurisdictionCode: 'DEMO-STANDARD',
          subsystems: [],
          registeredBy: administrator,
        });
        const extinguishers = await certiflow.catalog.listAssets({ type: extinguisherType });
        expect(extinguishers.map((asset) => asset.code)).toEqual([extinguisherCode]);
        expect((await certiflow.catalog.listAssets({})).map((asset) => asset.code)).toEqual(
          expect.arrayContaining([extinguisherCode, unique('BLR-0007')]),
        );
      });

      test('an unknown asset is reported as not found', async () => {
        await expectRejection(certiflow.catalog.findAsset('NOPE'), 'AssetNotFoundException');
      });
    });

    describe('inspection schemas and versions', () => {
      test('an asset type has a single inspection schema', async () => {
        await createExtinguisherSchema();
        await expectRejection(createExtinguisherSchema(), 'SchemaAlreadyDefinedForAssetTypeException');
      });

      test('a new version is numbered consecutively and the previous one is kept', async () => {
        await createExtinguisherSchema();
        const sections = extinguisherSections(vocabulary);
        const published = await certiflow.schemas.publishSchemaVersion({
          assetType: extinguisherType,
          sections: [sections[0]!],
          requiredSubsystems: [],
          publishedBy: administrator,
        });
        const schema = await certiflow.schemas.findSchema(extinguisherType);
        expect(published.number).toBe(2);
        expect(published.effectiveFrom).toBe(published.publishedAt);
        expect(schema.currentVersion).toBe(2);
        expect(schema.latestPublishedVersion).toBe(2);
        expect(schema.versions.map((version) => version.sections.length)).toEqual([2, 1]);
      });

      test('creating a schema and adding a version are recorded in its history', async () => {
        await createExtinguisherSchema();
        await certiflow.schemas.publishSchemaVersion({
          assetType: extinguisherType,
          sections: extinguisherSections(vocabulary),
          requiredSubsystems: [],
          publishedBy: administrator,
        });
        const history = await certiflow.audit.auditHistory({ kind: 'schema', assetType: extinguisherType });
        expect(history.map((entry) => entry.action)).toEqual(['CREATED', 'VERSION_ADDED']);
      });

      test('schemas can be listed', async () => {
        await createExtinguisherSchema();
        expect((await certiflow.schemas.listSchemas()).map((schema) => schema.assetType)).toContain(extinguisherType);
      });

      const invalidSchemas: [string, (sections: Section[]) => Section[], DomainErrorName][] = [
        ['a schema without sections', () => [], 'InvalidSchemaException'],
        ['a section without criteria', (sections) => [{ ...sections[0]!, criteria: [] }], 'InvalidSchemaException'],
        ['a section without title', (sections) => [{ ...sections[0]!, title: ' ' }], 'InvalidSchemaException'],
        ['two criteria with the same code', (sections) => [sections[0]!, sections[0]!], 'InvalidSchemaException'],
        [
          'a range whose minimum exceeds its maximum',
          (sections) => [
            {
              title: 'Broken',
              criteria: [
                {
                  ...sections[0]!.criteria[0]!,
                  rule: { type: 'numeric-range', unit: 'bar', minimum: '16', maximum: '10' },
                },
              ],
            },
          ],
          'InvalidRuleConfigurationException',
        ],
        [
          'an option rule without accepted options',
          (sections) => [
            {
              title: 'Broken',
              criteria: [{ ...sections[1]!.criteria[1]!, rule: { type: 'option-in-list', acceptedOptions: [] } }],
            },
          ],
          'InvalidRuleConfigurationException',
        ],
        [
          'the same evidence kind required twice by a criterion',
          (sections) => {
            const pressure = sections[0]!.criteria[0]!;
            return [
              {
                title: 'Broken',
                criteria: [{ ...pressure, requiredEvidence: [...pressure.requiredEvidence, ...pressure.requiredEvidence] }],
              },
            ];
          },
          'InvalidSchemaException',
        ],
      ];

      test.each(invalidSchemas)('%s is rejected', async (_description, breakSections, expectedName) => {
        const creation = certiflow.schemas.createInspectionSchema({
          name: 'Extinguisher inspection',
          assetType: extinguisherType,
          sections: breakSections(extinguisherSections(vocabulary)),
          requiredSubsystems: [],
          createdBy: administrator,
        });
        await expectRejection(creation, expectedName);
      });
    });

    describe('inspection assignment', () => {
      test('an inspection keeps the schema version in force when it was assigned', async () => {
        const inspectionId = await assetWithSchemaAndOpenInspection();
        await certiflow.schemas.publishSchemaVersion({
          assetType: extinguisherType,
          sections: [extinguisherSections(vocabulary)[0]!],
          requiredSubsystems: [],
          publishedBy: administrator,
        });
        const laterInspectionId = await assignInspection();
        const earlier = await certiflow.inspections.findInspection(inspectionId);
        const later = await certiflow.inspections.findInspection(laterInspectionId);
        expect(earlier.schemaVersion).toBe(1);
        expect(earlier.responses).toHaveLength(4);
        expect(later.schemaVersion).toBe(2);
        expect(later.responses).toHaveLength(2);
      });

      test('an asset whose type has no schema cannot be inspected', async () => {
        await registerExtinguisher();
        await expectRejection(assignInspection(), 'NoInspectionSchemaForAssetTypeException');
      });

      const independenceRules: [string, Person, Person, DomainErrorName][] = [
        ['whoever assigns the inspection cannot carry it out', coordinator, coordinator, 'AssignerCannotInspectException'],
        [
          'the asset responsible cannot inspect the asset',
          coordinator,
          assetResponsible,
          'AssetResponsibleCannotInspectOwnAssetException',
        ],
      ];

      test.each(independenceRules)('%s', async (_rule, assignedBy, assignedInspector, expectedName) => {
        await registerExtinguisher();
        await createExtinguisherSchema();
        const assignment = certiflow.inspections.assignInspection({
          assetCode: extinguisherCode,
          inspector: assignedInspector,
          plannedDate: today,
          scope: { kind: 'full' },
          assignedBy,
        });
        await expectRejection(assignment, expectedName);
      });

      test('the asset responsible cannot assign its inspection', async () => {
        await registerExtinguisher(extinguisherCode, 'DEMO-STANDARD', coordinator);
        await createExtinguisherSchema();
        const assignment = certiflow.inspections.assignInspection({
          assetCode: extinguisherCode,
          inspector,
          plannedDate: today,
          scope: { kind: 'full' },
          assignedBy: coordinator,
        });
        await expectRejection(assignment, 'AssetResponsibleCannotAssignInspectionException');
      });

      test('the scope can only name sections of the schema', async () => {
        await registerExtinguisher();
        await createExtinguisherSchema();
        await expectRejection(assignInspection(extinguisherCode, ['Paperwork']), 'SectionNotInSchemaException');
      });

      test('a partial inspection only expects answers for the sections in its scope', async () => {
        await registerExtinguisher();
        await createExtinguisherSchema();
        const inspectionId = await assignInspection(extinguisherCode, ['Installation']);
        const inspection = await certiflow.inspections.findInspection(inspectionId);
        expect(inspection.responses.map((response) => response.criterion.code)).toEqual(['SIGNAGE', 'HOSE']);
        expect(inspection.coversWholeSchema).toBe(false);
        await expectRejection(answer(inspectionId, 'SEAL', yes), 'CriterionOutOfScopeException');
      });

      test('inspections can be listed by asset and by status', async () => {
        await assetWithClosedInspection();
        await assignInspection();
        const openInspections = await certiflow.inspections.listInspections({ assetCode: extinguisherCode, status: 'open' });
        const closedInspections = await certiflow.inspections.listInspections({ assetCode: extinguisherCode, status: 'closed' });
        expect(openInspections.map((inspection) => inspection.id)).toHaveLength(1);
        expect(closedInspections.map((inspection) => inspection.closed)).toEqual([true]);
      });
    });

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
        const inspectionId = await assetWithSchemaAndOpenInspection();
        await answer(inspectionId, criterionCode, rawAnswer);
        if (withPhoto) {
          await attachPhoto(inspectionId, criterionCode);
        }
        expect(await outcomeOf(inspectionId, criterionCode)).toBe(expectedOutcome);
      });

      test('a criterion without an answer is pending', async () => {
        const inspectionId = await assetWithSchemaAndOpenInspection();
        expect(await outcomeOf(inspectionId, 'SEAL')).toBe('PENDING');
      });

      test('an answer of the wrong type for the criterion is rejected', async () => {
        const inspectionId = await assetWithSchemaAndOpenInspection();
        await expectRejection(answer(inspectionId, 'PRESSURE', chosen('high')), 'AnswerTypeMismatchException');
        await expectRejection(answer(inspectionId, 'SEAL', measured('5')), 'AnswerTypeMismatchException');
      });

      test('a criterion that is not in the captured version cannot be answered', async () => {
        const inspectionId = await assetWithSchemaAndOpenInspection();
        await expectRejection(answer(inspectionId, 'UNKNOWN', yes), 'CriterionNotInSchemaException');
      });

      test('the same file cannot back a criterion twice', async () => {
        const inspectionId = await assetWithSchemaAndOpenInspection();
        await attachPhoto(inspectionId, 'PRESSURE', 'gauge.jpg');
        await expectRejection(attachPhoto(inspectionId, 'PRESSURE', 'gauge.jpg'), 'DuplicateEvidenceException');
      });

      test('answers, evidence and observations keep who registered them', async () => {
        const inspectionId = await assetWithSchemaAndOpenInspection();
        await answer(inspectionId, 'SEAL', yes);
        await attachPhoto(inspectionId, 'SEAL', 'seal.jpg');
        await certiflow.inspections.addObservation({
          inspectionId,
          criterionCode: 'SEAL',
          text: 'Seal replaced last month',
          observedBy: inspector,
        });
        const inspection = await certiflow.inspections.findInspection(inspectionId);
        const sealResponse = inspection.responses.find((response) => response.criterion.code === 'SEAL')!;
        expect(sealResponse.given).toMatchObject({ value: yes, answeredBy: inspector });
        expect(sealResponse.attachments).toMatchObject([{ evidence: { reference: 'seal.jpg' }, attachedBy: inspector }]);
        expect(sealResponse.observations).toMatchObject([{ text: 'Seal replaced last month', observedBy: inspector }]);
      });

      test('an inspection with pending criteria cannot be closed and stays untouched', async () => {
        const inspectionId = await assetWithSchemaAndOpenInspection();
        await answer(inspectionId, 'SEAL', yes);
        const historyBefore = await certiflow.audit.auditHistory({ kind: 'inspection', id: inspectionId });
        await expectRejection(close(inspectionId), 'InspectionIncompleteException');
        const inspection = await certiflow.inspections.findInspection(inspectionId);
        const historyAfter = await certiflow.audit.auditHistory({ kind: 'inspection', id: inspectionId });
        expect(inspection.closed).toBe(false);
        expect(historyAfter).toEqual(historyBefore);
      });

      test('a closed inspection cannot be altered freely', async () => {
        const { inspectionId } = await assetWithClosedInspection();
        await expectRejection(answer(inspectionId, 'SEAL', no), 'InspectionAlreadyClosedException');
        await expectRejection(attachPhoto(inspectionId, 'SEAL', 'late.jpg'), 'InspectionAlreadyClosedException');
        await expectRejection(close(inspectionId), 'InspectionAlreadyClosedException');
      });
    });

    describe('findings', () => {
      test('closing raises one finding per non-conformity with its severity, evidence and responsible', async () => {
        const { inspectionId, findingIds } = await assetWithClosedInspection({ PRESSURE: measured('20'), SIGNAGE: no });
        const findings = await certiflow.findings.listFindings({ assetCode: extinguisherCode });
        expect(findingIds).toHaveLength(2);
        expect(findings).toMatchObject([
          {
            inspectionId,
            criterion: { code: 'PRESSURE' },
            severity: { name: 'CRITICAL' },
            responsible: assetResponsible,
            open: true,
            evidence: [{ evidence: { reference: 'gauge.jpg' } }],
          },
          { criterion: { code: 'SIGNAGE' }, severity: { name: 'MINOR' }, open: true },
        ]);
      });

      test('an inspection without non-conformities raises no findings', async () => {
        const { findingIds } = await assetWithClosedInspection();
        expect(findingIds).toEqual([]);
      });

      test('raising a finding is recorded in its history', async () => {
        const { findingIds } = await assetWithClosedInspection({ SEAL: no });
        const history = await certiflow.audit.auditHistory({ kind: 'finding', id: findingIds[0]! });
        expect(history).toMatchObject([{ action: 'RAISED', performedBy: inspector, detail: 'SEAL REJECTED' }]);
      });
    });

    describe('rectification', () => {
      function rectify(inspectionId: string, criterionCode: string, rawAnswer: RawAnswer, reason = 'Typing mistake') {
        return certiflow.inspections.rectifyInspection({
          inspectionId,
          criterionCode,
          answer: rawAnswer,
          reason,
          rectifiedBy: inspector,
        });
      }

      test('a rectification adds a revision and leaves the original one as it was signed', async () => {
        const { inspectionId } = await assetWithClosedInspection({ SIGNAGE: no });
        await rectify(inspectionId, 'SIGNAGE', yes);
        const inspection = await certiflow.inspections.findInspection(inspectionId);
        const signageOutcomes = inspection.revisions.map(
          (revision) => revision.outcomes.find((outcome) => outcome.criterionCode === 'SIGNAGE')!.outcome,
        );
        expect(inspection.revisions.map((revision) => revision.number)).toEqual([1, 2]);
        expect(inspection.revisions[1]).toMatchObject({ reason: 'Typing mistake', sealedBy: inspector });
        expect(signageOutcomes).toEqual(['OBSERVED', 'APPROVED']);
      });

      test('a rectification without a reason is rejected and changes nothing', async () => {
        const { inspectionId } = await assetWithClosedInspection();
        await expectRejection(rectify(inspectionId, 'SEAL', no, '  '), 'RectificationReasonRequiredException');
        const inspection = await certiflow.inspections.findInspection(inspectionId);
        expect(inspection.revisions).toHaveLength(1);
        expect(await outcomeOf(inspectionId, 'SEAL')).toBe('APPROVED');
      });

      test('an open inspection is corrected by answering again, not by rectifying', async () => {
        const inspectionId = await assetWithSchemaAndOpenInspection();
        await expectRejection(rectify(inspectionId, 'SEAL', no), 'InspectionNotClosedException');
      });

      test('a rectification that introduces a non-conformity raises its finding', async () => {
        const { inspectionId } = await assetWithClosedInspection();
        const raised = await rectify(inspectionId, 'SEAL', no);
        const findings = await certiflow.findings.listFindings({ assetCode: extinguisherCode });
        expect(raised).toHaveLength(1);
        expect(findings).toMatchObject([{ criterion: { code: 'SEAL' }, open: true }]);
      });

      test('a rectification does not duplicate a finding that is still open for the same criterion', async () => {
        const { inspectionId } = await assetWithClosedInspection({ PRESSURE: measured('20') });
        const raised = await rectify(inspectionId, 'PRESSURE', measured('21'));
        expect(raised).toEqual([]);
        expect(await certiflow.findings.listFindings({ assetCode: extinguisherCode })).toHaveLength(1);
      });
    });

    describe('corrective actions', () => {
      async function findingForBrokenSeal(): Promise<string> {
        const { findingIds } = await assetWithClosedInspection({ SEAL: no });
        return findingIds[0]!;
      }

      function verify(findingId: string, actionId: number, result: 'ACCEPTED' | 'REJECTED', notes = '', by: Person = verifier) {
        return certiflow.findings.verifyCorrectiveAction({ findingId, actionId, verifier: by, result, notes });
      }

      test('a corrective action is planned with its responsible, due date and who planned it', async () => {
        const findingId = await findingForBrokenSeal();
        const actionId = await planAction(findingId);
        const finding = await certiflow.findings.findFinding(findingId);
        expect(actionId).toBe(1);
        expect(finding.correctiveActions).toMatchObject([
          { id: 1, responsible: assetResponsible, dueDate: inEighteenDays, plannedBy: assetResponsible, state: 'OPEN', overdue: false },
        ]);
      });

      test('the due date cannot be before the day the action is planned', async () => {
        const findingId = await findingForBrokenSeal();
        await expectRejection(planAction(findingId, yesterday), 'DueDateInThePastException');
      });

      test('the responsible for an action cannot verify it', async () => {
        const findingId = await findingForBrokenSeal();
        await planAction(findingId, inEighteenDays, verifier);
        await expectRejection(
          verify(findingId, 1, 'ACCEPTED', '', verifier),
          'VerifierMustDifferFromResponsibleException',
        );
      });

      test('a rejected verification must explain why and leaves the action open', async () => {
        const findingId = await findingForBrokenSeal();
        await planAction(findingId);
        await expectRejection(verify(findingId, 1, 'REJECTED', ''), 'VerificationNotesRequiredException');
        await verify(findingId, 1, 'REJECTED', 'The seal is still missing');
        const finding = await certiflow.findings.findFinding(findingId);
        expect(finding.open).toBe(true);
        expect(finding.correctiveActions[0]).toMatchObject({
          state: 'OPEN',
          verifications: [{ result: 'REJECTED', notes: 'The seal is still missing', verifier }],
        });
      });

      test('an accepted verification closes the action and the finding and cancels the other open actions', async () => {
        const findingId = await findingForBrokenSeal();
        await planAction(findingId);
        await planAction(findingId);
        await verify(findingId, 2, 'ACCEPTED');
        const finding = await certiflow.findings.findFinding(findingId);
        const history = await certiflow.audit.auditHistory({ kind: 'finding', id: findingId });
        expect(finding.open).toBe(false);
        expect(finding.correctiveActions.map((action) => action.state)).toEqual(['CANCELLED', 'CLOSED']);
        expect(history.map((entry) => entry.action)).toEqual([
          'RAISED',
          'ACTION_PLANNED',
          'ACTION_PLANNED',
          'VERIFICATION_ACCEPTED',
        ]);
      });

      test('a closed finding accepts no more corrective actions or verifications', async () => {
        const findingId = await findingForBrokenSeal();
        await planAction(findingId);
        await verify(findingId, 1, 'ACCEPTED');
        await expectRejection(planAction(findingId), 'FindingAlreadyClosedException');
        await expectRejection(verify(findingId, 1, 'ACCEPTED'), 'FindingAlreadyClosedException');
      });

      test('a verification must point to an action of the finding', async () => {
        const findingId = await findingForBrokenSeal();
        await expectRejection(verify(findingId, 7, 'ACCEPTED'), 'CorrectiveActionNotInFindingException');
      });
    });

    describe('certification', () => {
      test('an open or partial inspection is rejected before the jurisdiction policy is consulted', async () => {
        await registerExtinguisher();
        await createExtinguisherSchema();
        await closedInspection({ SEAL: no });
        const partialInspectionId = await assignInspection(extinguisherCode, ['Installation']);
        const decision = await certiflow.certification.evaluateCertification(partialInspectionId);
        expect(decision).toMatchObject({ eligible: false, outcome: 'REJECTED', policyCode: 'STANDARD' });
        expect(decision.ineligibilities.map((ineligibility) => ineligibility.reason)).toEqual([
          'INSPECTION_NOT_CLOSED',
          'PARTIAL_INSPECTION',
        ]);
      });

      test('minor findings do not prevent certification under the standard policy', async () => {
        const { inspectionId } = await assetWithClosedInspection({ SIGNAGE: no });
        const decision = await certiflow.certification.evaluateCertification(inspectionId);
        expect(decision).toMatchObject({ eligible: true, outcome: 'APPROVED', ineligibilities: [] });
      });

      test('an asset becomes certifiable again once its blocking findings are closed', async () => {
        const { inspectionId, findingIds } = await assetWithClosedInspection({ SEAL: no });
        await expectRejection(issue(inspectionId), 'NotEligibleForCertificationException');
        await planAction(findingIds[0]!);
        await certiflow.findings.verifyCorrectiveAction({
          findingId: findingIds[0]!,
          actionId: 1,
          verifier,
          result: 'ACCEPTED',
          notes: '',
        });
        const number = await issue(inspectionId);
        expect(await certiflow.certification.findCertificate(number)).toMatchObject({ status: 'ACTIVE' });
      });

      test('a refused certificate explains every reason', async () => {
        const { inspectionId } = await assetWithClosedInspection({ SEAL: no });
        await expect(issue(inspectionId)).rejects.toMatchObject({
          name: 'NotEligibleForCertificationException',
          ineligibilities: [{ reason: 'BLOCKING_FINDINGS' }],
        });
      });

      test('a certificate is valid from the day it is issued for the term its policy grants, and records who issued it', async () => {
        const { inspectionId } = await assetWithClosedInspection();
        const number = await issue(inspectionId);
        const certificate = await certiflow.certification.findCertificate(number);
        const history = await certiflow.audit.auditHistory({ kind: 'certificate', number });
        expect(certificate).toMatchObject({
          assetCode: extinguisherCode,
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
        const { inspectionId } = await assetWithClosedInspection();
        await issue(inspectionId);
        await expectRejection(issue(inspectionId), 'AssetAlreadyCertifiedException');
      });

      function suspend(number: string, reason = 'Tampering reported') {
        return certiflow.certification.suspendCertificate({ number, reason, suspendedBy: certifier });
      }

      test('a suspended certificate keeps its reason and who suspended it', async () => {
        const { inspectionId } = await assetWithClosedInspection();
        const number = await issue(inspectionId);
        await suspend(number);
        const certificate = await certiflow.certification.findCertificate(number);
        expect(certificate).toMatchObject({
          status: 'SUSPENDED',
          suspensions: [{ reason: 'Tampering reported', suspendedBy: certifier }],
        });
      });

      test('only an active certificate can be suspended', async () => {
        const { inspectionId } = await assetWithClosedInspection();
        const number = await issue(inspectionId);
        await suspend(number);
        await expectRejection(suspend(number), 'CertificateNotActiveException');
      });

      test('a suspended certificate still prevents issuing another one', async () => {
        const { inspectionId } = await assetWithClosedInspection();
        const number = await issue(inspectionId);
        await suspend(number);
        await expectRejection(issue(inspectionId), 'AssetAlreadyCertifiedException');
      });

      function renew(currentNumber: string, inspectionId: string) {
        return certiflow.certification.renewCertificate({ currentNumber, inspectionId, renewedBy: certifier });
      }

      test('renewing issues a new certificate and retires the previous one', async () => {
        const { inspectionId } = await assetWithClosedInspection();
        const number = await issue(inspectionId);
        const { inspectionId: laterInspectionId } = await closedInspection();
        const renewedNumber = await renew(number, laterInspectionId);
        const previous = await certiflow.certification.findCertificate(number);
        const renewed = await certiflow.certification.findCertificate(renewedNumber);
        const previousHistory = await certiflow.audit.auditHistory({ kind: 'certificate', number });
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
        const { inspectionId } = await assetWithClosedInspection();
        const number = await issue(inspectionId);
        await expectRejection(renew(number, inspectionId), 'RenewalRequiresLaterInspectionException');
      });

      test('a renewal needs an inspection of the same asset', async () => {
        const { inspectionId } = await assetWithClosedInspection();
        const number = await issue(inspectionId);
        await registerExtinguisher(unique('EXT-0099'));
        const { inspectionId: otherAssetInspectionId } = await closedInspection({}, unique('EXT-0099'));
        await expectRejection(renew(number, otherAssetInspectionId), 'ReplacementForAnotherAssetException');
      });

      test('a certificate can only be renewed once', async () => {
        const { inspectionId } = await assetWithClosedInspection();
        const number = await issue(inspectionId);
        const { inspectionId: laterInspectionId } = await closedInspection();
        await renew(number, laterInspectionId);
        const { inspectionId: latestInspectionId } = await closedInspection();
        await expectRejection(renew(number, latestInspectionId), 'CertificateAlreadyRenewedException');
      });

      test('certificates can be listed by asset', async () => {
        const { inspectionId } = await assetWithClosedInspection();
        await issue(inspectionId);
        expect(await certiflow.certification.listCertificates({ assetCode: extinguisherCode })).toHaveLength(1);
        expect(await certiflow.certification.listCertificates({ assetCode: 'OTHER' })).toEqual([]);
      });
    });

    describe('jurisdiction policies', () => {
      const decisionsByJurisdiction: [string, Record<string, RawAnswer>, string, string][] = [
        ['DEMO-STRICT', {}, 'APPROVED', lastDayOfAYear],
        ['DEMO-STANDARD', {}, 'APPROVED', lastDayOfAYear],
        ['DEMO-CONDITIONAL', {}, 'APPROVED', lastDayOfSixMonths],
        ['DEMO-STRICT', { SIGNAGE: no }, 'REJECTED', ''],
        ['DEMO-STANDARD', { SIGNAGE: no }, 'APPROVED', lastDayOfAYear],
        ['DEMO-CONDITIONAL', { SIGNAGE: no }, 'CONDITIONAL', lastDayOfThirtyDays],
        ['DEMO-STRICT', { SEAL: no }, 'REJECTED', ''],
        ['DEMO-STANDARD', { SEAL: no }, 'REJECTED', ''],
        ['DEMO-CONDITIONAL', { SEAL: no }, 'CONDITIONAL', lastDayOfThirtyDays],
        ['DEMO-CONDITIONAL', { PRESSURE: measured('20') }, 'REJECTED', ''],
      ];

      test.each(decisionsByJurisdiction)(
        'under %s the answers %o give a %s decision valid until %s',
        async (jurisdictionCode, overrides, expectedOutcome, expectedValidUntil) => {
          await registerExtinguisher(extinguisherCode, jurisdictionCode);
          await createExtinguisherSchema();
          const { inspectionId } = await closedInspection(overrides);
          const decision = await certiflow.certification.evaluateCertification(inspectionId);
          expect(decision).toMatchObject({ outcome: expectedOutcome, jurisdictionCode, scope: { kind: 'wholeAsset' } });
          expect(decision.validity?.validUntil ?? '').toBe(expectedValidUntil);
        },
      );

      test('a conditional certificate lists every open finding as a condition due when it expires', async () => {
        await registerExtinguisher(extinguisherCode, 'DEMO-CONDITIONAL');
        await createExtinguisherSchema();
        const { inspectionId, findingIds } = await closedInspection({ SEAL: no });
        const number = await issue(inspectionId);
        const certificate = await certiflow.certification.findCertificate(number);
        expect(certificate).toMatchObject({ modality: 'CONDITIONAL', status: 'ACTIVE', validUntil: lastDayOfThirtyDays });
        expect(certificate.decision.conditions).toMatchObject([
          { findingId: findingIds[0], criterionCode: 'SEAL', dueDate: lastDayOfThirtyDays },
        ]);
      });

      test('a rejected decision names the policy that rejected it and why', async () => {
        await registerExtinguisher(extinguisherCode, 'DEMO-STRICT');
        await createExtinguisherSchema();
        const { inspectionId } = await closedInspection({ SIGNAGE: no });
        await expect(issue(inspectionId)).rejects.toMatchObject({
          name: 'NotEligibleForCertificationException',
          ineligibilities: [{ reason: 'BLOCKING_FINDINGS' }],
        });
        expect(await certiflow.certification.evaluateCertification(inspectionId)).toMatchObject({
          policyCode: 'STRICT',
          actions: [],
        });
      });

      test('an asset cannot be registered under a jurisdiction without a policy', async () => {
        await expectRejection(registerExtinguisher(extinguisherCode, 'ATLANTIS'), 'UnknownJurisdictionException');
      });

      test('the vocabulary lists the jurisdictions that have a policy', async () => {
        expect(vocabulary.jurisdictions.map((jurisdiction) => jurisdiction.code)).toEqual(
          expect.arrayContaining(['DEMO-STRICT', 'DEMO-CONDITIONAL', 'DEMO-STANDARD']),
        );
      });
    });

    describe('scheduled schema versions', () => {
      function publishOnlyPhysicalCondition(effectiveFrom?: string) {
        return certiflow.schemas.publishSchemaVersion({
          assetType: extinguisherType,
          sections: [extinguisherSections(vocabulary)[0]!],
          requiredSubsystems: [],
          effectiveFrom,
          publishedBy: administrator,
        });
      }

      test('a version scheduled for later does not apply to inspections assigned before it takes effect', async () => {
        const earlierInspectionId = await assetWithSchemaAndOpenInspection();
        const scheduled = await publishOnlyPhysicalCondition(startOfDayIn(10));
        const laterInspectionId = await assignInspection();
        const schema = await certiflow.schemas.findSchema(extinguisherType);

        expect(scheduled.number).toBe(2);
        expect(sameInstant(scheduled.effectiveFrom)).toBe(startOfDayIn(10));
        expect(schema).toMatchObject({ currentVersion: 1, latestPublishedVersion: 2 });
        expect((await certiflow.inspections.findInspection(earlierInspectionId)).schemaVersion).toBe(1);
        expect((await certiflow.inspections.findInspection(laterInspectionId)).schemaVersion).toBe(1);
      });

      test('the version in force at any instant can be consulted', async () => {
        await createExtinguisherSchema();
        await publishOnlyPhysicalCondition(startOfDayIn(10));
        const justBefore = await certiflow.schemas.effectiveVersion(extinguisherType, `${plusDays(today, 9)}T23:59:59.999Z`);
        const exactly = await certiflow.schemas.effectiveVersion(extinguisherType, startOfDayIn(10));
        expect([justBefore.number, exactly.number]).toEqual([1, 2]);
        await expectRejection(
          certiflow.schemas.effectiveVersion(extinguisherType, '2020-01-01T00:00:00.000Z'),
          'NoEffectiveSchemaVersionException',
        );
      });

      test('a version cannot take effect before it is published', async () => {
        await createExtinguisherSchema();
        await expectRejection(publishOnlyPhysicalCondition(startOfDayIn(-1)), 'InvalidSchemaException');
      });

      test('scheduled versions take effect in publication order', async () => {
        await createExtinguisherSchema();
        await publishOnlyPhysicalCondition(startOfDayIn(20));
        await expectRejection(publishOnlyPhysicalCondition(startOfDayIn(10)), 'SchemaEffectiveDateConflictException');
        await expectRejection(publishOnlyPhysicalCondition(), 'SchemaEffectiveDateConflictException');
        await publishOnlyPhysicalCondition(startOfDayIn(30));
        expect((await certiflow.schemas.findSchema(extinguisherType)).latestPublishedVersion).toBe(3);
      });
    });

    describe('partial certificates by subsystem', () => {
      const boilerType = () => unique('Pressure vessel');
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
        const severityNamed = (name: string) => vocabulary.severities.find((severity) => severity.name === name)!;
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

      function registerBoiler(code = unique('BLR-0007'), jurisdictionCode = 'DEMO-CONDITIONAL', subsystems = boilerSubsystems) {
        return certiflow.catalog.registerAsset({
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
        return certiflow.schemas.createInspectionSchema({
          name: 'Boiler inspection',
          assetType: boilerType(),
          sections,
          requiredSubsystems,
          createdBy: administrator,
        });
      }

      async function closedBoilerInspection(overrides: Record<string, RawAnswer> = {}, assetCode = unique('BLR-0007'), sectionTitles?: string[]) {
        const inspectionId = await assignInspection(assetCode, sectionTitles);
        const inspection = await certiflow.inspections.findInspection(inspectionId);
        const answers = { ...compliantBoilerAnswers, ...overrides };
        for (const response of inspection.responses) {
          await answer(inspectionId, response.criterion.code, answers[response.criterion.code]!);
        }
        const findingIds = await close(inspectionId);
        return { inspectionId, findingIds };
      }

      async function boilerWithClosedInspection(overrides: Record<string, RawAnswer> = {}, jurisdictionCode = 'DEMO-CONDITIONAL') {
        await registerBoiler(unique('BLR-0007'), jurisdictionCode);
        await createBoilerSchema();
        return closedBoilerInspection(overrides);
      }

      async function certifyBothSubsystems(inspectionId: string) {
        return {
          electrical: await issue(inspectionId, 'ELECTRICAL'),
          pressure: await issue(inspectionId, 'PRESSURE'),
        };
      }

      function issueGlobal(partialCertificateNumbers: string[], assetCode = unique('BLR-0007')) {
        return certiflow.certification.issueGlobalCertificate({
          assetCode,
          schemaVersion: 1,
          partialCertificateNumbers,
          issuedBy: certifier,
        });
      }

      test('every criterion of a schema with subsystems resolves to exactly one of them', async () => {
        const withoutSubsystem = boilerSections().map((section) => ({ ...section, subsystemCode: undefined }));
        await expectRejection(createBoilerSchema(withoutSubsystem), 'InvalidSchemaException');
        await expectRejection(createBoilerSchema(boilerSections(), ['ELECTRICAL', 'PRESSURE', 'FUEL']), 'InvalidSchemaException');
      });

      test('an asset that lacks a required subsystem cannot be inspected with that schema', async () => {
        await registerBoiler(unique('BLR-0007'), 'DEMO-CONDITIONAL', [boilerSubsystems[0]!]);
        await createBoilerSchema();
        await expectRejection(assignInspection(unique('BLR-0007')), 'AssetLacksRequiredSubsystemsException');
      });

      test('a finding belongs to the subsystem of its criterion, which overrides the one of its section', async () => {
        const { findingIds } = await boilerWithClosedInspection({ PILOT: no, GROUNDING: no });
        const findings = await Promise.all(findingIds.map((findingId) => certiflow.findings.findFinding(findingId)));
        expect(findings.map((finding) => [finding.criterion.code, finding.subsystemCode])).toEqual([
          ['GROUNDING', 'ELECTRICAL'],
          ['PILOT', 'PRESSURE'],
        ]);
      });

      test('a rejected subsystem does not prevent certifying another one', async () => {
        const { inspectionId } = await boilerWithClosedInspection({ RELIEF: measured('9.5') });
        const electrical = await certiflow.certification.evaluateCertification(inspectionId, 'ELECTRICAL');
        const pressure = await certiflow.certification.evaluateCertification(inspectionId, 'PRESSURE');
        expect(electrical).toMatchObject({ outcome: 'APPROVED', scope: { kind: 'subsystem', subsystemCode: 'ELECTRICAL' } });
        expect(pressure).toMatchObject({ outcome: 'REJECTED', ineligibilities: [{ reason: 'BLOCKING_FINDINGS' }] });
        const electricalNumber = await issue(inspectionId, 'ELECTRICAL');
        await expectRejection(issue(inspectionId, 'PRESSURE'), 'NotEligibleForCertificationException');
        await expect(issueGlobal([electricalNumber])).rejects.toMatchObject({
          name: 'NotEligibleForCertificationException',
          ineligibilities: [{ reason: 'MISSING_SUBSYSTEM', detail: 'Missing partial for PRESSURE' }],
        });
      });

      test('an inspection of some sections certifies only the subsystems it fully covers', async () => {
        await registerBoiler();
        await createBoilerSchema();
        const { inspectionId } = await closedBoilerInspection({}, unique('BLR-0007'), ['Electrical']);
        expect(await certiflow.certification.evaluateCertification(inspectionId, 'ELECTRICAL')).toMatchObject({
          outcome: 'APPROVED',
        });
        expect(await certiflow.certification.evaluateCertification(inspectionId, 'PRESSURE')).toMatchObject({
          outcome: 'REJECTED',
          ineligibilities: [{ reason: 'PARTIAL_INSPECTION' }],
        });
      });

      test('an asset split into subsystems is certified whole only through its partial certificates', async () => {
        const { inspectionId } = await boilerWithClosedInspection();
        expect(await certiflow.certification.evaluateCertification(inspectionId)).toMatchObject({
          eligible: false,
          outcome: 'REJECTED',
          scope: { kind: 'wholeAsset' },
          ineligibilities: [{ reason: 'REQUIRES_PARTIAL_CERTIFICATES' }],
        });
        await expectRejection(issue(inspectionId), 'NotEligibleForCertificationException');
        await expectRejection(certiflow.certification.evaluateCertification(inspectionId, 'FUEL'), 'SubsystemNotFoundException');
      });

      test('each subsystem holds its own current certificate', async () => {
        const { inspectionId } = await boilerWithClosedInspection();
        const { electrical, pressure } = await certifyBothSubsystems(inspectionId);
        await expectRejection(issue(inspectionId, 'ELECTRICAL'), 'AssetAlreadyCertifiedException');
        expect((await certiflow.certification.listCertificates({ assetCode: unique('BLR-0007'), subsystemCode: 'PRESSURE' })).map((c) => c.number)).toEqual([
          pressure,
        ]);
        expect(await certiflow.certification.findCertificate(electrical)).toMatchObject({
          scope: { kind: 'subsystem', subsystemCode: 'ELECTRICAL' },
          status: 'ACTIVE',
        });
      });

      test('the global certificate lasts no longer than its partials and inherits their conditions', async () => {
        const { inspectionId } = await boilerWithClosedInspection({ INSULATION: chosen('Damaged') });
        const { electrical, pressure } = await certifyBothSubsystems(inspectionId);
        const global = await certiflow.certification.findCertificate(await issueGlobal([electrical, pressure]));
        expect(await certiflow.certification.findCertificate(electrical)).toMatchObject({ modality: 'APPROVED', validUntil: lastDayOfSixMonths });
        expect(await certiflow.certification.findCertificate(pressure)).toMatchObject({ modality: 'CONDITIONAL', validUntil: lastDayOfThirtyDays });
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
        await certiflow.certification.suspendCertificate({ number: pressure, reason: 'Valve recall', suspendedBy: certifier });
        expect(await certiflow.certification.findCertificate(globalNumber)).toMatchObject({
          status: 'DEPENDENCY_INVALID',
          actions: ['renewGlobalCertificate'],
        });
        expect((await certiflow.certification.generateCertificateDocument(globalNumber)).status).toBe('DEPENDENCY_INVALID');
      });

      test('a global certificate only accepts active partials of the same asset', async () => {
        const { inspectionId } = await boilerWithClosedInspection();
        const { electrical } = await certifyBothSubsystems(inspectionId);
        await registerBoiler(unique('BLR-0099'));
        const { inspectionId: otherInspectionId } = await closedBoilerInspection({}, unique('BLR-0099'));
        const otherPressure = await issue(otherInspectionId, 'PRESSURE');
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
        const renewedElectrical = await certiflow.certification.renewCertificate({
          currentNumber: electrical,
          inspectionId: laterInspectionId,
          renewedBy: certifier,
        });
        expect(await certiflow.certification.findCertificate(globalNumber)).toMatchObject({ status: 'DEPENDENCY_INVALID' });
        const renewedGlobal = await certiflow.certification.renewGlobalCertificate({
          currentNumber: globalNumber,
          schemaVersion: 1,
          partialCertificateNumbers: [renewedElectrical, pressure],
          renewedBy: certifier,
        });
        expect(await certiflow.certification.findCertificate(renewedElectrical)).toMatchObject({
          scope: { kind: 'subsystem', subsystemCode: 'ELECTRICAL' },
        });
        expect((await certiflow.certification.findCertificate(globalNumber)).status).toBe('RENEWED');
        expect((await certiflow.certification.findCertificate(renewedGlobal)).status).toBe('ACTIVE');
      });

      test('an asset split into subsystems offers a global certificate', async () => {
        await registerBoiler();
        await createBoilerSchema();
        expect((await certiflow.catalog.findAsset(unique('BLR-0007'))).actions).toEqual(['assignInspection', 'issueGlobalCertificate']);
      });
    });

    describe('available actions', () => {
      test('an asset offers an inspection only when its type has a schema', async () => {
        const assetBeforeSchema = await registerExtinguisher();
        await createExtinguisherSchema();
        const assetAfterSchema = await certiflow.catalog.findAsset(extinguisherCode);
        expect(assetBeforeSchema.actions).toEqual([]);
        expect(assetAfterSchema.actions).toEqual(['assignInspection']);
      });

      test('an open inspection offers execution and closure; a closed one offers rectification and its act', async () => {
        const inspectionId = await assetWithSchemaAndOpenInspection();
        const openInspection = await certiflow.inspections.findInspection(inspectionId);
        await answerEverything(inspectionId);
        await close(inspectionId);
        const closedOne = await certiflow.inspections.findInspection(inspectionId);
        expect(openInspection.actions).toEqual(['recordAnswer', 'attachEvidence', 'addObservation', 'closeInspection']);
        expect(closedOne.actions).toEqual(['rectifyInspection', 'generateInspectionAct']);
      });

      test('issuing a certificate is offered only by a favourable eligibility decision', async () => {
        const { inspectionId } = await assetWithClosedInspection({ SEAL: no });
        const refused = await certiflow.certification.evaluateCertification(inspectionId);
        const { inspectionId: cleanInspectionId } = await (async () => {
          await registerExtinguisher(unique('EXT-0099'));
          return closedInspection({}, unique('EXT-0099'));
        })();
        const favourable = await certiflow.certification.evaluateCertification(cleanInspectionId);
        expect(refused.actions).toEqual([]);
        expect(favourable.actions).toEqual(['issueCertificate']);
      });

      test('findings and corrective actions offer their next step only while they are open', async () => {
        const { findingIds } = await assetWithClosedInspection({ SEAL: no });
        await planAction(findingIds[0]!);
        const openFinding = await certiflow.findings.findFinding(findingIds[0]!);
        await certiflow.findings.verifyCorrectiveAction({
          findingId: findingIds[0]!,
          actionId: 1,
          verifier,
          result: 'ACCEPTED',
          notes: '',
        });
        const closedFinding = await certiflow.findings.findFinding(findingIds[0]!);
        expect(openFinding.actions).toEqual(['planCorrectiveAction']);
        expect(openFinding.correctiveActions[0]!.actions).toEqual(['verifyCorrectiveAction']);
        expect(closedFinding.actions).toEqual([]);
        expect(closedFinding.correctiveActions[0]!.actions).toEqual([]);
      });

      test('a certificate offers suspension while active and renewal until it is renewed', async () => {
        const { inspectionId } = await assetWithClosedInspection();
        const number = await issue(inspectionId);
        const active = await certiflow.certification.findCertificate(number);
        await certiflow.certification.suspendCertificate({ number, reason: 'Tampering', suspendedBy: certifier });
        const suspended = await certiflow.certification.findCertificate(number);
        const { inspectionId: laterInspectionId } = await closedInspection();
        await certiflow.certification.renewCertificate({
          currentNumber: number,
          inspectionId: laterInspectionId,
          renewedBy: certifier,
        });
        const renewed = await certiflow.certification.findCertificate(number);
        expect(active.actions).toEqual(['suspendCertificate', 'renewCertificate']);
        expect(suspended.actions).toEqual(['renewCertificate']);
        expect(renewed.actions).toEqual([]);
      });
    });

    describe('reports', () => {
      test('the inspection act lists every answer with its outcome, the revisions and the history', async () => {
        const { inspectionId } = await assetWithClosedInspection({ SIGNAGE: no });
        const act = await certiflow.inspections.generateInspectionAct(inspectionId);
        expect(act).toMatchObject({ assetCode: extinguisherCode, assetName: 'Lobby extinguisher', schemaVersion: 1 });
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
        const inspectionId = await assetWithSchemaAndOpenInspection();
        await expectRejection(certiflow.inspections.generateInspectionAct(inspectionId), 'InspectionNotClosedException');
      });

      test('the findings summary of an asset lists each finding with its severity and counts the open ones', async () => {
        const { findingIds } = await assetWithClosedInspection({ SEAL: no, SIGNAGE: no });
        await planAction(findingIds[0]!);
        const summary = await certiflow.findings.generateFindingsSummary(extinguisherCode);
        expect(summary).toMatchObject({ assetCode: extinguisherCode, openFindings: 2 });
        expect(summary.findings).toMatchObject([
          { findingId: findingIds[0], criterionCode: 'SEAL', severity: 'Major', overdueActions: 0 },
          { findingId: findingIds[1], criterionCode: 'SIGNAGE', severity: 'Minor', overdueActions: 0 },
        ]);
      });

      test('the certificate document states the status on the day it is consulted', async () => {
        const { inspectionId } = await assetWithClosedInspection();
        const number = await issue(inspectionId);
        const document = await certiflow.certification.generateCertificateDocument(number);
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
      await registerExtinguisher();
      await createExtinguisherSchema();
      const inspectionId = await assignInspection();
      await answerEverything(inspectionId, { SEAL: no });
      const [findingId] = await close(inspectionId);
      const refusal = await certiflow.certification.evaluateCertification(inspectionId);
      const actionId = await planAction(findingId!);
      await certiflow.findings.verifyCorrectiveAction({ findingId: findingId!, actionId, verifier, result: 'ACCEPTED', notes: '' });
      const approval = await certiflow.certification.evaluateCertification(inspectionId);
      const number = await issue(inspectionId);
      const inspectionHistory = await certiflow.audit.auditHistory({ kind: 'inspection', id: inspectionId });

      expect(refusal.eligible).toBe(false);
      expect(approval.eligible).toBe(true);
      expect((await certiflow.certification.findCertificate(number)).status).toBe('ACTIVE');
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
  });
}
