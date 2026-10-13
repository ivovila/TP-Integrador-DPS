import { beforeEach, describe, expect, test } from 'vitest';
import type { Outcome, RawAnswer, Section, Vocabulary } from '../domain/schema';
import type { LocalDate, Person } from '../domain/shared';
import type { Certiflow } from './Certiflow';
import type { DomainErrorName } from './DomainError';

export interface SystemUnderTest {
  certiflow: Certiflow;
  setToday(date: LocalDate): void;
}

const coordinator: Person = { id: 'EMP-001', name: 'Paula Rios' };
const inspector: Person = { id: 'EMP-002', name: 'Ines Duarte' };
const assetResponsible: Person = { id: 'EMP-003', name: 'Raul Mendez' };
const verifier: Person = { id: 'EMP-004', name: 'Vera Soto' };
const certifier: Person = { id: 'EMP-005', name: 'Carlos Vidal' };

const extinguisherType = 'Fire extinguisher';
const today: LocalDate = '2026-03-02';
const nextYear: LocalDate = '2027-03-02';

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
  createSystem: (initialDate: LocalDate) => SystemUnderTest | Promise<SystemUnderTest>,
): void {
  describe(`Certiflow contract · ${adapterName}`, () => {
    let certiflow: Certiflow;
    let setToday: (date: LocalDate) => void;
    let vocabulary: Vocabulary;

    beforeEach(async () => {
      const system = await createSystem(today);
      certiflow = system.certiflow;
      setToday = system.setToday;
      vocabulary = await certiflow.schemas.vocabulary();
    });

    async function expectRejection(operation: Promise<unknown>, expectedName: DomainErrorName): Promise<void> {
      await expect(operation).rejects.toMatchObject({ name: expectedName });
    }

    function registerExtinguisher(code = 'EXT-0042') {
      return certiflow.catalog.registerAsset({
        code,
        name: 'Lobby extinguisher',
        type: extinguisherType,
        location: 'Lobby',
        responsible: assetResponsible,
        characteristics: [{ name: 'Agent', value: 'ABC powder' }],
        registeredBy: coordinator,
      });
    }

    function createExtinguisherSchema() {
      return certiflow.schemas.createInspectionSchema({
        name: 'Extinguisher inspection',
        assetType: extinguisherType,
        sections: extinguisherSections(vocabulary),
        createdBy: coordinator,
      });
    }

    function assignInspection(assetCode = 'EXT-0042', sectionTitles?: string[]) {
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

    async function closedInspection(overrides: Record<string, RawAnswer> = {}, assetCode = 'EXT-0042') {
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

    function planAction(findingId: string, dueDate: LocalDate = '2026-03-20', responsible: Person = assetResponsible) {
      return certiflow.findings.planCorrectiveAction({
        findingId,
        description: 'Replace the safety seal',
        responsible,
        dueDate,
        plannedBy: coordinator,
      });
    }

    function issue(inspectionId: string, validUntil: LocalDate = nextYear) {
      return certiflow.certification.issueCertificate({ inspectionId, validUntil, issuedBy: certifier });
    }

    async function outcomeOf(inspectionId: string, criterionCode: string): Promise<Outcome> {
      const inspection = await certiflow.inspections.findInspection(inspectionId);
      return inspection.responses.find((response) => response.criterion.code === criterionCode)!.outcome;
    }

    describe('asset catalogue', () => {
      test('a registered asset can be consulted with its type, location, responsible and characteristics', async () => {
        await registerExtinguisher();
        const asset = await certiflow.catalog.findAsset('EXT-0042');
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
        const history = await certiflow.audit.auditHistory({ kind: 'asset', code: 'EXT-0042' });
        expect(history).toMatchObject([{ action: 'REGISTERED', performedBy: coordinator }]);
      });

      test('the same asset code cannot be registered twice', async () => {
        await registerExtinguisher();
        await expectRejection(registerExtinguisher(), 'AssetAlreadyRegisteredException');
      });

      test('an asset cannot have two characteristics with the same name', async () => {
        const registration = certiflow.catalog.registerAsset({
          code: 'EXT-0001',
          name: 'Hall extinguisher',
          type: extinguisherType,
          location: 'Hall',
          responsible: assetResponsible,
          characteristics: [
            { name: 'Agent', value: 'ABC powder' },
            { name: 'Agent', value: 'CO2' },
          ],
          registeredBy: coordinator,
        });
        await expectRejection(registration, 'DuplicateCharacteristicException');
      });

      test('assets can be listed by type', async () => {
        await registerExtinguisher();
        await certiflow.catalog.registerAsset({
          code: 'BLR-0007',
          name: 'Boiler',
          type: 'Pressure vessel',
          location: 'Basement',
          responsible: assetResponsible,
          characteristics: [],
          registeredBy: coordinator,
        });
        const extinguishers = await certiflow.catalog.listAssets({ type: extinguisherType });
        expect(extinguishers.map((asset) => asset.code)).toEqual(['EXT-0042']);
        expect(await certiflow.catalog.listAssets({})).toHaveLength(2);
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
          publishedBy: coordinator,
        });
        const schema = await certiflow.schemas.findSchema(extinguisherType);
        expect(published.number).toBe(2);
        expect(schema.currentVersion).toBe(2);
        expect(schema.versions.map((version) => version.sections.length)).toEqual([2, 1]);
      });

      test('creating a schema and adding a version are recorded in its history', async () => {
        await createExtinguisherSchema();
        await certiflow.schemas.publishSchemaVersion({
          assetType: extinguisherType,
          sections: extinguisherSections(vocabulary),
          publishedBy: coordinator,
        });
        const history = await certiflow.audit.auditHistory({ kind: 'schema', assetType: extinguisherType });
        expect(history.map((entry) => entry.action)).toEqual(['CREATED', 'VERSION_ADDED']);
      });

      test('schemas can be listed', async () => {
        await createExtinguisherSchema();
        expect((await certiflow.schemas.listSchemas()).map((schema) => schema.assetType)).toEqual([extinguisherType]);
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
          createdBy: coordinator,
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
          publishedBy: coordinator,
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
          'the asset responsible cannot assign its inspection',
          assetResponsible,
          inspector,
          'AssetResponsibleCannotAssignInspectionException',
        ],
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
          assetCode: 'EXT-0042',
          inspector: assignedInspector,
          plannedDate: today,
          scope: { kind: 'full' },
          assignedBy,
        });
        await expectRejection(assignment, expectedName);
      });

      test('the scope can only name sections of the schema', async () => {
        await registerExtinguisher();
        await createExtinguisherSchema();
        await expectRejection(assignInspection('EXT-0042', ['Paperwork']), 'SectionNotInSchemaException');
      });

      test('a partial inspection only expects answers for the sections in its scope', async () => {
        await registerExtinguisher();
        await createExtinguisherSchema();
        const inspectionId = await assignInspection('EXT-0042', ['Installation']);
        const inspection = await certiflow.inspections.findInspection(inspectionId);
        expect(inspection.responses.map((response) => response.criterion.code)).toEqual(['SIGNAGE', 'HOSE']);
        expect(inspection.coversWholeSchema).toBe(false);
        await expectRejection(answer(inspectionId, 'SEAL', yes), 'CriterionOutOfScopeException');
      });

      test('inspections can be listed by asset and by status', async () => {
        await assetWithClosedInspection();
        await assignInspection();
        const openInspections = await certiflow.inspections.listInspections({ assetCode: 'EXT-0042', status: 'open' });
        const closedInspections = await certiflow.inspections.listInspections({ status: 'closed' });
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
        const findings = await certiflow.findings.listFindings({ assetCode: 'EXT-0042' });
        expect(findingIds).toHaveLength(2);
        expect(findings).toMatchObject([
          {
            inspectionId,
            criterion: { code: 'PRESSURE' },
            severity: { name: 'CRITICAL' },
            responsible: assetResponsible,
            open: true,
            blocksCertification: true,
            evidence: [{ evidence: { reference: 'gauge.jpg' } }],
          },
          { criterion: { code: 'SIGNAGE' }, severity: { name: 'MINOR' }, open: true, blocksCertification: false },
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
        const findings = await certiflow.findings.listFindings({ assetCode: 'EXT-0042' });
        expect(raised).toHaveLength(1);
        expect(findings).toMatchObject([{ criterion: { code: 'SEAL' }, open: true }]);
      });

      test('a rectification does not duplicate a finding that is still open for the same criterion', async () => {
        const { inspectionId } = await assetWithClosedInspection({ PRESSURE: measured('20') });
        const raised = await rectify(inspectionId, 'PRESSURE', measured('21'));
        expect(raised).toEqual([]);
        expect(await certiflow.findings.listFindings({})).toHaveLength(1);
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
          { id: 1, responsible: assetResponsible, dueDate: '2026-03-20', plannedBy: coordinator, state: 'OPEN', overdue: false },
        ]);
      });

      test('the due date cannot be before the day the action is planned', async () => {
        const findingId = await findingForBrokenSeal();
        await expectRejection(planAction(findingId, '2026-03-01'), 'DueDateInThePastException');
      });

      const overdueCases: [string, LocalDate, boolean][] = [
        ['the day before its due date an action is not overdue', '2026-03-19', false],
        ['on its due date an action is not overdue', '2026-03-20', false],
        ['the day after its due date an open action is overdue', '2026-03-21', true],
      ];

      test.each(overdueCases)('%s', async (_description, referenceDate, expectedOverdue) => {
        const findingId = await findingForBrokenSeal();
        await planAction(findingId, '2026-03-20');
        setToday(referenceDate);
        const finding = await certiflow.findings.findFinding(findingId);
        expect(finding.correctiveActions[0]!.overdue).toBe(expectedOverdue);
      });

      test('the responsible for an action cannot verify it', async () => {
        const findingId = await findingForBrokenSeal();
        await planAction(findingId);
        await expectRejection(
          verify(findingId, 1, 'ACCEPTED', '', assetResponsible),
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
      test('an open inspection, a partial scope and blocking findings each make an inspection ineligible', async () => {
        await registerExtinguisher();
        await createExtinguisherSchema();
        await closedInspection({ SEAL: no });
        const partialInspectionId = await assignInspection('EXT-0042', ['Installation']);
        const decision = await certiflow.certification.evaluateCertification(partialInspectionId);
        expect(decision.eligible).toBe(false);
        expect(decision.ineligibilities.map((ineligibility) => ineligibility.reason)).toEqual([
          'INSPECTION_NOT_CLOSED',
          'PARTIAL_INSPECTION',
          'BLOCKING_FINDINGS',
        ]);
      });

      test('minor findings do not prevent certification', async () => {
        const { inspectionId } = await assetWithClosedInspection({ SIGNAGE: no });
        const decision = await certiflow.certification.evaluateCertification(inspectionId);
        expect(decision).toMatchObject({ eligible: true, ineligibilities: [] });
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

      test('a certificate is valid from the day it is issued and records who issued it', async () => {
        const { inspectionId } = await assetWithClosedInspection();
        const number = await issue(inspectionId);
        const certificate = await certiflow.certification.findCertificate(number);
        const history = await certiflow.audit.auditHistory({ kind: 'certificate', number });
        expect(certificate).toMatchObject({
          assetCode: 'EXT-0042',
          inspectionId,
          validFrom: today,
          validUntil: nextYear,
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

      test('a certificate cannot expire before it starts', async () => {
        const { inspectionId } = await assetWithClosedInspection();
        await expectRejection(issue(inspectionId, '2026-03-01'), 'InvalidValidityPeriodException');
      });

      const expiryCases: [string, LocalDate, string][] = [
        ['a certificate is active on its last day of validity', '2027-03-02', 'ACTIVE'],
        ['a certificate is expired the day after its validity ends', '2027-03-03', 'EXPIRED'],
      ];

      test.each(expiryCases)('%s', async (_description, referenceDate, expectedStatus) => {
        const { inspectionId } = await assetWithClosedInspection();
        const number = await issue(inspectionId);
        setToday(referenceDate);
        expect((await certiflow.certification.findCertificate(number)).status).toBe(expectedStatus);
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

      test('a suspension does not stop the calendar: past its validity the certificate is expired', async () => {
        const { inspectionId } = await assetWithClosedInspection();
        const number = await issue(inspectionId);
        await suspend(number);
        setToday('2027-03-03');
        expect((await certiflow.certification.findCertificate(number)).status).toBe('EXPIRED');
      });

      test('a suspended certificate still prevents issuing another one', async () => {
        const { inspectionId } = await assetWithClosedInspection();
        const number = await issue(inspectionId);
        await suspend(number);
        await expectRejection(issue(inspectionId), 'AssetAlreadyCertifiedException');
      });

      function renew(currentNumber: string, inspectionId: string) {
        return certiflow.certification.renewCertificate({
          currentNumber,
          inspectionId,
          validUntil: '2028-03-02',
          renewedBy: certifier,
        });
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
        expect(renewed).toMatchObject({ status: 'ACTIVE', inspectionId: laterInspectionId, validUntil: '2028-03-02' });
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
        await registerExtinguisher('EXT-0099');
        const { inspectionId: otherAssetInspectionId } = await closedInspection({}, 'EXT-0099');
        await expectRejection(renew(number, otherAssetInspectionId), 'InspectionOfAnotherAssetException');
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
        expect(await certiflow.certification.listCertificates({ assetCode: 'EXT-0042' })).toHaveLength(1);
        expect(await certiflow.certification.listCertificates({ assetCode: 'OTHER' })).toEqual([]);
      });
    });

    describe('available actions', () => {
      test('an asset offers an inspection only when its type has a schema', async () => {
        const assetBeforeSchema = await registerExtinguisher();
        await createExtinguisherSchema();
        const assetAfterSchema = await certiflow.catalog.findAsset('EXT-0042');
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
          await registerExtinguisher('EXT-0099');
          return closedInspection({}, 'EXT-0099');
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
          validUntil: '2028-03-02',
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
        expect(act).toMatchObject({ assetCode: 'EXT-0042', assetName: 'Lobby extinguisher', schemaVersion: 1 });
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

      test('the findings summary of an asset counts open, blocking and overdue items', async () => {
        const { findingIds } = await assetWithClosedInspection({ SEAL: no, SIGNAGE: no });
        await planAction(findingIds[0]!, '2026-03-05');
        setToday('2026-03-10');
        const summary = await certiflow.findings.generateFindingsSummary('EXT-0042');
        expect(summary).toMatchObject({ assetCode: 'EXT-0042', openFindings: 2, blockingFindings: 1 });
        expect(summary.findings).toMatchObject([
          { criterionCode: 'SEAL', severity: 'Major', blocksCertification: true, overdueActions: 1 },
          { criterionCode: 'SIGNAGE', severity: 'Minor', blocksCertification: false, overdueActions: 0 },
        ]);
      });

      test('the certificate document states the status on the day it is consulted', async () => {
        const { inspectionId } = await assetWithClosedInspection();
        const number = await issue(inspectionId);
        setToday('2027-03-03');
        const document = await certiflow.certification.generateCertificateDocument(number);
        expect(document).toMatchObject({
          number,
          assetName: 'Lobby extinguisher',
          validFrom: today,
          validUntil: nextYear,
          statusDate: '2027-03-03',
          status: 'EXPIRED',
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
