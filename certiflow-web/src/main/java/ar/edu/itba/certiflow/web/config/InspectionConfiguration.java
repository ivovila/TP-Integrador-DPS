package ar.edu.itba.certiflow.web.config;

import ar.edu.itba.certiflow.application.inspection.AddObservationUseCaseImpl;
import ar.edu.itba.certiflow.application.inspection.AssignInspectionUseCaseImpl;
import ar.edu.itba.certiflow.application.inspection.AttachEvidenceUseCaseImpl;
import ar.edu.itba.certiflow.application.inspection.CloseInspectionUseCaseImpl;
import ar.edu.itba.certiflow.application.inspection.FindFindingUseCaseImpl;
import ar.edu.itba.certiflow.application.inspection.FindInspectionUseCaseImpl;
import ar.edu.itba.certiflow.application.inspection.FindingHistoryUseCaseImpl;
import ar.edu.itba.certiflow.application.inspection.GenerateFindingsSummaryUseCaseImpl;
import ar.edu.itba.certiflow.application.inspection.GenerateInspectionActUseCaseImpl;
import ar.edu.itba.certiflow.application.inspection.InspectionHistoryUseCaseImpl;
import ar.edu.itba.certiflow.application.inspection.ListFindingsUseCaseImpl;
import ar.edu.itba.certiflow.application.inspection.ListInspectionsUseCaseImpl;
import ar.edu.itba.certiflow.application.inspection.PlanCorrectiveActionUseCaseImpl;
import ar.edu.itba.certiflow.application.inspection.RecordAnswerUseCaseImpl;
import ar.edu.itba.certiflow.application.inspection.RectifyInspectionUseCaseImpl;
import ar.edu.itba.certiflow.application.inspection.VerifyCorrectiveActionUseCaseImpl;
import ar.edu.itba.certiflow.catalog.repository.AssetLock;
import ar.edu.itba.certiflow.catalog.repository.AssetRepository;
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
import ar.edu.itba.certiflow.inspection.usecase.FindingHistoryUseCase;
import ar.edu.itba.certiflow.inspection.usecase.GenerateFindingsSummaryUseCase;
import ar.edu.itba.certiflow.inspection.usecase.GenerateInspectionActUseCase;
import ar.edu.itba.certiflow.inspection.usecase.InspectionHistoryUseCase;
import ar.edu.itba.certiflow.inspection.usecase.ListFindingsUseCase;
import ar.edu.itba.certiflow.inspection.usecase.ListInspectionsUseCase;
import ar.edu.itba.certiflow.inspection.usecase.PlanCorrectiveActionUseCase;
import ar.edu.itba.certiflow.inspection.usecase.RecordAnswerUseCase;
import ar.edu.itba.certiflow.inspection.usecase.RectifyInspectionUseCase;
import ar.edu.itba.certiflow.inspection.usecase.VerifyCorrectiveActionUseCase;
import ar.edu.itba.certiflow.schema.repository.InspectionSchemaRepository;
import ar.edu.itba.certiflow.shared.model.AuditActionCatalog;
import ar.edu.itba.certiflow.shared.model.AuditLog;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.support.TransactionTemplate;

/** Casos de uso de inspecciones y hallazgos. */
@Configuration
public class InspectionConfiguration {

    @Bean
    AuditLog<InspectionId> inspectionLog(SpringDataAuditEntryRepository entries, AuditActionCatalog auditActions) {
        return new JpaAuditLog<>(entries, "inspection", InspectionId::value, auditActions);
    }

    @Bean
    AuditLog<FindingId> findingLog(SpringDataAuditEntryRepository entries, AuditActionCatalog auditActions) {
        return new JpaAuditLog<>(entries, "finding", FindingId::value, auditActions);
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
    AssignInspectionUseCase assignInspection(TransactionTemplate tx, AssetRepository assetRepository,
                                             InspectionSchemaRepository inspectionSchemaRepository,
                                             InspectionRepository inspectionRepository, InspectionService inspectionService,
                                             Clock clock) {
        return TransactionalUseCases.wrap(AssignInspectionUseCase.class, new AssignInspectionUseCaseImpl(assetRepository, inspectionSchemaRepository, inspectionRepository, inspectionService, clock), tx);
    }

    @Bean
    RecordAnswerUseCase recordAnswer(TransactionTemplate tx, InspectionRepository inspectionRepository,
                                     InspectionService inspectionService, Clock clock) {
        return TransactionalUseCases.wrap(RecordAnswerUseCase.class, new RecordAnswerUseCaseImpl(inspectionRepository, inspectionService, clock), tx);
    }

    @Bean
    AttachEvidenceUseCase attachEvidence(TransactionTemplate tx, InspectionRepository inspectionRepository,
                                         InspectionService inspectionService, Clock clock) {
        return TransactionalUseCases.wrap(AttachEvidenceUseCase.class, new AttachEvidenceUseCaseImpl(inspectionRepository, inspectionService, clock), tx);
    }

    @Bean
    AddObservationUseCase addObservation(TransactionTemplate tx, InspectionRepository inspectionRepository,
                                         InspectionService inspectionService, Clock clock) {
        return TransactionalUseCases.wrap(AddObservationUseCase.class, new AddObservationUseCaseImpl(inspectionRepository, inspectionService, clock), tx);
    }

    @Bean
    CloseInspectionUseCase closeInspection(TransactionTemplate tx, InspectionRepository inspectionRepository,
                                           FindingRepository findingRepository, InspectionService inspectionService,
                                           FindingService findingService, AssetLock assetLock, Clock clock) {
        return TransactionalUseCases.wrap(CloseInspectionUseCase.class,
                new CloseInspectionUseCaseImpl(inspectionRepository, findingRepository, inspectionService, findingService,
                        assetLock, clock), tx);
    }

    @Bean
    RectifyInspectionUseCase rectifyInspection(TransactionTemplate tx, InspectionRepository inspectionRepository,
                                               FindingRepository findingRepository, InspectionService inspectionService,
                                               FindingService findingService, AssetLock assetLock, Clock clock) {
        return TransactionalUseCases.wrap(RectifyInspectionUseCase.class,
                new RectifyInspectionUseCaseImpl(inspectionRepository, findingRepository, inspectionService, findingService,
                        assetLock, clock), tx);
    }

    @Bean
    PlanCorrectiveActionUseCase planCorrectiveAction(TransactionTemplate tx, FindingRepository findingRepository,
                                                     FindingService findingService, Clock clock) {
        return TransactionalUseCases.wrap(PlanCorrectiveActionUseCase.class, new PlanCorrectiveActionUseCaseImpl(findingRepository, findingService, clock), tx);
    }

    @Bean
    VerifyCorrectiveActionUseCase verifyCorrectiveAction(TransactionTemplate tx, FindingRepository findingRepository,
                                                         FindingService findingService, AssetLock assetLock,
                                                         Clock clock) {
        return TransactionalUseCases.wrap(VerifyCorrectiveActionUseCase.class,
                new VerifyCorrectiveActionUseCaseImpl(findingRepository, findingService, assetLock, clock), tx);
    }

    @Bean
    InspectionHistoryUseCase inspectionHistory(TransactionTemplate tx, AuditLog<InspectionId> inspectionLog) {
        return TransactionalUseCases.wrap(InspectionHistoryUseCase.class,
                new InspectionHistoryUseCaseImpl(inspectionLog), tx);
    }

    @Bean
    FindingHistoryUseCase findingHistory(TransactionTemplate tx, AuditLog<FindingId> findingLog) {
        return TransactionalUseCases.wrap(FindingHistoryUseCase.class, new FindingHistoryUseCaseImpl(findingLog), tx);
    }

    @Bean
    GenerateInspectionActUseCase generateInspectionAct(TransactionTemplate tx, InspectionRepository inspectionRepository,
                                                       AssetRepository assetRepository, AuditLog<InspectionId> inspectionLog) {
        return TransactionalUseCases.wrap(GenerateInspectionActUseCase.class, new GenerateInspectionActUseCaseImpl(inspectionRepository, assetRepository, inspectionLog), tx);
    }

    @Bean
    GenerateFindingsSummaryUseCase generateFindingsSummary(TransactionTemplate tx, AssetRepository assetRepository,
                                                           FindingRepository findingRepository, Clock clock) {
        return TransactionalUseCases.wrap(GenerateFindingsSummaryUseCase.class, new GenerateFindingsSummaryUseCaseImpl(assetRepository, findingRepository, clock), tx);
    }

    @Bean
    FindInspectionUseCase findInspection(TransactionTemplate tx, InspectionRepository inspectionRepository) {
        return TransactionalUseCases.wrap(FindInspectionUseCase.class, new FindInspectionUseCaseImpl(inspectionRepository), tx);
    }

    @Bean
    FindFindingUseCase findFinding(TransactionTemplate tx, FindingRepository findingRepository) {
        return TransactionalUseCases.wrap(FindFindingUseCase.class, new FindFindingUseCaseImpl(findingRepository), tx);
    }

    @Bean
    ListFindingsUseCase listFindings(TransactionTemplate tx, FindingRepository findingRepository) {
        return TransactionalUseCases.wrap(ListFindingsUseCase.class, new ListFindingsUseCaseImpl(findingRepository), tx);
    }

    @Bean
    ListInspectionsUseCase listInspections(TransactionTemplate tx, InspectionRepository inspectionRepository) {
        return TransactionalUseCases.wrap(ListInspectionsUseCase.class, new ListInspectionsUseCaseImpl(inspectionRepository), tx);
    }
}
