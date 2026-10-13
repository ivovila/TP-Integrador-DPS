import type { ActLine, InspectionAction, InspectionDetails, InspectionSummary, Revision } from '../../domain/inspection';
import type { Criterion, SchemaVersion } from '../../domain/schema';
import type { Instant, Person } from '../../domain/shared';
import type { InspectionPort } from '../../ports/InspectionPort';
import { storedAssetWithCode } from './catalog';
import { findingsOfAsset, isOpenFinding, raiseFinding } from './finding';
import { ensureAnswerFitsRule, outcomeOf, raisesFinding } from './rules';
import { currentVersionOf, storedSchemaFor } from './schema';
import type { InMemorySystem, StoredInspection, StoredResponse } from './store';
import { isSamePerson, reject, requireText } from './support';

const closureReason = 'Inspection closed';

const actionsWhileOpen: InspectionAction[] = ['recordAnswer', 'attachEvidence', 'addObservation', 'closeInspection'];
const actionsOnceClosed: InspectionAction[] = ['rectifyInspection', 'generateInspectionAct'];

export function isClosed(inspection: StoredInspection): boolean {
  return inspection.revisions.length > 0;
}

export function coversWholeSchema(inspection: StoredInspection): boolean {
  return inspection.responses.length === criteriaOf(inspection.schemaVersion).length;
}

export function storedInspectionWithId(system: InMemorySystem, inspectionId: string): StoredInspection {
  return (
    system.inspections.get(inspectionId) ?? reject('InspectionNotFoundException', `Inspection ${inspectionId} does not exist`)
  );
}

function criteriaOf(version: SchemaVersion): Criterion[] {
  return version.sections.flatMap((section) => section.criteria);
}

function criterionWithCode(inspection: StoredInspection, criterionCode: string): Criterion {
  return (
    criteriaOf(inspection.schemaVersion).find((criterion) => criterion.code === criterionCode) ??
    reject(
      'CriterionNotInSchemaException',
      `Criterion ${criterionCode} is not part of schema version ${inspection.schemaVersion.number}`,
    )
  );
}

function responseInScope(inspection: StoredInspection, criterionCode: string): StoredResponse {
  return (
    inspection.responses.find((response) => response.criterion.code === criterionCode) ??
    reject('CriterionOutOfScopeException', `Criterion ${criterionCode} is outside the scope of the inspection`)
  );
}

function ensureOpen(inspection: StoredInspection): void {
  if (isClosed(inspection)) {
    reject('InspectionAlreadyClosedException', 'The inspection is closed and can only be changed by a rectification');
  }
}

function ensureClosed(inspection: StoredInspection): void {
  if (!isClosed(inspection)) {
    reject('InspectionNotClosedException', 'The inspection is still open');
  }
}

function sealRevision(inspection: StoredInspection, reason: string, sealedBy: Person, sealedAt: Instant): void {
  const revision: Revision = {
    number: inspection.revisions.length + 1,
    reason,
    sealedBy: { ...sealedBy },
    sealedAt,
    outcomes: inspection.responses.map((response) => ({
      criterionCode: response.criterion.code,
      outcome: outcomeOf(response),
    })),
  };
  inspection.revisions.push(revision);
}

function summaryOf(inspection: StoredInspection): InspectionSummary {
  return {
    id: inspection.id,
    assetCode: inspection.assetCode,
    assignment: inspection.assignment,
    schemaVersion: inspection.schemaVersion.number,
    closed: isClosed(inspection),
    coversWholeSchema: coversWholeSchema(inspection),
  };
}

function detailsOf(inspection: StoredInspection): InspectionDetails {
  return structuredClone({
    ...summaryOf(inspection),
    responses: inspection.responses.map((response) => ({ ...response, outcome: outcomeOf(response) })),
    revisions: inspection.revisions,
    actions: isClosed(inspection) ? actionsOnceClosed : actionsWhileOpen,
  });
}

function responsesInScope(version: SchemaVersion, scopedSectionTitles: string[] | undefined): StoredResponse[] {
  return version.sections
    .filter((section) => scopedSectionTitles === undefined || scopedSectionTitles.includes(section.title))
    .flatMap((section) =>
      section.criteria.map((criterion) => ({
        sectionTitle: section.title,
        criterion: structuredClone(criterion),
        attachments: [],
        observations: [],
      })),
    );
}

export function createInspectionPort(system: InMemorySystem): InspectionPort {
  return {
    async assignInspection(command) {
      const asset = storedAssetWithCode(system, command.assetCode);
      const version = currentVersionOf(storedSchemaFor(system, asset.type));
      requireText(command.inspector.id, 'An inspection needs an inspector');
      requireText(command.plannedDate, 'An inspection needs a planned date');
      const scopedSectionTitles = command.scope.kind === 'sections' ? command.scope.titles : undefined;
      if (scopedSectionTitles !== undefined) {
        if (scopedSectionTitles.length === 0) {
          reject('RequiredValueMissingException', 'A partial inspection needs at least one section');
        }
        const knownSectionTitles = version.sections.map((section) => section.title);
        const unknownTitle = scopedSectionTitles.find((title) => !knownSectionTitles.includes(title));
        if (unknownTitle !== undefined) {
          reject('SectionNotInSchemaException', `Section '${unknownTitle}' is not part of schema version ${version.number}`);
        }
      }
      if (isSamePerson(command.assignedBy, command.inspector)) {
        reject('AssignerCannotInspectException', `${command.inspector.name} assigns the inspection and cannot carry it out`);
      }
      if (isSamePerson(command.assignedBy, asset.responsible)) {
        reject(
          'AssetResponsibleCannotAssignInspectionException',
          `${command.assignedBy.name} is responsible for the asset and cannot assign its inspection`,
        );
      }
      if (isSamePerson(command.inspector, asset.responsible)) {
        reject(
          'AssetResponsibleCannotInspectOwnAssetException',
          `${command.inspector.name} is responsible for the asset and cannot inspect it`,
        );
      }
      const inspection: StoredInspection = {
        id: `INS-${system.inspections.size + 1}`,
        assetCode: asset.code,
        assetResponsible: { ...asset.responsible },
        assignment: structuredClone({
          inspector: command.inspector,
          plannedDate: command.plannedDate,
          scope: command.scope,
        }),
        schemaVersion: structuredClone(version),
        responses: responsesInScope(version, scopedSectionTitles),
        revisions: [],
      };
      system.inspections.set(inspection.id, inspection);
      system.auditLog.record(
        { kind: 'inspection', id: inspection.id },
        'ASSIGNED',
        command.assignedBy,
        system.clock.now(),
        `Schema version ${version.number}`,
      );
      return inspection.id;
    },

    async listInspections(filter) {
      return structuredClone(
        [...system.inspections.values()]
          .filter((inspection) => filter.assetCode === undefined || inspection.assetCode === filter.assetCode)
          .filter((inspection) => filter.status === undefined || isClosed(inspection) === (filter.status === 'closed'))
          .map(summaryOf),
      );
    },

    async findInspection(inspectionId) {
      return detailsOf(storedInspectionWithId(system, inspectionId));
    },

    async recordAnswer(command) {
      const inspection = storedInspectionWithId(system, command.inspectionId);
      const criterion = criterionWithCode(inspection, command.criterionCode);
      ensureAnswerFitsRule(criterion, command.answer);
      ensureOpen(inspection);
      const response = responseInScope(inspection, command.criterionCode);
      const answeredAt = system.clock.now();
      response.given = { value: { ...command.answer }, answeredBy: { ...command.answeredBy }, answeredAt };
      system.auditLog.record(
        { kind: 'inspection', id: inspection.id },
        'ANSWER_RECORDED',
        command.answeredBy,
        answeredAt,
        `${criterion.code} = ${command.answer.value}`,
      );
    },

    async attachEvidence(command) {
      const inspection = storedInspectionWithId(system, command.inspectionId);
      const criterion = criterionWithCode(inspection, command.criterionCode);
      ensureOpen(inspection);
      const response = responseInScope(inspection, command.criterionCode);
      requireText(command.evidence.reference, 'An evidence needs a reference');
      if (response.attachments.some((attached) => attached.evidence.reference === command.evidence.reference)) {
        reject(
          'DuplicateEvidenceException',
          `Criterion ${criterion.code} already has the evidence ${command.evidence.reference}`,
        );
      }
      const attachedAt = system.clock.now();
      response.attachments.push({
        evidence: structuredClone(command.evidence),
        attachedBy: { ...command.attachedBy },
        attachedAt,
      });
      system.auditLog.record(
        { kind: 'inspection', id: inspection.id },
        'EVIDENCE_ATTACHED',
        command.attachedBy,
        attachedAt,
        `${criterion.code}: ${command.evidence.reference}`,
      );
    },

    async addObservation(command) {
      const inspection = storedInspectionWithId(system, command.inspectionId);
      const criterion = criterionWithCode(inspection, command.criterionCode);
      ensureOpen(inspection);
      const response = responseInScope(inspection, command.criterionCode);
      requireText(command.text, 'An observation needs a text');
      const observedAt = system.clock.now();
      response.observations.push({ text: command.text, observedBy: { ...command.observedBy }, observedAt });
      system.auditLog.record(
        { kind: 'inspection', id: inspection.id },
        'OBSERVATION_ADDED',
        command.observedBy,
        observedAt,
        `${criterion.code}: ${command.text}`,
      );
    },

    async closeInspection(command) {
      const inspection = storedInspectionWithId(system, command.inspectionId);
      ensureOpen(inspection);
      const pendingCodes = inspection.responses
        .filter((response) => outcomeOf(response) === 'PENDING')
        .map((response) => response.criterion.code);
      if (pendingCodes.length > 0) {
        reject('InspectionIncompleteException', `These criteria are still pending: ${pendingCodes.join(', ')}`);
      }
      const closedAt = system.clock.now();
      sealRevision(inspection, closureReason, command.closedBy, closedAt);
      system.auditLog.record({ kind: 'inspection', id: inspection.id }, 'CLOSED', command.closedBy, closedAt, '');
      return inspection.responses
        .filter((response) => raisesFinding(outcomeOf(response)))
        .map((nonConformity) => raiseFinding(system, inspection, nonConformity, command.closedBy, closedAt));
    },

    async rectifyInspection(command) {
      const inspection = storedInspectionWithId(system, command.inspectionId);
      const criterion = criterionWithCode(inspection, command.criterionCode);
      ensureAnswerFitsRule(criterion, command.answer);
      ensureClosed(inspection);
      if (command.reason.trim() === '') {
        reject('RectificationReasonRequiredException', 'A rectification must explain its reason');
      }
      const response = responseInScope(inspection, command.criterionCode);
      const rectifiedAt = system.clock.now();
      response.given = { value: { ...command.answer }, answeredBy: { ...command.rectifiedBy }, answeredAt: rectifiedAt };
      sealRevision(inspection, command.reason, command.rectifiedBy, rectifiedAt);
      system.auditLog.record(
        { kind: 'inspection', id: inspection.id },
        'RECTIFIED',
        command.rectifiedBy,
        rectifiedAt,
        `${criterion.code} = ${command.answer.value} (${command.reason})`,
      );
      const alreadyHasOpenFinding = findingsOfAsset(system, inspection.assetCode).some(
        (finding) =>
          isOpenFinding(finding) && finding.inspectionId === inspection.id && finding.criterion.code === criterion.code,
      );
      return raisesFinding(outcomeOf(response)) && !alreadyHasOpenFinding
        ? [raiseFinding(system, inspection, response, command.rectifiedBy, rectifiedAt)]
        : [];
    },

    async generateInspectionAct(inspectionId) {
      const inspection = storedInspectionWithId(system, inspectionId);
      ensureClosed(inspection);
      const lines: ActLine[] = inspection.responses.flatMap((response) =>
        response.given === undefined
          ? []
          : [
              {
                criterionCode: response.criterion.code,
                criterionText: response.criterion.text,
                criterion: response.criterion,
                answer: response.given.value,
                answeredBy: response.given.answeredBy,
                answeredAt: response.given.answeredAt,
                evidenceCount: response.attachments.length,
                observations: response.observations.map((observation) => observation.text),
                outcome: outcomeOf(response),
              },
            ],
      );
      return structuredClone({
        inspectionId: inspection.id,
        assetCode: inspection.assetCode,
        assetName: storedAssetWithCode(system, inspection.assetCode).name,
        assignment: inspection.assignment,
        schemaVersion: inspection.schemaVersion.number,
        lines,
        revisions: inspection.revisions,
        history: system.auditLog.entriesFor({ kind: 'inspection', id: inspection.id }),
      });
    },
  };
}
