// @vitest-environment jsdom
import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router';
import { afterEach, expect, test } from 'vitest';
import type { AssistantReview, Suggestion, SuggestionDecision } from '../../domain/assistance';
import type { CriterionResponse, InspectionAction, InspectionDetails } from '../../domain/inspection';
import type { Severity } from '../../domain/schema';
import type { Person } from '../../domain/shared';
import type { AcceptSuggestionCommand, DismissSuggestionCommand, RequestAssistantReviewCommand } from '../../ports/AssistancePort';
import type { Certiflow } from '../../ports/Certiflow';
import { ApplicationProvider } from '../app/CertiflowContext';
import type { Role } from '../app/roles';
import { SessionProvider } from '../app/SessionContext';
import { fakeAuthentication, signedInAs } from '../app/testing';
import { AssistantReviewCard } from './AssistantReviewCard';
import { CriterionResponseCard } from './CriterionResponseCard';
import { suggestionFor, useAssistantReviews } from './useAssistantReviews';

afterEach(cleanup);

const major: Severity = { name: 'MAJOR', label: 'Major', outcomeWhenUnmet: 'REJECTED' };
const minor: Severity = { name: 'MINOR', label: 'Minor', outcomeWhenUnmet: 'OBSERVED' };
const inspector: Person = signedInAs('inspector').person;

function hose(outcome: CriterionResponse['outcome'] = 'APPROVED'): CriterionResponse {
  return {
    sectionTitle: 'Physical condition',
    criterion: {
      code: 'HOSE',
      text: 'Hose condition',
      severity: minor,
      rule: { type: 'option-in-list', acceptedOptions: ['Good'] },
      requiredEvidence: [],
    },
    given: { value: { kind: 'option', value: outcome === 'APPROVED' ? 'Good' : 'Cracked' }, answeredBy: inspector, answeredAt: '2026-11-08T11:00:00Z' },
    attachments: [],
    observations: [{ text: 'La manguera tiene una grieta cerca del acople', observedBy: inspector, observedAt: '2026-11-08T11:01:00Z' }],
    outcome,
  };
}

function inspectionWith(response: CriterionResponse, actions: InspectionAction[]): InspectionDetails {
  return {
    id: 'INS-1',
    assetCode: 'EXT-0042',
    assignment: { inspector, plannedDate: '2026-03-02', scope: { kind: 'full' } },
    schemaVersion: 1,
    closed: false,
    coversWholeSchema: true,
    responses: [response],
    revisions: [],
    actions,
  };
}

function suggestion(decision: SuggestionDecision): Suggestion {
  return {
    position: 1,
    criterionCode: 'HOSE',
    criterionSeverity: minor,
    estimatedSeverity: major,
    rationale: 'La manguera tiene una grieta aunque se respondió en buen estado.',
    quotes: ['La manguera tiene una grieta cerca del acople'],
    decision,
    actions: decision.status === 'PENDING' ? ['acceptSuggestion', 'dismissSuggestion'] : [],
  };
}

function review(decision: SuggestionDecision): AssistantReview {
  return {
    id: 'R-1',
    inspectionId: 'INS-1',
    promptVersion: 'v2',
    modelId: 'nvidia/nemotron',
    requestedBy: inspector,
    requestedAt: '2026-11-08T12:00:00Z',
    suggestions: [suggestion(decision)],
    rejectedProposals: [
      { criterionCode: 'SEAL', estimatedSeverity: major, rationale: 'Seal looks loose', quotes: ['suelto'], reason: 'UNSUPPORTED_QUOTE' },
    ],
  };
}

interface Recorded {
  requested: RequestAssistantReviewCommand[];
  accepted: AcceptSuggestionCommand[];
  dismissed: DismissSuggestionCommand[];
}

function certiflowWith(reviews: AssistantReview[], recorded: Recorded): Certiflow {
  const decided = { decidedBy: inspector, decidedAt: '2026-11-08T12:05:00Z' };
  return {
    assistance: {
      listReviews: async () => [...reviews],
      async requestReview(command: RequestAssistantReviewCommand) {
        recorded.requested.push(command);
        reviews.push(review({ status: 'PENDING' }));
        return reviews[reviews.length - 1];
      },
      async acceptSuggestion(command: AcceptSuggestionCommand) {
        recorded.accepted.push(command);
        reviews[0] = review({ status: 'ACCEPTED', ...decided });
      },
      async dismissSuggestion(command: DismissSuggestionCommand) {
        recorded.dismissed.push(command);
        reviews[0] = review({ status: 'DISMISSED', ...decided, reason: command.reason });
      },
    },
  } as unknown as Certiflow;
}

/** El panel y la tarjeta del criterio, que comparten las revisiones como en el detalle de la inspección. */
function InspectionWithReview({ inspection }: { inspection: InspectionDetails }) {
  const reviews = useAssistantReviews(inspection.id);
  return (
    <>
      <AssistantReviewCard inspection={inspection} reviews={reviews} />
      <CriterionResponseCard
        inspectionId={inspection.id}
        response={inspection.responses[0]!}
        actions={[]}
        evidenceKinds={[{ name: 'PHOTO', label: 'Photo' }]}
        onRecorded={() => undefined}
        aiSuggestion={suggestionFor(reviews.latest, 'HOSE')}
      />
    </>
  );
}

function show(
  reviews: AssistantReview[],
  { role = 'inspector', actions = ['closeInspection', 'requestAssistantReview'], response = hose() }: {
    role?: Role;
    actions?: InspectionAction[];
    response?: CriterionResponse;
  } = {},
): Recorded {
  const recorded: Recorded = { requested: [], accepted: [], dismissed: [] };
  render(
    <ApplicationProvider application={{ certiflow: certiflowWith(reviews, recorded), authentication: fakeAuthentication(signedInAs(role)) }}>
      <MemoryRouter>
        <SessionProvider>
          <InspectionWithReview inspection={inspectionWith(response, actions)} />
        </SessionProvider>
      </MemoryRouter>
    </ApplicationProvider>,
  );
  return recorded;
}

test('before any review the inspector sees what the assistant does and how to ask for it', async () => {
  const recorded = show([]);

  expect(screen.getByText(/It points out answers that may contradict what you wrote/)).toBeTruthy();
  fireEvent.click(screen.getByText('Review with AI'));
  expect(screen.getByRole('status').textContent).toContain('This can take up to a minute');

  expect(await screen.findByText('AI suggestion')).toBeTruthy();
  expect(recorded.requested).toEqual([{ inspectionId: 'INS-1', requestedBy: inspector }]);
  expect(screen.getByText('Review again')).toBeTruthy();
});

test('a suggestion compares the answer with what the inspector wrote, and says where the text comes from', async () => {
  show([review({ status: 'PENDING' })]);

  expect(await screen.findByText('Possible non-conformity in', { exact: false })).toBeTruthy();
  expect(screen.getByText('Your answer').nextElementSibling!.textContent).toContain('Good');
  expect(screen.getByText('“La manguera tiene una grieta cerca del acople”', { exact: false })).toBeTruthy();
  expect(screen.getByText('Your observation')).toBeTruthy();
  expect(screen.getByText('The assistant found 1 answer that may contradict what you wrote. 1 needs your decision.')).toBeTruthy();
  expect(screen.getByText(/the assistant would rate it major/)).toBeTruthy();
  expect(screen.getByText(/The assistant can be wrong/).textContent).toContain('model nvidia/nemotron · prompt v2');
});

test('the criterion it points at is flagged, with a way back to the review', async () => {
  show([review({ status: 'PENDING' })]);

  const flag = await screen.findByText(/AI suggestion: this answer may contradict what you wrote/);
  expect(flag.querySelector('a')!.getAttribute('href')).toBe('#assisted-review');
});

test('agreeing records the decision and asks to correct the answer until the rule flags the criterion', async () => {
  const recorded = show([review({ status: 'PENDING' })]);

  fireEvent.click(await screen.findByText('Confirm issue'));

  expect(await screen.findByText(/Next step: change the answer of HOSE/)).toBeTruthy();
  expect(screen.getByText('Go to HOSE').getAttribute('href')).toBe('#criterion-HOSE');
  expect(screen.getByText('You confirmed the AI suggestion: change this answer.')).toBeTruthy();
  expect(recorded.accepted).toEqual([{ reviewId: 'R-1', position: 1, acceptedBy: inspector }]);
  expect(screen.queryByText('Confirm issue')).toBeNull();
});

test('once the answer is corrected the suggestion says the finding will be raised on closing', async () => {
  show([review({ status: 'ACCEPTED', decidedBy: inspector, decidedAt: '2026-11-08T12:05:00Z' })], { response: hose('OBSERVED') });

  expect(await screen.findByText(/Closing the inspection will raise the finding/)).toBeTruthy();
  expect(screen.queryByText(/change this answer/)).toBeNull();
});

test('dismissing asks why, keeps the reason with the decision and can be cancelled', async () => {
  const recorded = show([review({ status: 'PENDING' })]);

  fireEvent.click(await screen.findByText("Not an issue"));
  fireEvent.click(screen.getByText('Cancel'));
  fireEvent.click(screen.getByText("Not an issue"));
  const dismiss = screen.getByText('Dismiss suggestion') as HTMLButtonElement;
  expect(dismiss.disabled).toBe(true);
  fireEvent.change(screen.getByLabelText('Why is it not an issue?'), { target: { value: 'The crack is on the old hose' } });
  fireEvent.click(dismiss);

  expect(await screen.findByText(/Reason: The crack is on the old hose/)).toBeTruthy();
  expect(recorded.dismissed).toEqual([{ reviewId: 'R-1', position: 1, reason: 'The crack is on the old hose', dismissedBy: inspector }]);
  expect(screen.queryByText(/AI suggestion: this answer may contradict/)).toBeNull();
});

test('what the automatic checks discarded is shown apart, in plain words, and cannot be decided', async () => {
  show([review({ status: 'PENDING' })]);

  expect(await screen.findByText('1 more point was discarded by automatic checks')).toBeTruthy();
  expect(screen.getByText(/the quoted text does not appear in what you wrote/)).toBeTruthy();
  expect(screen.getAllByText('Confirm issue')).toHaveLength(1);
});

test('a role that only reads inspections sees the review but cannot ask for one or decide', async () => {
  show([review({ status: 'PENDING' })], { role: 'inspectionCoordinator' });

  expect(await screen.findByText('AI suggestion')).toBeTruthy();
  expect(screen.queryByText('Review again')).toBeNull();
  expect(screen.queryByText('Confirm issue')).toBeNull();
  expect(screen.queryByText("Not an issue")).toBeNull();
});

test('a closed inspection keeps its reviews but offers no new one', async () => {
  show([review({ status: 'PENDING' })], { actions: ['rectifyInspection', 'generateInspectionAct'] });

  expect(await screen.findByText('AI suggestion')).toBeTruthy();
  expect(screen.queryByText('Review again')).toBeNull();
});
