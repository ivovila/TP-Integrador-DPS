import { useState, type FormEvent } from 'react';
import type { SignedIn } from '../../ports/AuthenticationPort';
import { ErrorBanner } from '../shared/ErrorBanner';
import { useAuthentication } from './CertiflowContext';

export function LoginPage({ onSignedIn }: { onSignedIn(signedIn: SignedIn): void }) {
  const authentication = useAuthentication();
  const [personId, setPersonId] = useState('');
  const [password, setPassword] = useState('');
  const [failure, setFailure] = useState<unknown>(undefined);
  const [loggingIn, setLoggingIn] = useState(false);

  async function logIn(event: FormEvent): Promise<void> {
    event.preventDefault();
    setLoggingIn(true);
    setFailure(undefined);
    try {
      onSignedIn(await authentication.logIn(personId.trim(), password));
    } catch (error) {
      setFailure(error);
      setLoggingIn(false);
    }
  }

  return (
    <main className="page login-page">
      <h1>Log in to CertiFlow</h1>
      <form onSubmit={logIn}>
        <div className="field">
          <label>
            Person id
            <input value={personId} autoComplete="username" placeholder="EMP-002" onChange={(event) => setPersonId(event.target.value)} />
          </label>
        </div>
        <div className="field">
          <label>
            Password
            <input type="password" value={password} autoComplete="current-password" onChange={(event) => setPassword(event.target.value)} />
          </label>
        </div>
        <ErrorBanner error={failure} />
        <button type="submit" disabled={loggingIn || personId.trim() === '' || password === ''}>
          Log in
        </button>
      </form>
    </main>
  );
}
