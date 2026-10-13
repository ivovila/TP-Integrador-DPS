import type { InspectionDetails } from '../../domain/inspection';
import type { InspectionPort } from '../../ports/InspectionPort';
import { type HalClient, linksNamed, query, segment, SerializedJson } from './HalClient';
import type { SchemaReader } from './HttpSchemaPort';
import { inspectionActOf, inspectionDetailsOf, inspectionSummaryOf, raisedFindingIdsOf } from './inspectionMapping';
import { answerBody, answerLiteral } from './schemaMapping';
import type { WireCriterionResponse, WireInspection, WireInspectionAct, WireRevision } from './wire';

function inspectionPath(inspectionId: string): string {
  return `/api/v1/inspections/${segment(inspectionId)}`;
}

function responsePath(inspectionId: string, criterionCode: string): string {
  return `${inspectionPath(inspectionId)}/responses/${segment(criterionCode)}`;
}

export function createHttpInspectionPort(client: HalClient, schemas: SchemaReader): InspectionPort {
  async function findInspection(inspectionId: string): Promise<InspectionDetails> {
    const [inspection, responses, revisions, vocabulary] = await Promise.all([
      client.read<WireInspection>(inspectionPath(inspectionId)),
      client.readAll<WireCriterionResponse>(`${inspectionPath(inspectionId)}/responses`, 'responses'),
      client.readAll<WireRevision>(`${inspectionPath(inspectionId)}/revisions`, 'revisions'),
      schemas.vocabulary(),
    ]);
    const version = await schemas.versionAt(linksNamed(inspection, 'schema-version')[0]!.href);
    return inspectionDetailsOf(inspection, responses, revisions, version, vocabulary);
  }

  return {
    findInspection,

    async assignInspection(command) {
      const assigned = await client.write<WireInspection>('POST', '/api/v1/inspections', command.assignedBy, {
        assetCode: command.assetCode,
        inspector: { id: command.inspector.id, name: command.inspector.name },
        plannedDate: command.plannedDate,
        ...(command.scope.kind === 'sections' ? { sections: command.scope.titles } : {}),
      });
      return assigned.id;
    },

    async listInspections(filter) {
      const inspections = await client.readAll<WireInspection>(
        `/api/v1/inspections${query({ asset: filter.assetCode, status: filter.status })}`,
        'inspections',
      );
      return inspections.map(inspectionSummaryOf);
    },

    async recordAnswer(command) {
      await client.write('PUT', `${responsePath(command.inspectionId, command.criterionCode)}/answer`, command.answeredBy, answerBody(command.answer));
    },

    async attachEvidence(command) {
      await client.write('POST', `${responsePath(command.inspectionId, command.criterionCode)}/evidence`, command.attachedBy, {
        kind: command.evidence.kind.name,
        reference: command.evidence.reference,
        description: command.evidence.description,
      });
    },

    async addObservation(command) {
      await client.write('POST', `${responsePath(command.inspectionId, command.criterionCode)}/observations`, command.observedBy, {
        text: command.text,
      });
    },

    async closeInspection(command) {
      return raisedFindingIdsOf(await client.write<WireRevision>('POST', `${inspectionPath(command.inspectionId)}/closure`, command.closedBy));
    },

    async rectifyInspection(command) {
      const rectification = new SerializedJson(
        `{"criterionCode":${JSON.stringify(command.criterionCode)},"value":${answerLiteral(command.answer)},"reason":${JSON.stringify(command.reason)}}`,
      );
      return raisedFindingIdsOf(
        await client.write<WireRevision>('POST', `${inspectionPath(command.inspectionId)}/rectifications`, command.rectifiedBy, rectification),
      );
    },

    async generateInspectionAct(inspectionId) {
      const act = await client.read<WireInspectionAct>(`${inspectionPath(inspectionId)}/act`);
      return inspectionActOf(act, await findInspection(inspectionId));
    },
  };
}
