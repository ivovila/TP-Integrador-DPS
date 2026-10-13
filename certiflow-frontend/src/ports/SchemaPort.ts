import type { InspectionSchema, SchemaVersion, Section, Vocabulary } from '../domain/schema';
import type { Person } from '../domain/shared';

export interface CreateInspectionSchemaCommand {
  name: string;
  assetType: string;
  sections: Section[];
  createdBy: Person;
}

export interface PublishSchemaVersionCommand {
  assetType: string;
  sections: Section[];
  publishedBy: Person;
}

export interface SchemaPort {
  createInspectionSchema(command: CreateInspectionSchemaCommand): Promise<InspectionSchema>;
  publishSchemaVersion(command: PublishSchemaVersionCommand): Promise<SchemaVersion>;
  findSchema(assetType: string): Promise<InspectionSchema>;
  listSchemas(): Promise<InspectionSchema[]>;
  vocabulary(): Promise<Vocabulary>;
}
