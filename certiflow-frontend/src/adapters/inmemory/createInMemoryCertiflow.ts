import type { LocalDate } from '../../domain/shared';
import type { Certiflow } from '../../ports/Certiflow';
import type { DemoControls } from '../../ports/DemoControls';
import { createCatalogPort } from './catalog';
import { createCertificationPort } from './certification';
import { ControllableClock, localDateOf } from './clock';
import { createFindingPort } from './finding';
import { createInspectionPort } from './inspection';
import { createSchemaPort } from './schema';
import { createInMemorySystem } from './store';

export interface InMemoryCertiflow {
  certiflow: Certiflow;
  demoControls: DemoControls;
}

export function createInMemoryCertiflow(initialDate: LocalDate = localDateOf(new Date())): InMemoryCertiflow {
  const clock = new ControllableClock(initialDate);
  const system = createInMemorySystem(clock);
  return {
    certiflow: {
      catalog: createCatalogPort(system),
      schemas: createSchemaPort(system),
      inspections: createInspectionPort(system),
      findings: createFindingPort(system),
      certification: createCertificationPort(system),
      audit: {
        async auditHistory(subject) {
          return structuredClone(system.auditLog.entriesFor(subject));
        },
      },
    },
    demoControls: clock,
  };
}
