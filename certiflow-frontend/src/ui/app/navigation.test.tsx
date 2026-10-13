// @vitest-environment jsdom
import { act, cleanup, fireEvent, render, screen, within } from '@testing-library/react';
import { MemoryRouter } from 'react-router';
import { afterEach, expect, test } from 'vitest';
import type { Certiflow } from '../../ports/Certiflow';
import { ApplicationProvider } from './CertiflowContext';
import type { Role } from './roles';
import { ApplicationRoutes } from './routes';
import { SessionProvider } from './SessionContext';
import { fakeAuthentication, signedInAs, type FakeAuthentication } from './testing';

afterEach(cleanup);

const emptySystem = {
  catalog: { listAssets: async () => [] },
  schemas: { listSchemas: async () => [], vocabulary: async () => ({ severities: [], evidenceKinds: [], jurisdictions: [] }) },
  inspections: { listInspections: async () => [] },
  findings: { listFindings: async () => [] },
  certification: { listCertificates: async () => [] },
  audit: { auditHistory: async () => [] },
} as unknown as Certiflow;

function openAs(role: Role, path: string): FakeAuthentication {
  const authentication = fakeAuthentication(signedInAs(role));
  render(
    <ApplicationProvider application={{ certiflow: emptySystem, authentication }}>
      <MemoryRouter initialEntries={[path]}>
        <SessionProvider>
          <ApplicationRoutes />
        </SessionProvider>
      </MemoryRouter>
    </ApplicationProvider>,
  );
  return authentication;
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
  expect(within(screen.getByRole('main')).queryByRole('button')).toBeNull();
});

test('the header shows who is logged in and there is no way to act as someone else', async () => {
  openAs('verifier', '/findings');
  await screen.findByRole('heading', { level: 1, name: 'Findings' });
  expect(screen.getByText('Vera Soto · Verifier')).toBeDefined();
  expect(screen.queryByLabelText('Acting as')).toBeNull();
});

test('logging out returns to the login', async () => {
  openAs('inspector', '/inspections');
  await screen.findByRole('heading', { level: 1, name: 'Inspections' });
  fireEvent.click(screen.getByRole('button', { name: 'Logout' }));
  expect(await screen.findByRole('heading', { level: 1, name: 'Log in to CertiFlow' })).toBeDefined();
});

test('a token the server stops accepting returns to the login', async () => {
  const authentication = openAs('inspector', '/inspections');
  await screen.findByRole('heading', { level: 1, name: 'Inspections' });
  act(() => authentication.tokenRejectedByTheServer());
  expect(await screen.findByRole('heading', { level: 1, name: 'Log in to CertiFlow' })).toBeDefined();
});
