// @vitest-environment jsdom
import { cleanup, configure, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { MemoryRouter } from 'react-router';
import { afterEach, beforeEach, expect, test } from 'vitest';
import { createHttpCertiflow } from './adapters/http/createHttpCertiflow';
import type { Certiflow } from './ports/Certiflow';
import { ApplicationProvider } from './ui/app/CertiflowContext';
import { demoPeople } from './ui/app/roster';
import { ApplicationRoutes } from './ui/app/routes';
import { SessionProvider } from './ui/app/SessionContext';

configure({ asyncUtilTimeout: 10000 });

const certiflow: Certiflow = createHttpCertiflow(process.env.CERTIFLOW_API_URL!);
const today = new Date().toISOString().slice(0, 10);

function daysFromToday(days: number): string {
  const shifted = new Date(`${today}T00:00:00Z`);
  shifted.setUTCDate(shifted.getUTCDate() + days);
  return shifted.toISOString().slice(0, 10);
}

let suffix: string;
const unique = (base: string) => `${base}-${suffix}`;

beforeEach(() => {
  suffix = `${Date.now().toString(36)}${Math.random().toString(36).slice(2, 7)}`;
});

afterEach(() => {
  cleanup();
  window.localStorage.clear();
});

function open(path: string): void {
  render(
    <ApplicationProvider application={{ certiflow }}>
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

async function cardTitled(title: string) {
  return within((await screen.findByText(title)).closest('.card') as HTMLElement);
}

async function criterionCard(code: string) {
  return within((await screen.findByText(code)).closest('.criterion') as HTMLElement);
}

async function certificateNumberShown(): Promise<string> {
  const heading = await screen.findByText(/Certificate CERT-\d+/);
  return heading.textContent!.match(/CERT-\d+/)![0];
}

async function extinguisherWithAnAssignedInspection(): Promise<{ assetCode: string; assetType: string; inspectionId: string }> {
  const assetCode = unique('EXT');
  const assetType = unique('Fire extinguisher');
  const vocabulary = await certiflow.schemas.vocabulary();
  const severityNamed = (name: string) => vocabulary.severities.find((severity) => severity.name === name)!;
  await certiflow.catalog.registerAsset({
    code: assetCode,
    name: 'Lobby extinguisher',
    type: assetType,
    location: 'Lobby',
    responsible: demoPeople.assetResponsible,
    characteristics: [],
    jurisdictionCode: 'DEMO-STANDARD',
    subsystems: [],
    registeredBy: demoPeople.coordinator,
  });
  await certiflow.schemas.createInspectionSchema({
    name: 'Extinguisher inspection',
    assetType,
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
  const inspectionId = await certiflow.inspections.assignInspection({
    assetCode,
    inspector: demoPeople.inspector,
    plannedDate: today,
    scope: { kind: 'full' },
    assignedBy: demoPeople.coordinator,
  });
  return { assetCode, assetType, inspectionId };
}

test('an inspection is executed, closed, corrected and certified through the screens', async () => {
  const { inspectionId } = await extinguisherWithAnAssignedInspection();
  open(`/inspections/${inspectionId}`);
  await fill(page, 'Acting as', demoPeople.inspector.id);

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
  const raisedFindings = await cardTitled('Findings raised');
  await waitFor(() => expect(screen.queryByText('Close inspection')).toBeNull());
  expect(await screen.findByText('Open finding SEAL severity Major')).toBeDefined();
  expect(screen.getByText('STANDARD · jurisdiction DEMO-STANDARD')).toBeDefined();

  const findingLink = await raisedFindings.findByRole('link');
  const findingId = findingLink.textContent!;
  fireEvent.click(findingLink);
  await screen.findByText(new RegExp(`Finding ${findingId}`));
  expect(screen.queryByRole('button', { name: 'Plan corrective action' })).toBeNull();
  await fill(page, 'Acting as', demoPeople.assetResponsible.id);
  await fill(page, 'What will be done', 'Fit a new safety seal');
  await fill(page, 'Responsible', demoPeople.assetResponsible.id);
  await fill(page, 'Due date', daysFromToday(20));
  await press(page, 'Plan corrective action');
  await screen.findByText(/Action 1 · Fit a new safety seal/);

  expect(screen.queryByRole('button', { name: 'Accept' })).toBeNull();
  await fill(page, 'Acting as', demoPeople.verifier.id);
  await press(page, 'Accept');
  await screen.findByText(/Verification accepted/);
  await waitFor(() => expect(screen.queryByText('Plan corrective action')).toBeNull());
  expect(screen.queryByText('Accept')).toBeNull();

  await fill(page, 'Acting as', demoPeople.certifier.id);
  await press(page, inspectionId);
  await press(page, 'Issue certificate');
  const certificateNumber = await certificateNumberShown();
  expect(screen.getByText('active')).toBeDefined();

  await fill(page, 'Reason for the suspension', 'Manufacturer recall');
  await press(page, 'Suspend certificate');
  await screen.findByText('suspended');
  expect(screen.queryByText('Suspend certificate')).toBeNull();
  expect((await certiflow.certification.findCertificate(certificateNumber)).status).toBe('SUSPENDED');
});

test('a closed inspection is rectified through the screens and gains a revision', async () => {
  const { inspectionId } = await extinguisherWithAnAssignedInspection();
  await certiflow.inspections.recordAnswer({
    inspectionId,
    criterionCode: 'PRESSURE',
    answer: { kind: 'number', value: '12' },
    answeredBy: demoPeople.inspector,
  });
  await certiflow.inspections.attachEvidence({
    inspectionId,
    criterionCode: 'PRESSURE',
    evidence: { kind: { name: 'PHOTO', label: 'Photo' }, reference: 'gauge.jpg', description: '' },
    attachedBy: demoPeople.inspector,
  });
  await certiflow.inspections.recordAnswer({
    inspectionId,
    criterionCode: 'SEAL',
    answer: { kind: 'yes-no', value: true },
    answeredBy: demoPeople.inspector,
  });
  await certiflow.inspections.closeInspection({ inspectionId, closedBy: demoPeople.inspector });
  open(`/inspections/${inspectionId}`);
  await fill(page, 'Acting as', demoPeople.inspector.id);

  const rectification = await cardTitled('Rectify an answer');
  await fill(rectification, 'Criterion', 'SEAL');
  await fill(rectification, 'Answer', 'no');
  await fill(rectification, 'Reason', 'The seal was broken, typed by mistake');
  await press(rectification, 'Rectify');

  await screen.findByText('The seal was broken, typed by mistake');
  expect(screen.getByText('Findings raised')).toBeDefined();
  expect(await screen.findByText('Open finding SEAL severity Major')).toBeDefined();
  expect((await certiflow.inspections.findInspection(inspectionId)).revisions).toHaveLength(2);
});

async function boilerWithAClosedInspection(): Promise<{ assetCode: string; inspectionId: string }> {
  const assetCode = unique('BLR');
  const assetType = unique('Pressure vessel');
  const vocabulary = await certiflow.schemas.vocabulary();
  const severityNamed = (name: string) => vocabulary.severities.find((severity) => severity.name === name)!;
  await certiflow.catalog.registerAsset({
    code: assetCode,
    name: 'Boiler',
    type: assetType,
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
    assetType,
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
    assetCode,
    inspector: demoPeople.inspector,
    plannedDate: today,
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
  return { assetCode, inspectionId };
}

test('partial certificates are issued per subsystem and a global certificate is derived from them', async () => {
  const { assetCode, inspectionId } = await boilerWithAClosedInspection();
  open(`/inspections/${inspectionId}`);
  await fill(page, 'Acting as', demoPeople.certifier.id);

  await screen.findByText('Subsystem ELECTRICAL');
  expect(await screen.findByText('conditional')).toBeDefined();
  expect(screen.getByText(/Resolve and verify open nonconformity/)).toBeDefined();
  await press(page, 'Issue ELECTRICAL certificate');
  await certificateNumberShown();
  expect(screen.getAllByText('Subsystem ELECTRICAL').length).toBeGreaterThan(0);

  const pressureCertificateNumber = await certiflow.certification.issueCertificate({
    inspectionId,
    subsystemCode: 'PRESSURE',
    issuedBy: demoPeople.certifier,
  });
  await press(page, assetCode);
  await screen.findByText('Global certificate');
  await press(page, 'Issue global certificate');
  await screen.findByText('ALL-REQUIRED-SUBSYSTEMS · jurisdiction DEMO-CONDITIONAL');
  const globalCertificateNumber = await certificateNumberShown();

  await press(page, pressureCertificateNumber);
  await screen.findByText(new RegExp(`Certificate ${pressureCertificateNumber}`));
  await fill(page, 'Reason for the suspension', 'Insulation removed');
  await press(page, 'Suspend certificate');
  await screen.findByText('suspended');
  expect((await certiflow.certification.findCertificate(globalCertificateNumber)).status).toBe('DEPENDENCY_INVALID');
});

test('a schema is created and a new version published with a future effective date through the editor', async () => {
  const assetType = unique('Laboratory');
  const nextYear = new Date().getUTCFullYear() + 1;
  open('/schemas/new');
  await fill(page, 'Acting as', demoPeople.administrator.id);

  await screen.findByText('Add section');
  await fill(page, 'Schema name', 'Laboratory inspection');
  await fill(page, 'Asset type', assetType);
  await press(page, 'Add section');
  await fill(page, 'Section title', 'Calibration');
  await fill(page, 'Code', 'TEMP');
  await fill(page, 'Text', 'Room temperature');
  await fill(page, 'Unit', '°C');
  await fill(page, 'Minimum', '18');
  await fill(page, 'Maximum', '24');
  await press(page, 'Require evidence');
  await press(page, 'Create schema');

  await screen.findByText(`Inspection schema for ${assetType}`);
  await press(page, 'Version 1');
  await screen.findByText('Between 18 and 24 °C');
  expect(screen.getByText('1 × Photo')).toBeDefined();

  await press(page, assetType);
  await screen.findByText('Publish new version');
  await press(page, 'Publish new version');
  await screen.findByText('Publish version 2');
  await fill(page, 'Maximum', '26');
  await fill(page, 'Effective from', `${nextYear}-04-01T00:00`);
  await press(page, 'Publish version');
  await screen.findByText('Version 2');
  expect(screen.getByText('scheduled')).toBeDefined();
  await press(page, 'Version 2');
  await screen.findByText('Between 18 and 26 °C');

  const schema = await certiflow.schemas.findSchema(assetType);
  expect(schema).toMatchObject({ currentVersion: 1, latestPublishedVersion: 2 });
  expect(new Date(schema.versions[1]!.effectiveFrom).getTime()).toBe(new Date(`${nextYear}-04-01T00:00`).getTime());
});

test('an asset is registered and an inspection assigned through the forms', async () => {
  const { assetType } = await extinguisherWithAnAssignedInspection();
  const assetCode = unique('WHS');
  open('/assets/new');
  await fill(page, 'Acting as', demoPeople.administrator.id);

  await fill(page, 'Code', assetCode);
  await fill(page, 'Name', 'Warehouse extinguisher');
  await fill(page, 'Type', assetType);
  await fill(page, 'Location', 'Warehouse A');
  await fill(page, 'Responsible', demoPeople.assetResponsible.id);
  await fill(page, 'Jurisdiction', 'DEMO-STRICT');
  await press(page, 'Register asset');

  await screen.findByText(`Asset ${assetCode}`);
  expect(screen.getByText('DEMO-STRICT')).toBeDefined();
  expect(screen.queryByRole('link', { name: 'Assign inspection' })).toBeNull();
  await fill(page, 'Acting as', demoPeople.coordinator.id);
  await press(page, 'Assign inspection');
  await screen.findByText('Whole schema');
  await fill(page, 'Inspector', '__someone-else__');
  await fill(page, 'Inspector id', demoPeople.coordinator.id);
  await fill(page, 'Inspector name', demoPeople.coordinator.name);
  await fill(page, 'Planned date', daysFromToday(3));
  await press(page, 'Assign inspection');
  expect((await screen.findByRole('alert')).textContent).toContain('AssignerCannotInspectException');

  await fill(page, 'Inspector', demoPeople.inspector.id);
  await press(page, 'Assign inspection');
  await screen.findByText(/Inspection [0-9a-f-]{36}/);
  expect(screen.getByText('0 of 2 evaluated')).toBeDefined();
});
