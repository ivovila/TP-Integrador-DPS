import type { CertificateAction, CertificationDecisionAction } from '../../domain/certification';
import type { CorrectiveActionAction, FindingAction } from '../../domain/finding';
import type { InspectionAction } from '../../domain/inspection';
import { hasLink } from './HalClient';
import type { WireResource } from './wire';

type ActionByRel<Action extends string> = [rel: string, action: Action][];

export const inspectionActionByRel: ActionByRel<InspectionAction> = [
  ['closure', 'closeInspection'],
  ['rectifications', 'rectifyInspection'],
  ['act', 'generateInspectionAct'],
];

export const responseActionByRel: ActionByRel<InspectionAction> = [
  ['answer', 'recordAnswer'],
  ['evidence', 'attachEvidence'],
  ['observations', 'addObservation'],
];

export const eligibilityActionByRel: ActionByRel<CertificationDecisionAction> = [['certificates', 'issueCertificate']];

export const findingActionByRel: ActionByRel<FindingAction> = [['corrective-actions', 'planCorrectiveAction']];

export const correctiveActionActionByRel: ActionByRel<CorrectiveActionAction> = [['verifications', 'verifyCorrectiveAction']];

export const certificateActionByRel: ActionByRel<CertificateAction> = [
  ['suspensions', 'suspendCertificate'],
  ['renewals', 'renewCertificate'],
  ['global-renewals', 'renewGlobalCertificate'],
];

export function actionsOfferedBy<Action extends string>(resource: WireResource, actionByRel: ActionByRel<Action>): Action[] {
  return actionByRel.filter(([rel]) => hasLink(resource, rel)).map(([, action]) => action);
}
