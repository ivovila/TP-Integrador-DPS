import { expect, test } from 'vitest';
import type { CertificateAction } from '../../domain/certification';
import type { InspectionAction } from '../../domain/inspection';
import type { RawAnswer } from '../../domain/schema';
import actFixture from './__fixtures__/act.json';
import assetWithSubsystems from './__fixtures__/asset-with-subsystems.json';
import assetWithoutSubsystems from './__fixtures__/asset-without-subsystems.json';
import auditEntries from './__fixtures__/audit-entries.json';
import certificateDocument from './__fixtures__/certificate-document.json';
import globalCertificateWithInvalidDependency from './__fixtures__/certificate-global-dependency-invalid.json';
import globalCertificate from './__fixtures__/certificate-global.json';
import partialCertificate from './__fixtures__/certificate-partial.json';
import suspendedCertificate from './__fixtures__/certificate-suspended.json';
import closureWithOneFinding from './__fixtures__/closure-one-finding.json';
import closureWithTwoFindings from './__fixtures__/closure-two-findings.json';
import approvedEligibility from './__fixtures__/eligibility-approved.json';
import conditionalEligibility from './__fixtures__/eligibility-conditional.json';
import rejectedEligibility from './__fixtures__/eligibility-rejected.json';
import closedFinding from './__fixtures__/finding-closed.json';
import openFinding from './__fixtures__/finding-open.json';
import findingsSummary from './__fixtures__/findings-summary.json';
import closedInspection from './__fixtures__/inspection-closed.json';
import openInspection from './__fixtures__/inspection-open.json';
import partialInspection from './__fixtures__/inspection-partial-assigned.json';
import rectificationWithoutFinding from './__fixtures__/rectification-no-finding.json';
import closedResponses from './__fixtures__/responses-closed.json';
import openResponses from './__fixtures__/responses-open.json';
import revisions from './__fixtures__/revisions.json';
import schemaVersions from './__fixtures__/schema-versions.json';
import schemaWithScheduledVersion from './__fixtures__/schema-with-scheduled-version.json';
import scheduledVersion from './__fixtures__/version-published-future.json';
import vocabularyFixture from './__fixtures__/vocabulary.json';
import { auditEntryOf } from './auditMapping';
import { assetOf } from './catalogMapping';
import { basisOf, certificateDocumentOf, certificateOf, decisionOf, eligibilityDecisionOf, scopeOf } from './certificationMapping';
import { findingOf, findingsSummaryOf } from './findingMapping';
import {
  inspectionActOf,
  inspectionDetailsOf,
  inspectionSummaryOf,
  raisedFindingIdsOf,
  revisionOf,
} from './inspectionMapping';
import { answerLiteral, schemaOf, sectionsToWire, versionOf, vocabularyOf } from './schemaMapping';
import type {
  WireAsset,
  WireAuditEntry,
  WireCertificate,
  WireCertificateDocument,
  WireCriterionResponse,
  WireEligibility,
  WireFinding,
  WireFindingsSummary,
  WireInspection,
  WireInspectionAct,
  WireRevision,
  WireSchema,
  WireSchemaVersion,
  WireVocabulary,
} from './wire';

function captured<Wire>(fixture: unknown): Wire {
  return fixture as Wire;
}

const vocabulary = vocabularyOf(captured<WireVocabulary>(vocabularyFixture));
const firstVersionOnTheWire = captured<WireSchemaVersion>(schemaVersions._embedded.versions[0]);
const firstVersion = versionOf(firstVersionOnTheWire, vocabulary);
const responsesOf = (fixture: { _embedded: { responses: unknown[] } }) => captured<WireCriterionResponse[]>(fixture._embedded.responses);
const revisionsOnTheWire = captured<WireRevision[]>(revisions._embedded.revisions);

function openInspectionDetails() {
  return inspectionDetailsOf(captured<WireInspection>(openInspection), responsesOf(openResponses), [], firstVersion, vocabulary);
}

function closedInspectionDetails() {
  return inspectionDetailsOf(
    captured<WireInspection>(closedInspection),
    responsesOf(closedResponses),
    revisionsOnTheWire,
    firstVersion,
    vocabulary,
  );
}

test('the vocabulary offers severities, evidence kinds and the jurisdictions with a registered policy', () => {
  expect(vocabulary.severities.map((severity) => [severity.name, severity.label, severity.outcomeWhenUnmet])).toEqual([
    ['MINOR', 'Minor', 'OBSERVED'],
    ['MAJOR', 'Major', 'REJECTED'],
    ['CRITICAL', 'Critical', 'REJECTED'],
  ]);
  expect(vocabulary.evidenceKinds).toEqual([
    { name: 'PHOTO', label: 'Photo' },
    { name: 'DOCUMENT', label: 'Document' },
  ]);
  expect(vocabulary.jurisdictions).toContainEqual({ code: 'DEMO-STRICT', policyCode: 'STRICT' });
});

test('an asset keeps its jurisdiction and subsystems', () => {
  expect(assetOf(captured<WireAsset>(assetWithSubsystems), [])).toMatchObject({
    code: 'CAP-0001',
    jurisdictionCode: 'DEMO-CONDITIONAL',
    responsible: { id: 'EMP-003', name: 'Raul Mendez' },
    characteristics: [{ name: 'Area', value: '120 m2' }],
    subsystems: [
      { code: 'ELECTRICAL', name: 'Electrical installation' },
      { code: 'PRESSURE', name: 'Pressure system' },
    ],
  });
});

test('an asset can be inspected only when its type has a schema', () => {
  expect(assetOf(captured<WireAsset>(assetWithoutSubsystems), []).actions).toEqual([]);
  expect(assetOf(captured<WireAsset>(assetWithoutSubsystems), ['Capture tank']).actions).toEqual(['assignInspection']);
});

test('a global certificate is offered only by the asset that links to it', () => {
  expect(assetOf(captured<WireAsset>(assetWithSubsystems), ['Capture plant']).actions).toEqual([
    'assignInspection',
    'issueGlobalCertificate',
  ]);
  expect(assetOf(captured<WireAsset>(assetWithoutSubsystems), ['Capture tank']).actions).not.toContain('issueGlobalCertificate');
});

test('a criterion gets its severity and evidence kinds from the vocabulary and its rule in the shape of its type', () => {
  const [electrical, pressure] = firstVersion.sections;
  expect(electrical!.criteria[0]).toEqual({
    code: 'EARTH',
    text: 'Earthing resistance',
    severity: { name: 'MAJOR', label: 'Major', outcomeWhenUnmet: 'REJECTED' },
    rule: { type: 'numeric-range', unit: 'ohm', minimum: '0', maximum: '5' },
    requiredEvidence: [{ kind: { name: 'PHOTO', label: 'Photo' }, minimum: 1 }],
  });
  expect(electrical!.criteria[1]!.rule).toEqual({ type: 'yes-no', expected: 'YES' });
  expect(pressure!.criteria[0]!.rule).toEqual({ type: 'option-in-list', acceptedOptions: ['Good', 'Acceptable'] });
});

test('a subsystem can come from the section or from the criterion itself', () => {
  const [electrical, pressure] = firstVersion.sections;
  expect(electrical!.subsystemCode).toBe('ELECTRICAL');
  expect(electrical!.criteria[0]).not.toHaveProperty('subsystemCode');
  expect(pressure).not.toHaveProperty('subsystemCode');
  expect(pressure!.criteria[0]!.subsystemCode).toBe('PRESSURE');
  expect(firstVersion.requiredSubsystems).toEqual(['ELECTRICAL', 'PRESSURE']);
});

test('a schema tells apart the version in force from the latest one published', () => {
  const schema = schemaOf(
    captured<WireSchema>(schemaWithScheduledVersion),
    [captured<WireSchemaVersion>(scheduledVersion), firstVersionOnTheWire],
    vocabulary,
  );
  expect(schema).toMatchObject({ assetType: 'Capture plant', currentVersion: 1, latestPublishedVersion: 2 });
  expect(schema.versions.map((version) => version.number)).toEqual([1, 2]);
  expect(schema.versions[1]!.effectiveFrom).toBe('2031-01-01T03:00:00Z');
});

test('sections are sent with names for severity and evidence kind and only the fields of their rule type', () => {
  const [electrical, pressure] = sectionsToWire(firstVersion.sections);
  expect(electrical).toEqual({
    title: 'Electrical',
    subsystemCode: 'ELECTRICAL',
    criteria: [
      {
        code: 'EARTH',
        text: 'Earthing resistance',
        severity: 'MAJOR',
        rule: { type: 'numeric-range', unit: 'ohm', minimum: 0, maximum: 5 },
        requiredEvidence: [{ kind: 'PHOTO', minimum: 1 }],
      },
      { code: 'PANEL', text: 'Panel labelled', severity: 'MINOR', rule: { type: 'yes-no', expected: 'YES' }, requiredEvidence: [] },
    ],
  });
  expect(pressure!.criteria[0]).toMatchObject({ rule: { type: 'option-in-list', options: ['Good', 'Acceptable'] }, subsystemCode: 'PRESSURE' });
});

test('a limit left blank is sent as missing so the server rejects the rule', () => {
  const [section] = sectionsToWire([
    {
      title: 'Broken',
      criteria: [{ ...firstVersion.sections[0]!.criteria[0]!, rule: { type: 'numeric-range', unit: 'bar', minimum: '', maximum: 'abc' } }],
    },
  ]);
  expect(section!.criteria[0]!.rule).toEqual({ type: 'numeric-range', unit: 'bar', minimum: null, maximum: null });
});

const answersAsSent: [string, RawAnswer, string][] = [
  ['a measurement is sent as the number that was typed', { kind: 'number', value: '2.50' }, '2.50'],
  ['a measurement that is not a number is sent as text for the server to reject', { kind: 'number', value: 'abc' }, '"abc"'],
  ['a yes or no answer is sent as a boolean', { kind: 'yes-no', value: false }, 'false'],
  ['an option is sent as text', { kind: 'option', value: 'Good' }, '"Good"'],
];

test.each(answersAsSent)('%s', (_description, answer, expectedLiteral) => {
  expect(answerLiteral(answer)).toBe(expectedLiteral);
});

test('an inspection that covers the whole schema has a full scope', () => {
  expect(inspectionSummaryOf(captured<WireInspection>(openInspection))).toMatchObject({
    assetCode: 'CAP-0001',
    assignment: { inspector: { id: 'EMP-002', name: 'Ines Duarte' }, plannedDate: '2026-10-20', scope: { kind: 'full' } },
    schemaVersion: 1,
    closed: false,
    coversWholeSchema: true,
  });
});

test('a partial inspection names the sections in its scope', () => {
  expect(inspectionSummaryOf(captured<WireInspection>(partialInspection))).toMatchObject({
    assignment: { scope: { kind: 'sections', titles: ['Electrical'] } },
    coversWholeSchema: false,
  });
});

const inspectionActions: [string, () => { actions: InspectionAction[] }, InspectionAction[]][] = [
  ['an open inspection offers execution and closure', openInspectionDetails, ['recordAnswer', 'attachEvidence', 'addObservation', 'closeInspection']],
  ['a closed inspection offers rectification and its act', closedInspectionDetails, ['rectifyInspection', 'generateInspectionAct']],
];

test.each(inspectionActions)('%s', (_description, inspection, expectedActions) => {
  expect(inspection().actions).toEqual(expectedActions);
});

test('each answer is placed in the section its criterion has in the captured schema version', () => {
  expect(openInspectionDetails().responses.map((response) => [response.criterion.code, response.sectionTitle])).toEqual([
    ['EARTH', 'Electrical'],
    ['PANEL', 'Electrical'],
    ['VALVE', 'Pressure'],
  ]);
});

test('an answer keeps its value, who gave it, its evidence and its observations', () => {
  const [earth, panel, valve] = openInspectionDetails().responses;
  expect(earth).toMatchObject({
    given: { value: { kind: 'number', value: '2.5' }, answeredBy: { id: 'EMP-002', name: 'Ines Duarte' } },
    attachments: [{ evidence: { kind: { name: 'PHOTO', label: 'Photo' }, reference: 'earth.jpg', description: 'Meter reading' } }],
    outcome: 'APPROVED',
  });
  expect(panel).toMatchObject({
    given: { value: { kind: 'yes-no', value: false } },
    observations: [{ text: 'Two labels missing', observedBy: { id: 'EMP-002' } }],
    outcome: 'OBSERVED',
  });
  expect(valve!.given!.value).toEqual({ kind: 'option', value: 'Good' });
});

test('a criterion without an answer has none', () => {
  const unanswered = { ...responsesOf(openResponses)[0]!, answer: null, answeredBy: null, answeredAt: null, outcome: 'PENDING' as const };
  const details = inspectionDetailsOf(captured<WireInspection>(openInspection), [unanswered], [], firstVersion, vocabulary);
  expect(details.responses[0]).not.toHaveProperty('given');
  expect(details.responses[0]!.outcome).toBe('PENDING');
});

test('a revision lists the outcome of each criterion when it was sealed', () => {
  const [closure, rectification] = revisionsOnTheWire.map(revisionOf);
  expect(closure).toMatchObject({ number: 1, reason: 'Inspection closed', sealedBy: { id: 'EMP-002' } });
  expect(closure!.outcomes).toEqual([
    { criterionCode: 'EARTH', outcome: 'APPROVED' },
    { criterionCode: 'PANEL', outcome: 'OBSERVED' },
    { criterionCode: 'VALVE', outcome: 'APPROVED' },
  ]);
  expect(rectification).toMatchObject({ number: 2, reason: 'Meter misread' });
});

test('closing tells which findings it raised, whether it was one, several or none', () => {
  expect(raisedFindingIdsOf(captured<WireRevision>(closureWithOneFinding))).toEqual([openFinding.id]);
  expect(raisedFindingIdsOf(captured<WireRevision>(closureWithTwoFindings))).toHaveLength(2);
  expect(raisedFindingIdsOf(captured<WireRevision>(rectificationWithoutFinding))).toEqual([]);
});

test('the inspection act joins the answers with their criteria, the revisions and the history', () => {
  const act = inspectionActOf(captured<WireInspectionAct>(actFixture), closedInspectionDetails());
  expect(act).toMatchObject({ assetCode: 'CAP-0001', assetName: 'Capture plant', schemaVersion: 1, assignment: { scope: { kind: 'full' } } });
  expect(act.lines[0]).toMatchObject({
    criterionCode: 'EARTH',
    criterion: { rule: { type: 'numeric-range', unit: 'ohm' } },
    answer: { kind: 'number', value: '3.1' },
    evidenceCount: 1,
    outcome: 'APPROVED',
  });
  expect(act.revisions.map((revision) => revision.number)).toEqual([1, 2]);
  expect(act.history[0]).toMatchObject({ action: 'ASSIGNED', performedBy: { id: 'EMP-001', name: 'Paula Rios' } });
});

test('a history entry names who acted as a person', () => {
  const [assignment] = captured<WireAuditEntry[]>(auditEntries._embedded.auditEntries).map(auditEntryOf);
  expect(assignment).toEqual({
    action: 'ASSIGNED',
    label: 'Inspection assigned',
    performedBy: { id: 'EMP-001', name: 'Paula Rios' },
    occurredAt: auditEntries._embedded.auditEntries[0]!.occurredAt,
    detail: 'Schema version 1',
  });
});

test('an open finding carries its severity, its subsystem and offers to plan an action', () => {
  const finding = findingOf(captured<WireFinding>(openFinding), vocabulary);
  expect(finding).toMatchObject({
    assetCode: 'CAP-0001',
    criterion: { code: 'PANEL' },
    severity: { name: 'MINOR', label: 'Minor' },
    subsystemCode: 'ELECTRICAL',
    open: true,
    actions: ['planCorrectiveAction'],
  });
  expect(finding.correctiveActions.map((action) => [action.id, action.state, action.actions])).toEqual([
    [1, 'OPEN', ['verifyCorrectiveAction']],
    [2, 'OPEN', ['verifyCorrectiveAction']],
  ]);
  expect(finding.correctiveActions[0]!.verifications).toMatchObject([{ result: 'REJECTED', notes: 'Still missing', verifier: { id: 'EMP-004' } }]);
});

test('a closed finding shows its accepted and cancelled actions and offers nothing', () => {
  const finding = findingOf(captured<WireFinding>(closedFinding), vocabulary);
  expect(finding.open).toBe(false);
  expect(finding.actions).toEqual([]);
  expect(finding.correctiveActions.map((action) => [action.state, action.actions])).toEqual([
    ['CLOSED', []],
    ['CANCELLED', []],
  ]);
});

test('each line of the findings summary identifies its finding and its subsystem', () => {
  expect(findingsSummaryOf(captured<WireFindingsSummary>(findingsSummary))).toEqual({
    assetCode: 'CAP-0001',
    openFindings: 1,
    findings: [
      {
        findingId: openFinding.id,
        criterionCode: 'PANEL',
        description: 'Panel labelled',
        severity: 'Minor',
        responsible: { id: 'EMP-003', name: 'Raul Mendez' },
        open: true,
        subsystemCode: 'ELECTRICAL',
        overdueActions: 0,
      },
    ],
  });
});

test('a scope is told apart by the type the server sends', () => {
  expect(scopeOf({ type: 'WHOLE_ASSET', subsystemCode: null })).toEqual({ kind: 'wholeAsset' });
  expect(scopeOf({ type: 'SUBSYSTEM', subsystemCode: 'ELECTRICAL' })).toEqual({ kind: 'subsystem', subsystemCode: 'ELECTRICAL' });
});

test('a certificate is based on an inspection or on its partial certificates', () => {
  expect(basisOf(captured<WireCertificate>(partialCertificate).basis)).toEqual({ kind: 'inspection', inspectionId: openInspection.id });
  expect(basisOf(captured<WireCertificate>(globalCertificate).basis)).toEqual({
    kind: 'partialCertificates',
    partialCertificateNumbers: ['CERT-00001', 'CERT-00002'],
    globalPolicy: 'ALL-REQUIRED-SUBSYSTEMS',
  });
});

test('a conditional decision carries its conditions, the validity the policy grants and the policy applied', () => {
  expect(eligibilityDecisionOf(captured<WireEligibility>(conditionalEligibility), openInspection.id)).toMatchObject({
    inspectionId: openInspection.id,
    scope: { kind: 'subsystem', subsystemCode: 'ELECTRICAL' },
    outcome: 'CONDITIONAL',
    eligible: true,
    conditions: [{ findingId: openFinding.id, criterionCode: 'PANEL' }],
    validity: { validFrom: conditionalEligibility.decision.validFrom, validUntil: conditionalEligibility.decision.validUntil },
    policyCode: 'CONDITIONAL',
    jurisdictionCode: 'DEMO-CONDITIONAL',
    actions: ['issueCertificate'],
  });
});

test('an approved decision has no conditions and offers to issue', () => {
  expect(eligibilityDecisionOf(captured<WireEligibility>(approvedEligibility), openInspection.id)).toMatchObject({
    outcome: 'APPROVED',
    conditions: [],
    actions: ['issueCertificate'],
  });
});

test('a rejected decision explains every reason, grants no validity and offers nothing', () => {
  const decision = eligibilityDecisionOf(captured<WireEligibility>(rejectedEligibility), 'tank-inspection');
  expect(decision).toMatchObject({ outcome: 'REJECTED', eligible: false, scope: { kind: 'wholeAsset' }, policyCode: 'STRICT', actions: [] });
  expect(decision.ineligibilities.map((ineligibility) => ineligibility.reason)).toEqual(['BLOCKING_FINDINGS', 'BLOCKING_FINDINGS']);
  expect(decision).not.toHaveProperty('validity');
});

const certificateActions: [string, unknown, string, CertificateAction[]][] = [
  ['an active partial certificate can be suspended or renewed', partialCertificate, 'ACTIVE', ['suspendCertificate', 'renewCertificate']],
  ['a suspended certificate can only be renewed', suspendedCertificate, 'SUSPENDED', ['renewCertificate']],
  ['an active global certificate is renewed from partial certificates', globalCertificate, 'ACTIVE', ['suspendCertificate', 'renewGlobalCertificate']],
  [
    'a global certificate that lost a backing can only be renewed',
    globalCertificateWithInvalidDependency,
    'DEPENDENCY_INVALID',
    ['renewGlobalCertificate'],
  ],
];

test.each(certificateActions)('%s', (_description, fixture, expectedStatus, expectedActions) => {
  const certificate = certificateOf(captured<WireCertificate>(fixture), ['ELECTRICAL', 'PRESSURE']);
  expect(certificate.status).toBe(expectedStatus);
  expect(certificate.actions).toEqual(expectedActions);
});

test('a certificate keeps its scope, the schema version behind it, its modality and the decision that granted it', () => {
  expect(certificateOf(captured<WireCertificate>(partialCertificate), ['ELECTRICAL', 'PRESSURE'])).toMatchObject({
    number: 'CERT-00001',
    assetCode: 'CAP-0001',
    scope: { kind: 'subsystem', subsystemCode: 'ELECTRICAL' },
    schemaVersion: 1,
    requiredSubsystems: ['ELECTRICAL', 'PRESSURE'],
    modality: 'CONDITIONAL',
    decision: { outcome: 'CONDITIONAL', policyCode: 'CONDITIONAL', jurisdictionCode: 'DEMO-CONDITIONAL' },
    issuedBy: { id: 'EMP-005', name: 'Carlos Vidal' },
    suspensions: [],
  });
});

test('a suspended certificate keeps the reason and who suspended it', () => {
  expect(certificateOf(captured<WireCertificate>(suspendedCertificate), []).suspensions).toMatchObject([
    { reason: 'Valve recall', suspendedBy: { id: 'EMP-005' } },
  ]);
});

test('the certificate document states scope, basis, modality and status on its date', () => {
  expect(certificateDocumentOf(captured<WireCertificateDocument>(certificateDocument))).toMatchObject({
    number: 'CERT-00003',
    assetName: 'Capture plant',
    scope: { kind: 'wholeAsset' },
    basis: { kind: 'partialCertificates', partialCertificateNumbers: ['CERT-00001', 'CERT-00002'] },
    modality: 'CONDITIONAL',
    status: 'ACTIVE',
    statusDate: certificateDocument.statusDate,
  });
});

test('the decision of a global certificate names the global policy that derived it', () => {
  expect(decisionOf(captured<WireCertificate>(globalCertificate).decision)).toMatchObject({
    scope: { kind: 'wholeAsset' },
    policyCode: 'ALL-REQUIRED-SUBSYSTEMS',
  });
});
