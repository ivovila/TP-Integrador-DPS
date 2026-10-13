import type { Ineligibility } from '../domain/certification';

export const domainErrorNames = [
  'AssetAlreadyRegisteredException',
  'AssetNotFoundException',
  'DuplicateCharacteristicException',
  'DuplicateSubsystemCodeException',
  'AssetAlreadyCertifiedException',
  'CertificateAlreadyRenewedException',
  'CertificateNotActiveException',
  'CertificateNotFoundException',
  'InspectionNotClosedForCertificationException',
  'InspectionOfAnotherAssetException',
  'JurisdictionPolicyNotFoundException',
  'NotAGlobalCertificateException',
  'NotEligibleForCertificationException',
  'SubsystemNotFoundException',
  'RenewalRequiresLaterInspectionException',
  'RenewalScopeMismatchException',
  'AssetLacksRequiredSubsystemsException',
  'AssetResponsibleCannotAssignInspectionException',
  'AssetResponsibleCannotInspectOwnAssetException',
  'AssignerCannotInspectException',
  'CorrectiveActionAlreadyClosedException',
  'CorrectiveActionNotInFindingException',
  'CriterionOutOfScopeException',
  'DueDateInThePastException',
  'DuplicateEvidenceException',
  'FindingAlreadyClosedException',
  'FindingNotFoundException',
  'InspectionAlreadyClosedException',
  'InspectionIncompleteException',
  'InspectionNotClosedException',
  'InspectionNotFoundException',
  'RectificationReasonRequiredException',
  'VerificationNotesRequiredException',
  'VerifierMustDifferFromResponsibleException',
  'AnswerTypeMismatchException',
  'CriterionNotInSchemaException',
  'InvalidRuleConfigurationException',
  'InvalidSchemaException',
  'NoEffectiveSchemaVersionException',
  'NoInspectionSchemaForAssetTypeException',
  'SchemaEffectiveDateConflictException',
  'SchemaAlreadyDefinedForAssetTypeException',
  'SectionNotInSchemaException',
  'RequiredValueMissingException',
  'InvalidPageRequestException',
  'FindingRequiresNonConformityException',
  'InvalidValidityPeriodException',
] as const;

export type DomainErrorName = (typeof domainErrorNames)[number];

export function isDomainErrorName(candidate: unknown): candidate is DomainErrorName {
  return domainErrorNames.some((name) => name === candidate);
}

export class DomainError extends Error {
  override readonly name: DomainErrorName;
  readonly ineligibilities: Ineligibility[];

  constructor(name: DomainErrorName, message: string, ineligibilities: Ineligibility[] = []) {
    super(message);
    this.name = name;
    this.ineligibilities = ineligibilities;
  }
}
