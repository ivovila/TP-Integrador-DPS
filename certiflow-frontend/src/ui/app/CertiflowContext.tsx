import { createContext, useContext, type ReactNode } from 'react';
import type { Certiflow } from '../../ports/Certiflow';

export interface Application {
  certiflow: Certiflow;
}

const ApplicationContext = createContext<Application | undefined>(undefined);

export function ApplicationProvider({ application, children }: { application: Application; children: ReactNode }) {
  return <ApplicationContext.Provider value={application}>{children}</ApplicationContext.Provider>;
}

export function useCertiflow(): Certiflow {
  const application = useContext(ApplicationContext);
  if (application === undefined) {
    throw new Error('ApplicationProvider is missing');
  }
  return application.certiflow;
}
