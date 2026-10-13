import { expect, test } from 'vitest';
import type { Person } from '../../domain/shared';
import { PersonLogins } from './PersonLogins';

const paula = { id: 'EMP-001', name: 'Paula Rios' };
const ines = { id: 'EMP-002', name: 'Ines Duarte' };
const raul = { id: 'EMP-003', name: 'Raul Mendez' };
const usernames: Record<string, string> = { 'EMP-001': 'paula.rios', 'EMP-002': 'ines.duarte', 'EMP-003': 'raul.mendez' };
const usernameOf = (person: Person) => usernames[person.id]!;
const readerFor = (path: string) => (path.includes('/findings') ? raul : paula);

function answeredWith(token: string): Response {
  return new Response(null, { headers: { Authorization: `Bearer ${token}` } });
}

test('the first request of a person logs in with their username and the next ones use the token it returned', async () => {
  const logins = new PersonLogins('certiflow', usernameOf, readerFor);

  expect(await logins.headersFor(ines, '/api/v1/inspections')).toEqual({ Authorization: `Basic ${btoa('ines.duarte:certiflow')}` });
  logins.received(answeredWith('ines-token'), ines, '/api/v1/inspections');

  expect(await logins.headersFor(ines, '/api/v1/inspections')).toEqual({ Authorization: 'Bearer ines-token' });
});

test('each person has a token of their own', async () => {
  const logins = new PersonLogins('certiflow', usernameOf, readerFor);
  logins.received(answeredWith('ines-token'), ines, '/api/v1/inspections');

  expect(await logins.headersFor(paula, '/api/v1/assets')).toEqual({ Authorization: `Basic ${btoa('paula.rios:certiflow')}` });
});

test('a read goes as someone whose role can read that path', async () => {
  const logins = new PersonLogins('certiflow', usernameOf, readerFor);

  expect(await logins.headersFor(undefined, '/api/v1/findings?asset=A')).toEqual({ Authorization: `Basic ${btoa('raul.mendez:certiflow')}` });
  logins.received(answeredWith('raul-token'), undefined, '/api/v1/findings?asset=A');
  expect(await logins.headersFor(undefined, '/api/v1/findings/F-1')).toEqual({ Authorization: 'Bearer raul-token' });
  expect(await logins.headersFor(undefined, '/api/v1/assets')).toEqual({ Authorization: `Basic ${btoa('paula.rios:certiflow')}` });
});

test('a rejection forgets every token', async () => {
  const logins = new PersonLogins('certiflow', usernameOf, readerFor);
  logins.received(answeredWith('ines-token'), ines, '/api/v1/inspections');

  logins.rejected();

  expect(await logins.headersFor(ines, '/api/v1/inspections')).toEqual({ Authorization: `Basic ${btoa('ines.duarte:certiflow')}` });
});
