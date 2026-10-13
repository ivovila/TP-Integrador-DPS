import { createHttpCertiflow } from '../src/adapters/http/createHttpCertiflow';
import type { RawAnswer, Section, Vocabulary } from '../src/domain/schema';
import { DomainError } from '../src/ports/DomainError';
import { demoPeople } from '../src/ui/app/roster';

const apiUrl = process.env.CERTIFLOW_API_URL ?? 'http://localhost:8080';
const certiflow = createHttpCertiflow(apiUrl);
const { administrator, coordinator, inspector, assetResponsible, verifier, certifier } = demoPeople;

const extinguisherType = 'Fire extinguisher';
const pressureVesselType = 'Pressure vessel';
const today = new Date().toISOString().slice(0, 10);

function daysFromToday(days: number): string {
  const shifted = new Date(`${today}T00:00:00Z`);
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

function nextExtinguisherVersion(vocabulary: Vocabulary): Section[] {
  const [physicalCondition, installationAndRecords] = extinguisherSections(vocabulary) as [Section, Section];
  return [
    {
      ...physicalCondition,
      criteria: physicalCondition.criteria.map((criterion) =>
        criterion.code === 'PRESSURE'
          ? { ...criterion, rule: { type: 'numeric-range', unit: 'bar', minimum: '12', maximum: '16' } }
          : criterion,
      ),
    },
    {
      ...installationAndRecords,
      criteria: [
        ...installationAndRecords.criteria,
        {
          code: 'ACCESS',
          text: 'Access to the extinguisher unobstructed',
          severity: vocabulary.severities.find((severity) => severity.name === 'MAJOR')!,
          rule: { type: 'yes-no', expected: 'YES' },
          requiredEvidence: [],
        },
      ],
    },
  ];
}

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
      title: 'Electrical installation',
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
          code: 'PANEL',
          text: 'Control panel labelled',
          severity: severityNamed('MINOR'),
          rule: { type: 'yes-no', expected: 'YES' },
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
  PANEL: yes,
};

function said(step: string): void {
  console.log(`  ${step}`);
}

async function alreadySeeded(): Promise<boolean> {
  try {
    await certiflow.catalog.findAsset('EXT-0042');
    return true;
  } catch (failure) {
    if (failure instanceof DomainError && failure.name === 'AssetNotFoundException') {
      return false;
    }
    throw failure;
  }
}

async function registerAsset(code: string, name: string, type: string, location: string, jurisdictionCode: string, withSubsystems: boolean) {
  await certiflow.catalog.registerAsset({
    code,
    name,
    type,
    location,
    responsible: assetResponsible,
    characteristics: withSubsystems
      ? [
          { name: 'Manufacturer', value: 'Calderas del Sur' },
          { name: 'Design pressure', value: '10 bar' },
        ]
      : [{ name: 'Agent', value: 'ABC powder' }],
    jurisdictionCode,
    subsystems: withSubsystems
      ? [
          { code: 'ELECTRICAL', name: 'Electrical installation' },
          { code: 'PRESSURE', name: 'Pressure system' },
        ]
      : [],
    registeredBy: administrator,
  });
  said(`asset ${code} · ${name} (${jurisdictionCode})`);
}

async function inspectAndClose(assetCode: string, answers: Record<string, RawAnswer>) {
  const inspectionId = await certiflow.inspections.assignInspection({
    assetCode,
    inspector,
    plannedDate: today,
    scope: { kind: 'full' },
    assignedBy: coordinator,
  });
  const inspection = await certiflow.inspections.findInspection(inspectionId);
  for (const response of inspection.responses) {
    const criterionCode = response.criterion.code;
    await certiflow.inspections.recordAnswer({ inspectionId, criterionCode, answer: answers[criterionCode]!, answeredBy: inspector });
    for (const requirement of response.criterion.requiredEvidence) {
      const extension = requirement.kind.name === 'PHOTO' ? 'jpg' : 'pdf';
      await certiflow.inspections.attachEvidence({
        inspectionId,
        criterionCode,
        evidence: {
          kind: requirement.kind,
          reference: `${assetCode}-${criterionCode}.${extension}`.toLowerCase(),
          description: requirement.kind.name === 'PHOTO' ? 'Taken during the visit' : 'Scanned maintenance tag',
        },
        attachedBy: inspector,
      });
    }
  }
  const findingIds = await certiflow.inspections.closeInspection({ inspectionId, closedBy: inspector });
  said(`inspection of ${assetCode} closed with ${findingIds.length} finding(s)`);
  return { inspectionId, findingIds };
}

console.log(`Loading demo data into ${apiUrl}`);

if (await alreadySeeded()) {
  console.log('The demo data is already there (asset EXT-0042 exists). Nothing was loaded.');
  process.exit(0);
}

const vocabulary = await certiflow.schemas.vocabulary();

await registerAsset('EXT-0042', 'Lobby extinguisher', extinguisherType, 'Main building · Lobby', 'DEMO-STRICT', false);
await registerAsset('EXT-0043', 'Server room extinguisher', extinguisherType, 'Main building · Server room', 'DEMO-CONDITIONAL', false);
await registerAsset('EXT-0044', 'Warehouse extinguisher', extinguisherType, 'Warehouse A', 'DEMO-STANDARD', false);
await registerAsset('BLR-0007', 'Boiler A', pressureVesselType, 'Plant 2 · Boiler room', 'DEMO-STANDARD', true);
await registerAsset('BLR-0008', 'Boiler B', pressureVesselType, 'Plant 2 · Boiler room', 'DEMO-CONDITIONAL', true);
await registerAsset('LAB-0003', 'Metrology laboratory', 'Laboratory', 'Plant 1 · First floor', 'DEMO-STANDARD', false);

await certiflow.schemas.createInspectionSchema({
  name: 'Portable extinguisher inspection',
  assetType: extinguisherType,
  sections: extinguisherSections(vocabulary),
  requiredSubsystems: [],
  createdBy: administrator,
});
await certiflow.schemas.createInspectionSchema({
  name: 'Pressure vessel inspection',
  assetType: pressureVesselType,
  sections: pressureVesselSections(vocabulary),
  requiredSubsystems: ['ELECTRICAL', 'PRESSURE'],
  createdBy: administrator,
});
said('schemas for fire extinguishers (8 criteria) and pressure vessels (2 subsystems)');

console.log('F3 · the same major finding under two jurisdictions');
const strictInspection = await inspectAndClose('EXT-0042', { ...compliantExtinguisherAnswers, SEAL: no });
const strictActionId = await certiflow.findings.planCorrectiveAction({
  findingId: strictInspection.findingIds[0]!,
  description: 'Fit a new safety seal and pin',
  responsible: assetResponsible,
  dueDate: daysFromToday(15),
  plannedBy: assetResponsible,
});
await certiflow.findings.verifyCorrectiveAction({
  findingId: strictInspection.findingIds[0]!,
  actionId: strictActionId,
  verifier,
  result: 'REJECTED',
  notes: 'The seal is still missing',
});
said('EXT-0042 (DEMO-STRICT): not certifiable; its corrective action was verified and rejected');
const conditionalInspection = await inspectAndClose('EXT-0043', { ...compliantExtinguisherAnswers, SEAL: no });
const conditionalCertificate = await certiflow.certification.issueCertificate({
  inspectionId: conditionalInspection.inspectionId,
  issuedBy: certifier,
});
said(`EXT-0043 (DEMO-CONDITIONAL): conditional certificate ${conditionalCertificate}`);

console.log('Findings, corrective actions and a suspended certificate');
const warehouseInspection = await inspectAndClose('EXT-0044', { ...compliantExtinguisherAnswers, HOSE: chosen('Cracked') });
const hoseActionId = await certiflow.findings.planCorrectiveAction({
  findingId: warehouseInspection.findingIds[0]!,
  description: 'Replace the discharge hose',
  responsible: assetResponsible,
  dueDate: daysFromToday(10),
  plannedBy: assetResponsible,
});
await certiflow.findings.verifyCorrectiveAction({
  findingId: warehouseInspection.findingIds[0]!,
  actionId: hoseActionId,
  verifier,
  result: 'ACCEPTED',
  notes: 'New hose fitted',
});
const warehouseCertificate = await certiflow.certification.issueCertificate({
  inspectionId: warehouseInspection.inspectionId,
  issuedBy: certifier,
});
await certiflow.certification.suspendCertificate({
  number: warehouseCertificate,
  reason: 'Tampering reported by the building manager',
  suspendedBy: certifier,
});
said(`EXT-0044: minor finding closed by an accepted action; certificate ${warehouseCertificate} issued and then suspended`);

console.log('F2 · a version scheduled for later');
const openInspectionId = await certiflow.inspections.assignInspection({
  assetCode: 'EXT-0044',
  inspector,
  plannedDate: daysFromToday(3),
  scope: { kind: 'full' },
  assignedBy: coordinator,
});
await certiflow.inspections.recordAnswer({ inspectionId: openInspectionId, criterionCode: 'WEIGHT', answer: measured('6.1'), answeredBy: inspector });
await certiflow.inspections.addObservation({
  inspectionId: openInspectionId,
  criterionCode: 'SEAL',
  text: 'Seal replaced after the tampering report',
  observedBy: inspector,
});
const scheduledVersion = await certiflow.schemas.publishSchemaVersion({
  assetType: extinguisherType,
  sections: nextExtinguisherVersion(vocabulary),
  requiredSubsystems: [],
  effectiveFrom: `${daysFromToday(30)}T03:00:00.000Z`,
  publishedBy: administrator,
});
said(`open inspection ${openInspectionId} keeps version 1; version ${scheduledVersion.number} takes effect in 30 days`);

console.log('F1 · partial certificates by subsystem');
const boilerAInspection = await inspectAndClose('BLR-0007', { ...compliantPressureVesselAnswers, 'RELIEF-VALVE': measured('9.5') });
const boilerAElectrical = await certiflow.certification.issueCertificate({
  inspectionId: boilerAInspection.inspectionId,
  subsystemCode: 'ELECTRICAL',
  issuedBy: certifier,
});
said(`BLR-0007: ELECTRICAL certified (${boilerAElectrical}); PRESSURE blocked by a critical finding`);
const boilerBInspection = await inspectAndClose('BLR-0008', compliantPressureVesselAnswers);
const boilerBElectrical = await certiflow.certification.issueCertificate({
  inspectionId: boilerBInspection.inspectionId,
  subsystemCode: 'ELECTRICAL',
  issuedBy: certifier,
});
const boilerBPressure = await certiflow.certification.issueCertificate({
  inspectionId: boilerBInspection.inspectionId,
  subsystemCode: 'PRESSURE',
  issuedBy: certifier,
});
const boilerBGlobal = await certiflow.certification.issueGlobalCertificate({
  assetCode: 'BLR-0008',
  schemaVersion: 1,
  partialCertificateNumbers: [boilerBElectrical, boilerBPressure],
  issuedBy: certifier,
});
said(`BLR-0008: partials ${boilerBElectrical} and ${boilerBPressure}, global certificate ${boilerBGlobal}`);

console.log('Done.');
