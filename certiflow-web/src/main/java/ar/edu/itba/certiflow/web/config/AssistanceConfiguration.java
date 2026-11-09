package ar.edu.itba.certiflow.web.config;

import ar.edu.itba.certiflow.application.assistance.AcceptSuggestionUseCaseImpl;
import ar.edu.itba.certiflow.assistance.usecase.AcceptSuggestionUseCase;
import ar.edu.itba.certiflow.application.assistance.DismissSuggestionUseCaseImpl;
import ar.edu.itba.certiflow.assistance.usecase.DismissSuggestionUseCase;
import ar.edu.itba.certiflow.application.assistance.FindAssistantReviewUseCaseImpl;
import ar.edu.itba.certiflow.assistance.usecase.FindAssistantReviewUseCase;
import ar.edu.itba.certiflow.application.assistance.ListAssistantReviewsUseCaseImpl;
import ar.edu.itba.certiflow.assistance.usecase.ListAssistantReviewsUseCase;
import ar.edu.itba.certiflow.application.assistance.AssistantReviewHistoryUseCaseImpl;
import ar.edu.itba.certiflow.assistance.usecase.AssistantReviewHistoryUseCase;
import ar.edu.itba.certiflow.application.assistance.RequestAssistantReviewUseCaseImpl;
import ar.edu.itba.certiflow.assistance.usecase.RequestAssistantReviewUseCase;
import ar.edu.itba.certiflow.application.assistance.InspectionReviewTranslatorImpl;
import ar.edu.itba.certiflow.assistance.model.AssistantReviewFactory;
import ar.edu.itba.certiflow.assistance.model.AssistantReviewService;
import ar.edu.itba.certiflow.assistance.model.InspectionAssistant;
import ar.edu.itba.certiflow.assistance.model.InspectionReviewTranslator;
import ar.edu.itba.certiflow.assistance.model.ReviewId;
import ar.edu.itba.certiflow.assistance.repository.AssistantReviewRepository;
import ar.edu.itba.certiflow.catalog.repository.AssetLock;
import ar.edu.itba.certiflow.infrastructure.assistance.StubInspectionAssistant;
import ar.edu.itba.certiflow.infrastructure.shared.persistence.JpaAuditLog;
import ar.edu.itba.certiflow.infrastructure.shared.persistence.SpringDataAuditEntryRepository;
import ar.edu.itba.certiflow.inspection.repository.InspectionRepository;
import ar.edu.itba.certiflow.shared.model.AuditActionCatalog;
import ar.edu.itba.certiflow.shared.model.AuditLog;
import ar.edu.itba.certiflow.shared.model.Transactions;
import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.support.TransactionTemplate;

/** La llamada al asistente queda fuera del proxy transaccional; las decisiones y consultas sí lo usan (D36). */
@Configuration
public class AssistanceConfiguration {

    @Bean
    @ConditionalOnProperty(name = "certiflow.assistant.provider", havingValue = "stub", matchIfMissing = true)
    InspectionAssistant inspectionAssistant() {
        return new StubInspectionAssistant();
    }

    @Bean
    AuditLog<ReviewId> reviewLog(SpringDataAuditEntryRepository entries, AuditActionCatalog actions) {
        return new JpaAuditLog<>(entries, "assistance", ReviewId::value, actions);
    }

    @Bean
    AssistantReviewService assistantReviewService(AuditLog<ReviewId> reviewLog) {
        return new AssistantReviewService(new AssistantReviewFactory(), reviewLog);
    }

    @Bean
    InspectionReviewTranslator inspectionReviewTranslator(InspectionRepository inspections) {
        return new InspectionReviewTranslatorImpl(inspections);
    }

    @Bean
    RequestAssistantReviewUseCase requestAssistantReview(InspectionReviewTranslator translator, InspectionAssistant assistant,
            AssistantReviewRepository reviews, AssistantReviewService service, AssetLock lock, Transactions transactions, Clock clock) {
        return new RequestAssistantReviewUseCaseImpl(translator, assistant, reviews, service, lock, transactions, clock);
    }

    @Bean
    AcceptSuggestionUseCase acceptSuggestion(TransactionTemplate tx, AssistantReviewRepository reviews,
            AssistantReviewService service, InspectionReviewTranslator translator, AssetLock lock, Clock clock) {
        return TransactionalUseCases.wrap(AcceptSuggestionUseCase.class,
                new AcceptSuggestionUseCaseImpl(reviews, service, translator, lock, clock), tx);
    }

    @Bean
    DismissSuggestionUseCase dismissSuggestion(TransactionTemplate tx, AssistantReviewRepository reviews,
            AssistantReviewService service, InspectionReviewTranslator translator, AssetLock lock, Clock clock) {
        return TransactionalUseCases.wrap(DismissSuggestionUseCase.class,
                new DismissSuggestionUseCaseImpl(reviews, service, translator, lock, clock), tx);
    }

    @Bean
    FindAssistantReviewUseCase findAssistantReview(TransactionTemplate tx, AssistantReviewRepository reviews) {
        return TransactionalUseCases.wrap(FindAssistantReviewUseCase.class, new FindAssistantReviewUseCaseImpl(reviews), tx);
    }

    @Bean
    ListAssistantReviewsUseCase listAssistantReviews(TransactionTemplate tx, AssistantReviewRepository reviews,
            InspectionReviewTranslator translator) {
        return TransactionalUseCases.wrap(ListAssistantReviewsUseCase.class,
                new ListAssistantReviewsUseCaseImpl(reviews, translator), tx);
    }

    @Bean
    AssistantReviewHistoryUseCase assistantReviewHistory(TransactionTemplate tx, AuditLog<ReviewId> reviewLog) {
        return TransactionalUseCases.wrap(AssistantReviewHistoryUseCase.class,
                new AssistantReviewHistoryUseCaseImpl(reviewLog), tx);
    }
}
