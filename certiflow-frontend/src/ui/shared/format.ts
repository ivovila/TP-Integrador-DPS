import type { CertificationScope } from '../../domain/certification';
import type { Instant, LocalDate, Person } from '../../domain/shared';

const dateFormat = new Intl.DateTimeFormat('en-GB', { day: '2-digit', month: 'short', year: 'numeric', timeZone: 'UTC' });
const timeFormat = new Intl.DateTimeFormat('en-GB', { hour: '2-digit', minute: '2-digit', timeZone: 'UTC' });

export function formatDate(date: LocalDate): string {
  return dateFormat.format(new Date(`${date}T00:00:00Z`));
}

export function formatInstant(instant: Instant): string {
  const moment = new Date(instant);
  return `${dateFormat.format(moment)}, ${timeFormat.format(moment)}`;
}

export function formatPerson(person: Person): string {
  return `${person.name} (${person.id})`;
}

export function formatScope(scope: CertificationScope): string {
  return scope.kind === 'wholeAsset' ? 'Whole asset' : `Subsystem ${scope.subsystemCode}`;
}

/**
 * Convierte el valor de un `<input type="datetime-local">` en un instante. Igual que `formatInstant`,
 * trata la hora como UTC, porque el reloj en memoria marca la hora local como UTC.
 */
export function instantFromLocalInput(value: string): Instant {
  return `${value}:00.000Z`;
}
