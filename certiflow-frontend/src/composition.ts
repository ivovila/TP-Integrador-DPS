import { createHttpCertiflow } from './adapters/http/createHttpCertiflow';
import type { Application } from './ui/app/CertiflowContext';

export function composeApplication(): Application {
  return { certiflow: createHttpCertiflow('') };
}
