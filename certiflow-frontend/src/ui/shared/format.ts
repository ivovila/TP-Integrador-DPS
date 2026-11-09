import type { CertificationScope } from '../../domain/certification';
import type { Instant, LocalDate, Person } from '../../domain/shared';

const calendarDateFormat = new Intl.DateTimeFormat('en-GB', { day: '2-digit', month: 'short', year: 'numeric', timeZone: 'UTC' });
const localDateFormat = new Intl.DateTimeFormat('en-GB', { day: '2-digit', month: 'short', year: 'numeric' });
const localTimeFormat = new Intl.DateTimeFormat('en-GB', { hour: '2-digit', minute: '2-digit' });

export function formatDate(date: LocalDate): string {
  return calendarDateFormat.format(new Date(`${date}T00:00:00Z`));
}

export function formatInstant(instant: Instant): string {
  const moment = new Date(instant);
  return `${localDateFormat.format(moment)}, ${localTimeFormat.format(moment)}`;
}

/** Los ids sintéticos son UUID: se muestran cortos y se leen enteros en el tooltip y en la URL. */
export function shortId(id: string): string {
  return id.length > 12 ? id.slice(0, 8) : id;
}

export function formatPerson(person: Person): string {
  return `${person.name} (${person.id})`;
}

export function formatScope(scope: CertificationScope): string {
  return scope.kind === 'wholeAsset' ? 'Whole asset' : `Subsystem ${scope.subsystemCode}`;
}

export function instantFromLocalInput(localDateTime: string): Instant {
  return new Date(localDateTime).toISOString();
}
