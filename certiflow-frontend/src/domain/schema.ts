import type { Instant } from './shared';

export type Outcome = 'PENDING' | 'APPROVED' | 'OBSERVED' | 'REJECTED';

export interface Severity {
  name: string;
  label: string;
  outcomeWhenUnmet: Outcome;
}

export interface EvidenceKind {
  name: string;
  label: string;
}

export interface Jurisdiction {
  code: string;
  policyCode: string;
}

export interface Vocabulary {
  severities: Severity[];
  evidenceKinds: EvidenceKind[];
  jurisdictions: Jurisdiction[];
}

export type YesNoAnswer = 'YES' | 'NO';

export interface NumericRangeRule {
  type: 'numeric-range';
  unit: string;
  minimum: string;
  maximum: string;
}

export interface YesNoRule {
  type: 'yes-no';
  expected: YesNoAnswer;
}

export interface OptionInListRule {
  type: 'option-in-list';
  acceptedOptions: string[];
}

export type ApprovalRule = NumericRangeRule | YesNoRule | OptionInListRule;
export type RuleType = ApprovalRule['type'];

export type RawAnswer =
  | { kind: 'number'; value: string }
  | { kind: 'yes-no'; value: boolean }
  | { kind: 'option'; value: string };

export interface EvidenceRequirement {
  kind: EvidenceKind;
  minimum: number;
}

export interface Criterion {
  code: string;
  text: string;
  severity: Severity;
  rule: ApprovalRule;
  requiredEvidence: EvidenceRequirement[];
  /** Subsistema propio; si falta, el criterio hereda el de su sección. */
  subsystemCode?: string;
}

export interface Section {
  title: string;
  criteria: Criterion[];
  subsystemCode?: string;
}

export interface SchemaVersion {
  number: number;
  publishedAt: Instant;
  /** Desde cuándo rige; igual a publishedAt si se publicó con vigencia inmediata. */
  effectiveFrom: Instant;
  requiredSubsystems: string[];
  sections: Section[];
}

export type SchemaAction = 'publishSchemaVersion';

export interface InspectionSchema {
  name: string;
  assetType: string;
  /** La versión vigente ahora: la que captura una inspección asignada en este momento. */
  currentVersion: number;
  /** La última publicada, que puede estar programada para más adelante. */
  latestPublishedVersion: number;
  versions: SchemaVersion[];
  actions: SchemaAction[];
}
