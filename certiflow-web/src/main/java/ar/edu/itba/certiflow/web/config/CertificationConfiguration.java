package ar.edu.itba.certiflow.web.config;

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
import ar.edu.itba.certiflow.catalog.repository.AssetRepository;
import ar.edu.itba.certiflow.certification.model.AllRequiredSubsystemsPolicy;
import ar.edu.itba.certiflow.certification.model.CertificateFactory;
import ar.edu.itba.certiflow.certification.model.CertificateNumber;
import ar.edu.itba.certiflow.certification.model.CertificateService;
import ar.edu.itba.certiflow.certification.model.CertificationPolicyResolver;
import ar.edu.itba.certiflow.certification.model.InspectionResultTranslator;
import ar.edu.itba.certiflow.certification.model.RegisteredJurisdictions;
import ar.edu.itba.certiflow.certification.model.SeverityCertificationPolicy;
import ar.edu.itba.certiflow.certification.repository.CertificateNumbering;
import ar.edu.itba.certiflow.certification.repository.CertificateRepository;
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
import ar.edu.itba.certiflow.infrastructure.certification.persistence.CertificationTransactionLocks;
import ar.edu.itba.certiflow.infrastructure.certification.policy.ConfiguredPolicyResolver;
import ar.edu.itba.certiflow.infrastructure.shared.persistence.JpaAuditLog;
import ar.edu.itba.certiflow.infrastructure.shared.persistence.SpringDataAuditEntryRepository;
import ar.edu.itba.certiflow.inspection.repository.FindingRepository;
import ar.edu.itba.certiflow.inspection.repository.InspectionRepository;
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

    private final CertificationTransactionLocks locks;

    public CertificationConfiguration(CertificationTransactionLocks locks) {
        this.locks = locks;
    }

    @Bean
    AuditLog<CertificateNumber> certificateLog(SpringDataAuditEntryRepository entries,
                                               AuditActionCatalog auditActions) {
        return new JpaAuditLog<>(entries, "certificate", CertificateNumber::value, auditActions);
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
    ListJurisdictionsUseCase listJurisdictions(RegisteredJurisdictions registeredJurisdictions) {
        return new ListJurisdictionsUseCaseImpl(registeredJurisdictions);
    }

    @Bean
    ListCertificatesUseCase listCertificates(TransactionTemplate tx, CertificateRepository certificateRepository, Clock clock) {
        return TransactionalUseCases.wrap(ListCertificatesUseCase.class, new ListCertificatesUseCaseImpl(certificateRepository, clock), tx, locks);
    }

    @Bean
    FindCertificateUseCase findCertificate(TransactionTemplate tx, CertificateRepository certificateRepository, Clock clock) {
        return TransactionalUseCases.wrap(FindCertificateUseCase.class, new FindCertificateUseCaseImpl(certificateRepository, clock), tx, locks);
    }
}
