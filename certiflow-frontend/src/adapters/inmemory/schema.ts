import type { InspectionSchema, SchemaVersion } from '../../domain/schema';
import type { SchemaPort } from '../../ports/SchemaPort';
import { standardEvidenceKinds, standardSeverities, validateSections } from './rules';
import type { InMemorySystem, StoredSchema } from './store';
import { reject, requireText } from './support';

export function storedSchemaFor(system: InMemorySystem, assetType: string): StoredSchema {
  return (
    system.schemas.get(assetType) ??
    reject('NoInspectionSchemaForAssetTypeException', `There is no inspection schema for asset type ${assetType}`)
  );
}

export function currentVersionOf(schema: StoredSchema): SchemaVersion {
  return schema.versions[schema.versions.length - 1]!;
}

function schemaView(schema: StoredSchema): InspectionSchema {
  return structuredClone({
    ...schema,
    currentVersion: currentVersionOf(schema).number,
    actions: ['publishSchemaVersion' as const],
  });
}

export function createSchemaPort(system: InMemorySystem): SchemaPort {
  return {
    async createInspectionSchema(command) {
      if (command.name.trim() === '') {
        reject('InvalidSchemaException', 'An inspection schema needs a name');
      }
      requireText(command.assetType, 'An inspection schema needs an asset type');
      validateSections(command.sections);
      if (system.schemas.has(command.assetType)) {
        reject('SchemaAlreadyDefinedForAssetTypeException', `Asset type ${command.assetType} already has an inspection schema`);
      }
      const createdAt = system.clock.now();
      const schema: StoredSchema = {
        name: command.name,
        assetType: command.assetType,
        versions: [{ number: 1, publishedAt: createdAt, sections: structuredClone(command.sections) }],
      };
      system.schemas.set(schema.assetType, schema);
      system.auditLog.record({ kind: 'schema', assetType: schema.assetType }, 'CREATED', command.createdBy, createdAt, schema.name);
      return schemaView(schema);
    },

    async publishSchemaVersion(command) {
      const schema = storedSchemaFor(system, command.assetType);
      validateSections(command.sections);
      const publishedAt = system.clock.now();
      const version: SchemaVersion = {
        number: currentVersionOf(schema).number + 1,
        publishedAt,
        sections: structuredClone(command.sections),
      };
      schema.versions.push(version);
      system.auditLog.record(
        { kind: 'schema', assetType: schema.assetType },
        'VERSION_ADDED',
        command.publishedBy,
        publishedAt,
        `Version ${version.number}`,
      );
      return structuredClone(version);
    },

    async findSchema(assetType) {
      return schemaView(storedSchemaFor(system, assetType));
    },

    async listSchemas() {
      return [...system.schemas.values()].map(schemaView);
    },

    async vocabulary() {
      return structuredClone({ severities: standardSeverities, evidenceKinds: standardEvidenceKinds });
    },
  };
}
