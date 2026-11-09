// @vitest-environment jsdom
import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router';
import { afterEach, expect, test } from 'vitest';
import type { AssistantReview, Suggestion, SuggestionDecision } from '../../domain/assistance';
import type { InspectionAction, InspectionDetails } from '../../domain/inspection';
import type { Severity } from '../../domain/schema';
import type { Person } from '../../domain/shared';
import type { AcceptSuggestionCommand, DismissSuggestionCommand, RequestAssistantReviewCommand } from '../../ports/AssistancePort';
import type { Certiflow } from '../../ports/Certiflow';
import { ApplicationProvider } from '../app/CertiflowContext';
import type { Role } from '../app/roles';
import { SessionProvider } from '../app/SessionContext';
import { fakeAuthentication, signedInAs } from '../app/testing';
import { AssistantReviewCard } from './AssistantReviewCard';

afterEach(cleanup);

const major: Severity = { name: 'MAJOR', label: 'Major', outcomeWhenUnmet: 'REJECTED' };
const minor: Severity = { name: 'MINOR', label: 'Minor', outcomeWhenUnmet: 'OBSERVED' };
const inspector: Person = signedInAs('inspector').person;

const inspection: InspectionDetails = {
  id: 'INS-1',
  assetCode: 'EXT-0042',
  assignment: { inspector, plannedDate: '2026-03-02', scope: { kind: 'full' } },
  schemaVersion: 1,
  closed: false,
  coversWholeSchema: true,
  responses: [
    {
      sectionTitle: 'Physical condition',
      criterion: { code: 'HOSE', text: 'Hose condition', severity: minor, rule: { type: 'option-in-list', acceptedOptions: ['Good'] }, requiredEvidence: [] },
      attachments: [],
      observations: [],
      outcome: 'APPROVED',
    },
  ],
  revisions: [],
  actions: ['closeInspection', 'requestAssistantReview'],
};

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
      listReviews: async () => reviews,
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

function show(reviews: AssistantReview[], role: Role = 'inspector', actions: InspectionAction[] = inspection.actions): Recorded {
  const recorded: Recorded = { requested: [], accepted: [], dismissed: [] };
  render(
    <ApplicationProvider application={{ certiflow: certiflowWith(reviews, recorded), authentication: fakeAuthentication(signedInAs(role)) }}>
      <MemoryRouter>
        <SessionProvider>
          <AssistantReviewCard inspection={{ ...inspection, actions }} />
        </SessionProvider>
      </MemoryRouter>
    </ApplicationProvider>,
  );
  return recorded;
}

test('a suggestion is marked as coming from the AI, with both severities, the rationale and the quotes', async () => {
  show([review({ status: 'PENDING' })]);

  expect(await screen.findByText('Suggested by AI')).toBeTruthy();
  expect(screen.getByText('Hose condition')).toBeTruthy();
  expect(screen.getByText('Criterion severity: Minor · Estimated by the assistant: Major')).toBeTruthy();
  expect(screen.getByText('La manguera tiene una grieta cerca del acople')).toBeTruthy();
  expect(screen.getByText(/prompt v2 · model nvidia\/nemotron/)).toBeTruthy();
});

test('the inspector asks for a review of an open inspection and sees what the assistant suggested', async () => {
  const recorded = show([]);

  expect(await screen.findByText('No review was requested for this inspection yet.')).toBeTruthy();
  fireEvent.click(screen.getByText('Review with AI'));

  expect(await screen.findByText('Suggested by AI')).toBeTruthy();
  expect(recorded.requested).toEqual([{ inspectionId: 'INS-1', requestedBy: inspector }]);
});

test('accepting records the decision and points to the criterion, whose answer the inspector corrects', async () => {
  const recorded = show([review({ status: 'PENDING' })]);

  fireEvent.click(await screen.findByText('Accept'));

  const correct = await screen.findByText('Correct the answer');
  expect(correct.getAttribute('href')).toBe('#criterion-HOSE');
  expect(screen.getByText(/decided by/).textContent).toContain(inspector.name);
  expect(recorded.accepted).toEqual([{ reviewId: 'R-1', position: 1, acceptedBy: inspector }]);
  expect(screen.queryByText('Accept')).toBeNull();
});

test('dismissing asks for a reason and keeps it with the decision', async () => {
  const recorded = show([review({ status: 'PENDING' })]);
  const dismiss = (await screen.findByText('Dismiss')) as HTMLButtonElement;
  expect(dismiss.disabled).toBe(true);

  fireEvent.change(screen.getByLabelText('Reason to dismiss'), { target: { value: 'The crack is on the old hose, already replaced' } });
  fireEvent.click(dismiss);

  expect(await screen.findByText(/Reason: The crack is on the old hose, already replaced/)).toBeTruthy();
  expect(recorded.dismissed).toEqual([
    { reviewId: 'R-1', position: 1, reason: 'The crack is on the old hose, already replaced', dismissedBy: inspector },
  ]);
});

test('what a guardrail stopped is shown apart, with the reason, and cannot be decided', async () => {
  show([review({ status: 'PENDING' })]);

  expect(await screen.findByText('Stopped by the guardrails (1)')).toBeTruthy();
  expect(screen.getByText(/the quote does not appear in what the inspector wrote/)).toBeTruthy();
  expect(screen.getAllByText('Accept')).toHaveLength(1);
});

test('a role that only reads inspections sees the reviews but cannot ask for one or decide', async () => {
  show([review({ status: 'PENDING' })], 'inspectionCoordinator');

  expect(await screen.findByText('Suggested by AI')).toBeTruthy();
  expect(screen.queryByText('Review with AI')).toBeNull();
  expect(screen.queryByText('Accept')).toBeNull();
  expect(screen.queryByText('Dismiss')).toBeNull();
});

test('a closed inspection keeps its reviews but offers no new one', async () => {
  show([review({ status: 'PENDING' })], 'inspector', ['rectifyInspection', 'generateInspectionAct']);

  expect(await screen.findByText('Suggested by AI')).toBeTruthy();
  expect(screen.queryByText('Review with AI')).toBeNull();
});
