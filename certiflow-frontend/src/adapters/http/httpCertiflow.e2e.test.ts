import { describeCertiflowContract } from '../../ports/certiflowContract';
import { createHttpCertiflow } from './createHttpCertiflow';
import { PersonLogins } from './PersonLogins';

describeCertiflowContract('HTTP adapter against the running backend', () => ({
  certiflow: createHttpCertiflow(process.env.CERTIFLOW_API_URL!, new PersonLogins('certiflow', { id: 'EMP-001', name: 'Paula Rios' })),
}));
