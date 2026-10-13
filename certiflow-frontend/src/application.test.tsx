// @vitest-environment jsdom
import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router';
import { afterEach, expect, test } from 'vitest';
import { composeApplication } from './composition';
import { ApplicationProvider } from './ui/app/CertiflowContext';
import { ApplicationRoutes } from './ui/app/routes';
import { SessionProvider } from './ui/app/SessionContext';

afterEach(cleanup);

async function openApplicationAt(path: string): Promise<void> {
  const application = await composeApplication();
  render(
    <ApplicationProvider application={application}>
      <SessionProvider>
        <MemoryRouter initialEntries={[path]}>
          <ApplicationRoutes />
        </MemoryRouter>
      </SessionProvider>
    </ApplicationProvider>,
  );
}

test('the application opens on the asset catalogue with the demo assets', async () => {
  await openApplicationAt('/');
  expect(await screen.findByText('Lobby extinguisher')).toBeDefined();
  expect(screen.getByText('Boiler B')).toBeDefined();
});

test('closing the demo inspection with pending criteria shows why it was rejected', async () => {
  await openApplicationAt('/inspections/INS-4');
  fireEvent.click(await screen.findByText('Close inspection'));
  expect((await screen.findByRole('alert')).textContent).toContain('InspectionIncompleteException');
});

test('a closed inspection with a blocking finding explains why its jurisdiction rejects it', async () => {
  await openApplicationAt('/inspections/INS-1');
  expect(await screen.findByText('Open finding SEAL severity Major')).toBeDefined();
  expect(screen.getByText('STRICT · jurisdiction DEMO-STRICT')).toBeDefined();
  expect(screen.queryByText('Issue certificate')).toBeNull();
});

test('an eligible inspection offers to issue the certificate', async () => {
  await openApplicationAt('/inspections/INS-2');
  expect(await screen.findByText('Issue certificate')).toBeDefined();
});

test('an inspection of an asset split into subsystems shows one decision per subsystem', async () => {
  await openApplicationAt('/inspections/INS-5');
  expect(await screen.findByText('Subsystem ELECTRICAL')).toBeDefined();
  expect(screen.getByText('Subsystem PRESSURE')).toBeDefined();
  expect(await screen.findByText('Issue PRESSURE certificate')).toBeDefined();
});

test('a global certificate whose partial was suspended is shown as no longer valid', async () => {
  await openApplicationAt('/certificates/CERT-4');
  expect(await screen.findByText(/no longer active/)).toBeDefined();
  expect(screen.getByText('dependency invalid')).toBeDefined();
});

test('a version scheduled for later is shown apart from the one in force', async () => {
  await openApplicationAt('/schemas/Fire%20extinguisher');
  expect(await screen.findByText('scheduled')).toBeDefined();
  expect(screen.getByText('in force')).toBeDefined();
});
