import { BrowserSession } from './adapters/http/BrowserSession';
import { createHttpCertiflow } from './adapters/http/createHttpCertiflow';
import type { Application } from './ui/app/CertiflowContext';

export function composeApplication(): Application {
  const session = new BrowserSession('');
  return { certiflow: createHttpCertiflow('', session), authentication: session };
}
