import { createContext, useContext, type ReactNode } from 'react';
import type { Certiflow } from '../../ports/Certiflow';
import type { DemoControls } from '../../ports/DemoControls';

export interface Application {
  certiflow: Certiflow;
  demoControls?: DemoControls;
}

const ApplicationContext = createContext<Application | undefined>(undefined);

export function ApplicationProvider({ application, children }: { application: Application; children: ReactNode }) {
  return <ApplicationContext.Provider value={application}>{children}</ApplicationContext.Provider>;
}

function useApplication(): Application {
  const application = useContext(ApplicationContext);
  if (application === undefined) {
    throw new Error('ApplicationProvider is missing');
  }
  return application;
}

export function useCertiflow(): Certiflow {
  return useApplication().certiflow;
}

export function useDemoControls(): DemoControls | undefined {
  return useApplication().demoControls;
}
