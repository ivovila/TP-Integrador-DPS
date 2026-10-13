import type { SchemaVersion, Vocabulary } from '../../domain/schema';
import type { SchemaPort } from '../../ports/SchemaPort';
import { type HalClient, pathOf, query, segment } from './HalClient';
import { schemaOf, sectionsToWire, versionOf, vocabularyOf } from './schemaMapping';
import type { WireSchema, WireSchemaVersion, WireVocabulary } from './wire';

export interface SchemaReader {
  vocabulary(): Promise<Vocabulary>;
  versionAt(versionHref: string): Promise<SchemaVersion>;
  assetTypesWithSchema(): Promise<string[]>;
}

function schemaPath(assetType: string): string {
  return `/api/v1/schemas/${segment(assetType)}`;
}

export function schemaVersionPath(assetType: string, number: number): string {
  return `${schemaPath(assetType)}/versions/${segment(number)}`;
}

export function createHttpSchemaPort(client: HalClient): SchemaPort & SchemaReader {
  let knownVocabulary: Promise<Vocabulary> | undefined;
  const publishedVersions = new Map<string, Promise<SchemaVersion>>();

  function vocabulary(): Promise<Vocabulary> {
    knownVocabulary ??= client.read<WireVocabulary>('/api/v1/vocabulary').then(vocabularyOf);
    knownVocabulary.catch(() => {
      knownVocabulary = undefined;
    });
    return knownVocabulary;
  }

  function versionAt(versionHref: string): Promise<SchemaVersion> {
    const versionPath = pathOf(versionHref);
    const alreadyRead = publishedVersions.get(versionPath);
    if (alreadyRead !== undefined) {
      return alreadyRead;
    }
    const reading = Promise.all([client.read<WireSchemaVersion>(versionPath), vocabulary()]).then(([wire, known]) =>
      versionOf(wire, known),
    );
    publishedVersions.set(versionPath, reading);
    reading.catch(() => publishedVersions.delete(versionPath));
    return reading;
  }

  async function schemaWithItsVersions(wire: WireSchema) {
    const [wireVersions, known] = await Promise.all([
      client.readAll<WireSchemaVersion>(`${schemaPath(wire.assetType)}/versions`, 'versions'),
      vocabulary(),
    ]);
    return schemaOf(wire, wireVersions, known);
  }

  function listedSchemas(): Promise<WireSchema[]> {
    return client.readAll<WireSchema>('/api/v1/schemas', 'schemas');
  }

  return {
    vocabulary,
    versionAt,

    async assetTypesWithSchema() {
      return (await listedSchemas()).map((schema) => schema.assetType);
    },

    async createInspectionSchema(command) {
      const created = await client.write<WireSchema>('POST', '/api/v1/schemas', command.createdBy, {
        name: command.name,
        assetType: command.assetType,
        sections: sectionsToWire(command.sections),
        requiredSubsystems: command.requiredSubsystems,
      });
      return schemaWithItsVersions(created);
    },

    async publishSchemaVersion(command) {
      const published = await client.write<WireSchemaVersion>('POST', `${schemaPath(command.assetType)}/versions`, command.publishedBy, {
        sections: sectionsToWire(command.sections),
        requiredSubsystems: command.requiredSubsystems,
        ...(command.effectiveFrom === undefined ? {} : { effectiveFrom: command.effectiveFrom }),
      });
      return versionOf(published, await vocabulary());
    },

    async findSchema(assetType) {
      return schemaWithItsVersions(await client.read<WireSchema>(schemaPath(assetType)));
    },

    async effectiveVersion(assetType, at) {
      const effective = await client.read<WireSchemaVersion>(`${schemaPath(assetType)}/effective-version${query({ at })}`);
      return versionOf(effective, await vocabulary());
    },

    async listSchemas() {
      return Promise.all((await listedSchemas()).map(schemaWithItsVersions));
    },
  };
}
