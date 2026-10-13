import type {
  ApprovalRule,
  Criterion,
  EvidenceKind,
  Outcome,
  RawAnswer,
  RuleType,
  Section,
  Severity,
} from '../../domain/schema';
import type { StoredResponse } from './store';
import { reject, requireText } from './support';

export const standardSeverities: Severity[] = [
  { name: 'MINOR', label: 'Minor', outcomeWhenUnmet: 'OBSERVED', blocksCertification: false },
  { name: 'MAJOR', label: 'Major', outcomeWhenUnmet: 'REJECTED', blocksCertification: true },
  { name: 'CRITICAL', label: 'Critical', outcomeWhenUnmet: 'REJECTED', blocksCertification: true },
];

export const standardEvidenceKinds: EvidenceKind[] = [
  { name: 'PHOTO', label: 'Photo' },
  { name: 'DOCUMENT', label: 'Document' },
];

type RuleOfType<Type extends RuleType> = Extract<ApprovalRule, { type: Type }>;

interface RuleBehaviour<Type extends RuleType> {
  answerKind: RawAnswer['kind'];
  validate(rule: RuleOfType<Type>): void;
  accepts(rule: RuleOfType<Type>, answer: RawAnswer): boolean;
}

function isNumeric(text: string): boolean {
  return text.trim() !== '' && Number.isFinite(Number(text));
}

const ruleBehaviours: { [Type in RuleType]: RuleBehaviour<Type> } = {
  'numeric-range': {
    answerKind: 'number',
    validate(rule) {
      requireText(rule.unit, 'A numeric rule needs a unit');
      if (!isNumeric(rule.minimum) || !isNumeric(rule.maximum)) {
        reject('InvalidRuleConfigurationException', 'A numeric rule needs a minimum and a maximum');
      }
      if (Number(rule.minimum) > Number(rule.maximum)) {
        reject('InvalidRuleConfigurationException', `The minimum ${rule.minimum} is greater than the maximum ${rule.maximum}`);
      }
    },
    accepts(rule, answer) {
      const measuredValue = Number(answer.value);
      return measuredValue >= Number(rule.minimum) && measuredValue <= Number(rule.maximum);
    },
  },
  'yes-no': {
    answerKind: 'yes-no',
    validate() {},
    accepts(rule, answer) {
      return answer.value === (rule.expected === 'YES');
    },
  },
  'option-in-list': {
    answerKind: 'option',
    validate(rule) {
      if (rule.acceptedOptions.length === 0) {
        reject('InvalidRuleConfigurationException', 'An option rule needs at least one accepted option');
      }
    },
    accepts(rule, answer) {
      return rule.acceptedOptions.includes(String(answer.value));
    },
  },
};

function behaviourOf(rule: ApprovalRule): RuleBehaviour<RuleType> {
  return ruleBehaviours[rule.type] as RuleBehaviour<RuleType>;
}

export function ensureAnswerFitsRule(criterion: Criterion, answer: RawAnswer): void {
  const expectedKind = behaviourOf(criterion.rule).answerKind;
  const numberIsUnreadable = answer.kind === 'number' && !isNumeric(answer.value);
  if (answer.kind !== expectedKind || numberIsUnreadable) {
    reject('AnswerTypeMismatchException', `Criterion ${criterion.code} expects a ${expectedKind} answer`);
  }
}

export function outcomeOf(response: StoredResponse): Outcome {
  const criterion = response.criterion;
  const evidenceRequirementsMet = criterion.requiredEvidence.every(
    (requirement) =>
      response.attachments.filter((attached) => attached.evidence.kind.name === requirement.kind.name).length >=
      requirement.minimum,
  );
  if (response.given === undefined || !evidenceRequirementsMet) {
    return 'PENDING';
  }
  return behaviourOf(criterion.rule).accepts(criterion.rule, response.given.value)
    ? 'APPROVED'
    : criterion.severity.outcomeWhenUnmet;
}

export function raisesFinding(outcome: Outcome): boolean {
  return outcome === 'OBSERVED' || outcome === 'REJECTED';
}

function validateCriterion(criterion: Criterion): void {
  if (criterion.code.trim() === '' || criterion.text.trim() === '') {
    reject('InvalidSchemaException', 'A criterion needs a code and a text');
  }
  behaviourOf(criterion.rule).validate(criterion.rule);
  const requiredKindNames = criterion.requiredEvidence.map((requirement) => requirement.kind.name);
  if (new Set(requiredKindNames).size !== requiredKindNames.length) {
    reject('InvalidSchemaException', `Criterion ${criterion.code} requires the same evidence kind more than once`);
  }
  if (criterion.requiredEvidence.some((requirement) => !Number.isInteger(requirement.minimum) || requirement.minimum < 1)) {
    reject('InvalidRuleConfigurationException', 'An evidence requirement must ask for at least one item');
  }
}

export function validateSections(sections: Section[]): void {
  if (sections.length === 0) {
    reject('InvalidSchemaException', 'A schema version needs at least one section');
  }
  sections.forEach((section) => {
    if (section.title.trim() === '') {
      reject('InvalidSchemaException', 'A section needs a title');
    }
    if (section.criteria.length === 0) {
      reject('InvalidSchemaException', `Section '${section.title}' needs at least one criterion`);
    }
    section.criteria.forEach(validateCriterion);
  });
  const criterionCodes = sections.flatMap((section) => section.criteria.map((criterion) => criterion.code));
  if (new Set(criterionCodes).size !== criterionCodes.length) {
    reject('InvalidSchemaException', 'Criterion codes must be unique within a schema version');
  }
}
