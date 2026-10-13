import type { ReactNode } from 'react';
import type { ApprovalRule, RawAnswer, RuleType } from '../../domain/schema';

type RuleOfType<Type extends RuleType> = Extract<ApprovalRule, { type: Type }>;

interface ConfigFieldsProps<Rule> {
  rule: Rule;
  onChange(rule: Rule): void;
}

interface AnswerInputProps<Rule> {
  rule: Rule;
  answer: RawAnswer | undefined;
  onChange(answer: RawAnswer): void;
}

export interface RuleKind<Rule extends ApprovalRule = ApprovalRule> {
  type: Rule['type'];
  label: string;
  emptyRule(): Rule;
  ConfigFields(props: ConfigFieldsProps<Rule>): ReactNode;
  AnswerInput(props: AnswerInputProps<Rule>): ReactNode;
  describe(rule: Rule): string;
  formatAnswer(rule: Rule, answer: RawAnswer): string;
}

const numericRange: RuleKind<RuleOfType<'numeric-range'>> = {
  type: 'numeric-range',
  label: 'Measurement within a range',
  emptyRule: () => ({ type: 'numeric-range', unit: '', minimum: '', maximum: '' }),
  ConfigFields: ({ rule, onChange }) => (
    <>
      <div className="field">
        <label>
          Unit
          <input value={rule.unit} placeholder="bar" onChange={(event) => onChange({ ...rule, unit: event.target.value })} />
        </label>
      </div>
      <div className="field">
        <label>
          Minimum
          <input inputMode="decimal" value={rule.minimum} onChange={(event) => onChange({ ...rule, minimum: event.target.value })} />
        </label>
      </div>
      <div className="field">
        <label>
          Maximum
          <input inputMode="decimal" value={rule.maximum} onChange={(event) => onChange({ ...rule, maximum: event.target.value })} />
        </label>
      </div>
    </>
  ),
  AnswerInput: ({ rule, answer, onChange }) => (
    <div className="field">
      <label>
        Measurement ({rule.unit})
        <input
          inputMode="decimal"
          value={answer === undefined ? '' : String(answer.value)}
          onChange={(event) => onChange({ kind: 'number', value: event.target.value })}
        />
      </label>
    </div>
  ),
  describe: (rule) => `Between ${rule.minimum} and ${rule.maximum} ${rule.unit}`,
  formatAnswer: (rule, answer) => `${answer.value} ${rule.unit}`,
};

const yesNo: RuleKind<RuleOfType<'yes-no'>> = {
  type: 'yes-no',
  label: 'Yes or no',
  emptyRule: () => ({ type: 'yes-no', expected: 'YES' }),
  ConfigFields: ({ rule, onChange }) => (
    <div className="field">
      <label>
        Expected answer
        <select value={rule.expected} onChange={(event) => onChange({ ...rule, expected: event.target.value === 'YES' ? 'YES' : 'NO' })}>
          <option value="YES">Yes</option>
          <option value="NO">No</option>
        </select>
      </label>
    </div>
  ),
  AnswerInput: ({ answer, onChange }) => (
    <div className="field">
      <label>
        Answer
        <select
          value={answer === undefined ? '' : answer.value === true ? 'yes' : 'no'}
          onChange={(event) => onChange({ kind: 'yes-no', value: event.target.value === 'yes' })}
        >
          <option value="" disabled>
            Choose…
          </option>
          <option value="yes">Yes</option>
          <option value="no">No</option>
        </select>
      </label>
    </div>
  ),
  describe: (rule) => `Expected answer: ${rule.expected === 'YES' ? 'Yes' : 'No'}`,
  formatAnswer: (_rule, answer) => (answer.value === true ? 'Yes' : 'No'),
};

const optionInList: RuleKind<RuleOfType<'option-in-list'>> = {
  type: 'option-in-list',
  label: 'One of a list of options',
  emptyRule: () => ({ type: 'option-in-list', acceptedOptions: [] }),
  ConfigFields: ({ rule, onChange }) => (
    <div className="field">
      <label>
        Accepted options (comma separated)
        <input
          defaultValue={rule.acceptedOptions.join(', ')}
          placeholder="Good, Acceptable"
          onChange={(event) =>
            onChange({
              ...rule,
              acceptedOptions: event.target.value
                .split(',')
                .map((option) => option.trim())
                .filter((option) => option !== ''),
            })
          }
        />
      </label>
    </div>
  ),
  AnswerInput: ({ rule, answer, onChange }) => {
    const suggestionsId = `options-${rule.acceptedOptions.join('-')}`;
    return (
      <div className="field">
        <label>
          Observed option
          <input
            list={suggestionsId}
            value={answer === undefined ? '' : String(answer.value)}
            onChange={(event) => onChange({ kind: 'option', value: event.target.value })}
          />
        </label>
        <datalist id={suggestionsId}>
          {rule.acceptedOptions.map((option) => (
            <option key={option} value={option} />
          ))}
        </datalist>
      </div>
    );
  },
  describe: (rule) => `Accepted: ${rule.acceptedOptions.join(', ')}`,
  formatAnswer: (_rule, answer) => String(answer.value),
};

const ruleKindsByType: { [Type in RuleType]: RuleKind<RuleOfType<Type>> } = {
  'numeric-range': numericRange,
  'yes-no': yesNo,
  'option-in-list': optionInList,
};

function ruleKindOfType(type: RuleType): RuleKind {
  return ruleKindsByType[type] as unknown as RuleKind;
}

export const ruleKinds: RuleKind[] = (Object.keys(ruleKindsByType) as RuleType[]).map(ruleKindOfType);

export function emptyRuleOfType(type: string): ApprovalRule {
  return (ruleKinds.find((kind) => kind.type === type) ?? ruleKinds[0]!).emptyRule();
}

export function RuleConfigFields({ rule, onChange }: ConfigFieldsProps<ApprovalRule>) {
  const kind = ruleKindOfType(rule.type);
  return <kind.ConfigFields rule={rule} onChange={onChange} />;
}

export function RuleAnswerInput({ rule, answer, onChange }: AnswerInputProps<ApprovalRule>) {
  const kind = ruleKindOfType(rule.type);
  return <kind.AnswerInput rule={rule} answer={answer} onChange={onChange} />;
}

export function describeRule(rule: ApprovalRule): string {
  return ruleKindOfType(rule.type).describe(rule);
}

export function formatAnswer(rule: ApprovalRule, answer: RawAnswer): string {
  return ruleKindOfType(rule.type).formatAnswer(rule, answer);
}
