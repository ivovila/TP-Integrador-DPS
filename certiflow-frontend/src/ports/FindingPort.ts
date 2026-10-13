import type { Finding, FindingsSummary, Verification, VerificationResult } from '../domain/finding';
import type { LocalDate, Person } from '../domain/shared';

export interface PlanCorrectiveActionCommand {
  findingId: string;
  description: string;
  responsible: Person;
  dueDate: LocalDate;
  plannedBy: Person;
}

export interface VerifyCorrectiveActionCommand {
  findingId: string;
  actionId: number;
  verifier: Person;
  result: VerificationResult;
  notes: string;
}

export interface FindingFilter {
  assetCode?: string;
}

export interface FindingPort {
  listFindings(filter: FindingFilter): Promise<Finding[]>;
  findFinding(findingId: string): Promise<Finding>;
  planCorrectiveAction(command: PlanCorrectiveActionCommand): Promise<number>;
  verifyCorrectiveAction(command: VerifyCorrectiveActionCommand): Promise<Verification>;
  generateFindingsSummary(assetCode: string): Promise<FindingsSummary>;
}
