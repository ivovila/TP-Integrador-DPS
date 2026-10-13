import type { InspectionSchema, SchemaVersion, Section, Vocabulary } from '../domain/schema';
import type { Instant, Person } from '../domain/shared';

export interface CreateInspectionSchemaCommand {
  name: string;
  assetType: string;
  sections: Section[];
  requiredSubsystems: string[];
  createdBy: Person;
}

export interface PublishSchemaVersionCommand {
  assetType: string;
  sections: Section[];
  requiredSubsystems: string[];
  /** Sin fecha, la versión rige desde que se publica. */
  effectiveFrom?: Instant;
  publishedBy: Person;
}

export interface SchemaPort {
  createInspectionSchema(command: CreateInspectionSchemaCommand): Promise<InspectionSchema>;
  publishSchemaVersion(command: PublishSchemaVersionCommand): Promise<SchemaVersion>;
  findSchema(assetType: string): Promise<InspectionSchema>;
  effectiveVersion(assetType: string, at: Instant): Promise<SchemaVersion>;
  listSchemas(): Promise<InspectionSchema[]>;
  vocabulary(): Promise<Vocabulary>;
}
