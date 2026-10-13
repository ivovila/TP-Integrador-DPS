import { expect, test, vi } from 'vitest';
import firstPageOfAssets from './__fixtures__/asset-list-first-page.json';
import lastPageOfAssets from './__fixtures__/asset-list-last-page.json';
import refusedCertificate from './__fixtures__/error-not-eligible.json';
import unknownFieldRejection from './__fixtures__/error-unknown-field.json';
import noFindings from './__fixtures__/findings-list-empty.json';
import type { CredentialSource } from './CredentialSource';
import { HalClient, lastSegmentOf, pathOf, query, SerializedJson } from './HalClient';

const paula = { id: 'EMP-001', name: 'Paula Rios' };

function recordingCredentials(): CredentialSource & { askedFor: unknown[]; rejections: number } {
  const credentials = {
    askedFor: [] as unknown[],
    rejections: 0,
    async headersFor(actingPerson: unknown) {
      credentials.askedFor.push(actingPerson);
      return { Authorization: 'Bearer token', 'X-Requested-With': 'XMLHttpRequest' };
    },
    received() {},
    rejected() {
      credentials.rejections += 1;
    },
  };
  return credentials;
}

const noCredentials = recordingCredentials();

function answering(...responses: { status?: number; body?: unknown }[]) {
  const pending = [...responses];
  return vi.fn(async (_input: string, _init?: RequestInit) => {
    const { status = 200, body } = pending.shift()!;
    return new Response(body === undefined ? null : JSON.stringify(body), { status });
  });
}

const hrefs: [string, string, string][] = [
  ['an absolute link loses its origin', 'http://localhost:8080/api/v1/inspections/abc/responses', '/api/v1/inspections/abc/responses'],
  ['a relative link is kept', '/api/v1/assets/CAP-0001', '/api/v1/assets/CAP-0001'],
  ['a link keeps its query', '/api/v1/findings?asset=CAP-0001&page=1', '/api/v1/findings?asset=CAP-0001&page=1'],
  [
    'a templated link loses its variables',
    'http://localhost:8080/api/v1/inspections/abc/certification-eligibility{?subsystem}',
    '/api/v1/inspections/abc/certification-eligibility',
  ],
  ['an encoded segment stays encoded', 'http://localhost:8080/api/v1/schemas/Capture%20plant', '/api/v1/schemas/Capture%20plant'],
];

test.each(hrefs)('%s', (_description, href, expectedPath) => {
  expect(pathOf(href)).toBe(expectedPath);
});

test('the identifier of a linked resource is its last path segment, decoded', () => {
  expect(lastSegmentOf('http://localhost:8080/api/v1/findings/66f977a8-bf24')).toBe('66f977a8-bf24');
  expect(lastSegmentOf('/api/v1/schemas/Capture%20plant')).toBe('Capture plant');
});

test('absent filters are left out of the query', () => {
  expect(query({ asset: 'CAP-0001', status: undefined })).toBe('?asset=CAP-0001');
  expect(query({ asset: undefined })).toBe('');
});

test('a read asks for HAL with the credentials of nobody in particular', async () => {
  const fetchFunction = answering({ body: { code: 'CAP-0001' } });
  const credentials = recordingCredentials();
  await new HalClient('', credentials, fetchFunction).read('/api/v1/assets/CAP-0001');
  const [url, init] = fetchFunction.mock.calls[0]!;
  expect(url).toBe('/api/v1/assets/CAP-0001');
  expect(init).toMatchObject({
    method: 'GET',
    headers: { Accept: 'application/hal+json, application/problem+json', Authorization: 'Bearer token' },
  });
  expect(credentials.askedFor).toEqual([undefined]);
});

test('a write sends the credentials of who acts and no person headers', async () => {
  const fetchFunction = answering({ status: 201, body: {} });
  const credentials = recordingCredentials();
  await new HalClient('', credentials, fetchFunction).write('POST', '/api/v1/assets', paula, { code: 'CAP-0001' });
  const init = fetchFunction.mock.calls[0]![1]!;
  expect(init).toMatchObject({
    method: 'POST',
    headers: { Authorization: 'Bearer token', 'X-Requested-With': 'XMLHttpRequest', 'Content-Type': 'application/json' },
    body: '{"code":"CAP-0001"}',
  });
  expect(init.headers).not.toHaveProperty('X-Person-Id');
  expect(credentials.askedFor).toEqual([paula]);
});

test('an unauthorized answer drops the credentials before it is reported', async () => {
  const fetchFunction = answering({ status: 401, body: { title: 'Unauthorized', detail: 'Log in' } });
  const credentials = recordingCredentials();
  await expect(new HalClient('', credentials, fetchFunction).read('/api/v1/assets')).rejects.toMatchObject({
    name: 'RejectedRequestError',
    status: 401,
  });
  expect(credentials.rejections).toBe(1);
});

test('a body that is already serialized is sent as it is', async () => {
  const fetchFunction = answering({ status: 204 });
  await new HalClient('', noCredentials, fetchFunction).write('PUT', '/answer', paula, new SerializedJson('{"value":2.50}'));
  expect(fetchFunction.mock.calls[0]![1]!.body).toBe('{"value":2.50}');
});

test('a write without content answers nothing', async () => {
  const fetchFunction = answering({ status: 204 });
  expect(await new HalClient('', noCredentials, fetchFunction).write('PUT', '/answer', paula, { value: true })).toBeUndefined();
});

test('requests go to the base address when there is one', async () => {
  const fetchFunction = answering({ body: {} });
  await new HalClient('http://backend:8080', noCredentials, fetchFunction).read('http://localhost:8080/api/v1/assets');
  expect(fetchFunction.mock.calls[0]![0]).toBe('http://backend:8080/api/v1/assets');
});

test('a list follows the next link until the last page', async () => {
  const fetchFunction = answering({ body: firstPageOfAssets }, { body: lastPageOfAssets });
  const assets = await new HalClient('', noCredentials, fetchFunction).readAll<{ code: string }>('/api/v1/assets?page=0&size=1', 'assets');
  expect(assets.map((asset) => asset.code)).toEqual(['CAP-0001', 'CAP-0010']);
  expect(fetchFunction.mock.calls.map((call) => call[0])).toEqual(['/api/v1/assets?page=0&size=1', '/api/v1/assets?page=1&size=1']);
});

test('an empty collection, which the API sends without its embedded list, is an empty list', async () => {
  const fetchFunction = answering({ body: noFindings });
  expect(await new HalClient('', noCredentials, fetchFunction).readAll('/api/v1/findings?asset=NO-FINDINGS', 'findings')).toEqual([]);
});

test('a business rejection becomes a domain error with its name, message and reasons', async () => {
  const fetchFunction = answering({ status: 409, body: refusedCertificate });
  await expect(new HalClient('', noCredentials, fetchFunction).write('POST', '/api/v1/certificates', paula, {})).rejects.toMatchObject({
    name: 'NotEligibleForCertificationException',
    message: refusedCertificate.detail,
    ineligibilities: [{ reason: 'BLOCKING_FINDINGS' }, { reason: 'BLOCKING_FINDINGS' }],
  });
});

test('a rejection that is not a business rule keeps its status, title and explanation', async () => {
  const fetchFunction = answering({ status: 400, body: unknownFieldRejection });
  await expect(new HalClient('', noCredentials, fetchFunction).write('POST', '/api/v1/assets', paula, {})).rejects.toMatchObject({
    name: 'RejectedRequestError',
    status: 400,
    title: 'Bad Request',
    message: 'Failed to read request',
  });
});

test('a rejection without a body still says which status the server answered', async () => {
  const fetchFunction = answering({ status: 502 });
  await expect(new HalClient('', noCredentials, fetchFunction).read('/api/v1/assets')).rejects.toMatchObject({
    name: 'RejectedRequestError',
    status: 502,
    message: 'The server answered 502',
  });
});

test('a server that cannot be reached is reported as unreachable', async () => {
  const failingFetch = vi.fn(async () => {
    throw new TypeError('fetch failed');
  });
  await expect(new HalClient('', noCredentials, failingFetch).read('/api/v1/assets')).rejects.toMatchObject({
    name: 'RejectedRequestError',
    status: 0,
    title: 'ServerUnreachable',
  });
});
