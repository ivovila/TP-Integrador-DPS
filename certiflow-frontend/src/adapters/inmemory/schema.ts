import type { InspectionSchema, SchemaVersion } from '../../domain/schema';
import type { Instant } from '../../domain/shared';
import type { SchemaPort } from '../../ports/SchemaPort';
import { demoJurisdictions } from './policies';
import { standardEvidenceKinds, standardSeverities, validateSections } from './rules';
import type { InMemorySystem, StoredSchema } from './store';
import { reject, requireText } from './support';

export function storedSchemaFor(system: InMemorySystem, assetType: string): StoredSchema {
  return (
    system.schemas.get(assetType) ??
    reject('NoInspectionSchemaForAssetTypeException', `There is no inspection schema for asset type ${assetType}`)
  );
}

/** Cada versión rige desde su `effectiveFrom` hasta la siguiente; antes de la primera no hay ninguna. */
export function versionAt(schema: StoredSchema, at: Instant): SchemaVersion {
  return (
    schema.versions.filter((version) => version.effectiveFrom <= at).at(-1) ??
    reject('NoEffectiveSchemaVersionException', `No schema version was effective at ${at}`)
  );
}

export function latestPublishedVersionOf(schema: StoredSchema): SchemaVersion {
  return schema.versions.at(-1)!;
}

function schemaView(system: InMemorySystem, schema: StoredSchema): InspectionSchema {
  return structuredClone({
    ...schema,
    currentVersion: versionAt(schema, system.clock.now()).number,
    latestPublishedVersion: latestPublishedVersionOf(schema).number,
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
      validateSections(command.sections, command.requiredSubsystems);
      if (system.schemas.has(command.assetType)) {
        reject('SchemaAlreadyDefinedForAssetTypeException', `Asset type ${command.assetType} already has an inspection schema`);
      }
      const createdAt = system.clock.now();
      const schema: StoredSchema = {
        name: command.name,
        assetType: command.assetType,
        versions: [
          {
            number: 1,
            publishedAt: createdAt,
            effectiveFrom: createdAt,
            requiredSubsystems: [...command.requiredSubsystems],
            sections: structuredClone(command.sections),
          },
        ],
      };
      system.schemas.set(schema.assetType, schema);
      system.auditLog.record({ kind: 'schema', assetType: schema.assetType }, 'CREATED', command.createdBy, createdAt, schema.name);
      return schemaView(system, schema);
    },

    async publishSchemaVersion(command) {
      const schema = storedSchemaFor(system, command.assetType);
      validateSections(command.sections, command.requiredSubsystems);
      const publishedAt = system.clock.now();
      const effectiveFrom = command.effectiveFrom ?? publishedAt;
      if (effectiveFrom < publishedAt) {
        reject('InvalidSchemaException', 'Effective date cannot precede publication');
      }
      if (effectiveFrom <= latestPublishedVersionOf(schema).effectiveFrom) {
        reject('SchemaEffectiveDateConflictException', 'Effective dates must increase strictly in publication order');
      }
      const version: SchemaVersion = {
        number: latestPublishedVersionOf(schema).number + 1,
        publishedAt,
        effectiveFrom,
        requiredSubsystems: [...command.requiredSubsystems],
        sections: structuredClone(command.sections),
      };
      schema.versions.push(version);
      system.auditLog.record(
        { kind: 'schema', assetType: schema.assetType },
        'VERSION_ADDED',
        command.publishedBy,
        publishedAt,
        `Version ${version.number}, effective from ${effectiveFrom}`,
      );
      return structuredClone(version);
    },

    async findSchema(assetType) {
      return schemaView(system, storedSchemaFor(system, assetType));
    },

    async effectiveVersion(assetType, at) {
      return structuredClone(versionAt(storedSchemaFor(system, assetType), at));
    },

    async listSchemas() {
      return [...system.schemas.values()].map((schema) => schemaView(system, schema));
    },

    async vocabulary() {
      return structuredClone({
        severities: standardSeverities,
        evidenceKinds: standardEvidenceKinds,
        jurisdictions: demoJurisdictions,
      });
    },
  };
}
