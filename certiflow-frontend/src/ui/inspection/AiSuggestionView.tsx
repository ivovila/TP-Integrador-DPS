import { useState, type FormEvent } from 'react';
import type { AssistantReview, Suggestion } from '../../domain/assistance';
import type { CriterionResponse, InspectionDetails } from '../../domain/inspection';
import { useCertiflow } from '../app/CertiflowContext';
import { useSession } from '../app/SessionContext';
import { Badge, StatusBadge, type Tone } from '../shared/Badge';
import { ErrorBanner } from '../shared/ErrorBanner';
import { formatInstant } from '../shared/format';
import { TextField } from '../shared/Page';
import { formatAnswer } from '../shared/ruleKinds';
import { useCommand } from '../shared/useCommand';
import { criterionAnchor } from './anchors';

const stateOfDecision: Record<Suggestion['decision']['status'], { label: string; tone: Tone }> = {
  PENDING: { label: 'Needs your decision', tone: 'warning' },
  ACCEPTED: { label: 'Confirmed', tone: 'positive' },
  DISMISSED: { label: 'Not an issue', tone: 'neutral' },
};

function normalized(text: string): string {
  return text.toLowerCase().replace(/\s+/g, ' ').trim();
}

/** De dónde sale una cita: el inspector tiene que reconocer su propio texto, y saber si lo escribió en otro criterio. */
function sourceOf(quote: string, criterionCode: string, inspection: InspectionDetails): string {
  const wanted = normalized(quote);
  for (const response of inspection.responses) {
    const where = response.criterion.code === criterionCode ? '' : ` on ${response.criterion.code}`;
    if (response.observations.some((observation) => normalized(observation.text).includes(wanted))) {
      return `Your observation${where}`;
    }
    const attached = response.attachments.find((attachment) => normalized(attachment.evidence.description).includes(wanted));
    if (attached !== undefined) {
      return `${attached.evidence.kind.label} description${where}`;
    }
  }
  return 'Your notes';
}

function NextStep({ response }: { response: CriterionResponse | undefined }) {
  if (response === undefined) {
    return null;
  }
  const code = response.criterion.code;
  if (response.outcome === 'OBSERVED' || response.outcome === 'REJECTED') {
    return (
      <p className="callout tone-positive">
        Done: {code} is now <strong>{response.outcome.toLowerCase()}</strong>. Closing the inspection will raise the finding.
      </p>
    );
  }
  return (
    <p className="callout tone-warning">
      Next step: change the answer of {code}. Accepting does not change it for you; the rule raises the finding when you
      close the inspection. <a href={`#${criterionAnchor(code)}`}>Go to {code}</a>
    </p>
  );
}

function DismissalForm({ onDismiss, onCancel, running }: { onDismiss(reason: string): void; onCancel(): void; running: boolean }) {
  const [reason, setReason] = useState('');

  function dismiss(event: FormEvent): void {
    event.preventDefault();
    onDismiss(reason);
  }

  return (
    <form onSubmit={dismiss} className="row">
      <TextField label="Why is it not an issue?" value={reason} onChange={setReason} placeholder="It is kept with your decision" />
      <button disabled={running || reason.trim() === ''}>Dismiss suggestion</button>
      <button type="button" className="quiet" onClick={onCancel}>
        Cancel
      </button>
    </form>
  );
}

export function AiSuggestionView({
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
  const [dismissing, setDismissing] = useState(false);
  const actions = permitted(suggestion.actions);
  const response = inspection.responses.find((candidate) => candidate.criterion.code === suggestion.criterionCode);
  const state = stateOfDecision[suggestion.decision.status];
  const decision = suggestion.decision;

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
    <article className={`ai-suggestion state-${decision.status.toLowerCase()}`}>
      <header className="ai-suggestion-heading">
        <div>
          <Badge tone="warning">AI suggestion</Badge>{' '}
          <strong>
            Possible non-conformity in <span className="criterion-code">{suggestion.criterionCode}</span> ·{' '}
            {response?.criterion.text ?? suggestion.criterionCode}
          </strong>
        </div>
        <Badge tone={state.tone}>{state.label}</Badge>
      </header>
      <dl className="ai-comparison">
        <div>
          <dt>Your answer</dt>
          <dd>
            {response?.given === undefined ? 'Not answered' : <strong>{formatAnswer(response.criterion.rule, response.given.value)}</strong>}
            {response !== undefined && (
              <>
                {' '}
                <span className="muted">· the rule says</span> <StatusBadge status={response.outcome} />
              </>
            )}
          </dd>
        </div>
        <div>
          <dt>What you wrote</dt>
          <dd>
            {suggestion.quotes.map((quote) => (
              <blockquote className="quote" key={quote}>
                “{quote}”<span className="quote-source">{sourceOf(quote, suggestion.criterionCode, inspection)}</span>
              </blockquote>
            ))}
          </dd>
        </div>
        <div>
          <dt>Why it may not comply</dt>
          <dd>{suggestion.rationale}</dd>
        </div>
        <div>
          <dt>Severity if confirmed</dt>
          <dd>
            {suggestion.criterionSeverity.label} <span className="muted">(set by the criterion)</span>
            {suggestion.estimatedSeverity.name !== suggestion.criterionSeverity.name && (
              <span className="muted"> · the assistant would rate it {suggestion.estimatedSeverity.label.toLowerCase()}</span>
            )}
          </dd>
        </div>
      </dl>
      <ErrorBanner error={deciding.error} />
      {decision.status === 'PENDING' && (actions.includes('acceptSuggestion') || actions.includes('dismissSuggestion')) && (
        <div className="ai-actions">
          {dismissing ? (
            <DismissalForm onDismiss={dismiss} onCancel={() => setDismissing(false)} running={deciding.running} />
          ) : (
            <div className="row">
              {actions.includes('acceptSuggestion') && (
                <button className="primary" onClick={accept} disabled={deciding.running}>
                  Confirm issue
                </button>
              )}
              {actions.includes('dismissSuggestion') && (
                <button onClick={() => setDismissing(true)} disabled={deciding.running}>
                  Not an issue
                </button>
              )}
            </div>
          )}
        </div>
      )}
      {decision.status !== 'PENDING' && (
        <p className="muted decision-line">
          {state.label} · {decision.decidedBy.name}, {formatInstant(decision.decidedAt)}
          {decision.status === 'DISMISSED' && <> · Reason: {decision.reason}</>}
        </p>
      )}
      {decision.status === 'ACCEPTED' && <NextStep response={response} />}
    </article>
  );
}
