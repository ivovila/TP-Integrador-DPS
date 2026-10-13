export class RejectedRequestError extends Error {
  readonly status: number;
  readonly title: string;

  constructor(status: number, title: string, detail: string) {
    super(detail);
    this.name = 'RejectedRequestError';
    this.status = status;
    this.title = title;
  }
}
