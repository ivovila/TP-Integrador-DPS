import { createContext, useContext, useEffect, useState, type ReactNode } from 'react';
import type { Person } from '../../domain/shared';
import type { SignedIn } from '../../ports/AuthenticationPort';
import { useAuthentication } from './CertiflowContext';
import { LoginPage } from './LoginPage';
import { roleMayDo, roleOf, type Role, type SystemAction } from './roles';

interface Session {
  actingPerson: Person;
  role: Role;
  mayDo(action: SystemAction): boolean;
  permitted<Action extends SystemAction>(offeredActions: Action[]): Action[];
  logOut(): Promise<void>;
}

const SessionContext = createContext<Session | undefined>(undefined);

export function SessionProvider({ children }: { children: ReactNode }) {
  const authentication = useAuthentication();
  const [signedIn, setSignedIn] = useState<SignedIn | undefined>(() => authentication.current());

  useEffect(() => authentication.onSignedOut(() => setSignedIn(undefined)), [authentication]);

  if (signedIn === undefined) {
    return <LoginPage onSignedIn={setSignedIn} />;
  }
  const role = roleOf(signedIn.role);
  const session: Session = {
    actingPerson: signedIn.person,
    role,
    mayDo: (action) => roleMayDo(role, action),
    permitted: (offeredActions) => offeredActions.filter((action) => roleMayDo(role, action)),
    logOut: async () => {
      await authentication.logOut();
      setSignedIn(undefined);
    },
  };
  return <SessionContext.Provider value={session}>{children}</SessionContext.Provider>;
}

export function useSession(): Session {
  const session = useContext(SessionContext);
  if (session === undefined) {
    throw new Error('SessionProvider is missing');
  }
  return session;
}
