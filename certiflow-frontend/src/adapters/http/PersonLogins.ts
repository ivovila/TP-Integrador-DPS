import type { Person } from '../../domain/shared';
import { basicCredentials, bearerTokenOf, type CredentialSource } from './CredentialSource';

export class PersonLogins implements CredentialSource {
  private readonly tokensByPersonId = new Map<string, string>();

  constructor(
    private readonly password: string,
    private readonly reader: Person,
  ) {}

  async headersFor(actingPerson: Person | undefined): Promise<Record<string, string>> {
    const person = actingPerson ?? this.reader;
    const token = this.tokensByPersonId.get(person.id);
    return { Authorization: token === undefined ? basicCredentials(person.id, this.password) : `Bearer ${token}` };
  }

  received(response: Response, actingPerson: Person | undefined): void {
    const token = bearerTokenOf(response);
    if (token !== undefined) {
      this.tokensByPersonId.set((actingPerson ?? this.reader).id, token);
    }
  }

  rejected(): void {
    this.tokensByPersonId.clear();
  }
}
