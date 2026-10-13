import { expect, test } from 'vitest';
import { PersonLogins } from './PersonLogins';

const paula = { id: 'EMP-001', name: 'Paula Rios' };
const ines = { id: 'EMP-002', name: 'Ines Duarte' };

function answeredWith(token: string): Response {
  return new Response(null, { headers: { Authorization: `Bearer ${token}` } });
}

test('the first request of a person logs in with Basic and the next ones use the token it returned', async () => {
  const logins = new PersonLogins('certiflow', paula);

  expect(await logins.headersFor(ines)).toEqual({ Authorization: `Basic ${btoa('EMP-002:certiflow')}` });
  logins.received(answeredWith('ines-token'), ines);

  expect(await logins.headersFor(ines)).toEqual({ Authorization: 'Bearer ines-token' });
});

test('each person has a token of their own', async () => {
  const logins = new PersonLogins('certiflow', paula);
  logins.received(answeredWith('ines-token'), ines);

  expect(await logins.headersFor(paula)).toEqual({ Authorization: `Basic ${btoa('EMP-001:certiflow')}` });
});

test('reads go as the reader', async () => {
  const logins = new PersonLogins('certiflow', paula);
  logins.received(answeredWith('paula-token'), undefined);

  expect(await logins.headersFor(undefined)).toEqual({ Authorization: 'Bearer paula-token' });
  expect(await logins.headersFor(paula)).toEqual({ Authorization: 'Bearer paula-token' });
});

test('a rejection forgets every token', async () => {
  const logins = new PersonLogins('certiflow', paula);
  logins.received(answeredWith('ines-token'), ines);

  logins.rejected();

  expect(await logins.headersFor(ines)).toEqual({ Authorization: `Basic ${btoa('EMP-002:certiflow')}` });
});
