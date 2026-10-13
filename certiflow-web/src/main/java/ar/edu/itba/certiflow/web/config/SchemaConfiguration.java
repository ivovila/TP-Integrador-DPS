package ar.edu.itba.certiflow.web.config;

import ar.edu.itba.certiflow.application.schema.CreateInspectionSchemaUseCaseImpl;
import ar.edu.itba.certiflow.application.schema.FindEffectiveSchemaVersionUseCaseImpl;
import ar.edu.itba.certiflow.application.schema.FindSchemaUseCaseImpl;
import ar.edu.itba.certiflow.application.schema.ListSchemasUseCaseImpl;
import ar.edu.itba.certiflow.application.schema.PublishSchemaVersionUseCaseImpl;
import ar.edu.itba.certiflow.catalog.model.AssetType;
import ar.edu.itba.certiflow.infrastructure.certification.persistence.CertificationTransactionLocks;
import ar.edu.itba.certiflow.infrastructure.shared.persistence.JpaAuditLog;
import ar.edu.itba.certiflow.infrastructure.shared.persistence.SpringDataAuditEntryRepository;
import ar.edu.itba.certiflow.schema.model.EvidenceKindCatalog;
import ar.edu.itba.certiflow.schema.model.InspectionSchemaService;
import ar.edu.itba.certiflow.schema.model.SeverityCatalog;
import ar.edu.itba.certiflow.schema.model.StandardEvidenceKind;
import ar.edu.itba.certiflow.schema.model.StandardSeverity;
import ar.edu.itba.certiflow.schema.repository.InspectionSchemaRepository;
import ar.edu.itba.certiflow.schema.usecase.CreateInspectionSchemaUseCase;
import ar.edu.itba.certiflow.schema.usecase.FindEffectiveSchemaVersionUseCase;
import ar.edu.itba.certiflow.schema.usecase.FindSchemaUseCase;
import ar.edu.itba.certiflow.schema.usecase.ListSchemasUseCase;
import ar.edu.itba.certiflow.schema.usecase.PublishSchemaVersionUseCase;
import ar.edu.itba.certiflow.shared.model.AuditActionCatalog;
import ar.edu.itba.certiflow.shared.model.AuditLog;
import java.time.Clock;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.support.TransactionTemplate;

/** Casos de uso de los esquemas de inspección. */
@Configuration
public class SchemaConfiguration {

    private final CertificationTransactionLocks locks;

    public SchemaConfiguration(CertificationTransactionLocks locks) {
        this.locks = locks;
    }

    @Bean
    AuditLog<AssetType> schemaLog(SpringDataAuditEntryRepository entries, AuditActionCatalog auditActions) {
        return new JpaAuditLog<>(entries, "schema", AssetType::name, auditActions);
    }

    /** El único lugar que nombra los enums de severidad y de tipo de evidencia. */
    @Bean
    SeverityCatalog severityCatalog() {
        return new SeverityCatalog(List.of(StandardSeverity.values()));
    }

    @Bean
    EvidenceKindCatalog evidenceKindCatalog() {
        return new EvidenceKindCatalog(List.of(StandardEvidenceKind.values()));
    }

    @Bean
    InspectionSchemaService inspectionSchemaService(AuditLog<AssetType> schemaLog) {
        return new InspectionSchemaService(schemaLog);
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
    FindSchemaUseCase findSchema(TransactionTemplate tx, InspectionSchemaRepository inspectionSchemaRepository) {
        return TransactionalUseCases.wrap(FindSchemaUseCase.class, new FindSchemaUseCaseImpl(inspectionSchemaRepository), tx, locks);
    }
}
