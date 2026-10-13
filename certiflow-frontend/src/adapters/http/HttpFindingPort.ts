import type { FindingPort } from '../../ports/FindingPort';
import { findingOf, findingsSummaryOf } from './findingMapping';
import { type HalClient, query, segment } from './HalClient';
import type { SchemaReader } from './HttpSchemaPort';
import type { WireCorrectiveAction, WireFinding, WireFindingsSummary, WireVerification } from './wire';

function findingPath(findingId: string): string {
  return `/api/v1/findings/${segment(findingId)}`;
}

export function createHttpFindingPort(client: HalClient, schemas: SchemaReader): FindingPort {
  return {
    async listFindings(filter) {
      const [findings, vocabulary] = await Promise.all([
        client.readAll<WireFinding>(`/api/v1/findings${query({ asset: filter.assetCode })}`, 'findings'),
        schemas.vocabulary(),
      ]);
      return findings.map((finding) => findingOf(finding, vocabulary));
    },

    async findFinding(findingId) {
      const [finding, vocabulary] = await Promise.all([client.read<WireFinding>(findingPath(findingId)), schemas.vocabulary()]);
      return findingOf(finding, vocabulary);
    },

    async planCorrectiveAction(command) {
      const planned = await client.write<WireCorrectiveAction>('POST', `${findingPath(command.findingId)}/corrective-actions`, command.plannedBy, {
        description: command.description,
        responsible: { id: command.responsible.id, name: command.responsible.name },
        dueDate: command.dueDate,
      });
      return planned.id;
    },

    async verifyCorrectiveAction(command) {
      const verification = await client.write<WireVerification>(
        'POST',
        `${findingPath(command.findingId)}/corrective-actions/${segment(command.actionId)}/verifications`,
        command.verifier,
        { result: command.result, notes: command.notes },
      );
      return { verifier: verification.verifier, result: verification.result, notes: verification.notes, verifiedAt: verification.verifiedAt };
    },

    async generateFindingsSummary(assetCode) {
      return findingsSummaryOf(await client.read<WireFindingsSummary>(`/api/v1/assets/${segment(assetCode)}/findings-summary`));
    },
  };
}
