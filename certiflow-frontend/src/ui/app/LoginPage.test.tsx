// @vitest-environment jsdom
import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router';
import { afterEach, expect, test } from 'vitest';
import type { Certiflow } from '../../ports/Certiflow';
import { ApplicationProvider } from './CertiflowContext';
import { ApplicationRoutes } from './routes';
import { SessionProvider } from './SessionContext';
import { demoPassword, fakeAuthentication } from './testing';

afterEach(cleanup);

const emptySystem = {
  catalog: { listAssets: async () => [] },
  schemas: { listSchemas: async () => [], vocabulary: async () => ({ severities: [], evidenceKinds: [], jurisdictions: [] }) },
  inspections: { listInspections: async () => [] },
  findings: { listFindings: async () => [] },
  certification: { listCertificates: async () => [] },
  audit: { auditHistory: async () => [] },
} as unknown as Certiflow;

function openSignedOut(path: string): void {
  render(
    <ApplicationProvider application={{ certiflow: emptySystem, authentication: fakeAuthentication() }}>
      <MemoryRouter initialEntries={[path]}>
        <SessionProvider>
          <ApplicationRoutes />
        </SessionProvider>
      </MemoryRouter>
    </ApplicationProvider>,
  );
}

function logIn(username: string, password: string): void {
  fireEvent.change(screen.getByLabelText('Username'), { target: { value: username } });
  fireEvent.change(screen.getByLabelText('Password'), { target: { value: password } });
  fireEvent.click(screen.getByRole('button', { name: 'Log in' }));
}

test('without a session any page asks to log in', async () => {
  openSignedOut('/certificates');
  expect(await screen.findByRole('heading', { level: 1, name: 'Log in to CertiFlow' })).toBeDefined();
  expect(screen.queryByRole('navigation')).toBeNull();
});

test('the right credentials open the first section of the role', async () => {
  openSignedOut('/');
  logIn('ines.duarte', demoPassword);
  expect(await screen.findByRole('heading', { level: 1, name: 'Inspections' })).toBeDefined();
  expect(screen.getByText('Ines Duarte · Inspector')).toBeDefined();
});

test('the person id is not a username', async () => {
  openSignedOut('/');
  logIn('EMP-002', demoPassword);
  expect((await screen.findByRole('alert')).textContent).toContain('Wrong username or password');
});

test('wrong credentials say so and stay on the login', async () => {
  openSignedOut('/');
  logIn('ines.duarte', 'wrong');
  expect((await screen.findByRole('alert')).textContent).toContain('Wrong username or password');
  expect(screen.getByRole('heading', { level: 1, name: 'Log in to CertiFlow' })).toBeDefined();
});
