package ar.edu.itba.certiflow.web.config;

import ar.edu.itba.certiflow.application.shared.AuditHistoryUseCaseImpl;
import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.catalog.model.AssetType;
import ar.edu.itba.certiflow.certification.model.CertificateNumber;
import ar.edu.itba.certiflow.infrastructure.certification.persistence.CertificationTransactionLocks;
import ar.edu.itba.certiflow.inspection.model.FindingId;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.shared.model.AuditLog;
import ar.edu.itba.certiflow.shared.usecase.AuditHistoryUseCase;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.support.TransactionTemplate;

/** El reloj y la bitácora, compartidos por todos los contextos. */
@Configuration
public class SharedConfiguration {

    private final CertificationTransactionLocks locks;

    public SharedConfiguration(CertificationTransactionLocks locks) {
        this.locks = locks;
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    AuditHistoryUseCase auditHistory(TransactionTemplate tx, AuditLog<AssetCode> assetLog, AuditLog<AssetType> schemaLog,
                                     AuditLog<InspectionId> inspectionLog, AuditLog<FindingId> findingLog,
                                     AuditLog<CertificateNumber> certificateLog) {
        return TransactionalUseCases.wrap(AuditHistoryUseCase.class, new AuditHistoryUseCaseImpl(assetLog, schemaLog, inspectionLog, findingLog, certificateLog), tx, locks);
    }
}
