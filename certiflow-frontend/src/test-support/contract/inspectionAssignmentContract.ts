import { describe, expect, test } from 'vitest';
import type { ContractContext } from './fixture';
import { administrator, coordinator, inspector, assetResponsible, today, yes, extinguisherSections } from './fixture';

import type { Person } from '../../domain/shared';
import type { DomainErrorName } from '../../ports/DomainError';

export function inspectionAssignmentContract(context: ContractContext): void {
    describe('inspection assignment', () => {
      test('an inspection keeps the schema version in force when it was assigned', async () => {
        const inspectionId = await context.assetWithSchemaAndOpenInspection();
        await context.certiflow.schemas.publishSchemaVersion({
          assetType: context.extinguisherType,
          sections: [extinguisherSections(context.vocabulary)[0]!],
          requiredSubsystems: [],
          publishedBy: administrator,
        });
        const laterInspectionId = await context.assignInspection();
        const earlier = await context.certiflow.inspections.findInspection(inspectionId);
        const later = await context.certiflow.inspections.findInspection(laterInspectionId);
        expect(earlier.schemaVersion).toBe(1);
        expect(earlier.responses).toHaveLength(4);
        expect(later.schemaVersion).toBe(2);
        expect(later.responses).toHaveLength(2);
      });

      test('an asset whose type has no schema cannot be inspected', async () => {
        await context.registerExtinguisher();
        await context.expectRejection(context.assignInspection(), 'NoInspectionSchemaForAssetTypeException');
      });

      const independenceRules: [string, Person, Person, DomainErrorName][] = [
        ['whoever assigns the inspection cannot carry it out', coordinator, coordinator, 'AssignerCannotInspectException'],
        [
          'the asset responsible cannot inspect the asset',
          coordinator,
          assetResponsible,
          'AssetResponsibleCannotInspectOwnAssetException',
        ],
      ];

      test.each(independenceRules)('%s', async (_rule, assignedBy, assignedInspector, expectedName) => {
        await context.registerExtinguisher();
        await context.createExtinguisherSchema();
        const assignment = context.certiflow.inspections.assignInspection({
          assetCode: context.extinguisherCode,
          inspector: assignedInspector,
          plannedDate: today,
          scope: { kind: 'full' },
          assignedBy,
        });
        await context.expectRejection(assignment, expectedName);
      });

      test('the asset responsible cannot assign its inspection', async () => {
        await context.registerExtinguisher(context.extinguisherCode, 'DEMO-STANDARD', coordinator);
        await context.createExtinguisherSchema();
        const assignment = context.certiflow.inspections.assignInspection({
          assetCode: context.extinguisherCode,
          inspector,
          plannedDate: today,
          scope: { kind: 'full' },
          assignedBy: coordinator,
        });
        await context.expectRejection(assignment, 'AssetResponsibleCannotAssignInspectionException');
      });

      test('the scope can only name sections of the schema', async () => {
        await context.registerExtinguisher();
        await context.createExtinguisherSchema();
        await context.expectRejection(context.assignInspection(context.extinguisherCode, ['Paperwork']), 'SectionNotInSchemaException');
      });

      test('a partial inspection only expects answers for the sections in its scope', async () => {
        await context.registerExtinguisher();
        await context.createExtinguisherSchema();
        const inspectionId = await context.assignInspection(context.extinguisherCode, ['Installation']);
        const inspection = await context.certiflow.inspections.findInspection(inspectionId);
        expect(inspection.responses.map((response) => response.criterion.code)).toEqual(['SIGNAGE', 'HOSE']);
        expect(inspection.coversWholeSchema).toBe(false);
        await context.expectRejection(context.answer(inspectionId, 'SEAL', yes), 'CriterionOutOfScopeException');
      });

      test('inspections can be listed by asset and by status', async () => {
        await context.assetWithClosedInspection();
        await context.assignInspection();
        const openInspections = await context.certiflow.inspections.listInspections({ assetCode: context.extinguisherCode, status: 'open' });
        const closedInspections = await context.certiflow.inspections.listInspections({ assetCode: context.extinguisherCode, status: 'closed' });
        expect(openInspections.map((inspection) => inspection.id)).toHaveLength(1);
        expect(closedInspections.map((inspection) => inspection.closed)).toEqual([true]);
      });
    });

}
