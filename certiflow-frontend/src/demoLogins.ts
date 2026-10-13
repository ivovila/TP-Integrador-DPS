import { PersonLogins } from './adapters/http/PersonLogins';
import type { Person } from './domain/shared';
import { demoPeople, demoUsernames } from './ui/app/roster';

export const demoPassword = 'certiflow';

function demoReaderFor(path: string): Person {
  if (path.includes('/findings')) {
    return demoPeople.assetResponsible;
  }
  if (path.startsWith('/api/v1/certificates')) {
    return demoPeople.certifier;
  }
  return demoPeople.coordinator;
}

export function demoLogins(): PersonLogins {
  return new PersonLogins(demoPassword, (person) => demoUsernames[person.id]!, demoReaderFor);
}
