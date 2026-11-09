import { describe, expect, test } from 'vitest';
import type { ContractContext } from './fixture';
import { administrator, extinguisherSections } from './fixture';
import type { Section } from '../../domain/schema';

import type { DomainErrorName } from '../../ports/DomainError';

export function inspectionSchemasAndVersionsContract(context: ContractContext): void {
    describe('inspection schemas and versions', () => {
      test('an asset type has a single inspection schema', async () => {
        await context.createExtinguisherSchema();
        await context.expectRejection(context.createExtinguisherSchema(), 'SchemaAlreadyDefinedForAssetTypeException');
      });

      test('a new version is numbered consecutively and the previous one is kept', async () => {
        await context.createExtinguisherSchema();
        const sections = extinguisherSections(context.vocabulary);
        const published = await context.certiflow.schemas.publishSchemaVersion({
          assetType: context.extinguisherType,
          sections: [sections[0]!],
          requiredSubsystems: [],
          publishedBy: administrator,
        });
        const schema = await context.certiflow.schemas.findSchema(context.extinguisherType);
        expect(published.number).toBe(2);
        expect(published.effectiveFrom).toBe(published.publishedAt);
        expect(schema.currentVersion).toBe(2);
        expect(schema.latestPublishedVersion).toBe(2);
        expect(schema.versions.map((version) => version.sections.length)).toEqual([2, 1]);
      });

      test('creating a schema and adding a version are recorded in its history', async () => {
        await context.createExtinguisherSchema();
        await context.certiflow.schemas.publishSchemaVersion({
          assetType: context.extinguisherType,
          sections: extinguisherSections(context.vocabulary),
          requiredSubsystems: [],
          publishedBy: administrator,
        });
        const history = await context.certiflow.audit.auditHistory({ kind: 'schema', assetType: context.extinguisherType });
        expect(history.map((entry) => entry.action)).toEqual(['CREATED', 'VERSION_ADDED']);
      });

      test('schemas can be listed', async () => {
        await context.createExtinguisherSchema();
        expect((await context.certiflow.schemas.listSchemas()).map((schema) => schema.assetType)).toContain(context.extinguisherType);
      });

      const invalidSchemas: [string, (sections: Section[]) => Section[], DomainErrorName][] = [
        ['a schema without sections', () => [], 'InvalidSchemaException'],
        ['a section without criteria', (sections) => [{ ...sections[0]!, criteria: [] }], 'InvalidSchemaException'],
        ['a section without title', (sections) => [{ ...sections[0]!, title: ' ' }], 'InvalidSchemaException'],
        ['two criteria with the same code', (sections) => [sections[0]!, sections[0]!], 'InvalidSchemaException'],
        [
          'a range whose minimum exceeds its maximum',
          (sections) => [
            {
              title: 'Broken',
              criteria: [
                {
                  ...sections[0]!.criteria[0]!,
                  rule: { type: 'numeric-range', unit: 'bar', minimum: '16', maximum: '10' },
                },
              ],
            },
          ],
          'InvalidRuleConfigurationException',
        ],
        [
          'an option rule without accepted options',
          (sections) => [
            {
              title: 'Broken',
              criteria: [{ ...sections[1]!.criteria[1]!, rule: { type: 'option-in-list', acceptedOptions: [] } }],
            },
          ],
          'InvalidRuleConfigurationException',
        ],
        [
          'the same evidence kind required twice by a criterion',
          (sections) => {
            const pressure = sections[0]!.criteria[0]!;
            return [
              {
                title: 'Broken',
                criteria: [{ ...pressure, requiredEvidence: [...pressure.requiredEvidence, ...pressure.requiredEvidence] }],
              },
            ];
          },
          'InvalidSchemaException',
        ],
      ];

      test.each(invalidSchemas)('%s is rejected', async (_description, breakSections, expectedName) => {
        const creation = context.certiflow.schemas.createInspectionSchema({
          name: 'Extinguisher inspection',
          assetType: context.extinguisherType,
          sections: breakSections(extinguisherSections(context.vocabulary)),
          requiredSubsystems: [],
          createdBy: administrator,
        });
        await context.expectRejection(creation, expectedName);
      });
    });

}
