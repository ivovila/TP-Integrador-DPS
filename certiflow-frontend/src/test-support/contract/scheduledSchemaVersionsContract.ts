import { describe, expect, test } from 'vitest';
import type { ContractContext } from './fixture';
import { administrator, plusDays, startOfDayIn, sameInstant, today, extinguisherSections } from './fixture';




export function scheduledSchemaVersionsContract(context: ContractContext): void {
    describe('scheduled schema versions', () => {
      function publishOnlyPhysicalCondition(effectiveFrom?: string) {
        return context.certiflow.schemas.publishSchemaVersion({
          assetType: context.extinguisherType,
          sections: [extinguisherSections(context.vocabulary)[0]!],
          requiredSubsystems: [],
          effectiveFrom,
          publishedBy: administrator,
        });
      }

      test('a version scheduled for later does not apply to inspections assigned before it takes effect', async () => {
        const earlierInspectionId = await context.assetWithSchemaAndOpenInspection();
        const scheduled = await publishOnlyPhysicalCondition(startOfDayIn(10));
        const laterInspectionId = await context.assignInspection();
        const schema = await context.certiflow.schemas.findSchema(context.extinguisherType);

        expect(scheduled.number).toBe(2);
        expect(sameInstant(scheduled.effectiveFrom)).toBe(startOfDayIn(10));
        expect(schema).toMatchObject({ currentVersion: 1, latestPublishedVersion: 2 });
        expect((await context.certiflow.inspections.findInspection(earlierInspectionId)).schemaVersion).toBe(1);
        expect((await context.certiflow.inspections.findInspection(laterInspectionId)).schemaVersion).toBe(1);
      });

      test('the version in force at any instant can be consulted', async () => {
        await context.createExtinguisherSchema();
        await publishOnlyPhysicalCondition(startOfDayIn(10));
        const justBefore = await context.certiflow.schemas.effectiveVersion(context.extinguisherType, `${plusDays(today, 9)}T23:59:59.999Z`);
        const exactly = await context.certiflow.schemas.effectiveVersion(context.extinguisherType, startOfDayIn(10));
        expect([justBefore.number, exactly.number]).toEqual([1, 2]);
        await context.expectRejection(
          context.certiflow.schemas.effectiveVersion(context.extinguisherType, '2020-01-01T00:00:00.000Z'),
          'NoEffectiveSchemaVersionException',
        );
      });

      test('a version cannot take effect before it is published', async () => {
        await context.createExtinguisherSchema();
        await context.expectRejection(publishOnlyPhysicalCondition(startOfDayIn(-1)), 'InvalidSchemaException');
      });

      test('scheduled versions take effect in publication order', async () => {
        await context.createExtinguisherSchema();
        await publishOnlyPhysicalCondition(startOfDayIn(20));
        await context.expectRejection(publishOnlyPhysicalCondition(startOfDayIn(10)), 'SchemaEffectiveDateConflictException');
        await context.expectRejection(publishOnlyPhysicalCondition(), 'SchemaEffectiveDateConflictException');
        await publishOnlyPhysicalCondition(startOfDayIn(30));
        expect((await context.certiflow.schemas.findSchema(context.extinguisherType)).latestPublishedVersion).toBe(3);
      });
    });

}
