import type { Criterion, EvidenceRequirement, Section, Vocabulary } from '../../domain/schema';
import { TextField } from '../shared/Page';
import { emptyRuleOfType, RuleConfigFields, ruleKinds } from '../shared/ruleKinds';

function replaceAt<Item>(items: Item[], position: number, replacement: Item): Item[] {
  return items.map((existing, index) => (index === position ? replacement : existing));
}

function removeAt<Item>(items: Item[], position: number): Item[] {
  return items.filter((_, index) => index !== position);
}

function CriterionEditor({
  criterion,
  vocabulary,
  onChange,
  onRemove,
}: {
  criterion: Criterion;
  vocabulary: Vocabulary;
  onChange(criterion: Criterion): void;
  onRemove(): void;
}) {
  function changeRequirement(position: number, requirement: EvidenceRequirement): void {
    onChange({ ...criterion, requiredEvidence: replaceAt(criterion.requiredEvidence, position, requirement) });
  }

  return (
    <div className="nested">
      <div className="row">
        <TextField label="Code" value={criterion.code} onChange={(code) => onChange({ ...criterion, code })} />
        <TextField label="Text" value={criterion.text} onChange={(text) => onChange({ ...criterion, text })} />
        <div className="field">
          <label>
            Severity
            <select
              value={criterion.severity.name}
              onChange={(event) =>
                onChange({
                  ...criterion,
                  severity: vocabulary.severities.find((severity) => severity.name === event.target.value)!,
                })
              }
            >
              {vocabulary.severities.map((severity) => (
                <option key={severity.name} value={severity.name}>
                  {severity.label}
                </option>
              ))}
            </select>
          </label>
        </div>
        <button type="button" className="quiet" onClick={onRemove}>
          Remove criterion
        </button>
      </div>
      <div className="row">
        <div className="field">
          <label>
            Approval rule
            <select value={criterion.rule.type} onChange={(event) => onChange({ ...criterion, rule: emptyRuleOfType(event.target.value) })}>
              {ruleKinds.map((kind) => (
                <option key={kind.type} value={kind.type}>
                  {kind.label}
                </option>
              ))}
            </select>
          </label>
        </div>
        <RuleConfigFields key={criterion.rule.type} rule={criterion.rule} onChange={(rule) => onChange({ ...criterion, rule })} />
      </div>
      {criterion.requiredEvidence.map((requirement, position) => (
        <div className="row" key={position}>
          <div className="field">
            <label>
              Required evidence
              <select
                value={requirement.kind.name}
                onChange={(event) =>
                  changeRequirement(position, {
                    ...requirement,
                    kind: vocabulary.evidenceKinds.find((kind) => kind.name === event.target.value)!,
                  })
                }
              >
                {vocabulary.evidenceKinds.map((kind) => (
                  <option key={kind.name} value={kind.name}>
                    {kind.label}
                  </option>
                ))}
              </select>
            </label>
          </div>
          <TextField
            label="Minimum items"
            type="number"
            value={String(requirement.minimum)}
            onChange={(minimum) => changeRequirement(position, { ...requirement, minimum: Number(minimum) })}
          />
          <button
            type="button"
            className="quiet"
            onClick={() => onChange({ ...criterion, requiredEvidence: removeAt(criterion.requiredEvidence, position) })}
          >
            Remove
          </button>
        </div>
      ))}
      <button
        type="button"
        onClick={() =>
          onChange({
            ...criterion,
            requiredEvidence: [...criterion.requiredEvidence, { kind: vocabulary.evidenceKinds[0]!, minimum: 1 }],
          })
        }
      >
        Require evidence
      </button>
    </div>
  );
}

export function SectionsEditor({
  sections,
  vocabulary,
  onChange,
}: {
  sections: Section[];
  vocabulary: Vocabulary;
  onChange(sections: Section[]): void;
}) {
  function emptyCriterion(): Criterion {
    return { code: '', text: '', severity: vocabulary.severities[0]!, rule: ruleKinds[0]!.emptyRule(), requiredEvidence: [] };
  }

  return (
    <div className="stacked">
      {sections.map((section, sectionPosition) => {
        const changeSection = (changed: Section) => onChange(replaceAt(sections, sectionPosition, changed));
        return (
          <section className="card" key={sectionPosition}>
            <div className="row">
              <TextField label="Section title" value={section.title} onChange={(title) => changeSection({ ...section, title })} />
              <button type="button" className="quiet" onClick={() => onChange(removeAt(sections, sectionPosition))}>
                Remove section
              </button>
            </div>
            {section.criteria.map((criterion, criterionPosition) => (
              <CriterionEditor
                key={criterionPosition}
                criterion={criterion}
                vocabulary={vocabulary}
                onChange={(changed) =>
                  changeSection({ ...section, criteria: replaceAt(section.criteria, criterionPosition, changed) })
                }
                onRemove={() => changeSection({ ...section, criteria: removeAt(section.criteria, criterionPosition) })}
              />
            ))}
            <button type="button" onClick={() => changeSection({ ...section, criteria: [...section.criteria, emptyCriterion()] })}>
              Add criterion
            </button>
          </section>
        );
      })}
      <div>
        <button type="button" onClick={() => onChange([...sections, { title: '', criteria: [emptyCriterion()] }])}>
          Add section
        </button>
      </div>
    </div>
  );
}
