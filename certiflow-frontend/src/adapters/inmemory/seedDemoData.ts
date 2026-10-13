import type { Subsystem } from '../../domain/catalog';
import type { Criterion, RawAnswer, Section, Vocabulary } from '../../domain/schema';
import type { LocalDate, Person } from '../../domain/shared';
import type { Certiflow } from '../../ports/Certiflow';
import type { DemoControls } from '../../ports/DemoControls';

export interface DemoPeople {
  coordinator: Person;
  inspector: Person;
  assetResponsible: Person;
  verifier: Person;
  certifier: Person;
}

const extinguisherType = 'Fire extinguisher';
const pressureVesselType = 'Pressure vessel';

function daysFrom(date: LocalDate, days: number): LocalDate {
  const shifted = new Date(`${date}T00:00:00Z`);
  shifted.setUTCDate(shifted.getUTCDate() + days);
  return shifted.toISOString().slice(0, 10);
}

function measured(value: string): RawAnswer {
  return { kind: 'number', value };
}

function chosen(value: string): RawAnswer {
  return { kind: 'option', value };
}

const yes: RawAnswer = { kind: 'yes-no', value: true };
const no: RawAnswer = { kind: 'yes-no', value: false };

function extinguisherSections(vocabulary: Vocabulary): Section[] {
  const severityNamed = (name: string) => vocabulary.severities.find((severity) => severity.name === name)!;
  const evidenceKindNamed = (name: string) => vocabulary.evidenceKinds.find((kind) => kind.name === name)!;
  return [
    {
      title: 'Physical condition',
      criteria: [
        {
          code: 'PRESSURE',
          text: 'Gauge pressure',
          severity: severityNamed('CRITICAL'),
          rule: { type: 'numeric-range', unit: 'bar', minimum: '10', maximum: '16' },
          requiredEvidence: [{ kind: evidenceKindNamed('PHOTO'), minimum: 1 }],
        },
        {
          code: 'WEIGHT',
          text: 'Extinguishing agent weight',
          severity: severityNamed('MAJOR'),
          rule: { type: 'numeric-range', unit: 'kg', minimum: '5.8', maximum: '6.2' },
          requiredEvidence: [],
        },
        {
          code: 'SEAL',
          text: 'Safety seal intact',
          severity: severityNamed('MAJOR'),
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
        {
          code: 'CORROSION',
          text: 'Cylinder free of corrosion',
          severity: severityNamed('MAJOR'),
          rule: { type: 'yes-no', expected: 'YES' },
          requiredEvidence: [],
        },
      ],
    },
    {
      title: 'Installation and records',
      criteria: [
        {
          code: 'SIGNAGE',
          text: 'Signage visible from the corridor',
          severity: severityNamed('MINOR'),
          rule: { type: 'yes-no', expected: 'YES' },
          requiredEvidence: [],
        },
        {
          code: 'HEIGHT',
          text: 'Mounting height',
          severity: severityNamed('MINOR'),
          rule: { type: 'numeric-range', unit: 'm', minimum: '0.8', maximum: '1.5' },
          requiredEvidence: [],
        },
        {
          code: 'TAG',
          text: 'Maintenance tag status',
          severity: severityNamed('MAJOR'),
          rule: { type: 'option-in-list', acceptedOptions: ['Current'] },
          requiredEvidence: [{ kind: evidenceKindNamed('DOCUMENT'), minimum: 1 }],
        },
      ],
    },
  ];
}

function secondExtinguisherVersion(vocabulary: Vocabulary): Section[] {
  const [physicalCondition, installationAndRecords] = extinguisherSections(vocabulary) as [Section, Section];
  const tighterPressure = (criterion: Criterion): Criterion =>
    criterion.code === 'PRESSURE'
      ? { ...criterion, rule: { type: 'numeric-range', unit: 'bar', minimum: '12', maximum: '16' } }
      : criterion;
  const unobstructedAccess: Criterion = {
    code: 'ACCESS',
    text: 'Access to the extinguisher unobstructed',
    severity: vocabulary.severities.find((severity) => severity.name === 'MAJOR')!,
    rule: { type: 'yes-no', expected: 'YES' },
    requiredEvidence: [],
  };
  return [
    { ...physicalCondition, criteria: physicalCondition.criteria.map(tighterPressure) },
    { ...installationAndRecords, criteria: [...installationAndRecords.criteria, unobstructedAccess] },
  ];
}

const pressureVesselSubsystems: Subsystem[] = [
  { code: 'ELECTRICAL', name: 'Electrical system' },
  { code: 'PRESSURE', name: 'Pressure system' },
];

function pressureVesselSections(vocabulary: Vocabulary): Section[] {
  const severityNamed = (name: string) => vocabulary.severities.find((severity) => severity.name === name)!;
  return [
    {
      title: 'Pressure system',
      subsystemCode: 'PRESSURE',
      criteria: [
        {
          code: 'RELIEF-VALVE',
          text: 'Relief valve opening pressure',
          severity: severityNamed('CRITICAL'),
          rule: { type: 'numeric-range', unit: 'bar', minimum: '7.5', maximum: '8.5' },
          requiredEvidence: [],
        },
        {
          code: 'LEAKS',
          text: 'No visible leaks',
          severity: severityNamed('MAJOR'),
          rule: { type: 'yes-no', expected: 'YES' },
          requiredEvidence: [],
        },
        {
          code: 'INSULATION',
          text: 'Insulation condition',
          severity: severityNamed('MINOR'),
          rule: { type: 'option-in-list', acceptedOptions: ['Good', 'Acceptable'] },
          requiredEvidence: [],
        },
      ],
    },
    {
      title: 'Electrical system',
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
          code: 'CONTROL-PANEL',
          text: 'Control panel status',
          severity: severityNamed('MINOR'),
          rule: { type: 'option-in-list', acceptedOptions: ['Working'] },
          requiredEvidence: [],
        },
      ],
    },
  ];
}

const compliantExtinguisherAnswers: Record<string, RawAnswer> = {
  PRESSURE: measured('13.5'),
  WEIGHT: measured('6.0'),
  SEAL: yes,
  HOSE: chosen('Good'),
  CORROSION: yes,
  SIGNAGE: yes,
  HEIGHT: measured('1.20'),
  TAG: chosen('Current'),
};

const compliantPressureVesselAnswers: Record<string, RawAnswer> = {
  'RELIEF-VALVE': measured('8.0'),
  LEAKS: yes,
  INSULATION: chosen('Acceptable'),
  GROUNDING: yes,
  'CONTROL-PANEL': chosen('Working'),
};

export async function seedDemoData(certiflow: Certiflow, demoControls: DemoControls, people: DemoPeople): Promise<void> {
  const realToday = demoControls.today();
  const vocabulary = await certiflow.schemas.vocabulary();
  const photo = vocabulary.evidenceKinds.find((kind) => kind.name === 'PHOTO')!;
  const document = vocabulary.evidenceKinds.find((kind) => kind.name === 'DOCUMENT')!;

  async function inspectAndClose(assetCode: string, answers: Record<string, RawAnswer>): Promise<{ inspectionId: string; findingIds: string[] }> {
    const inspectionId = await certiflow.inspections.assignInspection({
      assetCode,
      inspector: people.inspector,
      plannedDate: demoControls.today(),
      scope: { kind: 'full' },
      assignedBy: people.coordinator,
    });
    const inspection = await certiflow.inspections.findInspection(inspectionId);
    for (const response of inspection.responses) {
      const criterionCode = response.criterion.code;
      await certiflow.inspections.recordAnswer({
        inspectionId,
        criterionCode,
        answer: answers[criterionCode]!,
        answeredBy: people.inspector,
      });
      for (const requirement of response.criterion.requiredEvidence) {
        await certiflow.inspections.attachEvidence({
          inspectionId,
          criterionCode,
          evidence: {
            kind: requirement.kind,
            reference: `${assetCode}-${criterionCode}.${requirement.kind.name === photo.name ? 'jpg' : 'pdf'}`.toLowerCase(),
            description: requirement.kind.name === document.name ? 'Scanned maintenance tag' : 'Taken during the visit',
          },
          attachedBy: people.inspector,
        });
      }
    }
    const findingIds = await certiflow.inspections.closeInspection({ inspectionId, closedBy: people.inspector });
    return { inspectionId, findingIds };
  }

  demoControls.setToday(daysFrom(realToday, -60));
  await certiflow.catalog.registerAsset({
    code: 'EXT-0042',
    name: 'Lobby extinguisher',
    type: extinguisherType,
    location: 'Main building · Lobby',
    responsible: people.assetResponsible,
    characteristics: [
      { name: 'Agent', value: 'ABC powder' },
      { name: 'Capacity', value: '6 kg' },
    ],
    jurisdictionCode: 'DEMO-STRICT',
    subsystems: [],
    registeredBy: people.coordinator,
  });
  await certiflow.catalog.registerAsset({
    code: 'EXT-0043',
    name: 'Server room extinguisher',
    type: extinguisherType,
    location: 'Main building · Server room',
    responsible: people.assetResponsible,
    characteristics: [{ name: 'Agent', value: 'CO2' }],
    jurisdictionCode: 'DEMO-STANDARD',
    subsystems: [],
    registeredBy: people.coordinator,
  });
  await certiflow.catalog.registerAsset({
    code: 'BLR-0007',
    name: 'Boiler B',
    type: pressureVesselType,
    location: 'Plant 2 · Boiler room',
    responsible: people.assetResponsible,
    characteristics: [
      { name: 'Manufacturer', value: 'Calderas del Sur' },
      { name: 'Design pressure', value: '10 bar' },
    ],
    jurisdictionCode: 'DEMO-STRICT',
    subsystems: pressureVesselSubsystems,
    registeredBy: people.coordinator,
  });
  await certiflow.catalog.registerAsset({
    code: 'BLR-0012',
    name: 'Boiler C',
    type: pressureVesselType,
    location: 'Plant 3 · Boiler room',
    responsible: people.assetResponsible,
    characteristics: [{ name: 'Design pressure', value: '8 bar' }],
    jurisdictionCode: 'DEMO-CONDITIONAL',
    subsystems: pressureVesselSubsystems,
    registeredBy: people.coordinator,
  });
  await certiflow.catalog.registerAsset({
    code: 'LAB-0003',
    name: 'Metrology laboratory',
    type: 'Laboratory',
    location: 'Plant 1 · First floor',
    responsible: people.assetResponsible,
    characteristics: [],
    jurisdictionCode: 'DEMO-STRICT',
    subsystems: [],
    registeredBy: people.coordinator,
  });
  await certiflow.schemas.createInspectionSchema({
    name: 'Portable extinguisher inspection',
    assetType: extinguisherType,
    sections: extinguisherSections(vocabulary),
    requiredSubsystems: [],
    createdBy: people.coordinator,
  });
  await certiflow.schemas.createInspectionSchema({
    name: 'Pressure vessel inspection',
    assetType: pressureVesselType,
    sections: pressureVesselSections(vocabulary),
    requiredSubsystems: pressureVesselSubsystems.map((subsystem) => subsystem.code),
    createdBy: people.coordinator,
  });

  demoControls.setToday(daysFrom(realToday, -45));
  const lobbyInspection = await inspectAndClose('EXT-0042', {
    ...compliantExtinguisherAnswers,
    PRESSURE: measured('9.2'),
    SEAL: no,
    HOSE: chosen('Cracked'),
  });
  const [lowPressureFindingId, brokenSealFindingId] = lobbyInspection.findingIds as [string, string, string];

  demoControls.setToday(daysFrom(realToday, -44));
  const rechargeActionId = await certiflow.findings.planCorrectiveAction({
    findingId: lowPressureFindingId,
    description: 'Recharge the extinguisher and replace the gauge',
    responsible: people.assetResponsible,
    dueDate: daysFrom(realToday, -30),
    plannedBy: people.coordinator,
  });
  const sealActionId = await certiflow.findings.planCorrectiveAction({
    findingId: brokenSealFindingId,
    description: 'Fit a new safety seal and pin',
    responsible: people.assetResponsible,
    dueDate: daysFrom(realToday, -10),
    plannedBy: people.coordinator,
  });

  demoControls.setToday(daysFrom(realToday, -40));
  const serverRoomInspection = await inspectAndClose('EXT-0043', compliantExtinguisherAnswers);
  await certiflow.certification.issueCertificate({
    inspectionId: serverRoomInspection.inspectionId,
    issuedBy: people.certifier,
  });

  async function certifyEverySubsystem(assetCode: string, inspectionId: string): Promise<Record<string, string>> {
    const partialNumbers: Record<string, string> = {};
    for (const subsystem of pressureVesselSubsystems) {
      partialNumbers[subsystem.code] = await certiflow.certification.issueCertificate({
        inspectionId,
        subsystemCode: subsystem.code,
        issuedBy: people.certifier,
      });
    }
    await certiflow.certification.issueGlobalCertificate({
      assetCode,
      schemaVersion: 1,
      partialCertificateNumbers: Object.values(partialNumbers),
      issuedBy: people.certifier,
    });
    return partialNumbers;
  }

  demoControls.setToday(daysFrom(realToday, -38));
  const boilerInspection = await inspectAndClose('BLR-0007', compliantPressureVesselAnswers);
  const boilerPartialNumbers = await certifyEverySubsystem('BLR-0007', boilerInspection.inspectionId);

  demoControls.setToday(daysFrom(realToday, -32));
  await certiflow.findings.verifyCorrectiveAction({
    findingId: lowPressureFindingId,
    actionId: rechargeActionId,
    verifier: people.verifier,
    result: 'ACCEPTED',
    notes: 'Gauge reads 13.8 bar after the recharge',
  });

  demoControls.setToday(daysFrom(realToday, -20));
  await certiflow.certification.suspendCertificate({
    number: boilerPartialNumbers['PRESSURE']!,
    reason: 'Manufacturer recall of the relief valve',
    suspendedBy: people.certifier,
  });

  demoControls.setToday(daysFrom(realToday, -15));
  const followUpInspectionId = await certiflow.inspections.assignInspection({
    assetCode: 'EXT-0042',
    inspector: people.inspector,
    plannedDate: daysFrom(realToday, 3),
    scope: { kind: 'full' },
    assignedBy: people.coordinator,
  });
  await certiflow.inspections.recordAnswer({
    inspectionId: followUpInspectionId,
    criterionCode: 'WEIGHT',
    answer: measured('6.1'),
    answeredBy: people.inspector,
  });
  await certiflow.inspections.addObservation({
    inspectionId: followUpInspectionId,
    criterionCode: 'SEAL',
    text: 'New seal ordered, not fitted yet',
    observedBy: people.inspector,
  });

  demoControls.setToday(daysFrom(realToday, -12));
  await certiflow.findings.verifyCorrectiveAction({
    findingId: brokenSealFindingId,
    actionId: sealActionId,
    verifier: people.verifier,
    result: 'REJECTED',
    notes: 'The seal is still missing',
  });

  demoControls.setToday(daysFrom(realToday, -7));
  await certiflow.schemas.publishSchemaVersion({
    assetType: extinguisherType,
    sections: secondExtinguisherVersion(vocabulary),
    requiredSubsystems: [],
    effectiveFrom: `${daysFrom(realToday, 20)}T00:00:00.000Z`,
    publishedBy: people.coordinator,
  });

  demoControls.setToday(daysFrom(realToday, -5));
  const conditionalBoilerInspection = await inspectAndClose('BLR-0012', {
    ...compliantPressureVesselAnswers,
    INSULATION: chosen('Damaged'),
  });
  await certifyEverySubsystem('BLR-0012', conditionalBoilerInspection.inspectionId);

  demoControls.setToday(realToday);
}
