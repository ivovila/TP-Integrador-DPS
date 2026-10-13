package ar.edu.itba.certiflow.web.config;

import ar.edu.itba.certiflow.application.catalog.FindAssetUseCaseImpl;
import ar.edu.itba.certiflow.application.catalog.ListAssetsUseCaseImpl;
import ar.edu.itba.certiflow.application.catalog.RegisterAssetUseCaseImpl;
import ar.edu.itba.certiflow.application.certification.CertificateIssuanceService;
import ar.edu.itba.certiflow.application.certification.CertificationAssessmentService;
import ar.edu.itba.certiflow.application.certification.EvaluateCertificationUseCaseImpl;
import ar.edu.itba.certiflow.application.certification.FindCertificateUseCaseImpl;
import ar.edu.itba.certiflow.application.certification.GenerateCertificateDocumentUseCaseImpl;
import ar.edu.itba.certiflow.application.certification.GlobalCertificationService;
import ar.edu.itba.certiflow.application.certification.InspectionResultTranslatorImpl;
import ar.edu.itba.certiflow.application.certification.IssueCertificateUseCaseImpl;
import ar.edu.itba.certiflow.application.certification.IssueGlobalCertificateUseCaseImpl;
import ar.edu.itba.certiflow.application.certification.ListCertificatesUseCaseImpl;
import ar.edu.itba.certiflow.application.certification.RenewCertificateUseCaseImpl;
import ar.edu.itba.certiflow.application.certification.RenewGlobalCertificateUseCaseImpl;
import ar.edu.itba.certiflow.application.certification.SuspendCertificateUseCaseImpl;
import ar.edu.itba.certiflow.application.inspection.AddObservationUseCaseImpl;
import ar.edu.itba.certiflow.application.inspection.AssignInspectionUseCaseImpl;
import ar.edu.itba.certiflow.application.inspection.AttachEvidenceUseCaseImpl;
import ar.edu.itba.certiflow.application.inspection.CloseInspectionUseCaseImpl;
import ar.edu.itba.certiflow.application.inspection.FindFindingUseCaseImpl;
import ar.edu.itba.certiflow.application.inspection.FindInspectionUseCaseImpl;
import ar.edu.itba.certiflow.application.inspection.GenerateFindingsSummaryUseCaseImpl;
import ar.edu.itba.certiflow.application.inspection.GenerateInspectionActUseCaseImpl;
import ar.edu.itba.certiflow.application.inspection.ListFindingsUseCaseImpl;
import ar.edu.itba.certiflow.application.inspection.ListInspectionsUseCaseImpl;
import ar.edu.itba.certiflow.application.inspection.PlanCorrectiveActionUseCaseImpl;
import ar.edu.itba.certiflow.application.inspection.RecordAnswerUseCaseImpl;
import ar.edu.itba.certiflow.application.inspection.RectifyInspectionUseCaseImpl;
import ar.edu.itba.certiflow.application.inspection.VerifyCorrectiveActionUseCaseImpl;
import ar.edu.itba.certiflow.application.schema.CreateInspectionSchemaUseCaseImpl;
import ar.edu.itba.certiflow.application.schema.FindEffectiveSchemaVersionUseCaseImpl;
import ar.edu.itba.certiflow.application.schema.FindSchemaUseCaseImpl;
import ar.edu.itba.certiflow.application.schema.PublishSchemaVersionUseCaseImpl;
import ar.edu.itba.certiflow.application.shared.AuditHistoryUseCaseImpl;
import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.catalog.model.AssetService;
import ar.edu.itba.certiflow.catalog.model.AssetType;
import ar.edu.itba.certiflow.catalog.model.JurisdictionCode;
import ar.edu.itba.certiflow.catalog.repository.AssetRepository;
import ar.edu.itba.certiflow.catalog.usecase.FindAssetUseCase;
import ar.edu.itba.certiflow.catalog.usecase.ListAssetsUseCase;
import ar.edu.itba.certiflow.catalog.usecase.RegisterAssetUseCase;
import ar.edu.itba.certiflow.certification.model.AllRequiredSubsystemsPolicy;
import ar.edu.itba.certiflow.certification.model.CertificateFactory;
import ar.edu.itba.certiflow.certification.model.CertificateNumber;
import ar.edu.itba.certiflow.certification.model.CertificateService;
import ar.edu.itba.certiflow.application.certification.ListJurisdictionsUseCaseImpl;
import ar.edu.itba.certiflow.application.schema.ListSchemasUseCaseImpl;
import ar.edu.itba.certiflow.certification.model.CertificationPolicyResolver;
import ar.edu.itba.certiflow.certification.model.RegisteredJurisdictions;
import ar.edu.itba.certiflow.certification.usecase.ListJurisdictionsUseCase;
import ar.edu.itba.certiflow.schema.usecase.ListSchemasUseCase;
import ar.edu.itba.certiflow.certification.model.SeverityCertificationPolicy;
import ar.edu.itba.certiflow.certification.model.InspectionResultTranslator;
import ar.edu.itba.certiflow.certification.repository.CertificateNumbering;
import ar.edu.itba.certiflow.certification.repository.CertificateRepository;
import ar.edu.itba.certiflow.certification.usecase.EvaluateCertificationUseCase;
import ar.edu.itba.certiflow.certification.usecase.FindCertificateUseCase;
import ar.edu.itba.certiflow.certification.usecase.GenerateCertificateDocumentUseCase;
import ar.edu.itba.certiflow.certification.usecase.IssueCertificateUseCase;
import ar.edu.itba.certiflow.certification.usecase.IssueGlobalCertificateUseCase;
import ar.edu.itba.certiflow.certification.usecase.ListCertificatesUseCase;
import ar.edu.itba.certiflow.certification.usecase.RenewCertificateUseCase;
import ar.edu.itba.certiflow.certification.usecase.RenewGlobalCertificateUseCase;
import ar.edu.itba.certiflow.certification.usecase.SuspendCertificateUseCase;
import ar.edu.itba.certiflow.infrastructure.certification.persistence.CertificationTransactionLocks;
import ar.edu.itba.certiflow.infrastructure.certification.policy.ConfiguredPolicyResolver;
import ar.edu.itba.certiflow.infrastructure.shared.persistence.JpaAuditLog;
import ar.edu.itba.certiflow.infrastructure.shared.persistence.SpringDataAuditEntryRepository;
import ar.edu.itba.certiflow.inspection.model.FindingFactory;
import ar.edu.itba.certiflow.inspection.model.FindingId;
import ar.edu.itba.certiflow.inspection.model.FindingService;
import ar.edu.itba.certiflow.inspection.model.InspectionFactory;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.inspection.model.InspectionService;
import ar.edu.itba.certiflow.inspection.repository.FindingRepository;
import ar.edu.itba.certiflow.inspection.repository.InspectionRepository;
import ar.edu.itba.certiflow.inspection.usecase.AddObservationUseCase;
import ar.edu.itba.certiflow.inspection.usecase.AssignInspectionUseCase;
import ar.edu.itba.certiflow.inspection.usecase.AttachEvidenceUseCase;
import ar.edu.itba.certiflow.inspection.usecase.CloseInspectionUseCase;
import ar.edu.itba.certiflow.inspection.usecase.FindFindingUseCase;
import ar.edu.itba.certiflow.inspection.usecase.FindInspectionUseCase;
import ar.edu.itba.certiflow.inspection.usecase.GenerateFindingsSummaryUseCase;
import ar.edu.itba.certiflow.inspection.usecase.GenerateInspectionActUseCase;
import ar.edu.itba.certiflow.inspection.usecase.ListFindingsUseCase;
import ar.edu.itba.certiflow.inspection.usecase.ListInspectionsUseCase;
import ar.edu.itba.certiflow.inspection.usecase.PlanCorrectiveActionUseCase;
import ar.edu.itba.certiflow.inspection.usecase.RecordAnswerUseCase;
import ar.edu.itba.certiflow.inspection.usecase.RectifyInspectionUseCase;
import ar.edu.itba.certiflow.inspection.usecase.VerifyCorrectiveActionUseCase;
import ar.edu.itba.certiflow.schema.model.InspectionSchemaService;
import ar.edu.itba.certiflow.schema.repository.InspectionSchemaRepository;
import ar.edu.itba.certiflow.schema.usecase.CreateInspectionSchemaUseCase;
import ar.edu.itba.certiflow.schema.usecase.FindEffectiveSchemaVersionUseCase;
import ar.edu.itba.certiflow.schema.usecase.FindSchemaUseCase;
import ar.edu.itba.certiflow.schema.usecase.PublishSchemaVersionUseCase;
import ar.edu.itba.certiflow.shared.model.AuditLog;
import ar.edu.itba.certiflow.shared.usecase.AuditHistoryUseCase;
import java.time.Clock;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
public class UseCaseConfiguration {
    private final CertificationTransactionLocks locks;
    public UseCaseConfiguration(CertificationTransactionLocks locks) {
        this.locks = locks;
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    AuditLog<AssetCode> assetLog(SpringDataAuditEntryRepository entries) {
        return new JpaAuditLog<>(entries, "asset", AssetCode::value);
    }

    @Bean
    AuditLog<AssetType> schemaLog(SpringDataAuditEntryRepository entries) {
        return new JpaAuditLog<>(entries, "schema", AssetType::name);
    }

    @Bean
    AuditLog<InspectionId> inspectionLog(SpringDataAuditEntryRepository entries) {
        return new JpaAuditLog<>(entries, "inspection", InspectionId::value);
    }

    @Bean
    AuditLog<FindingId> findingLog(SpringDataAuditEntryRepository entries) {
        return new JpaAuditLog<>(entries, "finding", FindingId::value);
    }

    @Bean
    AuditLog<CertificateNumber> certificateLog(SpringDataAuditEntryRepository entries) {
        return new JpaAuditLog<>(entries, "certificate", CertificateNumber::value);
    }

    @Bean
    AssetService assetService(AuditLog<AssetCode> assetLog) {
        return new AssetService(assetLog);
    }

    @Bean
    InspectionSchemaService inspectionSchemaService(AuditLog<AssetType> schemaLog) {
        return new InspectionSchemaService(schemaLog);
    }

    @Bean
    InspectionService inspectionService(AuditLog<InspectionId> inspectionLog) {
        return new InspectionService(new InspectionFactory(), inspectionLog);
    }

    @Bean
    FindingService findingService(AuditLog<FindingId> findingLog) {
        return new FindingService(new FindingFactory(), findingLog);
    }

    @Bean
    CertificateService certificateService(AuditLog<CertificateNumber> certificateLog) {
        return new CertificateService(new CertificateFactory(), certificateLog);
    }

    @Bean
    ConfiguredPolicyResolver certificationPolicies() {
        return new ConfiguredPolicyResolver(Map.of(
                new JurisdictionCode("DEMO-STRICT"), SeverityCertificationPolicy.strict(),
                new JurisdictionCode("DEMO-CONDITIONAL"), SeverityCertificationPolicy.conditional(),
                new JurisdictionCode("DEMO-STANDARD"), SeverityCertificationPolicy.standard()));
    }

    @Bean
    CertificationAssessmentService certificationAssessment(AssetRepository assets,
            CertificationPolicyResolver policies, Clock clock) {
        return new CertificationAssessmentService(assets, policies, clock);
    }

    @Bean
    InspectionResultTranslator inspectionResultTranslator(InspectionRepository inspectionRepository, FindingRepository findingRepository, AssetRepository assetRepository) {
        return new InspectionResultTranslatorImpl(inspectionRepository, findingRepository, assetRepository);
    }

    @Bean
    RegisterAssetUseCase registerAsset(TransactionTemplate tx, AssetRepository assetRepository, AssetService assetService,
                                       Clock clock, CertificationPolicyResolver policies) {
        return TransactionalUseCases.wrap(RegisterAssetUseCase.class, new RegisterAssetUseCaseImpl(assetRepository, assetService, policies, clock), tx, locks);
    }

    @Bean
    CreateInspectionSchemaUseCase createInspectionSchema(TransactionTemplate tx,
                                                         InspectionSchemaRepository inspectionSchemaRepository,
                                                         InspectionSchemaService inspectionSchemaService, Clock clock) {
        return TransactionalUseCases.wrap(CreateInspectionSchemaUseCase.class, new CreateInspectionSchemaUseCaseImpl(inspectionSchemaRepository, inspectionSchemaService, clock), tx, locks);
    }

    @Bean
    PublishSchemaVersionUseCase publishSchemaVersion(TransactionTemplate tx,
                                                     InspectionSchemaRepository inspectionSchemaRepository,
                                                     InspectionSchemaService inspectionSchemaService, Clock clock) {
        return TransactionalUseCases.wrap(PublishSchemaVersionUseCase.class, new PublishSchemaVersionUseCaseImpl(inspectionSchemaRepository, inspectionSchemaService, clock), tx, locks);
    }

    @Bean
    AssignInspectionUseCase assignInspection(TransactionTemplate tx, AssetRepository assetRepository,
                                             InspectionSchemaRepository inspectionSchemaRepository,
                                             InspectionRepository inspectionRepository, InspectionService inspectionService,
                                             Clock clock) {
        return TransactionalUseCases.wrap(AssignInspectionUseCase.class, new AssignInspectionUseCaseImpl(assetRepository, inspectionSchemaRepository, inspectionRepository, inspectionService, clock), tx, locks);
    }

    @Bean
    RecordAnswerUseCase recordAnswer(TransactionTemplate tx, InspectionRepository inspectionRepository,
                                     InspectionService inspectionService, Clock clock) {
        return TransactionalUseCases.wrap(RecordAnswerUseCase.class, new RecordAnswerUseCaseImpl(inspectionRepository, inspectionService, clock), tx, locks);
    }

    @Bean
    AttachEvidenceUseCase attachEvidence(TransactionTemplate tx, InspectionRepository inspectionRepository,
                                         InspectionService inspectionService, Clock clock) {
        return TransactionalUseCases.wrap(AttachEvidenceUseCase.class, new AttachEvidenceUseCaseImpl(inspectionRepository, inspectionService, clock), tx, locks);
    }

    @Bean
    AddObservationUseCase addObservation(TransactionTemplate tx, InspectionRepository inspectionRepository,
                                         InspectionService inspectionService, Clock clock) {
        return TransactionalUseCases.wrap(AddObservationUseCase.class, new AddObservationUseCaseImpl(inspectionRepository, inspectionService, clock), tx, locks);
    }

    @Bean
    CloseInspectionUseCase closeInspection(TransactionTemplate tx, InspectionRepository inspectionRepository,
                                           FindingRepository findingRepository, InspectionService inspectionService,
                                           FindingService findingService, Clock clock) {
        return TransactionalUseCases.wrap(CloseInspectionUseCase.class, new CloseInspectionUseCaseImpl(inspectionRepository, findingRepository, inspectionService, findingService, clock), tx, locks);
    }

    @Bean
    RectifyInspectionUseCase rectifyInspection(TransactionTemplate tx, InspectionRepository inspectionRepository,
                                               FindingRepository findingRepository, InspectionService inspectionService,
                                               FindingService findingService, Clock clock) {
        return TransactionalUseCases.wrap(RectifyInspectionUseCase.class, new RectifyInspectionUseCaseImpl(inspectionRepository, findingRepository, inspectionService, findingService, clock), tx, locks);
    }

    @Bean
    PlanCorrectiveActionUseCase planCorrectiveAction(TransactionTemplate tx, FindingRepository findingRepository,
                                                     FindingService findingService, Clock clock) {
        return TransactionalUseCases.wrap(PlanCorrectiveActionUseCase.class, new PlanCorrectiveActionUseCaseImpl(findingRepository, findingService, clock), tx, locks);
    }

    @Bean
    VerifyCorrectiveActionUseCase verifyCorrectiveAction(TransactionTemplate tx, FindingRepository findingRepository,
                                                         FindingService findingService, Clock clock) {
        return TransactionalUseCases.wrap(VerifyCorrectiveActionUseCase.class, new VerifyCorrectiveActionUseCaseImpl(findingRepository, findingService, clock), tx, locks);
    }

    @Bean
    GenerateInspectionActUseCase generateInspectionAct(TransactionTemplate tx, InspectionRepository inspectionRepository,
                                                       AssetRepository assetRepository, AuditLog<InspectionId> inspectionLog) {
        return TransactionalUseCases.wrap(GenerateInspectionActUseCase.class, new GenerateInspectionActUseCaseImpl(inspectionRepository, assetRepository, inspectionLog), tx, locks);
    }

    @Bean
    GenerateFindingsSummaryUseCase generateFindingsSummary(TransactionTemplate tx, AssetRepository assetRepository,
                                                           FindingRepository findingRepository, Clock clock) {
        return TransactionalUseCases.wrap(GenerateFindingsSummaryUseCase.class, new GenerateFindingsSummaryUseCaseImpl(assetRepository, findingRepository, clock), tx, locks);
    }

    @Bean
    GenerateCertificateDocumentUseCase generateCertificateDocument(TransactionTemplate tx,
                                                                   CertificateRepository certificateRepository,
                                                                   AssetRepository assetRepository, Clock clock) {
        return TransactionalUseCases.wrap(GenerateCertificateDocumentUseCase.class, new GenerateCertificateDocumentUseCaseImpl(certificateRepository, assetRepository, clock), tx, locks);
    }

    @Bean
    CertificateIssuanceService certificateIssuance(InspectionResultTranslator inspectionResultTranslator,
                                                   CertificationAssessmentService certificationAssessment,
                                                   CertificateRepository certificateRepository) {
        return new CertificateIssuanceService(inspectionResultTranslator, certificationAssessment, certificateRepository);
    }

    @Bean
    GlobalCertificationService globalCertification(AssetRepository assetRepository,
                                                   InspectionSchemaRepository inspectionSchemaRepository,
                                                   CertificateRepository certificateRepository, Clock clock) {
        return new GlobalCertificationService(assetRepository, inspectionSchemaRepository, certificateRepository,
                new AllRequiredSubsystemsPolicy(), clock);
    }

    @Bean
    IssueGlobalCertificateUseCase issueGlobalCertificate(TransactionTemplate tx, GlobalCertificationService globalCertification,
                                                         CertificateIssuanceService certificateIssuance,
                                                         CertificateRepository certificateRepository,
                                                         CertificateNumbering certificateNumbering,
                                                         CertificateService certificateService) {
        return TransactionalUseCases.wrap(IssueGlobalCertificateUseCase.class,
                new IssueGlobalCertificateUseCaseImpl(globalCertification, certificateIssuance, certificateRepository,
                        certificateNumbering, certificateService), tx, locks);
    }

    @Bean
    RenewGlobalCertificateUseCase renewGlobalCertificate(TransactionTemplate tx, GlobalCertificationService globalCertification,
                                                         CertificateIssuanceService certificateIssuance,
                                                         CertificateRepository certificateRepository,
                                                         CertificateNumbering certificateNumbering,
                                                         CertificateService certificateService) {
        return TransactionalUseCases.wrap(RenewGlobalCertificateUseCase.class,
                new RenewGlobalCertificateUseCaseImpl(globalCertification, certificateIssuance, certificateRepository,
                        certificateNumbering, certificateService), tx, locks);
    }

    @Bean
    IssueCertificateUseCase issueCertificate(TransactionTemplate tx, CertificateIssuanceService certificateIssuance,
                                             CertificateRepository certificateRepository,
                                             CertificateNumbering certificateNumbering,
                                             CertificateService certificateService) {
        return TransactionalUseCases.wrap(IssueCertificateUseCase.class, new IssueCertificateUseCaseImpl(certificateIssuance,
                certificateRepository, certificateNumbering, certificateService), tx, locks);
    }

    @Bean
    EvaluateCertificationUseCase evaluateCertification(TransactionTemplate tx, CertificateIssuanceService certificateIssuance) {
        return TransactionalUseCases.wrap(EvaluateCertificationUseCase.class,
                new EvaluateCertificationUseCaseImpl(certificateIssuance), tx, locks);
    }

    @Bean
    SuspendCertificateUseCase suspendCertificate(TransactionTemplate tx, CertificateRepository certificateRepository,
                                                 CertificateService certificateService, Clock clock) {
        return TransactionalUseCases.wrap(SuspendCertificateUseCase.class, new SuspendCertificateUseCaseImpl(certificateRepository, certificateService, clock), tx, locks);
    }

    @Bean
    RenewCertificateUseCase renewCertificate(TransactionTemplate tx, CertificateIssuanceService certificateIssuance,
                                             CertificateRepository certificateRepository,
                                             CertificateNumbering certificateNumbering,
                                             CertificateService certificateService) {
        return TransactionalUseCases.wrap(RenewCertificateUseCase.class, new RenewCertificateUseCaseImpl(certificateIssuance,
                certificateRepository, certificateNumbering, certificateService), tx, locks);
    }

    @Bean
    FindAssetUseCase findAsset(TransactionTemplate tx, AssetRepository assetRepository) {
        return TransactionalUseCases.wrap(FindAssetUseCase.class, new FindAssetUseCaseImpl(assetRepository), tx, locks);
    }

    @Bean
    ListAssetsUseCase listAssets(TransactionTemplate tx, AssetRepository assetRepository) {
        return TransactionalUseCases.wrap(ListAssetsUseCase.class, new ListAssetsUseCaseImpl(assetRepository), tx, locks);
    }

    @Bean
    FindEffectiveSchemaVersionUseCase findEffectiveSchemaVersion(TransactionTemplate tx,
            InspectionSchemaRepository schemas) {
        return TransactionalUseCases.wrap(FindEffectiveSchemaVersionUseCase.class,
                new FindEffectiveSchemaVersionUseCaseImpl(schemas), tx, locks);
    }

    @Bean
    ListSchemasUseCase listSchemas(TransactionTemplate tx, InspectionSchemaRepository inspectionSchemaRepository) {
        return TransactionalUseCases.wrap(ListSchemasUseCase.class, new ListSchemasUseCaseImpl(inspectionSchemaRepository), tx, locks);
    }

    @Bean
    ListJurisdictionsUseCase listJurisdictions(RegisteredJurisdictions registeredJurisdictions) {
        return new ListJurisdictionsUseCaseImpl(registeredJurisdictions);
    }

    @Bean
    FindSchemaUseCase findSchema(TransactionTemplate tx, InspectionSchemaRepository inspectionSchemaRepository) {
        return TransactionalUseCases.wrap(FindSchemaUseCase.class, new FindSchemaUseCaseImpl(inspectionSchemaRepository), tx, locks);
    }

    @Bean
    FindInspectionUseCase findInspection(TransactionTemplate tx, InspectionRepository inspectionRepository) {
        return TransactionalUseCases.wrap(FindInspectionUseCase.class, new FindInspectionUseCaseImpl(inspectionRepository), tx, locks);
    }

    @Bean
    FindFindingUseCase findFinding(TransactionTemplate tx, FindingRepository findingRepository) {
        return TransactionalUseCases.wrap(FindFindingUseCase.class, new FindFindingUseCaseImpl(findingRepository), tx, locks);
    }

    @Bean
    ListFindingsUseCase listFindings(TransactionTemplate tx, FindingRepository findingRepository) {
        return TransactionalUseCases.wrap(ListFindingsUseCase.class, new ListFindingsUseCaseImpl(findingRepository), tx, locks);
    }

    @Bean
    ListCertificatesUseCase listCertificates(TransactionTemplate tx, CertificateRepository certificateRepository) {
        return TransactionalUseCases.wrap(ListCertificatesUseCase.class, new ListCertificatesUseCaseImpl(certificateRepository), tx, locks);
    }

    @Bean
    FindCertificateUseCase findCertificate(TransactionTemplate tx, CertificateRepository certificateRepository) {
        return TransactionalUseCases.wrap(FindCertificateUseCase.class, new FindCertificateUseCaseImpl(certificateRepository), tx, locks);
    }

    @Bean
    AuditHistoryUseCase auditHistory(TransactionTemplate tx, AuditLog<AssetCode> assetLog, AuditLog<AssetType> schemaLog,
                                     AuditLog<InspectionId> inspectionLog, AuditLog<FindingId> findingLog,
                                     AuditLog<CertificateNumber> certificateLog) {
        return TransactionalUseCases.wrap(AuditHistoryUseCase.class, new AuditHistoryUseCaseImpl(assetLog, schemaLog, inspectionLog, findingLog, certificateLog), tx, locks);
    }

    @Bean
    ListInspectionsUseCase listInspections(TransactionTemplate tx, InspectionRepository inspectionRepository) {
        return TransactionalUseCases.wrap(ListInspectionsUseCase.class, new ListInspectionsUseCaseImpl(inspectionRepository), tx, locks);
    }
}
