import { BrowserRouter } from 'react-router';
import { ApplicationProvider, type Application } from './CertiflowContext';
import { ApplicationRoutes } from './routes';
import { SessionProvider } from './SessionContext';
import './styles.css';

export function App({ application }: { application: Application }) {
  return (
    <ApplicationProvider application={application}>
      <SessionProvider>
        <BrowserRouter>
          <ApplicationRoutes />
        </BrowserRouter>
      </SessionProvider>
    </ApplicationProvider>
  );
}
