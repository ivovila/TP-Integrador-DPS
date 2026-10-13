// @vitest-environment jsdom
import { cleanup, render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router';
import { afterEach, expect, test, vi } from 'vitest';
import type { Asset } from '../../domain/catalog';
import type { Certiflow } from '../../ports/Certiflow';
import { ApplicationProvider } from '../app/CertiflowContext';
import type { Role } from '../app/roles';
import { ApplicationRoutes } from '../app/routes';
import { SessionProvider } from '../app/SessionContext';
import { fakeAuthentication, signedInAs } from '../app/testing';

afterEach(cleanup);

const boiler: Asset = {
  code: 'BLR-0007',
  name: 'Boiler B',
  type: 'Pressure vessel',
  location: 'Basement',
  responsible: { id: 'EMP-003', name: 'Raul Mendez' },
  characteristics: [],
  jurisdictionCode: 'DEMO-STRICT',
  subsystems: [],
  actions: [],
};

function systemThatOnlyAnswersAssets() {
  return {
    catalog: { findAsset: async () => boiler },
    inspections: { listInspections: vi.fn(async () => []) },
    certification: { listCertificates: vi.fn(async () => []) },
    audit: { auditHistory: async () => [] },
  };
}

async function openTheBoilerAs(role: Role) {
  const system = systemThatOnlyAnswersAssets();
  render(
    <ApplicationProvider
      application={{ certiflow: system as unknown as Certiflow, authentication: fakeAuthentication(signedInAs(role)) }}
    >
      <MemoryRouter initialEntries={['/assets/BLR-0007']}>
        <SessionProvider>
          <ApplicationRoutes />
        </SessionProvider>
      </MemoryRouter>
    </ApplicationProvider>,
  );
  await screen.findByRole('heading', { level: 1, name: 'Boiler B' });
  return system;
}

test('the administrator sees the asset without asking for its inspections or certificates', async () => {
  const system = await openTheBoilerAs('administrator');

  expect(screen.queryByRole('heading', { name: 'Inspections' })).toBeNull();
  expect(screen.queryByRole('heading', { name: 'Certificates' })).toBeNull();
  expect(screen.queryByRole('link', { name: 'Findings summary' })).toBeNull();
  expect(system.inspections.listInspections).not.toHaveBeenCalled();
  expect(system.certification.listCertificates).not.toHaveBeenCalled();
});

test('the certification officer sees the inspections and certificates of the asset', async () => {
  const system = await openTheBoilerAs('certificationOfficer');

  expect(await screen.findByRole('heading', { name: 'Inspections' })).toBeDefined();
  expect(screen.getByRole('heading', { name: 'Certificates' })).toBeDefined();
  expect(system.inspections.listInspections).toHaveBeenCalled();
});

test('the asset responsible reaches the findings summary but not the inspections', async () => {
  const system = await openTheBoilerAs('assetResponsible');

  expect(screen.getByRole('link', { name: 'Findings summary' })).toBeDefined();
  expect(screen.queryByRole('heading', { name: 'Inspections' })).toBeNull();
  expect(system.inspections.listInspections).not.toHaveBeenCalled();
});
