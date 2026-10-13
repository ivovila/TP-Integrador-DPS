import type { CertificationDecision } from '../../domain/certification';
import { StatusBadge } from '../shared/Badge';
import { formatDate, formatInstant } from '../shared/format';
import { Details } from '../shared/Page';
import { SectionLink } from '../shared/SectionLink';

/** Resultado, motivos, condiciones y vigencia que fijó la política de la jurisdicción. */
export function DecisionDetails({ decision }: { decision: CertificationDecision }) {
  return (
    <div className="stacked">
      <Details
        entries={[
          ['Outcome', <StatusBadge status={decision.outcome} />],
          ['Policy', `${decision.policyCode} · jurisdiction ${decision.jurisdictionCode}`],
          [
            'Validity granted',
            decision.validity === undefined
              ? 'None'
              : `${formatDate(decision.validity.validFrom)} to ${formatDate(decision.validity.validUntil)}`,
          ],
          ['Evaluated at', formatInstant(decision.evaluatedAt)],
        ]}
      />
      {decision.ineligibilities.length > 0 && (
        <ul className="plain-list">
          {decision.ineligibilities.map((ineligibility, position) => (
            <li key={position}>{ineligibility.detail}</li>
          ))}
        </ul>
      )}
      {decision.conditions.length > 0 && (
        <div>
          <h3>Conditions</h3>
          <ul className="plain-list">
            {decision.conditions.map((condition) => (
              <li key={condition.findingId}>
                <SectionLink to={`/findings/${condition.findingId}`}>{condition.findingId}</SectionLink> ·{' '}
                <span className="criterion-code">{condition.criterionCode}</span> {condition.description}{' '}
                <span className="muted">by {formatDate(condition.dueDate)}</span>
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
}
