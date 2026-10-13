// @vitest-environment jsdom
import { beforeEach, expect, test, vi } from 'vitest';
import { BrowserSession } from './BrowserSession';

function tokenFor(payload: Record<string, unknown>): string {
  const encoded = btoa(JSON.stringify(payload)).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
  return `header.${encoded}.signature`;
}

const inesToken = tokenFor({ sub: 'EMP-002', name: 'Ines Duarte', role: 'INSPECTOR', exp: 4102444800 });

function answering(...responses: { status?: number; headers?: Record<string, string>; body?: unknown }[]) {
  const pending = [...responses];
  return vi.fn(async (_input: string, _init?: RequestInit) => {
    const { status = 200, headers = {}, body } = pending.shift()!;
    return new Response(body === undefined ? null : JSON.stringify(body), { status, headers });
  });
}

beforeEach(() => {
  window.sessionStorage.clear();
});

test('logging in sends Basic credentials on an ordinary request and keeps the token it gets back', async () => {
  const fetchFunction = answering({ headers: { Authorization: `Bearer ${inesToken}` }, body: {} });
  const session = new BrowserSession('', fetchFunction);

  const signedIn = await session.logIn('EMP-002', 'certiflow');

  const [url, init] = fetchFunction.mock.calls[0]!;
  expect(url).toBe('/api/v1');
  expect(init!.headers).toMatchObject({ Authorization: `Basic ${btoa('EMP-002:certiflow')}`, 'X-Requested-With': 'XMLHttpRequest' });
  expect(signedIn).toEqual({ person: { id: 'EMP-002', name: 'Ines Duarte' }, role: 'INSPECTOR', expiresAt: '2100-01-01T00:00:00.000Z' });
  expect(session.current()).toEqual(signedIn);
  expect(await session.headersFor(undefined)).toEqual({ Authorization: `Bearer ${inesToken}`, 'X-Requested-With': 'XMLHttpRequest' });
});

test('a reload keeps the session', async () => {
  await new BrowserSession('', answering({ headers: { Authorization: `Bearer ${inesToken}` } })).logIn('EMP-002', 'certiflow');

  expect(new BrowserSession('', answering()).current()?.person.id).toBe('EMP-002');
});

test('wrong credentials are rejected and nothing is kept', async () => {
  const session = new BrowserSession('', answering({ status: 401, body: { title: 'Unauthorized', detail: 'Log in' } }));

  await expect(session.logIn('EMP-002', 'wrong')).rejects.toMatchObject({ status: 401, message: 'Wrong person id or password' });
  expect(session.current()).toBeUndefined();
});

test('logging out revokes the current token and forgets it', async () => {
  const fetchFunction = answering({ headers: { Authorization: `Bearer ${inesToken}` } }, { status: 204 });
  const session = new BrowserSession('', fetchFunction);
  await session.logIn('EMP-002', 'certiflow');

  await session.logOut();

  const [url, init] = fetchFunction.mock.calls[1]!;
  expect(url).toBe('/api/v1/tokens/current');
  expect(init).toMatchObject({ method: 'DELETE', headers: { Authorization: `Bearer ${inesToken}` } });
  expect(session.current()).toBeUndefined();
});

test('logging out forgets the token even when the server fails', async () => {
  const session = new BrowserSession('', answering({ headers: { Authorization: `Bearer ${inesToken}` } }, { status: 500 }));
  await session.logIn('EMP-002', 'certiflow');

  await session.logOut();

  expect(session.current()).toBeUndefined();
});

test('a rejected token signs the person out and tells who is listening', async () => {
  const session = new BrowserSession('', answering({ headers: { Authorization: `Bearer ${inesToken}` } }));
  await session.logIn('EMP-002', 'certiflow');
  const listener = vi.fn();
  session.onSignedOut(listener);

  session.rejected();

  expect(listener).toHaveBeenCalledOnce();
  expect(session.current()).toBeUndefined();
});

test('an expired token is not a session', () => {
  window.sessionStorage.setItem('certiflow.token', tokenFor({ sub: 'EMP-002', name: 'Ines Duarte', role: 'INSPECTOR', exp: 1 }));

  expect(new BrowserSession('', answering()).current()).toBeUndefined();
});
