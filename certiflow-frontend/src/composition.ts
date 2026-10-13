import { createInMemoryCertiflow } from './adapters/inmemory/createInMemoryCertiflow';
import { seedDemoData } from './adapters/inmemory/seedDemoData';
import type { Application } from './ui/app/CertiflowContext';
import { demoPeople } from './ui/app/roster';

export async function composeApplication(): Promise<Application> {
  const { certiflow, demoControls } = createInMemoryCertiflow();
  await seedDemoData(certiflow, demoControls, demoPeople);
  return { certiflow, demoControls };
}
