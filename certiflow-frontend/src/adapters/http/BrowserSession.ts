import type { Person } from '../../domain/shared';
import type { AuthenticationPort, SignedIn, StaffRole } from '../../ports/AuthenticationPort';
import { RejectedRequestError } from '../../ports/RejectedRequestError';
import { basicCredentials, bearerTokenOf, browserRequestHeader, type CredentialSource } from './CredentialSource';
import type { FetchFunction } from './HalClient';

const storageKey = 'certiflow.token';

interface TokenPayload {
  sub: string;
  name: string;
  role: StaffRole;
  exp: number;
}

function payloadOf(token: string): TokenPayload | undefined {
  try {
    const encoded = token.split('.')[1]!.replace(/-/g, '+').replace(/_/g, '/');
    const bytes = Uint8Array.from(atob(encoded), (character) => character.charCodeAt(0));
    return JSON.parse(new TextDecoder().decode(bytes)) as TokenPayload;
  } catch {
    return undefined;
  }
}

function signedInWith(token: string): SignedIn | undefined {
  const payload = payloadOf(token);
  if (payload === undefined || payload.exp * 1000 <= Date.now()) {
    return undefined;
  }
  return {
    person: { id: payload.sub, name: payload.name },
    role: payload.role,
    expiresAt: new Date(payload.exp * 1000).toISOString(),
  };
}

export class BrowserSession implements CredentialSource, AuthenticationPort {
  private readonly listeners = new Set<() => void>();

  constructor(
    private readonly baseUrl: string,
    private readonly fetchFunction: FetchFunction = (input, init) => fetch(input, init),
  ) {}

  current(): SignedIn | undefined {
    const token = this.storedToken();
    return token === undefined ? undefined : signedInWith(token);
  }

  async logIn(personId: string, password: string): Promise<SignedIn> {
    let response: Response;
    try {
      response = await this.fetchFunction(`${this.baseUrl}/api/v1`, {
        method: 'GET',
        headers: { Accept: 'application/hal+json', Authorization: basicCredentials(personId, password), ...browserRequestHeader },
      });
    } catch {
      throw new RejectedRequestError(0, 'ServerUnreachable', 'The server could not be reached. Check that the backend is running.');
    }
    if (response.status === 401) {
      throw new RejectedRequestError(401, 'Unauthorized', 'Wrong person id or password');
    }
    const token = bearerTokenOf(response);
    const signedIn = token === undefined ? undefined : signedInWith(token);
    if (!response.ok || token === undefined || signedIn === undefined) {
      throw new RejectedRequestError(response.status, 'LoginFailed', `The server answered ${response.status} without a valid token`);
    }
    window.sessionStorage.setItem(storageKey, token);
    return signedIn;
  }

  async logOut(): Promise<void> {
    const token = this.storedToken();
    if (token !== undefined) {
      await this.fetchFunction(`${this.baseUrl}/api/v1/tokens/current`, {
        method: 'DELETE',
        headers: { Authorization: `Bearer ${token}`, ...browserRequestHeader },
      }).catch(() => undefined);
    }
    this.signOut();
  }

  onSignedOut(listener: () => void): () => void {
    this.listeners.add(listener);
    return () => this.listeners.delete(listener);
  }

  async headersFor(_actingPerson: Person | undefined): Promise<Record<string, string>> {
    const token = this.storedToken();
    return token === undefined ? { ...browserRequestHeader } : { Authorization: `Bearer ${token}`, ...browserRequestHeader };
  }

  received(): void {}

  rejected(): void {
    this.signOut();
  }

  private signOut(): void {
    window.sessionStorage.removeItem(storageKey);
    this.listeners.forEach((listener) => listener());
  }

  private storedToken(): string | undefined {
    return window.sessionStorage.getItem(storageKey) ?? undefined;
  }
}
