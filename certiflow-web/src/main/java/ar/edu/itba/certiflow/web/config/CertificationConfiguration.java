package ar.edu.itba.certiflow.web.config;

import ar.edu.itba.certiflow.application.certification.CertificateHistoryUseCaseImpl;
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
import ar.edu.itba.certiflow.application.certification.ListJurisdictionsUseCaseImpl;
import ar.edu.itba.certiflow.application.certification.RenewCertificateUseCaseImpl;
import ar.edu.itba.certiflow.application.certification.RenewGlobalCertificateUseCaseImpl;
import ar.edu.itba.certiflow.application.certification.SuspendCertificateUseCaseImpl;
import ar.edu.itba.certiflow.catalog.model.JurisdictionCode;
import ar.edu.itba.certiflow.catalog.repository.AssetLock;
import ar.edu.itba.certiflow.catalog.repository.AssetRepository;
import ar.edu.itba.certiflow.certification.model.AllRequiredSubsystemsPolicy;
import ar.edu.itba.certiflow.certification.model.CertificateFactory;
import ar.edu.itba.certiflow.certification.model.CertificateNumber;
import ar.edu.itba.certiflow.certification.model.CertificateService;
import ar.edu.itba.certiflow.certification.model.CertificationEligibility;
import ar.edu.itba.certiflow.certification.model.CertificationPolicyResolver;
import ar.edu.itba.certiflow.certification.model.ConditionalTreatment;
import ar.edu.itba.certiflow.certification.model.DaysTerm;
import ar.edu.itba.certiflow.certification.model.InspectionResultTranslator;
import ar.edu.itba.certiflow.certification.model.MonthsTerm;
import ar.edu.itba.certiflow.certification.model.PolicyCode;
import ar.edu.itba.certiflow.certification.model.RegisteredJurisdictions;
import ar.edu.itba.certiflow.certification.model.SeverityCertificationPolicy;
import ar.edu.itba.certiflow.certification.model.StandardFindingTreatment;
import ar.edu.itba.certiflow.certification.repository.CertificateNumbering;
import ar.edu.itba.certiflow.certification.repository.CertificateRepository;
import ar.edu.itba.certiflow.certification.usecase.CertificateHistoryUseCase;
import ar.edu.itba.certiflow.certification.usecase.EvaluateCertificationUseCase;
import ar.edu.itba.certiflow.certification.usecase.FindCertificateUseCase;
import ar.edu.itba.certiflow.certification.usecase.GenerateCertificateDocumentUseCase;
import ar.edu.itba.certiflow.certification.usecase.IssueCertificateUseCase;
import ar.edu.itba.certiflow.certification.usecase.IssueGlobalCertificateUseCase;
import ar.edu.itba.certiflow.certification.usecase.ListCertificatesUseCase;
import ar.edu.itba.certiflow.certification.usecase.ListJurisdictionsUseCase;
import ar.edu.itba.certiflow.certification.usecase.RenewCertificateUseCase;
import ar.edu.itba.certiflow.certification.usecase.RenewGlobalCertificateUseCase;
import ar.edu.itba.certiflow.certification.usecase.SuspendCertificateUseCase;
import ar.edu.itba.certiflow.infrastructure.certification.policy.ConfiguredPolicyResolver;
import ar.edu.itba.certiflow.infrastructure.shared.persistence.JpaAuditLog;
import ar.edu.itba.certiflow.infrastructure.shared.persistence.SpringDataAuditEntryRepository;
import ar.edu.itba.certiflow.inspection.repository.FindingRepository;
import ar.edu.itba.certiflow.inspection.repository.InspectionRepository;
import ar.edu.itba.certiflow.schema.model.StandardSeverity;
import ar.edu.itba.certiflow.schema.repository.InspectionSchemaRepository;
import ar.edu.itba.certiflow.shared.model.AuditActionCatalog;
import ar.edu.itba.certiflow.shared.model.AuditLog;
import java.time.Clock;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.support.TransactionTemplate;

/** Casos de uso de certificación: evaluación, emisión, renovación y suspensión. */
@Configuration
public class CertificationConfiguration {

    @Bean
    AuditLog<CertificateNumber> certificateLog(SpringDataAuditEntryRepository entries,
                                               AuditActionCatalog auditActions) {
        return new JpaAuditLog<>(entries, "certificate", CertificateNumber::value, auditActions);
    }

    @Bean
    CertificateService certificateService(AuditLog<CertificateNumber> certificateLog) {
        return new CertificateService(new CertificateFactory(), certificateLog);
    }

    /** El único lugar que asocia una jurisdicción con su política, y por eso también donde se arman las de ejemplo. */
    @Bean
    ConfiguredPolicyResolver certificationPolicies() {
        return new ConfiguredPolicyResolver(Map.of(
                new JurisdictionCode("DEMO-STRICT"), strictPolicy(),
                new JurisdictionCode("DEMO-CONDITIONAL"), conditionalPolicy(),
                new JurisdictionCode("DEMO-STANDARD"), standardPolicy()));
    }

    /** Cualquier hallazgo abierto rechaza; aprueba por 12 meses. */
    private SeverityCertificationPolicy strictPolicy() {
        return new SeverityCertificationPolicy(new PolicyCode("STRICT"),
                Map.of(StandardSeverity.MINOR, StandardFindingTreatment.BLOCKS,
                        StandardSeverity.MAJOR, StandardFindingTreatment.BLOCKS,
                        StandardSeverity.CRITICAL, StandardFindingTreatment.BLOCKS),
                new MonthsTerm(12));
    }

    /** MAJOR y CRITICAL rechazan, MINOR se tolera; aprueba por 12 meses. */
    private SeverityCertificationPolicy standardPolicy() {
        return new SeverityCertificationPolicy(new PolicyCode("STANDARD"),
                Map.of(StandardSeverity.MINOR, StandardFindingTreatment.TOLERATED,
                        StandardSeverity.MAJOR, StandardFindingTreatment.BLOCKS,
                        StandardSeverity.CRITICAL, StandardFindingTreatment.BLOCKS),
                new MonthsTerm(12));
    }

    /** CRITICAL rechaza; MINOR o MAJOR dan un certificado condicional por 30 días; sin hallazgos aprueba por 6 meses. */
    private SeverityCertificationPolicy conditionalPolicy() {
        return new SeverityCertificationPolicy(new PolicyCode("CONDITIONAL"),
                Map.of(StandardSeverity.MINOR, new ConditionalTreatment(new DaysTerm(30)),
                        StandardSeverity.MAJOR, new ConditionalTreatment(new DaysTerm(30)),
                        StandardSeverity.CRITICAL, StandardFindingTreatment.BLOCKS),
                new MonthsTerm(6));
    }

    @Bean
    CertificationEligibility certificationEligibility(CertificationPolicyResolver certificationPolicies) {
        return new CertificationEligibility(certificationPolicies);
    }

    @Bean
    CertificationAssessmentService certificationAssessment(AssetRepository assetRepository,
                                                           CertificationEligibility certificationEligibility,
                                                           Clock clock) {
        return new CertificationAssessmentService(assetRepository, certificationEligibility, clock);
    }

    @Bean
    InspectionResultTranslator inspectionResultTranslator(InspectionRepository inspectionRepository, FindingRepository findingRepository, AssetRepository assetRepository) {
        return new InspectionResultTranslatorImpl(inspectionRepository, findingRepository, assetRepository);
    }

    @Bean
    GenerateCertificateDocumentUseCase generateCertificateDocument(TransactionTemplate tx,
                                                                   CertificateRepository certificateRepository,
                                                                   AssetRepository assetRepository, Clock clock) {
        return TransactionalUseCases.wrap(GenerateCertificateDocumentUseCase.class, new GenerateCertificateDocumentUseCaseImpl(certificateRepository, assetRepository, clock), tx);
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
                                                         CertificateService certificateService, AssetLock assetLock) {
        return TransactionalUseCases.wrap(IssueGlobalCertificateUseCase.class,
                new IssueGlobalCertificateUseCaseImpl(globalCertification, certificateIssuance, certificateRepository,
                        certificateNumbering, certificateService, assetLock), tx);
    }

    @Bean
    RenewGlobalCertificateUseCase renewGlobalCertificate(TransactionTemplate tx, GlobalCertificationService globalCertification,
                                                         CertificateIssuanceService certificateIssuance,
                                                         CertificateRepository certificateRepository,
                                                         CertificateNumbering certificateNumbering,
                                                         CertificateService certificateService, AssetLock assetLock) {
        return TransactionalUseCases.wrap(RenewGlobalCertificateUseCase.class,
                new RenewGlobalCertificateUseCaseImpl(globalCertification, certificateIssuance, certificateRepository,
                        certificateNumbering, certificateService, assetLock), tx);
    }

    @Bean
    IssueCertificateUseCase issueCertificate(TransactionTemplate tx, CertificateIssuanceService certificateIssuance,
                                             CertificateRepository certificateRepository,
                                             CertificateNumbering certificateNumbering,
                                             CertificateService certificateService,
                                             InspectionRepository inspectionRepository, AssetLock assetLock) {
        return TransactionalUseCases.wrap(IssueCertificateUseCase.class, new IssueCertificateUseCaseImpl(certificateIssuance,
                certificateRepository, certificateNumbering, certificateService, inspectionRepository, assetLock), tx);
    }

    @Bean
    EvaluateCertificationUseCase evaluateCertification(TransactionTemplate tx, CertificateIssuanceService certificateIssuance) {
        return TransactionalUseCases.wrap(EvaluateCertificationUseCase.class,
                new EvaluateCertificationUseCaseImpl(certificateIssuance), tx);
    }

    @Bean
    SuspendCertificateUseCase suspendCertificate(TransactionTemplate tx, CertificateRepository certificateRepository,
                                                 CertificateService certificateService, AssetLock assetLock,
                                                 Clock clock) {
        return TransactionalUseCases.wrap(SuspendCertificateUseCase.class,
                new SuspendCertificateUseCaseImpl(certificateRepository, certificateService, assetLock, clock), tx);
    }

    @Bean
    RenewCertificateUseCase renewCertificate(TransactionTemplate tx, CertificateIssuanceService certificateIssuance,
                                             CertificateRepository certificateRepository,
                                             CertificateNumbering certificateNumbering,
                                             CertificateService certificateService, AssetLock assetLock) {
        return TransactionalUseCases.wrap(RenewCertificateUseCase.class, new RenewCertificateUseCaseImpl(certificateIssuance,
                certificateRepository, certificateNumbering, certificateService, assetLock), tx);
    }

    @Bean
    CertificateHistoryUseCase certificateHistory(TransactionTemplate tx, AuditLog<CertificateNumber> certificateLog) {
        return TransactionalUseCases.wrap(CertificateHistoryUseCase.class,
                new CertificateHistoryUseCaseImpl(certificateLog), tx);
    }

    @Bean
    ListJurisdictionsUseCase listJurisdictions(RegisteredJurisdictions registeredJurisdictions) {
        return new ListJurisdictionsUseCaseImpl(registeredJurisdictions);
    }

    @Bean
    ListCertificatesUseCase listCertificates(TransactionTemplate tx, CertificateRepository certificateRepository, Clock clock) {
        return TransactionalUseCases.wrap(ListCertificatesUseCase.class, new ListCertificatesUseCaseImpl(certificateRepository, clock), tx);
    }

    @Bean
    FindCertificateUseCase findCertificate(TransactionTemplate tx, CertificateRepository certificateRepository, Clock clock) {
        return TransactionalUseCases.wrap(FindCertificateUseCase.class, new FindCertificateUseCaseImpl(certificateRepository, clock), tx);
    }
}
