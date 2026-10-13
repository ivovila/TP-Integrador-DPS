// @vitest-environment jsdom
import { cleanup, render, screen } from '@testing-library/react';
import { afterEach, expect, test } from 'vitest';
import { DomainError } from '../../ports/DomainError';
import { RejectedRequestError } from '../../ports/RejectedRequestError';
import { ErrorBanner } from './ErrorBanner';

afterEach(cleanup);

test('a business rejection is shown with its message and the name of the rule', () => {
  render(<ErrorBanner error={new DomainError('InspectionIncompleteException', 'These criteria are still pending: SEAL')} />);
  expect(screen.getByRole('alert').textContent).toContain('These criteria are still pending: SEAL');
  expect(screen.getByRole('alert').textContent).toContain('InspectionIncompleteException');
});

test('a refused certificate lists every reason', () => {
  const refusal = new DomainError('NotEligibleForCertificationException', 'Inspection INS-1 cannot back a certificate', [
    { reason: 'PARTIAL_INSPECTION', detail: 'Only an inspection that covers the whole schema can back a certificate' },
    { reason: 'BLOCKING_FINDINGS', detail: 'Asset EXT-0042 has 1 open finding(s) whose severity blocks certification' },
  ]);
  render(<ErrorBanner error={refusal} />);
  expect(screen.getAllByRole('listitem')).toHaveLength(2);
});

test('nothing is shown while there is no error', () => {
  const { container } = render(<ErrorBanner error={undefined} />);
  expect(container.textContent).toBe('');
});

test('a rejection that is not a business rule still shows what the server explained', () => {
  render(<ErrorBanner error={new RejectedRequestError(400, 'Bad Request', "Required header 'X-Person-Id' is not present.")} />);
  expect(screen.getByRole('alert').textContent).toContain("Required header 'X-Person-Id' is not present.");
  expect(screen.getByRole('alert').textContent).toContain('Bad Request');
});

test('an unreachable server is reported as such', () => {
  render(<ErrorBanner error={new RejectedRequestError(0, 'ServerUnreachable', 'The server could not be reached. Check that the backend is running.')} />);
  expect(screen.getByRole('alert').textContent).toContain('The server could not be reached');
});
