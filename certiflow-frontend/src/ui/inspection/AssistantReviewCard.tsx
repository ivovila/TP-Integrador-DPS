import { useState, type FormEvent } from 'react';
import type { AssistantReview, RejectedProposal, Suggestion } from '../../domain/assistance';
import type { InspectionDetails } from '../../domain/inspection';
import { useCertiflow } from '../app/CertiflowContext';
import { useSession } from '../app/SessionContext';
import { Badge, StatusBadge } from '../shared/Badge';
import { ErrorBanner } from '../shared/ErrorBanner';
import { formatInstant } from '../shared/format';
import { Card, EmptyState } from '../shared/Page';
import { QueryView } from '../shared/QueryView';
import { useCommand } from '../shared/useCommand';
import { useQuery } from '../shared/useQuery';
import { criterionAnchor } from './CriterionResponseCard';

const guardrailExplanation: Record<string, string> = {
  OUT_OF_SCOPE: 'the criterion is not part of this inspection',
  ALREADY_NONCONFORMING: 'the rule already flags this criterion, so closing raises the finding anyway',
  UNSUPPORTED_QUOTE: 'the quote does not appear in what the inspector wrote',
};

function criterionTextOf(inspection: InspectionDetails, criterionCode: string): string {
  return inspection.responses.find((response) => response.criterion.code === criterionCode)?.criterion.text ?? '';
}

function Quotes({ quotes }: { quotes: string[] }) {
  return (
    <>
      {quotes.map((quote) => (
        <blockquote className="quote" key={quote}>
          {quote}
        </blockquote>
      ))}
    </>
  );
}

function DismissalForm({ onDismiss, running }: { onDismiss(reason: string): void; running: boolean }) {
  const [reason, setReason] = useState('');

  function dismiss(event: FormEvent): void {
    event.preventDefault();
    onDismiss(reason);
  }

  return (
    <form onSubmit={dismiss} className="row">
      <label>
        Reason to dismiss
        <input value={reason} onChange={(event) => setReason(event.target.value)} />
      </label>
      <button disabled={running || reason.trim() === ''}>Dismiss</button>
    </form>
  );
}

function Decision({ suggestion }: { suggestion: Suggestion }) {
  const decision = suggestion.decision;
  if (decision.status === 'PENDING') {
    return <StatusBadge status="PENDING" />;
  }
  return (
    <p>
      <StatusBadge status={decision.status} />{' '}
      <span className="muted">
        decided by {decision.decidedBy.name}, {formatInstant(decision.decidedAt)}
      </span>
      {decision.status === 'DISMISSED' && <> · Reason: {decision.reason}</>}
      {decision.status === 'ACCEPTED' && (
        <>
          {' '}
          · <a href={`#${criterionAnchor(suggestion.criterionCode)}`}>Correct the answer</a>
        </>
      )}
    </p>
  );
}

function SuggestionView({
  review,
  suggestion,
  inspection,
  onDecided,
}: {
  review: AssistantReview;
  suggestion: Suggestion;
  inspection: InspectionDetails;
  onDecided(): void;
}) {
  const certiflow = useCertiflow();
  const { actingPerson, permitted } = useSession();
  const deciding = useCommand();
  const actions = permitted(suggestion.actions);

  function accept(): void {
    deciding.run(
      () => certiflow.assistance.acceptSuggestion({ reviewId: review.id, position: suggestion.position, acceptedBy: actingPerson }),
      () => {
        onDecided();
        document.getElementById(criterionAnchor(suggestion.criterionCode))?.scrollIntoView?.({ behavior: 'smooth' });
      },
    );
  }

  function dismiss(reason: string): void {
    deciding.run(
      () =>
        certiflow.assistance.dismissSuggestion({ reviewId: review.id, position: suggestion.position, reason, dismissedBy: actingPerson }),
      onDecided,
    );
  }

  return (
    <div className="criterion suggestion">
      <div className="criterion-heading">
        <div>
          <Badge tone="warning">Suggested by AI</Badge> <span className="criterion-code">{suggestion.criterionCode}</span>{' '}
          <strong>{criterionTextOf(inspection, suggestion.criterionCode)}</strong>
          <p className="muted">
            Criterion severity: {suggestion.criterionSeverity.label} · Estimated by the assistant: {suggestion.estimatedSeverity.label}
          </p>
        </div>
        <Decision suggestion={suggestion} />
      </div>
      <p>{suggestion.rationale}</p>
      <Quotes quotes={suggestion.quotes} />
      <ErrorBanner error={deciding.error} />
      {actions.includes('acceptSuggestion') && (
        <div className="row">
          <button className="primary" onClick={accept} disabled={deciding.running}>
            Accept
          </button>
          <span className="muted">Accepting records your decision; correct the answer of the criterion yourself.</span>
        </div>
      )}
      {actions.includes('dismissSuggestion') && <DismissalForm onDismiss={dismiss} running={deciding.running} />}
    </div>
  );
}

function RejectedProposals({ rejected }: { rejected: RejectedProposal[] }) {
  if (rejected.length === 0) {
    return null;
  }
  return (
    <details className="more">
      <summary>Stopped by the guardrails ({rejected.length})</summary>
      <ul className="plain-list">
        {rejected.map((proposal, index) => (
          <li key={index}>
            <span className="criterion-code">{proposal.criterionCode}</span> {proposal.rationale}{' '}
            <span className="muted">— not shown because {guardrailExplanation[proposal.reason] ?? proposal.reason}.</span>
          </li>
        ))}
      </ul>
    </details>
  );
}

function ReviewView({ review, inspection, onDecided }: { review: AssistantReview; inspection: InspectionDetails; onDecided(): void }) {
  return (
    <div className="assistant-review">
      <p className="muted">
        Requested by {review.requestedBy.name}, {formatInstant(review.requestedAt)} · prompt {review.promptVersion} · model{' '}
        {review.modelId}
      </p>
      {review.suggestions.length === 0 ? (
        <EmptyState>The assistant found nothing that the answers do not already reflect.</EmptyState>
      ) : (
        review.suggestions.map((suggestion) => (
          <SuggestionView key={suggestion.position} review={review} suggestion={suggestion} inspection={inspection} onDecided={onDecided} />
        ))
      )}
      <RejectedProposals rejected={review.rejectedProposals} />
    </div>
  );
}

/**
 * La revisión asistida antes del cierre. Lo que sugiere el asistente se marca como tal y nunca cambia la inspección:
 * aceptar o descartar solo deja la decisión del inspector, que corrige la respuesta por el flujo habitual.
 */
export function AssistantReviewCard({ inspection }: { inspection: InspectionDetails }) {
  const certiflow = useCertiflow();
  const { actingPerson, permitted } = useSession();
  const requesting = useCommand();
  const [changeCount, setChangeCount] = useState(0);
  const reviews = useQuery(() => certiflow.assistance.listReviews(inspection.id), [inspection.id, changeCount]);
  const recordChange = () => setChangeCount((count) => count + 1);

  function request(): void {
    requesting.run(() => certiflow.assistance.requestReview({ inspectionId: inspection.id, requestedBy: actingPerson }), recordChange);
  }

  return (
    <Card
      title={
        <>
          Assisted review <Badge tone="neutral">AI</Badge>
        </>
      }
    >
      <p className="muted">
        The assistant reads the observations and evidence descriptions and suggests non-conformities that the answers do not reflect. It
        never changes the inspection: you decide on each suggestion.
      </p>
      {permitted(inspection.actions).includes('requestAssistantReview') && (
        <button onClick={request} disabled={requesting.running}>
          {requesting.running ? 'Reviewing…' : 'Review with AI'}
        </button>
      )}
      <ErrorBanner error={requesting.error} />
      <QueryView query={reviews}>
        {(found) => {
          const [latest, ...earlier] = [...found].reverse();
          if (latest === undefined) {
            return <EmptyState>No review was requested for this inspection yet.</EmptyState>;
          }
          return (
            <>
              <ReviewView review={latest} inspection={inspection} onDecided={recordChange} />
              {earlier.length > 0 && (
                <details className="more">
                  <summary>Earlier reviews ({earlier.length})</summary>
                  {earlier.map((review) => (
                    <ReviewView key={review.id} review={review} inspection={inspection} onDecided={recordChange} />
                  ))}
                </details>
              )}
            </>
          );
        }}
      </QueryView>
    </Card>
  );
}
