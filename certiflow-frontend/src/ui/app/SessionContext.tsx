import { createContext, useContext, useState, type ReactNode } from 'react';
import type { Person } from '../../domain/shared';
import { roster } from './roster';

interface Session {
  actingPerson: Person;
  actAs(person: Person): void;
}

const storageKey = 'certiflow.actingPerson';
const SessionContext = createContext<Session | undefined>(undefined);

function rememberedPerson(): Person {
  try {
    const remembered = window.localStorage.getItem(storageKey);
    return remembered === null ? roster[0]!.person : (JSON.parse(remembered) as Person);
  } catch {
    return roster[0]!.person;
  }
}

function remember(person: Person): void {
  try {
    window.localStorage.setItem(storageKey, JSON.stringify(person));
  } catch {
    return;
  }
}

export function SessionProvider({ children }: { children: ReactNode }) {
  const [actingPerson, setActingPerson] = useState<Person>(rememberedPerson);
  const actAs = (person: Person) => {
    setActingPerson(person);
    remember(person);
  };
  return <SessionContext.Provider value={{ actingPerson, actAs }}>{children}</SessionContext.Provider>;
}

export function useSession(): Session {
  const session = useContext(SessionContext);
  if (session === undefined) {
    throw new Error('SessionProvider is missing');
  }
  return session;
}
