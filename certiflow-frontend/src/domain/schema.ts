import type { Instant } from './shared';

export type Outcome = 'PENDING' | 'APPROVED' | 'OBSERVED' | 'REJECTED';

export interface Severity {
  name: string;
  label: string;
  outcomeWhenUnmet: Outcome;
  blocksCertification: boolean;
}

export interface EvidenceKind {
  name: string;
  label: string;
}

export interface Vocabulary {
  severities: Severity[];
  evidenceKinds: EvidenceKind[];
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
}

export interface Section {
  title: string;
  criteria: Criterion[];
}

export interface SchemaVersion {
  number: number;
  publishedAt: Instant;
  sections: Section[];
}

export type SchemaAction = 'publishSchemaVersion';

export interface InspectionSchema {
  name: string;
  assetType: string;
  currentVersion: number;
  versions: SchemaVersion[];
  actions: SchemaAction[];
}
