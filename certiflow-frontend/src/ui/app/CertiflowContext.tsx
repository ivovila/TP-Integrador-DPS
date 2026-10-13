import { createContext, useContext, type ReactNode } from 'react';
import type { AuthenticationPort } from '../../ports/AuthenticationPort';
import type { Certiflow } from '../../ports/Certiflow';

export interface Application {
  certiflow: Certiflow;
  authentication: AuthenticationPort;
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

export function useAuthentication(): AuthenticationPort {
  return useApplication().authentication;
}
