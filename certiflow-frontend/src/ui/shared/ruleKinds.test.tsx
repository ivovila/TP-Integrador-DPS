// @vitest-environment jsdom
import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import { afterEach, expect, test, vi } from 'vitest';
import type { ApprovalRule, RawAnswer } from '../../domain/schema';
import { describeRule, emptyRuleOfType, formatAnswer, RuleAnswerInput, RuleConfigFields, ruleKinds } from './ruleKinds';

afterEach(cleanup);

const pressureRule: ApprovalRule = { type: 'numeric-range', unit: 'bar', minimum: '10', maximum: '16' };
const sealRule: ApprovalRule = { type: 'yes-no', expected: 'YES' };
const hoseRule: ApprovalRule = { type: 'option-in-list', acceptedOptions: ['Good', 'Acceptable'] };

const answerControls: [string, ApprovalRule, string, string, RawAnswer][] = [
  ['a measurement is typed with its unit', pressureRule, 'Measurement (bar)', '12.5', { kind: 'number', value: '12.5' }],
  ['a yes or no question is chosen from a list', sealRule, 'Answer', 'no', { kind: 'yes-no', value: false }],
  ['an option is typed or picked from the accepted ones', hoseRule, 'Observed option', 'Cracked', { kind: 'option', value: 'Cracked' }],
];

test.each(answerControls)('%s', (_description, rule, controlLabel, typedValue, expectedAnswer) => {
  const recordAnswer = vi.fn();
  render(<RuleAnswerInput rule={rule} answer={undefined} onChange={recordAnswer} />);
  fireEvent.change(screen.getByLabelText(controlLabel), { target: { value: typedValue } });
  expect(recordAnswer).toHaveBeenCalledWith(expectedAnswer);
});

test('each rule kind is described and formats its answers in its own words', () => {
  expect(describeRule(pressureRule)).toBe('Between 10 and 16 bar');
  expect(describeRule(sealRule)).toBe('Expected answer: Yes');
  expect(describeRule(hoseRule)).toBe('Accepted: Good, Acceptable');
  expect(formatAnswer(pressureRule, { kind: 'number', value: '12.5' })).toBe('12.5 bar');
  expect(formatAnswer(sealRule, { kind: 'yes-no', value: false })).toBe('No');
  expect(formatAnswer(hoseRule, { kind: 'option', value: 'Good' })).toBe('Good');
});

test('the schema editor asks for the configuration of the chosen rule kind', () => {
  const changeRule = vi.fn();
  render(<RuleConfigFields rule={pressureRule} onChange={changeRule} />);
  fireEvent.change(screen.getByLabelText('Maximum'), { target: { value: '18' } });
  expect(changeRule).toHaveBeenCalledWith({ ...pressureRule, maximum: '18' });
});

test('every registered rule kind can start an empty rule of its own type', () => {
  expect(ruleKinds.map((kind) => emptyRuleOfType(kind.type).type)).toEqual(ruleKinds.map((kind) => kind.type));
});
