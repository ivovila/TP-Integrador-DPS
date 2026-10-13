import { demoLogins } from '../../demoLogins';
import { describeCertiflowContract } from '../../ports/certiflowContract';
import { createHttpCertiflow } from './createHttpCertiflow';

describeCertiflowContract('HTTP adapter against the running backend', () => ({
  certiflow: createHttpCertiflow(process.env.CERTIFLOW_API_URL!, demoLogins()),
}));
