import type { Person } from '../domain/shared';

export type StaffRole =
  | 'ADMINISTRATOR'
  | 'INSPECTION_COORDINATOR'
  | 'INSPECTOR'
  | 'ASSET_RESPONSIBLE'
  | 'VERIFIER'
  | 'CERTIFICATION_OFFICER';

export interface SignedIn {
  person: Person;
  role: StaffRole;
  expiresAt: string;
}

export interface AuthenticationPort {
  current(): SignedIn | undefined;
  logIn(personId: string, password: string): Promise<SignedIn>;
  logOut(): Promise<void>;
  onSignedOut(listener: () => void): () => void;
}
