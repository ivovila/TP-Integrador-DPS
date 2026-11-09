import { beforeEach, expect } from 'vitest';
import type { Outcome, RawAnswer, Section, Vocabulary } from '../../domain/schema';
import type { LocalDate, Person } from '../../domain/shared';
import type { Certiflow } from '../../ports/Certiflow';
import type { DomainErrorName } from '../../ports/DomainError';

export interface SystemUnderTest {
  certiflow: Certiflow;
}

export const administrator: Person = { id: 'EMP-006', name: 'Ana Torres' };
export const coordinator: Person = { id: 'EMP-001', name: 'Paula Rios' };
export const inspector: Person = { id: 'EMP-002', name: 'Ines Duarte' };
export const assetResponsible: Person = { id: 'EMP-003', name: 'Raul Mendez' };
export const verifier: Person = { id: 'EMP-004', name: 'Vera Soto' };
export const certifier: Person = { id: 'EMP-005', name: 'Carlos Vidal' };

export function plusDays(date: LocalDate, days: number): LocalDate {
  const shifted = new Date(`${date}T00:00:00Z`);
  shifted.setUTCDate(shifted.getUTCDate() + days);
  return shifted.toISOString().slice(0, 10);
}

export function plusMonths(date: LocalDate, months: number): LocalDate {
  const [year, month, day] = date.split('-').map(Number) as [number, number, number];
  const monthsSinceYearZero = year * 12 + (month - 1) + months;
  const targetYear = Math.floor(monthsSinceYearZero / 12);
  const targetMonth = monthsSinceYearZero % 12;
  const lastDayOfTargetMonth = new Date(Date.UTC(targetYear, targetMonth + 1, 0)).getUTCDate();
  return new Date(Date.UTC(targetYear, targetMonth, Math.min(day, lastDayOfTargetMonth))).toISOString().slice(0, 10);
}

export function startOfDayIn(days: number): string {
  return `${plusDays(today, days)}T00:00:00.000Z`;
}

export function sameInstant(instant: string): string {
  return new Date(instant).toISOString();
}

export const today: LocalDate = new Date().toISOString().slice(0, 10);
export const yesterday = plusDays(today, -1);
export const inEighteenDays = plusDays(today, 18);
export const lastDayOfAYear = plusDays(plusMonths(today, 12), -1);
export const lastDayOfSixMonths = plusDays(plusMonths(today, 6), -1);
export const lastDayOfThirtyDays = plusDays(today, 29);

export const yes: RawAnswer = { kind: 'yes-no', value: true };
export const no: RawAnswer = { kind: 'yes-no', value: false };

export function measured(value: string): RawAnswer {
  return { kind: 'number', value };
}

export function chosen(value: string): RawAnswer {
  return { kind: 'option', value };
}

export function extinguisherSections(vocabulary: Vocabulary): Section[] {
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

export const compliantAnswers: Record<string, RawAnswer> = {
  PRESSURE: measured('12.5'),
  SEAL: yes,
  SIGNAGE: yes,
  HOSE: chosen('Good'),
};


export function contractContext(createSystem: () => SystemUnderTest | Promise<SystemUnderTest>) {
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

    return {
      get certiflow() { return certiflow; },
      get vocabulary() { return vocabulary; },
      get suffix() { return suffix; },
      get extinguisherType() { return extinguisherType; },
      get extinguisherCode() { return extinguisherCode; },
      get unique() { return unique; },
      get expectRejection() { return expectRejection; },
      get registerExtinguisher() { return registerExtinguisher; },
      get createExtinguisherSchema() { return createExtinguisherSchema; },
      get assignInspection() { return assignInspection; },
      get assetWithSchemaAndOpenInspection() { return assetWithSchemaAndOpenInspection; },
      get attachPhoto() { return attachPhoto; },
      get answer() { return answer; },
      get answerEverything() { return answerEverything; },
      get close() { return close; },
      get closedInspection() { return closedInspection; },
      get assetWithClosedInspection() { return assetWithClosedInspection; },
      get planAction() { return planAction; },
      get issue() { return issue; },
      get outcomeOf() { return outcomeOf; },
    };
}

export type ContractContext = ReturnType<typeof contractContext>;
