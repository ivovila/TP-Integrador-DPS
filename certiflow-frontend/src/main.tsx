import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { composeApplication } from './composition';
import { App } from './ui/app/App';

composeApplication().then((application) => {
  createRoot(document.getElementById('root')!).render(
    <StrictMode>
      <App application={application} />
    </StrictMode>,
  );
});
