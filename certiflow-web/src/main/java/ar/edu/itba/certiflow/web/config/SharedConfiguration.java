package ar.edu.itba.certiflow.web.config;

import ar.edu.itba.certiflow.application.shared.AuditHistoryUseCaseImpl;
import ar.edu.itba.certiflow.catalog.model.AssetAudit;
import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.catalog.model.AssetType;
import ar.edu.itba.certiflow.certification.model.CertificateAudit;
import ar.edu.itba.certiflow.certification.model.CertificateNumber;
import ar.edu.itba.certiflow.infrastructure.certification.persistence.CertificationTransactionLocks;
import ar.edu.itba.certiflow.inspection.model.FindingAudit;
import ar.edu.itba.certiflow.inspection.model.FindingId;
import ar.edu.itba.certiflow.inspection.model.InspectionAudit;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.schema.model.SchemaAudit;
import ar.edu.itba.certiflow.shared.model.AuditAction;
import ar.edu.itba.certiflow.shared.model.AuditActionCatalog;
import ar.edu.itba.certiflow.shared.model.AuditLog;
import ar.edu.itba.certiflow.shared.usecase.AuditHistoryUseCase;
import java.time.Clock;
import java.time.Duration;
import java.util.Arrays;
import java.util.stream.Stream;
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

    /** Los instantes se guardan con microsegundos: el reloj ya los entrega con esa precisión, truncando. */
    @Bean
    Clock clock() {
        return Clock.tick(Clock.systemUTC(), Duration.ofNanos(1_000));
    }

    /** El único lugar que nombra los enums de acciones: la persistencia las busca por su código. */
    @Bean
    AuditActionCatalog auditActionCatalog() {
        return new AuditActionCatalog(Stream.<AuditAction[]>of(AssetAudit.values(), SchemaAudit.values(),
                        InspectionAudit.values(), FindingAudit.values(), CertificateAudit.values())
                .flatMap(Arrays::stream)
                .toList());
    }

    @Bean
    AuditHistoryUseCase auditHistory(TransactionTemplate tx, AuditLog<AssetCode> assetLog, AuditLog<AssetType> schemaLog,
                                     AuditLog<InspectionId> inspectionLog, AuditLog<FindingId> findingLog,
                                     AuditLog<CertificateNumber> certificateLog) {
        return TransactionalUseCases.wrap(AuditHistoryUseCase.class, new AuditHistoryUseCaseImpl(assetLog, schemaLog, inspectionLog, findingLog, certificateLog), tx, locks);
    }
}
