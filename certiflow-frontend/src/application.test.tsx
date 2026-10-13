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

test('a closed inspection with a blocking finding explains why it cannot be certified', async () => {
  await openApplicationAt('/inspections/INS-1');
  expect(await screen.findByText(/open finding\(s\) whose severity blocks certification/)).toBeDefined();
  expect(screen.queryByText('Issue certificate')).toBeNull();
});

test('an eligible inspection offers to issue the certificate', async () => {
  await openApplicationAt('/inspections/INS-2');
  expect(await screen.findByText('Issue certificate')).toBeDefined();
});
