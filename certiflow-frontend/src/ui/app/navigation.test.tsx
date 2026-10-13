// @vitest-environment jsdom
import { cleanup, fireEvent, render, screen, within } from '@testing-library/react';
import { MemoryRouter } from 'react-router';
import { afterEach, expect, test } from 'vitest';
import type { Certiflow } from '../../ports/Certiflow';
import { ApplicationProvider } from './CertiflowContext';
import type { Role } from './roles';
import { roster } from './roster';
import { ApplicationRoutes } from './routes';
import { SessionProvider } from './SessionContext';

afterEach(() => {
  cleanup();
  window.localStorage.clear();
});

const emptySystem = {
  catalog: { listAssets: async () => [] },
  schemas: { listSchemas: async () => [], vocabulary: async () => ({ severities: [], evidenceKinds: [], jurisdictions: [] }) },
  inspections: { listInspections: async () => [] },
  findings: { listFindings: async () => [] },
  certification: { listCertificates: async () => [] },
  audit: { auditHistory: async () => [] },
} as unknown as Certiflow;

function openAs(role: Role, path: string): void {
  render(
    <ApplicationProvider application={{ certiflow: emptySystem }}>
      <SessionProvider startingAs={roster.find((member) => member.role === role)!}>
        <MemoryRouter initialEntries={[path]}>
          <ApplicationRoutes />
        </MemoryRouter>
      </SessionProvider>
    </ApplicationProvider>,
  );
}

function menu(): string[] {
  return within(screen.getByRole('navigation'))
    .getAllByRole('link')
    .map((link) => link.textContent ?? '');
}

const menuAndHomeByRole: [Role, string[], string][] = [
  ['administrator', ['Assets', 'Schemas'], 'Assets'],
  ['inspectionCoordinator', ['Inspections', 'Assets'], 'Inspections'],
  ['inspector', ['Inspections'], 'Inspections'],
  ['assetResponsible', ['Findings', 'Assets'], 'Findings'],
  ['verifier', ['Findings'], 'Findings'],
  ['certificationOfficer', ['Certificates', 'Inspections', 'Assets'], 'Certificates'],
];

test.each(menuAndHomeByRole)('the %s sees its own sections and starts on the first', async (role, expectedMenu, expectedHome) => {
  openAs(role, '/');
  expect(await screen.findByRole('heading', { level: 1, name: new RegExp(`^${expectedHome === 'Schemas' ? 'Inspection schemas' : expectedHome}`) })).toBeDefined();
  expect(menu()).toEqual(expectedMenu);
});

const creationButtonByRole: [Role, string, string, boolean][] = [
  ['administrator', '/assets', 'Register asset', true],
  ['inspectionCoordinator', '/assets', 'Register asset', false],
  ['administrator', '/schemas', 'Create schema', true],
  ['inspector', '/schemas', 'Create schema', false],
  ['inspectionCoordinator', '/inspections', 'Assign inspection', true],
  ['inspector', '/inspections', 'Assign inspection', false],
];

test.each(creationButtonByRole)('as %s, on %s, "%s" is offered: %s', async (role, path, buttonLabel, expectedOffered) => {
  openAs(role, path);
  await screen.findByRole('heading', { level: 1 });
  expect(screen.queryByRole('link', { name: buttonLabel }) !== null).toBe(expectedOffered);
});

test('a section outside the menu can still be opened and read', async () => {
  openAs('verifier', '/schemas');
  expect(await screen.findByRole('heading', { level: 1, name: 'Inspection schemas' })).toBeDefined();
  expect(menu()).toEqual(['Findings']);
});

const formsThatNeedARole: [string, Role, string][] = [
  ['/assets/new', 'inspector', 'Only the Administrator can do this'],
  ['/schemas/new', 'inspectionCoordinator', 'Only the Administrator can do this'],
  ['/schemas/Boiler/versions/new', 'verifier', 'Only the Administrator can do this'],
  ['/inspections/new', 'administrator', 'Only the Inspection coordinator can do this'],
];

test.each(formsThatNeedARole)('opening %s as %s says which role it needs instead of showing the form', async (path, role, expectedNotice) => {
  openAs(role, path);
  expect((await screen.findByText(/Only the .* can do this/)).textContent).toContain(expectedNotice);
  expect(screen.queryByRole('button')).toBeNull();
});

test('choosing another person changes the menu at once', async () => {
  openAs('inspector', '/inspections');
  await screen.findByRole('heading', { level: 1, name: 'Inspections' });
  fireEvent.change(screen.getByLabelText('Acting as'), { target: { value: roster.find((member) => member.role === 'certificationOfficer')!.person.id } });
  expect(menu()).toEqual(['Certificates', 'Inspections', 'Assets']);
});

test('someone who is not on the list chooses their role', async () => {
  openAs('inspector', '/inspections');
  await screen.findByRole('heading', { level: 1, name: 'Inspections' });
  fireEvent.change(screen.getByLabelText('Acting as'), { target: { value: '__someone-else__' } });
  fireEvent.change(screen.getByLabelText('Acting as id'), { target: { value: 'EMP-099' } });
  fireEvent.change(screen.getByLabelText('Acting as name'), { target: { value: 'Nora Paz' } });
  fireEvent.change(screen.getByLabelText('Acting as role'), { target: { value: 'verifier' } });
  expect(menu()).toEqual(['Findings']);
});
