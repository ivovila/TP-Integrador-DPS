import type { Ineligibility } from '../domain/certification';

export type DomainErrorName =
  | 'AssetAlreadyRegisteredException'
  | 'AssetNotFoundException'
  | 'DuplicateCharacteristicException'
  | 'AssetAlreadyCertifiedException'
  | 'CertificateAlreadyRenewedException'
  | 'CertificateNotActiveException'
  | 'CertificateNotFoundException'
  | 'CertificationConflictException'
  | 'InspectionOfAnotherAssetException'
  | 'JurisdictionPolicyNotFoundException'
  | 'NotEligibleForCertificationException'
  | 'SubsystemNotFoundException'
  | 'RenewalRequiresLaterInspectionException'
  | 'AssetResponsibleCannotAssignInspectionException'
  | 'AssetResponsibleCannotInspectOwnAssetException'
  | 'AssignerCannotInspectException'
  | 'CorrectiveActionAlreadyClosedException'
  | 'CorrectiveActionNotInFindingException'
  | 'CriterionOutOfScopeException'
  | 'DueDateInThePastException'
  | 'DuplicateEvidenceException'
  | 'FindingAlreadyClosedException'
  | 'FindingNotFoundException'
  | 'InspectionAlreadyClosedException'
  | 'InspectionIncompleteException'
  | 'InspectionNotClosedException'
  | 'InspectionNotFoundException'
  | 'RectificationReasonRequiredException'
  | 'VerificationNotesRequiredException'
  | 'VerifierMustDifferFromResponsibleException'
  | 'AnswerTypeMismatchException'
  | 'CriterionNotInSchemaException'
  | 'InvalidRuleConfigurationException'
  | 'InvalidSchemaException'
  | 'NoEffectiveSchemaVersionException'
  | 'NoInspectionSchemaForAssetTypeException'
  | 'SchemaEffectiveDateConflictException'
  | 'SchemaAlreadyDefinedForAssetTypeException'
  | 'SectionNotInSchemaException'
  | 'RequiredValueMissingException';

export class DomainError extends Error {
  override readonly name: DomainErrorName;
  readonly ineligibilities: Ineligibility[];

  constructor(name: DomainErrorName, message: string, ineligibilities: Ineligibility[] = []) {
    super(message);
    this.name = name;
    this.ineligibilities = ineligibilities;
  }
}
