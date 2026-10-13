import type {
  ApprovalRule,
  Criterion,
  EvidenceKind,
  InspectionSchema,
  RawAnswer,
  RuleType,
  SchemaVersion,
  Section,
  Severity,
  Vocabulary,
} from '../../domain/schema';
import { SerializedJson } from './HalClient';
import type { WireAnswer, WireCriterion, WireRule, WireSchema, WireSchemaVersion, WireSection, WireVocabulary } from './wire';

export function vocabularyOf(wire: WireVocabulary): Vocabulary {
  return { severities: wire.severities, evidenceKinds: wire.evidenceKinds, jurisdictions: wire.jurisdictions };
}

export function severityNamed(vocabulary: Vocabulary, name: string): Severity {
  const severity = vocabulary.severities.find((candidate) => candidate.name === name);
  if (severity === undefined) {
    throw new Error(`The vocabulary has no severity named ${name}`);
  }
  return severity;
}

export function evidenceKindNamed(vocabulary: Vocabulary, name: string): EvidenceKind {
  const evidenceKind = vocabulary.evidenceKinds.find((candidate) => candidate.name === name);
  if (evidenceKind === undefined) {
    throw new Error(`The vocabulary has no evidence kind named ${name}`);
  }
  return evidenceKind;
}

export function evidenceKindLabelled(vocabulary: Vocabulary, label: string): EvidenceKind {
  const evidenceKind = vocabulary.evidenceKinds.find((candidate) => candidate.label === label);
  if (evidenceKind === undefined) {
    throw new Error(`The vocabulary has no evidence kind labelled ${label}`);
  }
  return evidenceKind;
}

const ruleFromWire: { [Type in RuleType]: (wire: WireRule) => Extract<ApprovalRule, { type: Type }> } = {
  'numeric-range': (wire) => ({
    type: 'numeric-range',
    unit: wire.unit ?? '',
    minimum: String(wire.minimum ?? ''),
    maximum: String(wire.maximum ?? ''),
  }),
  'yes-no': (wire) => ({ type: 'yes-no', expected: wire.expected ?? 'YES' }),
  'option-in-list': (wire) => ({ type: 'option-in-list', acceptedOptions: wire.options ?? [] }),
};

function numberOrNull(text: string): number | null {
  return text.trim() === '' || !Number.isFinite(Number(text)) ? null : Number(text);
}

const ruleToWire: { [Type in RuleType]: (rule: Extract<ApprovalRule, { type: Type }>) => WireRule } = {
  'numeric-range': (rule) => ({
    type: 'numeric-range',
    unit: rule.unit,
    minimum: numberOrNull(rule.minimum),
    maximum: numberOrNull(rule.maximum),
  }),
  'yes-no': (rule) => ({ type: 'yes-no', expected: rule.expected }),
  'option-in-list': (rule) => ({ type: 'option-in-list', options: rule.acceptedOptions }),
};

export function criterionOf(wire: WireCriterion, vocabulary: Vocabulary): Criterion {
  return {
    code: wire.code,
    text: wire.text,
    severity: severityNamed(vocabulary, wire.severity),
    rule: ruleFromWire[wire.rule.type](wire.rule),
    requiredEvidence: wire.requiredEvidence.map((requirement) => ({
      kind: evidenceKindNamed(vocabulary, requirement.kind),
      minimum: requirement.minimum,
    })),
    ...(wire.subsystemCode == null ? {} : { subsystemCode: wire.subsystemCode }),
  };
}

function sectionOf(wire: WireSection, vocabulary: Vocabulary): Section {
  return {
    title: wire.title,
    criteria: wire.criteria.map((criterion) => criterionOf(criterion, vocabulary)),
    ...(wire.subsystemCode == null ? {} : { subsystemCode: wire.subsystemCode }),
  };
}

export function versionOf(wire: WireSchemaVersion, vocabulary: Vocabulary): SchemaVersion {
  return {
    number: wire.number,
    publishedAt: wire.publishedAt,
    effectiveFrom: wire.effectiveFrom,
    requiredSubsystems: [...wire.requiredSubsystems].sort(),
    sections: wire.sections.map((section) => sectionOf(section, vocabulary)),
  };
}

export function schemaOf(wire: WireSchema, wireVersions: WireSchemaVersion[], vocabulary: Vocabulary): InspectionSchema {
  const versions = wireVersions.map((version) => versionOf(version, vocabulary)).sort((first, second) => first.number - second.number);
  return {
    name: wire.name,
    assetType: wire.assetType,
    currentVersion: wire.currentVersion,
    latestPublishedVersion: Math.max(...versions.map((version) => version.number)),
    versions,
    actions: ['publishSchemaVersion'],
  };
}

function criterionToWire(criterion: Criterion): WireCriterion {
  const ruleOfCriterion = ruleToWire[criterion.rule.type] as (rule: ApprovalRule) => WireRule;
  return {
    code: criterion.code,
    text: criterion.text,
    severity: criterion.severity.name,
    rule: ruleOfCriterion(criterion.rule),
    requiredEvidence: criterion.requiredEvidence.map((requirement) => ({ kind: requirement.kind.name, minimum: requirement.minimum })),
    ...(criterion.subsystemCode === undefined ? {} : { subsystemCode: criterion.subsystemCode }),
  };
}

export function sectionsToWire(sections: Section[]): WireSection[] {
  return sections.map((section) => ({
    title: section.title,
    criteria: section.criteria.map(criterionToWire),
    ...(section.subsystemCode === undefined ? {} : { subsystemCode: section.subsystemCode }),
  }));
}

export function rawAnswerOf(wire: WireAnswer): RawAnswer {
  if (typeof wire === 'number') {
    return { kind: 'number', value: String(wire) };
  }
  return typeof wire === 'boolean' ? { kind: 'yes-no', value: wire } : { kind: 'option', value: wire };
}

const decimalLiteral = /^-?\d+(\.\d+)?$/;

export function answerLiteral(answer: RawAnswer): string {
  const typedNumber = String(answer.value).trim();
  return answer.kind === 'number' && decimalLiteral.test(typedNumber) ? typedNumber : JSON.stringify(answer.value);
}

export function answerBody(answer: RawAnswer): SerializedJson {
  return new SerializedJson(`{"value":${answerLiteral(answer)}}`);
}
