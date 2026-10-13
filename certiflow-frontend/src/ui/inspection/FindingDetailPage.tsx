import { useState, type FormEvent } from 'react';
import { Link, useParams } from 'react-router';
import type { CorrectiveAction, VerificationResult } from '../../domain/finding';
import { useCertiflow } from '../app/CertiflowContext';
import { useSession } from '../app/SessionContext';
import { AuditHistory } from '../shared/AuditHistory';
import { Badge, StatusBadge } from '../shared/Badge';
import { ErrorBanner } from '../shared/ErrorBanner';
import { formatDate, formatInstant, formatPerson } from '../shared/format';
import { Card, Details, EmptyState, PageHeader, TextField } from '../shared/Page';
import { nobody, PersonPicker } from '../shared/PersonPicker';
import { QueryView } from '../shared/QueryView';
import { describeRule } from '../shared/ruleKinds';
import { useCommand } from '../shared/useCommand';
import { useQuery } from '../shared/useQuery';

function PlanCorrectiveActionForm({ findingId, onPlanned }: { findingId: string; onPlanned(): void }) {
  const certiflow = useCertiflow();
  const { actingPerson } = useSession();
  const planning = useCommand();
  const [description, setDescription] = useState('');
  const [responsible, setResponsible] = useState(nobody);
  const [dueDate, setDueDate] = useState('');

  function plan(event: FormEvent): void {
    event.preventDefault();
    planning.run(
      () => certiflow.findings.planCorrectiveAction({ findingId, description, responsible, dueDate, plannedBy: actingPerson }),
      () => {
        setDescription('');
        setDueDate('');
        onPlanned();
      },
    );
  }

  return (
    <form className="stacked" onSubmit={plan}>
      <ErrorBanner error={planning.error} />
      <div className="form-grid">
        <TextField label="What will be done" value={description} onChange={setDescription} />
        <PersonPicker label="Responsible" person={responsible} onChange={setResponsible} />
        <TextField label="Due date" type="date" value={dueDate} onChange={setDueDate} />
      </div>
      <div>
        <button className="primary" disabled={planning.running}>
          Plan corrective action
        </button>
      </div>
    </form>
  );
}

function VerificationForm({ findingId, actionId, onVerified }: { findingId: string; actionId: number; onVerified(): void }) {
  const certiflow = useCertiflow();
  const { actingPerson } = useSession();
  const verification = useCommand();
  const [notes, setNotes] = useState('');

  function verify(result: VerificationResult): void {
    verification.run(
      () => certiflow.findings.verifyCorrectiveAction({ findingId, actionId, verifier: actingPerson, result, notes }),
      () => {
        setNotes('');
        onVerified();
      },
    );
  }

  return (
    <div>
      <ErrorBanner error={verification.error} />
      <div className="row">
        <TextField label={`Verification notes (verifying as ${actingPerson.name})`} value={notes} onChange={setNotes} />
        <button className="primary" disabled={verification.running} onClick={() => verify('ACCEPTED')}>
          Accept
        </button>
        <button disabled={verification.running} onClick={() => verify('REJECTED')}>
          Reject
        </button>
      </div>
    </div>
  );
}

function CorrectiveActionCard({ findingId, action, onVerified }: { findingId: string; action: CorrectiveAction; onVerified(): void }) {
  return (
    <div className="nested">
      <div className="criterion-heading">
        <strong>
          Action {action.id} · {action.description}
        </strong>
        <span>
          {action.overdue && <Badge tone="negative">overdue</Badge>} <StatusBadge status={action.state} />
        </span>
      </div>
      <p className="muted">
        Responsible: {formatPerson(action.responsible)} · Due {formatDate(action.dueDate)} · Planned by {action.plannedBy.name},{' '}
        {formatInstant(action.plannedAt)}
      </p>
      <ul className="plain-list">
        {action.verifications.map((verification) => (
          <li key={verification.verifiedAt}>
            <StatusBadge status={verification.result} /> by {verification.verifier.name}, {formatInstant(verification.verifiedAt)}
            {verification.notes !== '' && ` · ${verification.notes}`}
          </li>
        ))}
      </ul>
      {action.actions.includes('verifyCorrectiveAction') && (
        <VerificationForm findingId={findingId} actionId={action.id} onVerified={onVerified} />
      )}
    </div>
  );
}

export function FindingDetailPage() {
  const certiflow = useCertiflow();
  const findingId = useParams().id!;
  const [changeCount, setChangeCount] = useState(0);
  const finding = useQuery(() => certiflow.findings.findFinding(findingId), [findingId, changeCount]);
  const recordChange = () => setChangeCount((count) => count + 1);

  return (
    <QueryView query={finding}>
      {(foundFinding) => (
        <>
          <PageHeader
            title={
              <>
                Finding {foundFinding.id} <StatusBadge status={foundFinding.open ? 'OPEN' : 'CLOSED'} />
              </>
            }
            subtitle={
              <>
                Raised by inspection <Link to={`/inspections/${foundFinding.inspectionId}`}>{foundFinding.inspectionId}</Link> on asset{' '}
                <Link to={`/assets/${encodeURIComponent(foundFinding.assetCode)}`}>{foundFinding.assetCode}</Link>
              </>
            }
          />
          <Card>
            <Details
              entries={[
                ['Criterion', `${foundFinding.criterion.code} · ${foundFinding.criterion.text}`],
                ['Approval rule', describeRule(foundFinding.criterion.rule)],
                ['Severity', foundFinding.severity.label],
                ['Subsystem', foundFinding.subsystemCode ?? 'None: affects every subsystem of the asset'],
                ['Responsible', formatPerson(foundFinding.responsible)],
              ]}
            />
          </Card>
          <Card title="Evidence">
            {foundFinding.evidence.length === 0 ? (
              <EmptyState>No evidence was attached to this criterion.</EmptyState>
            ) : (
              <ul className="plain-list">
                {foundFinding.evidence.map((attached) => (
                  <li key={attached.evidence.reference}>
                    {attached.evidence.kind.label}: {attached.evidence.reference}
                    {attached.evidence.description !== '' && ` (${attached.evidence.description})`}{' '}
                    <span className="muted">
                      by {attached.attachedBy.name}, {formatInstant(attached.attachedAt)}
                    </span>
                  </li>
                ))}
              </ul>
            )}
          </Card>
          <Card title="Corrective actions">
            {foundFinding.correctiveActions.length === 0 && <EmptyState>No corrective actions planned yet.</EmptyState>}
            {foundFinding.correctiveActions.map((action) => (
              <CorrectiveActionCard key={action.id} findingId={foundFinding.id} action={action} onVerified={recordChange} />
            ))}
            {foundFinding.actions.includes('planCorrectiveAction') && (
              <PlanCorrectiveActionForm findingId={foundFinding.id} onPlanned={recordChange} />
            )}
          </Card>
          <AuditHistory subject={{ kind: 'finding', id: foundFinding.id }} refreshKey={changeCount} />
        </>
      )}
    </QueryView>
  );
}
