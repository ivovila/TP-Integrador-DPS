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

function signedInWith(payload: TokenPayload, expiresAt: Date): SignedIn {
  return { person: { id: payload.sub, name: payload.name }, role: payload.role, expiresAt: expiresAt.toISOString() };
}

function unexpiredSessionOf(token: string): SignedIn | undefined {
  const payload = payloadOf(token);
  if (payload === undefined || payload.exp * 1000 <= Date.now()) {
    return undefined;
  }
  return signedInWith(payload, new Date(payload.exp * 1000));
}

export class BrowserSession implements CredentialSource, AuthenticationPort {
  private readonly listeners = new Set<() => void>();

  constructor(
    private readonly baseUrl: string,
    private readonly fetchFunction: FetchFunction = (input, init) => fetch(input, init),
  ) {}

  current(): SignedIn | undefined {
    const token = this.storedToken();
    return token === undefined ? undefined : unexpiredSessionOf(token);
  }

  async logIn(username: string, password: string): Promise<SignedIn> {
    let response: Response;
    try {
      response = await this.fetchFunction(`${this.baseUrl}/api/v1`, {
        method: 'GET',
        headers: { Accept: 'application/hal+json', Authorization: basicCredentials(username, password), ...browserRequestHeader },
      });
    } catch {
      throw new RejectedRequestError(0, 'ServerUnreachable', 'The server could not be reached. Check that the backend is running.');
    }
    if (response.status === 401) {
      throw new RejectedRequestError(401, 'Unauthorized', 'Wrong username or password');
    }
    const token = bearerTokenOf(response);
    const payload = token === undefined ? undefined : payloadOf(token);
    if (!response.ok || token === undefined || payload === undefined) {
      throw new RejectedRequestError(response.status, 'LoginFailed', `The server answered ${response.status} without a valid token`);
    }
    window.sessionStorage.setItem(storageKey, token);
    const announcedExpiry = response.headers.get('Token-Expires');
    return signedInWith(payload, announcedExpiry === null ? new Date(payload.exp * 1000) : new Date(announcedExpiry));
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

  async headersFor(_actingPerson: Person | undefined, _path: string): Promise<Record<string, string>> {
    const token = this.storedToken();
    return token === undefined ? { ...browserRequestHeader } : { Authorization: `Bearer ${token}`, ...browserRequestHeader };
  }

  received(): void {}

  rejected(sentHeaders: Record<string, string>): void {
    const token = this.storedToken();
    if (token !== undefined && sentHeaders.Authorization === `Bearer ${token}`) {
      this.signOut();
    }
  }

  private signOut(): void {
    window.sessionStorage.removeItem(storageKey);
    this.listeners.forEach((listener) => listener());
  }

  private storedToken(): string | undefined {
    return window.sessionStorage.getItem(storageKey) ?? undefined;
  }
}
