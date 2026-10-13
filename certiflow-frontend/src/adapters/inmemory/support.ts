import type { Ineligibility } from '../../domain/certification';
import type { Person } from '../../domain/shared';
import { DomainError, type DomainErrorName } from '../../ports/DomainError';

export function reject(name: DomainErrorName, message: string, ineligibilities: Ineligibility[] = []): never {
  throw new DomainError(name, message, ineligibilities);
}

export function requireText(value: string, messageWhenMissing: string): void {
  if (value.trim() === '') {
    reject('RequiredValueMissingException', messageWhenMissing);
  }
}

export function isSamePerson(first: Person, second: Person): boolean {
  return first.id === second.id;
}
