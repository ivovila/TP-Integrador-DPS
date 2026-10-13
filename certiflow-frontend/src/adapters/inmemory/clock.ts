import type { Instant, LocalDate } from '../../domain/shared';
import type { DemoControls } from '../../ports/DemoControls';

export function localDateOf(date: Date): LocalDate {
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${date.getFullYear()}-${month}-${day}`;
}

function oneMillisecondAfter(instant: Instant): Instant {
  return new Date(new Date(instant).getTime() + 1).toISOString();
}

export class ControllableClock implements DemoControls {
  private currentDate: LocalDate;
  private lastInstant: Instant = '';

  constructor(initialDate: LocalDate) {
    this.currentDate = initialDate;
  }

  today(): LocalDate {
    return this.currentDate;
  }

  setToday(date: LocalDate): void {
    this.currentDate = date;
  }

  now(): Instant {
    const timeOfDay = new Date().toISOString().slice(11);
    const candidate = `${this.currentDate}T${timeOfDay}`;
    const sameDayAsLastInstant = this.lastInstant.startsWith(this.currentDate);
    const instant = sameDayAsLastInstant && candidate <= this.lastInstant ? oneMillisecondAfter(this.lastInstant) : candidate;
    this.lastInstant = instant;
    return instant;
  }
}
