import { createContext, useContext, useState, type ReactNode } from 'react';
import type { Person } from '../../domain/shared';
import { everyRole, roleMayDo, type Role, type SystemAction } from './roles';
import { roster, type RosterMember } from './roster';

interface Session {
  actingPerson: Person;
  role: Role;
  actAs(person: Person, role: Role): void;
  mayDo(action: SystemAction): boolean;
  permitted<Action extends SystemAction>(offeredActions: Action[]): Action[];
}

const storageKey = 'certiflow.actingAs';
const SessionContext = createContext<Session | undefined>(undefined);

function rememberedMember(): RosterMember {
  try {
    const remembered = JSON.parse(window.localStorage.getItem(storageKey) ?? 'null') as Partial<RosterMember> | null;
    const isComplete =
      remembered !== null &&
      typeof remembered.person?.id === 'string' &&
      typeof remembered.person.name === 'string' &&
      everyRole.some((role) => role === remembered.role);
    return isComplete ? (remembered as RosterMember) : roster[0]!;
  } catch {
    return roster[0]!;
  }
}

function remember(member: RosterMember): void {
  try {
    window.localStorage.setItem(storageKey, JSON.stringify(member));
  } catch {
    return;
  }
}

export function SessionProvider({ children, startingAs }: { children: ReactNode; startingAs?: RosterMember }) {
  const [actingMember, setActingMember] = useState<RosterMember>(() => startingAs ?? rememberedMember());
  const session: Session = {
    actingPerson: actingMember.person,
    role: actingMember.role,
    actAs(person, role) {
      setActingMember({ person, role });
      remember({ person, role });
    },
    mayDo: (action) => roleMayDo(actingMember.role, action),
    permitted: (offeredActions) => offeredActions.filter((action) => roleMayDo(actingMember.role, action)),
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
