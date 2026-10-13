import type {
  ActLine,
  AttachedEvidence,
  CriterionResponse,
  InspectionAct,
  InspectionDetails,
  InspectionSummary,
  Revision,
} from '../../domain/inspection';
import type { SchemaVersion, Vocabulary } from '../../domain/schema';
import { actionsOfferedBy, inspectionActionByRel, responseActionByRel } from './actions';
import { auditEntryOf } from './auditMapping';
import { lastSegmentOf, linksNamed } from './HalClient';
import { criterionOf, evidenceKindLabelled, rawAnswerOf } from './schemaMapping';
import type {
  WireAttachedEvidence,
  WireCriterionResponse,
  WireInspection,
  WireInspectionAct,
  WireRevision,
} from './wire';

export function inspectionSummaryOf(wire: WireInspection): InspectionSummary {
  return {
    id: wire.id,
    assetCode: wire.assetCode,
    assignment: {
      inspector: wire.inspector,
      plannedDate: wire.plannedDate,
      scope: wire.sections === null ? { kind: 'full' } : { kind: 'sections', titles: wire.sections },
    },
    schemaVersion: wire.schemaVersion,
    closed: wire.closed,
    coversWholeSchema: wire.coversWholeSchema,
  };
}

export function attachedEvidenceOf(wire: WireAttachedEvidence, vocabulary: Vocabulary): AttachedEvidence {
  return {
    evidence: { kind: evidenceKindLabelled(vocabulary, wire.kind), reference: wire.reference, description: wire.description },
    attachedBy: wire.attachedBy,
    attachedAt: wire.attachedAt,
  };
}

function sectionTitleOf(version: SchemaVersion, criterionCode: string): string {
  const section = version.sections.find((candidate) => candidate.criteria.some((criterion) => criterion.code === criterionCode));
  return section?.title ?? '';
}

export function criterionResponseOf(wire: WireCriterionResponse, version: SchemaVersion, vocabulary: Vocabulary): CriterionResponse {
  const answered = wire.answer !== null && wire.answeredBy !== null && wire.answeredAt !== null;
  return {
    sectionTitle: sectionTitleOf(version, wire.criterion.code),
    criterion: criterionOf(wire.criterion, vocabulary),
    ...(answered
      ? { given: { value: rawAnswerOf(wire.answer!), answeredBy: wire.answeredBy!, answeredAt: wire.answeredAt! } }
      : {}),
    attachments: wire.evidence.map((attached) => attachedEvidenceOf(attached, vocabulary)),
    observations: wire.observations,
    outcome: wire.outcome,
  };
}

export function revisionOf(wire: WireRevision): Revision {
  return {
    number: wire.number,
    reason: wire.reason,
    sealedBy: wire.sealedBy,
    sealedAt: wire.sealedAt,
    outcomes: Object.entries(wire.outcomes).map(([criterionCode, outcome]) => ({ criterionCode, outcome })),
  };
}

export function raisedFindingIdsOf(sealedRevision: WireRevision): string[] {
  return linksNamed(sealedRevision, 'findings').map((finding) => lastSegmentOf(finding.href));
}

export function inspectionDetailsOf(
  wire: WireInspection,
  wireResponses: WireCriterionResponse[],
  wireRevisions: WireRevision[],
  version: SchemaVersion,
  vocabulary: Vocabulary,
): InspectionDetails {
  const firstResponse = wireResponses[0];
  return {
    ...inspectionSummaryOf(wire),
    responses: wireResponses.map((response) => criterionResponseOf(response, version, vocabulary)),
    revisions: wireRevisions.map(revisionOf),
    actions: [
      ...(firstResponse === undefined ? [] : actionsOfferedBy(firstResponse, responseActionByRel)),
      ...actionsOfferedBy(wire, inspectionActionByRel),
    ],
  };
}

export function inspectionActOf(wire: WireInspectionAct, inspection: InspectionDetails): InspectionAct {
  const lines: ActLine[] = wire.lines.map((line) => ({
    criterionCode: line.criterionCode,
    criterionText: line.criterionText,
    criterion: inspection.responses.find((response) => response.criterion.code === line.criterionCode)!.criterion,
    answer: rawAnswerOf(line.answer),
    answeredBy: line.answeredBy,
    answeredAt: line.answeredAt,
    evidenceCount: line.evidenceCount,
    observations: line.observations,
    outcome: line.outcome,
  }));
  return {
    inspectionId: inspection.id,
    assetCode: wire.assetCode,
    assetName: wire.assetName,
    assignment: inspection.assignment,
    schemaVersion: wire.schemaVersion,
    lines,
    revisions: inspection.revisions,
    history: wire.history.map(auditEntryOf),
  };
}
