import { beforeEach, expect, test } from 'vitest';
import type { Certiflow } from '../../ports/Certiflow';
import { createInMemoryCertiflow } from './createInMemoryCertiflow';
import { seedDemoData } from './seedDemoData';

const people = {
  coordinator: { id: 'EMP-001', name: 'Paula Rios' },
  inspector: { id: 'EMP-002', name: 'Ines Duarte' },
  assetResponsible: { id: 'EMP-003', name: 'Raul Mendez' },
  verifier: { id: 'EMP-004', name: 'Vera Soto' },
  certifier: { id: 'EMP-005', name: 'Carlos Vidal' },
};

let certiflow: Certiflow;

beforeEach(async () => {
  const system = createInMemoryCertiflow('2026-10-12');
  certiflow = system.certiflow;
  await seedDemoData(certiflow, system.demoControls, people);
});

test('the demo schema has eight criteria using the three rule types and a second version scheduled for later', async () => {
  const schema = await certiflow.schemas.findSchema('Fire extinguisher');
  const firstVersionCriteria = schema.versions[0]!.sections.flatMap((section) => section.criteria);
  expect(schema.currentVersion).toBe(1);
  expect(schema.latestPublishedVersion).toBe(2);
  expect(schema.versions[1]!.effectiveFrom).toBe('2026-11-01T00:00:00.000Z');
  expect(firstVersionCriteria).toHaveLength(8);
  expect(new Set(firstVersionCriteria.map((criterion) => criterion.rule.type)).size).toBe(3);
});

test('the demo pressure vessel schema splits its criteria into two subsystems', async () => {
  const schema = await certiflow.schemas.findSchema('Pressure vessel');
  expect(schema.versions[0]!.requiredSubsystems).toEqual(['ELECTRICAL', 'PRESSURE']);
  expect(schema.versions[0]!.sections.map((section) => section.subsystemCode)).toEqual(['PRESSURE', 'ELECTRICAL']);
});

test('the demo has an open inspection that keeps the previous schema version', async () => {
  const openInspections = await certiflow.inspections.listInspections({ status: 'open' });
  expect(openInspections).toMatchObject([{ assetCode: 'EXT-0042', schemaVersion: 1 }]);
});

test('the demo has findings of every severity, one of them closed and one with an overdue action', async () => {
  const findings = await certiflow.findings.listFindings({ assetCode: 'EXT-0042' });
  const summary = await certiflow.findings.generateFindingsSummary('EXT-0042');
  expect(findings.map((finding) => [finding.severity.name, finding.open])).toEqual([
    ['CRITICAL', false],
    ['MAJOR', true],
    ['MINOR', true],
  ]);
  expect(summary.findings.map((line) => line.overdueActions)).toEqual([0, 1, 0]);
});

test('the demo has whole-asset, partial and global certificates under different jurisdictions', async () => {
  const certificates = await certiflow.certification.listCertificates({});
  const describe = (certificate: (typeof certificates)[number]) => [
    certificate.assetCode,
    certificate.scope.kind === 'subsystem' ? certificate.scope.subsystemCode : certificate.basis.kind,
    certificate.modality,
    certificate.status,
  ];
  expect(certificates.map(describe)).toEqual([
    ['EXT-0043', 'inspection', 'APPROVED', 'ACTIVE'],
    ['BLR-0007', 'ELECTRICAL', 'APPROVED', 'ACTIVE'],
    ['BLR-0007', 'PRESSURE', 'APPROVED', 'SUSPENDED'],
    ['BLR-0007', 'partialCertificates', 'APPROVED', 'DEPENDENCY_INVALID'],
    ['BLR-0012', 'ELECTRICAL', 'APPROVED', 'ACTIVE'],
    ['BLR-0012', 'PRESSURE', 'CONDITIONAL', 'ACTIVE'],
    ['BLR-0012', 'partialCertificates', 'CONDITIONAL', 'ACTIVE'],
  ]);
});

test('a conditional global certificate lasts no longer than its shortest partial', async () => {
  const [, , , , electrical, pressure, global] = await certiflow.certification.listCertificates({});
  expect(electrical!.validUntil).toBe('2027-04-06');
  expect(pressure!.validUntil).toBe('2026-11-05');
  expect(global).toMatchObject({ validFrom: '2026-10-07', validUntil: '2026-11-05' });
  expect(global!.decision.conditions.map((condition) => condition.criterionCode)).toEqual(['INSULATION']);
});

test('seeding leaves the clock on the real date', async () => {
  const system = createInMemoryCertiflow('2026-10-12');
  await seedDemoData(system.certiflow, system.demoControls, people);
  expect(system.demoControls.today()).toBe('2026-10-12');
});
