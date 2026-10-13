import type { LocalDate } from '../domain/shared';

export interface DemoControls {
  today(): LocalDate;
  setToday(date: LocalDate): void;
}
