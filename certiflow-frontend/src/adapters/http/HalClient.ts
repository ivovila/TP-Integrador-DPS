import type { Person } from '../../domain/shared';
import { DomainError, isDomainErrorName } from '../../ports/DomainError';
import { RejectedRequestError } from '../../ports/RejectedRequestError';
import type { CredentialSource } from './CredentialSource';
import type { WireCollection, WireLink, WireProblem, WireResource } from './wire';

export type FetchFunction = (input: string, init?: RequestInit) => Promise<Response>;

const acceptedMediaTypes = 'application/hal+json, application/problem+json';

export function pathOf(href: string): string {
  const withoutTemplateVariables = href.replace(/\{[^}]*\}/g, '');
  const location = new URL(withoutTemplateVariables, 'http://same-origin');
  return location.pathname + location.search;
}

export function linksNamed(resource: WireResource, rel: string): WireLink[] {
  const linked = resource._links?.[rel];
  if (linked === undefined) {
    return [];
  }
  return Array.isArray(linked) ? linked : [linked];
}

export function hasLink(resource: WireResource, rel: string): boolean {
  return linksNamed(resource, rel).length > 0;
}

export function lastSegmentOf(href: string): string {
  const segments = pathOf(href).split('?')[0]!.split('/');
  return decodeURIComponent(segments[segments.length - 1]!);
}

export function segment(value: string | number): string {
  return encodeURIComponent(String(value));
}

export function query(parameters: Record<string, string | undefined>): string {
  const present = Object.entries(parameters).filter((entry): entry is [string, string] => entry[1] !== undefined);
  return present.length === 0 ? '' : `?${new URLSearchParams(present).toString()}`;
}

export class SerializedJson {
  constructor(readonly text: string) {}
}

async function rejectionOf(response: Response): Promise<Error> {
  const problem: WireProblem = await response.json().catch(() => ({}));
  const detail = problem.detail ?? `The server answered ${response.status}`;
  return isDomainErrorName(problem.title)
    ? new DomainError(problem.title, detail, problem.ineligibilities ?? [])
    : new RejectedRequestError(response.status, problem.title ?? response.statusText, detail);
}

export class HalClient {
  constructor(
    private readonly baseUrl: string,
    private readonly credentials: CredentialSource,
    private readonly fetchFunction: FetchFunction = (input, init) => fetch(input, init),
  ) {}

  read<Body>(path: string): Promise<Body> {
    return this.request<Body>('GET', path, undefined, undefined);
  }

  async readAll<Item>(path: string, collectionName: string): Promise<Item[]> {
    const items: Item[] = [];
    let nextPath: string | undefined = path;
    while (nextPath !== undefined) {
      const page: WireCollection = await this.read<WireCollection>(nextPath);
      items.push(...((page._embedded?.[collectionName] ?? []) as Item[]));
      const nextLink: WireLink | undefined = linksNamed(page, 'next')[0];
      nextPath = nextLink === undefined ? undefined : pathOf(nextLink.href);
    }
    return items;
  }

  write<Body>(method: 'POST' | 'PUT', path: string, actingPerson: Person, body?: unknown): Promise<Body> {
    return this.request<Body>(method, path, actingPerson, body);
  }

  private async request<Body>(method: string, path: string, actingPerson: Person | undefined, body: unknown): Promise<Body> {
    const credentialHeaders = await this.credentials.headersFor(actingPerson, pathOf(path));
    const serializedBody = body instanceof SerializedJson ? body.text : body === undefined ? undefined : JSON.stringify(body);
    let response: Response;
    try {
      response = await this.fetchFunction(this.baseUrl + pathOf(path), {
        method,
        headers: {
          Accept: acceptedMediaTypes,
          ...credentialHeaders,
          ...(serializedBody === undefined ? {} : { 'Content-Type': 'application/json' }),
        },
        ...(serializedBody === undefined ? {} : { body: serializedBody }),
      });
    } catch {
      throw new RejectedRequestError(0, 'ServerUnreachable', 'The server could not be reached. Check that the backend is running.');
    }
    this.credentials.received(response, actingPerson, pathOf(path));
    if (response.status === 401) {
      this.credentials.rejected(credentialHeaders);
    }
    if (!response.ok) {
      throw await rejectionOf(response);
    }
    const text = await response.text();
    return (text === '' ? undefined : JSON.parse(text)) as Body;
  }
}
