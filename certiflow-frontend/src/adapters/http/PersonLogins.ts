import type { Person } from '../../domain/shared';
import { basicCredentials, bearerTokenOf, type CredentialSource } from './CredentialSource';

export class PersonLogins implements CredentialSource {
  private readonly tokensByPersonId = new Map<string, string>();

  constructor(
    private readonly password: string,
    private readonly usernameOf: (person: Person) => string,
    private readonly readerFor: (path: string) => Person,
  ) {}

  async headersFor(actingPerson: Person | undefined, path: string): Promise<Record<string, string>> {
    const person = actingPerson ?? this.readerFor(path);
    const token = this.tokensByPersonId.get(person.id);
    return { Authorization: token === undefined ? basicCredentials(this.usernameOf(person), this.password) : `Bearer ${token}` };
  }

  received(response: Response, actingPerson: Person | undefined, path: string): void {
    const token = bearerTokenOf(response);
    if (token !== undefined) {
      this.tokensByPersonId.set((actingPerson ?? this.readerFor(path)).id, token);
    }
  }

  rejected(): void {
    this.tokensByPersonId.clear();
  }
}
