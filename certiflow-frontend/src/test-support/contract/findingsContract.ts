import { describe, expect, test } from 'vitest';
import type { ContractContext } from './fixture';
import { inspector, assetResponsible, no, measured } from './fixture';




export function findingsContract(context: ContractContext): void {
    describe('findings', () => {
      test('closing raises one finding per non-conformity with its severity, evidence and responsible', async () => {
        const { inspectionId, findingIds } = await context.assetWithClosedInspection({ PRESSURE: measured('20'), SIGNAGE: no });
        const findings = await context.certiflow.findings.listFindings({ assetCode: context.extinguisherCode });
        expect(findingIds).toHaveLength(2);
        expect(findings).toMatchObject([
          {
            inspectionId,
            criterion: { code: 'PRESSURE' },
            severity: { name: 'CRITICAL' },
            responsible: assetResponsible,
            open: true,
            evidence: [{ evidence: { reference: 'gauge.jpg' } }],
          },
          { criterion: { code: 'SIGNAGE' }, severity: { name: 'MINOR' }, open: true },
        ]);
      });

      test('an inspection without non-conformities raises no findings', async () => {
        const { findingIds } = await context.assetWithClosedInspection();
        expect(findingIds).toEqual([]);
      });

      test('raising a finding is recorded in its history', async () => {
        const { findingIds } = await context.assetWithClosedInspection({ SEAL: no });
        const history = await context.certiflow.audit.auditHistory({ kind: 'finding', id: findingIds[0]! });
        expect(history).toMatchObject([{ action: 'RAISED', performedBy: inspector, detail: 'SEAL REJECTED' }]);
      });
    });

}
