import { describeCertiflowContract } from '../../ports/certiflowContract';
import { createInMemoryCertiflow } from './createInMemoryCertiflow';

describeCertiflowContract('in memory', (initialDate) => {
  const { certiflow, demoControls } = createInMemoryCertiflow(initialDate);
  return { certiflow, setToday: (date) => demoControls.setToday(date) };
});
