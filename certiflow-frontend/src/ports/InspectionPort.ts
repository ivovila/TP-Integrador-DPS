import type { Evidence, InspectionAct, InspectionDetails, InspectionScope, InspectionSummary } from '../domain/inspection';
import type { RawAnswer } from '../domain/schema';
import type { LocalDate, Person } from '../domain/shared';

export interface AssignInspectionCommand {
  assetCode: string;
  inspector: Person;
  plannedDate: LocalDate;
  scope: InspectionScope;
  assignedBy: Person;
}

export interface RecordAnswerCommand {
  inspectionId: string;
  criterionCode: string;
  answer: RawAnswer;
  answeredBy: Person;
}

export interface AttachEvidenceCommand {
  inspectionId: string;
  criterionCode: string;
  evidence: Evidence;
  attachedBy: Person;
}

export interface AddObservationCommand {
  inspectionId: string;
  criterionCode: string;
  text: string;
  observedBy: Person;
}

export interface CloseInspectionCommand {
  inspectionId: string;
  closedBy: Person;
}

export interface RectifyInspectionCommand {
  inspectionId: string;
  criterionCode: string;
  answer: RawAnswer;
  reason: string;
  rectifiedBy: Person;
}

export interface InspectionFilter {
  assetCode?: string;
  status?: 'open' | 'closed';
}

export interface InspectionPort {
  assignInspection(command: AssignInspectionCommand): Promise<string>;
  listInspections(filter: InspectionFilter): Promise<InspectionSummary[]>;
  findInspection(inspectionId: string): Promise<InspectionDetails>;
  recordAnswer(command: RecordAnswerCommand): Promise<void>;
  attachEvidence(command: AttachEvidenceCommand): Promise<void>;
  addObservation(command: AddObservationCommand): Promise<void>;
  closeInspection(command: CloseInspectionCommand): Promise<string[]>;
  rectifyInspection(command: RectifyInspectionCommand): Promise<string[]>;
  generateInspectionAct(inspectionId: string): Promise<InspectionAct>;
}
