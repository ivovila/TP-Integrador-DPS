// @vitest-environment jsdom
import { cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { MemoryRouter } from 'react-router';
import { afterEach, expect, test } from 'vitest';
import { createInMemoryCertiflow, type InMemoryCertiflow } from './adapters/inmemory/createInMemoryCertiflow';
import { ApplicationProvider } from './ui/app/CertiflowContext';
import { demoPeople } from './ui/app/roster';
import { ApplicationRoutes } from './ui/app/routes';
import { SessionProvider } from './ui/app/SessionContext';

afterEach(() => {
  cleanup();
  window.localStorage.clear();
});

function open(system: InMemoryCertiflow, path: string): void {
  render(
    <ApplicationProvider application={system}>
      <SessionProvider>
        <MemoryRouter initialEntries={[path]}>
          <ApplicationRoutes />
        </MemoryRouter>
      </SessionProvider>
    </ApplicationProvider>,
  );
}

async function fill(scope: ReturnType<typeof within>, label: string, value: string): Promise<void> {
  fireEvent.change(await scope.findByLabelText(label), { target: { value } });
}

async function press(scope: ReturnType<typeof within>, name: string): Promise<void> {
  fireEvent.click(await waitFor(() => scope.queryByRole('button', { name }) ?? scope.getByRole('link', { name })));
}

const page = within(document.body);

async function systemWithAnAssignedInspection(): Promise<InMemoryCertiflow> {
  const system = createInMemoryCertiflow('2026-03-02');
  const { certiflow } = system;
  const vocabulary = await certiflow.schemas.vocabulary();
  const severityNamed = (name: string) => vocabulary.severities.find((severity) => severity.name === name)!;
  await certiflow.catalog.registerAsset({
    code: 'EXT-1',
    name: 'Lobby extinguisher',
    type: 'Fire extinguisher',
    location: 'Lobby',
    responsible: demoPeople.assetResponsible,
    characteristics: [],
    jurisdictionCode: 'DEMO-STANDARD',
    subsystems: [],
    registeredBy: demoPeople.coordinator,
  });
  await certiflow.schemas.createInspectionSchema({
    name: 'Extinguisher inspection',
    assetType: 'Fire extinguisher',
    sections: [
      {
        title: 'Safety',
        criteria: [
          {
            code: 'PRESSURE',
            text: 'Gauge pressure',
            severity: severityNamed('CRITICAL'),
            rule: { type: 'numeric-range', unit: 'bar', minimum: '10', maximum: '16' },
            requiredEvidence: [{ kind: vocabulary.evidenceKinds[0]!, minimum: 1 }],
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
    ],
    requiredSubsystems: [],
    createdBy: demoPeople.coordinator,
  });
  await certiflow.inspections.assignInspection({
    assetCode: 'EXT-1',
    inspector: demoPeople.inspector,
    plannedDate: '2026-03-02',
    scope: { kind: 'full' },
    assignedBy: demoPeople.coordinator,
  });
  return system;
}

async function criterionCard(code: string) {
  return within((await screen.findByText(code)).closest('.criterion') as HTMLElement);
}

test('an inspection is executed, closed, corrected and certified through the screens', async () => {
  open(await systemWithAnAssignedInspection(), '/inspections/INS-1');

  const pressure = await criterionCard('PRESSURE');
  await fill(pressure, 'Measurement (bar)', '12');
  await press(pressure, 'Record answer');
  await pressure.findByText('12 bar');
  expect(pressure.getByText('pending')).toBeDefined();
  await fill(pressure, 'File reference', 'gauge.jpg');
  await press(pressure, 'Attach evidence');
  await pressure.findByText('approved');
  await fill(pressure, 'Observation', 'Gauge replaced last month');
  await press(pressure, 'Add observation');
  await pressure.findByText(/Gauge replaced last month/);

  const seal = await criterionCard('SEAL');
  await fill(seal, 'Answer', 'no');
  await press(seal, 'Record answer');
  await seal.findByText('rejected');

  await press(page, 'Close inspection');
  await screen.findByText('Findings raised');
  await waitFor(() => expect(screen.queryByText('Close inspection')).toBeNull());
  expect(await screen.findByText('Open finding SEAL severity Major')).toBeDefined();
  expect(screen.getByText('STANDARD · jurisdiction DEMO-STANDARD')).toBeDefined();

  await press(page, 'FND-1');
  await screen.findByText(/Finding FND-1/);
  await fill(page, 'What will be done', 'Fit a new safety seal');
  await fill(page, 'Responsible', demoPeople.assetResponsible.id);
  await fill(page, 'Due date', '2026-03-20');
  await press(page, 'Plan corrective action');
  await screen.findByText(/Action 1 · Fit a new safety seal/);

  await fill(page, 'Today (demo)', '2026-03-25');
  await screen.findByText('overdue');

  await fill(page, 'Acting as', demoPeople.assetResponsible.id);
  await press(page, 'Accept');
  expect((await screen.findByRole('alert')).textContent).toContain('VerifierMustDifferFromResponsibleException');

  await fill(page, 'Acting as', demoPeople.verifier.id);
  await press(page, 'Accept');
  await screen.findByText(/Verification accepted/);
  await waitFor(() => expect(screen.queryByText('Plan corrective action')).toBeNull());
  expect(screen.queryByText('Accept')).toBeNull();

  await fill(page, 'Acting as', demoPeople.certifier.id);
  await press(page, 'INS-1');
  await screen.findByText('25 Mar 2026 to 24 Mar 2027');
  await press(page, 'Issue certificate');
  await screen.findByText(/Certificate CERT-1/);
  expect(screen.getByText('active')).toBeDefined();

  await fill(page, 'Reason for the suspension', 'Manufacturer recall');
  await press(page, 'Suspend certificate');
  await screen.findByText('suspended');
  expect(screen.queryByText('Suspend certificate')).toBeNull();
});

test('a closed inspection is rectified through the screens and gains a revision', async () => {
  const system = await systemWithAnAssignedInspection();
  const inspectionId = 'INS-1';
  await system.certiflow.inspections.recordAnswer({
    inspectionId,
    criterionCode: 'PRESSURE',
    answer: { kind: 'number', value: '12' },
    answeredBy: demoPeople.inspector,
  });
  await system.certiflow.inspections.attachEvidence({
    inspectionId,
    criterionCode: 'PRESSURE',
    evidence: { kind: { name: 'PHOTO', label: 'Photo' }, reference: 'gauge.jpg', description: '' },
    attachedBy: demoPeople.inspector,
  });
  await system.certiflow.inspections.recordAnswer({
    inspectionId,
    criterionCode: 'SEAL',
    answer: { kind: 'yes-no', value: true },
    answeredBy: demoPeople.inspector,
  });
  await system.certiflow.inspections.closeInspection({ inspectionId, closedBy: demoPeople.inspector });
  open(system, '/inspections/INS-1');

  const rectification = within((await screen.findByText('Rectify an answer')).closest('.card') as HTMLElement);
  await fill(rectification, 'Criterion', 'SEAL');
  await fill(rectification, 'Answer', 'no');
  await fill(rectification, 'Reason', 'The seal was broken, typed by mistake');
  await press(rectification, 'Rectify');

  await screen.findByText('The seal was broken, typed by mistake');
  expect(screen.getByText('Findings raised')).toBeDefined();
  expect(await screen.findByText('Open finding SEAL severity Major')).toBeDefined();
});

async function systemWithAClosedBoilerInspection(): Promise<InMemoryCertiflow> {
  const system = createInMemoryCertiflow('2026-03-02');
  const { certiflow } = system;
  const vocabulary = await certiflow.schemas.vocabulary();
  const severityNamed = (name: string) => vocabulary.severities.find((severity) => severity.name === name)!;
  await certiflow.catalog.registerAsset({
    code: 'BLR-1',
    name: 'Boiler',
    type: 'Pressure vessel',
    location: 'Basement',
    responsible: demoPeople.assetResponsible,
    characteristics: [],
    jurisdictionCode: 'DEMO-CONDITIONAL',
    subsystems: [
      { code: 'ELECTRICAL', name: 'Electrical system' },
      { code: 'PRESSURE', name: 'Pressure system' },
    ],
    registeredBy: demoPeople.coordinator,
  });
  await certiflow.schemas.createInspectionSchema({
    name: 'Boiler inspection',
    assetType: 'Pressure vessel',
    sections: [
      {
        title: 'Pressure',
        subsystemCode: 'PRESSURE',
        criteria: [
          {
            code: 'INSULATION',
            text: 'Insulation in good condition',
            severity: severityNamed('MINOR'),
            rule: { type: 'yes-no', expected: 'YES' },
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
        ],
      },
    ],
    requiredSubsystems: ['ELECTRICAL', 'PRESSURE'],
    createdBy: demoPeople.coordinator,
  });
  const inspectionId = await certiflow.inspections.assignInspection({
    assetCode: 'BLR-1',
    inspector: demoPeople.inspector,
    plannedDate: '2026-03-02',
    scope: { kind: 'full' },
    assignedBy: demoPeople.coordinator,
  });
  for (const [criterionCode, value] of [['INSULATION', false], ['GROUNDING', true]] as const) {
    await certiflow.inspections.recordAnswer({
      inspectionId,
      criterionCode,
      answer: { kind: 'yes-no', value },
      answeredBy: demoPeople.inspector,
    });
  }
  await certiflow.inspections.closeInspection({ inspectionId, closedBy: demoPeople.inspector });
  return system;
}

test('partial certificates are issued per subsystem and a global certificate is derived from them', async () => {
  const system = await systemWithAClosedBoilerInspection();
  open(system, '/inspections/INS-1');

  await screen.findByText('Subsystem ELECTRICAL');
  expect(await screen.findByText('conditional')).toBeDefined();
  expect(screen.getByText(/Resolve and verify open nonconformity/)).toBeDefined();
  await press(page, 'Issue ELECTRICAL certificate');
  await screen.findByText(/Certificate CERT-1/);
  expect(screen.getAllByText('Subsystem ELECTRICAL').length).toBeGreaterThan(0);

  await system.certiflow.certification.issueCertificate({
    inspectionId: 'INS-1',
    subsystemCode: 'PRESSURE',
    issuedBy: demoPeople.certifier,
  });
  await press(page, 'BLR-1');
  await screen.findByText('Global certificate');
  await press(page, 'Issue global certificate');
  await screen.findByText(/Certificate CERT-3/);
  expect(screen.getByText('ALL-REQUIRED-SUBSYSTEMS · jurisdiction DEMO-CONDITIONAL')).toBeDefined();

  await press(page, 'CERT-2');
  await screen.findByText(/Certificate CERT-2/);
  await fill(page, 'Reason for the suspension', 'Insulation removed');
  await press(page, 'Suspend certificate');
  await screen.findByText('suspended');
  expect((await system.certiflow.certification.findCertificate('CERT-3')).status).toBe('DEPENDENCY_INVALID');
});

test('a schema is created and a new version published through the editor', async () => {
  open(createInMemoryCertiflow('2026-03-02'), '/schemas/new');

  await screen.findByText('Add section');
  await fill(page, 'Schema name', 'Laboratory inspection');
  await fill(page, 'Asset type', 'Laboratory');
  await press(page, 'Add section');
  await fill(page, 'Section title', 'Calibration');
  await fill(page, 'Code', 'TEMP');
  await fill(page, 'Text', 'Room temperature');
  await fill(page, 'Unit', '°C');
  await fill(page, 'Minimum', '18');
  await fill(page, 'Maximum', '24');
  await press(page, 'Require evidence');
  await press(page, 'Create schema');

  await screen.findByText('Inspection schema for Laboratory');
  await press(page, 'Version 1');
  await screen.findByText('Between 18 and 24 °C');
  expect(screen.getByText('1 × Photo')).toBeDefined();

  await press(page, 'Laboratory');
  await screen.findByText('Publish new version');
  await press(page, 'Publish new version');
  await screen.findByText('Publish version 2');
  await fill(page, 'Maximum', '26');
  await fill(page, 'Effective from', '2026-04-01T00:00');
  await press(page, 'Publish version');
  await screen.findByText('Version 2');
  expect(screen.getByText('scheduled')).toBeDefined();
  await press(page, 'Version 2');
  await screen.findByText('Between 18 and 26 °C');
});

test('an asset is registered and an inspection assigned through the forms', async () => {
  const system = await systemWithAnAssignedInspection();
  open(system, '/assets/new');

  await fill(page, 'Code', 'EXT-2');
  await fill(page, 'Name', 'Warehouse extinguisher');
  await fill(page, 'Type', 'Fire extinguisher');
  await fill(page, 'Location', 'Warehouse A');
  await fill(page, 'Responsible', demoPeople.assetResponsible.id);
  await fill(page, 'Jurisdiction', 'DEMO-STRICT');
  await press(page, 'Register asset');

  await screen.findByText('Asset EXT-2');
  expect(screen.getByText('DEMO-STRICT')).toBeDefined();
  await press(page, 'Assign inspection');
  await screen.findByText('Whole schema');
  await fill(page, 'Inspector', demoPeople.coordinator.id);
  await fill(page, 'Planned date', '2026-03-05');
  await press(page, 'Assign inspection');
  expect((await screen.findByRole('alert')).textContent).toContain('AssignerCannotInspectException');

  await fill(page, 'Inspector', demoPeople.inspector.id);
  await press(page, 'Assign inspection');
  await screen.findByText(/Inspection INS-2/);
  expect(screen.getByText('0 of 2 evaluated')).toBeDefined();
});
