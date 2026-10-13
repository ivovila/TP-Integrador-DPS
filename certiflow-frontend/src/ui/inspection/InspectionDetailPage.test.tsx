// @vitest-environment jsdom
import { cleanup, render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router';
import { afterEach, expect, test } from 'vitest';
import type { InspectionAction, InspectionDetails } from '../../domain/inspection';
import type { Certiflow } from '../../ports/Certiflow';
import { ApplicationProvider } from '../app/CertiflowContext';
import { SessionProvider } from '../app/SessionContext';
import { InspectionDetailPage } from './InspectionDetailPage';

afterEach(cleanup);

function inspectionOffering(actions: InspectionAction[]): InspectionDetails {
  return {
    id: 'INS-1',
    assetCode: 'EXT-0042',
    assignment: { inspector: { id: 'EMP-002', name: 'Ines Duarte' }, plannedDate: '2026-03-02', scope: { kind: 'full' } },
    schemaVersion: 1,
    closed: false,
    coversWholeSchema: true,
    responses: [
      {
        sectionTitle: 'Physical condition',
        criterion: {
          code: 'SEAL',
          text: 'Safety seal intact',
          severity: { name: 'MAJOR', label: 'Major', outcomeWhenUnmet: 'REJECTED', blocksCertification: true },
          rule: { type: 'yes-no', expected: 'YES' },
          requiredEvidence: [],
        },
        attachments: [],
        observations: [],
        outcome: 'PENDING',
      },
    ],
    revisions: [],
    actions,
  };
}

function certiflowReturning(inspection: InspectionDetails): Certiflow {
  return {
    inspections: { findInspection: async () => inspection },
    catalog: {
      findAsset: async () => ({
        code: 'EXT-0042',
        name: 'Lobby extinguisher',
        type: 'Fire extinguisher',
        location: 'Lobby',
        responsible: { id: 'EMP-003', name: 'Raul Mendez' },
        characteristics: [],
        actions: [],
      }),
    },
    schemas: { vocabulary: async () => ({ severities: [], evidenceKinds: [{ name: 'PHOTO', label: 'Photo' }] }) },
    certification: {
      evaluateCertification: async () => ({ inspectionId: 'INS-1', eligible: false, ineligibilities: [], actions: [] }),
    },
    audit: { auditHistory: async () => [] },
  } as unknown as Certiflow;
}

async function showInspectionOffering(actions: InspectionAction[]): Promise<void> {
  render(
    <ApplicationProvider application={{ certiflow: certiflowReturning(inspectionOffering(actions)) }}>
      <SessionProvider>
        <MemoryRouter initialEntries={['/inspections/INS-1']}>
          <Routes>
            <Route path="inspections/:id" element={<InspectionDetailPage />} />
          </Routes>
        </MemoryRouter>
      </SessionProvider>
    </ApplicationProvider>,
  );
  await screen.findByText('Safety seal intact');
}

const buttonLabels = ['Close inspection', 'Record answer', 'Rectify', 'Inspection act'];

const offeredActions: [string, InspectionAction[], string[]][] = [
  ['an inspection that offers nothing shows no action', [], []],
  ['closing is shown only when the inspection offers it', ['closeInspection'], ['Close inspection']],
  ['answering is shown only when the inspection offers it', ['recordAnswer'], ['Record answer']],
  ['rectifying and the act are shown when the inspection offers them', ['rectifyInspection', 'generateInspectionAct'], ['Rectify', 'Inspection act']],
];

test.each(offeredActions)('%s', async (_description, actions, expectedLabels) => {
  await showInspectionOffering(actions);
  const shownLabels = buttonLabels.filter((label) => screen.queryByText(label) !== null);
  expect(shownLabels).toEqual(expectedLabels);
});
