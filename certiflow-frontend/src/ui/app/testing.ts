import type { AuthenticationPort, SignedIn } from '../../ports/AuthenticationPort';
import { RejectedRequestError } from '../../ports/RejectedRequestError';
import { staffRoleOf, type Role } from './roles';
import { roster } from './roster';

export const demoPassword = 'certiflow';

export function signedInAs(role: Role): SignedIn {
  const member = roster.find((candidate) => candidate.role === role)!;
  return { person: member.person, role: staffRoleOf[role], expiresAt: '2100-01-01T00:00:00.000Z' };
}

export interface FakeAuthentication extends AuthenticationPort {
  tokenRejectedByTheServer(): void;
}

export function fakeAuthentication(startingAs?: SignedIn): FakeAuthentication {
  let signedIn = startingAs;
  const listeners = new Set<() => void>();
  const signOut = () => {
    signedIn = undefined;
    listeners.forEach((listener) => listener());
  };
  return {
    current: () => signedIn,
    async logIn(personId, password) {
      const member = roster.find((candidate) => candidate.person.id === personId);
      if (member === undefined || password !== demoPassword) {
        throw new RejectedRequestError(401, 'Unauthorized', 'Wrong person id or password');
      }
      signedIn = signedInAs(member.role);
      return signedIn;
    },
    async logOut() {
      signOut();
    },
    onSignedOut(listener) {
      listeners.add(listener);
      return () => listeners.delete(listener);
    },
    tokenRejectedByTheServer: signOut,
  };
}
