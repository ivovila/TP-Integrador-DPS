import type { Criterion, Section } from '../../domain/schema';
import { describeRule } from '../shared/ruleKinds';

export function describeRequiredEvidence(criterion: Criterion): string {
  return criterion.requiredEvidence.length === 0
    ? 'None'
    : criterion.requiredEvidence.map((requirement) => `${requirement.minimum} × ${requirement.kind.label}`).join(', ');
}

export function SectionsView({ sections }: { sections: Section[] }) {
  return (
    <>
      {sections.map((section) => (
        <section className="card" key={section.title}>
          <h2>{section.title}</h2>
          <table className="data">
            <thead>
              <tr>
                <th>Code</th>
                <th>Criterion</th>
                <th>Approval rule</th>
                <th>Severity</th>
                <th>Required evidence</th>
              </tr>
            </thead>
            <tbody>
              {section.criteria.map((criterion) => (
                <tr key={criterion.code}>
                  <td className="criterion-code">{criterion.code}</td>
                  <td>{criterion.text}</td>
                  <td>{describeRule(criterion.rule)}</td>
                  <td>{criterion.severity.label}</td>
                  <td>{describeRequiredEvidence(criterion)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </section>
      ))}
    </>
  );
}
