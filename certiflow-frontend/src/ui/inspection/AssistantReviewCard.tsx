import type { AssistantReview, RejectedProposal } from '../../domain/assistance';
import type { InspectionDetails } from '../../domain/inspection';
import { useCertiflow } from '../app/CertiflowContext';
import { useSession } from '../app/SessionContext';
import { Badge } from '../shared/Badge';
import { ErrorBanner } from '../shared/ErrorBanner';
import { formatInstant } from '../shared/format';
import { QueryView } from '../shared/QueryView';
import { useCommand } from '../shared/useCommand';
import { AiSuggestionView } from './AiSuggestionView';
import { assistedReviewAnchor } from './anchors';
import { type AssistantReviews, pendingSuggestions } from './useAssistantReviews';

const discardedBecause: Record<string, string> = {
  DUPLICATE_CRITERION: 'a suggestion for that criterion was already admitted',
  OUT_OF_SCOPE: 'that criterion is not part of this inspection',
  ALREADY_NONCONFORMING: 'the rule already flags that criterion, so closing raises its finding anyway',
  UNSUPPORTED_QUOTE: 'the quoted text does not appear in what you wrote',
};

function plural(count: number, one: string, many: string): string {
  return `${count} ${count === 1 ? one : many}`;
}

function Summary({ review }: { review: AssistantReview }) {
  const pending = pendingSuggestions(review).length;
  if (review.suggestions.length === 0) {
    return <p className="callout tone-positive">The assistant found no answer that contradicts what you wrote.</p>;
  }
  return (
    <p className={`callout ${pending === 0 ? 'tone-neutral' : 'tone-warning'}`}>
      The assistant found {plural(review.suggestions.length, 'answer', 'answers')} that may contradict what you wrote.{' '}
      {pending === 0 ? 'You decided on all of them.' : `${plural(pending, 'needs', 'need')} your decision.`}
    </p>
  );
}

function DiscardedProposals({ discarded }: { discarded: RejectedProposal[] }) {
  if (discarded.length === 0) {
    return null;
  }
  return (
    <details className="more">
      <summary>{plural(discarded.length, 'more point was', 'more points were')} discarded by automatic checks</summary>
      <ul className="plain-list">
        {discarded.map((proposal, index) => (
          <li key={index}>
            <span className="criterion-code">{proposal.criterionCode}</span> {proposal.rationale}{' '}
            <span className="muted">— not shown because {discardedBecause[proposal.reason] ?? proposal.reason}.</span>
          </li>
        ))}
      </ul>
    </details>
  );
}

function ReviewView({ review, inspection, onDecided }: { review: AssistantReview; inspection: InspectionDetails; onDecided(): void }) {
  return (
    <div className="assistant-review">
      <p className="muted">This review refers to the answers and notes captured when it was requested. Check the current inspection before deciding.</p>
      <Summary review={review} />
      {review.suggestions.map((suggestion) => (
        <AiSuggestionView key={suggestion.position} review={review} suggestion={suggestion} inspection={inspection} onDecided={onDecided} />
      ))}
      <DiscardedProposals discarded={review.rejectedProposals} />
      <p className="muted provenance">
        Requested by {review.requestedBy.name}, {formatInstant(review.requestedAt)} · model {review.modelId} · prompt{' '}
        {review.promptVersion}. The assistant can be wrong: check each point against the asset.
      </p>
    </div>
  );
}

/**
 * La revisión asistida antes del cierre. Cada sugerencia se muestra como una comparación entre la respuesta y lo que
 * escribió el inspector, marcada como de la IA. Nunca cambia la inspección: aceptar o descartar solo deja la decisión,
 * y el inspector corrige la respuesta por el flujo habitual.
 */
export function AssistantReviewCard({ inspection, reviews }: { inspection: InspectionDetails; reviews: AssistantReviews }) {
  const certiflow = useCertiflow();
  const { actingPerson, permitted } = useSession();
  const requesting = useCommand();
  const mayRequest = permitted(inspection.actions).includes('requestAssistantReview');

  function request(): void {
    requesting.run(
      () => certiflow.assistance.requestReview({ inspectionId: inspection.id, requestedBy: actingPerson }),
      reviews.recordChange,
    );
  }

  return (
    <section className="card ai-review" id={assistedReviewAnchor}>
      <div className="ai-review-heading">
        <h2>
          AI review before closing <Badge tone="neutral">AI</Badge>
        </h2>
        {mayRequest && (
          <button className={reviews.latest === undefined ? 'primary' : ''} onClick={request} disabled={requesting.running}>
            {reviews.latest === undefined ? 'Review with AI' : 'Review again'}
          </button>
        )}
      </div>
      {reviews.latest === undefined && (
        <ol className="ai-steps muted">
          <li>The assistant reads your observations and evidence descriptions.</li>
          <li>It points out answers that may contradict what you wrote. It never changes the inspection.</li>
          <li>If you agree, you correct the answer; the rule raises the finding when you close.</li>
        </ol>
      )}
      {requesting.running && (
        <p className="callout tone-neutral" role="status">
          The assistant is reading the inspection. This may take several minutes; keep this page open.
        </p>
      )}
      <ErrorBanner error={requesting.error} />
      <QueryView query={reviews.query}>
        {(found) => {
          const [latest, ...earlier] = [...found].reverse();
          if (latest === undefined) {
            return mayRequest ? null : <p className="muted">No AI review was requested for this inspection.</p>;
          }
          return (
            <>
              <ReviewView review={latest} inspection={inspection} onDecided={reviews.recordChange} />
              {earlier.length > 0 && (
                <details className="more">
                  <summary>Earlier reviews ({earlier.length})</summary>
                  {earlier.map((review) => (
                    <ReviewView key={review.id} review={review} inspection={inspection} onDecided={reviews.recordChange} />
                  ))}
                </details>
              )}
            </>
          );
        }}
      </QueryView>
    </section>
  );
}
