package ar.edu.itba.certiflow.web.config;

import ar.edu.itba.certiflow.application.catalog.AssetHistoryUseCaseImpl;
import ar.edu.itba.certiflow.application.catalog.FindAssetUseCaseImpl;
import ar.edu.itba.certiflow.application.catalog.ListAssetsUseCaseImpl;
import ar.edu.itba.certiflow.application.catalog.RegisterAssetUseCaseImpl;
import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.catalog.model.AssetService;
import ar.edu.itba.certiflow.catalog.repository.AssetRepository;
import ar.edu.itba.certiflow.catalog.repository.KnownJurisdictions;
import ar.edu.itba.certiflow.catalog.usecase.AssetHistoryUseCase;
import ar.edu.itba.certiflow.catalog.usecase.FindAssetUseCase;
import ar.edu.itba.certiflow.catalog.usecase.ListAssetsUseCase;
import ar.edu.itba.certiflow.catalog.usecase.RegisterAssetUseCase;
import ar.edu.itba.certiflow.infrastructure.shared.persistence.JpaAuditLog;
import ar.edu.itba.certiflow.infrastructure.shared.persistence.SpringDataAuditEntryRepository;
import ar.edu.itba.certiflow.shared.model.AuditActionCatalog;
import ar.edu.itba.certiflow.shared.model.AuditLog;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.support.TransactionTemplate;

/** Casos de uso del catálogo de activos. */
@Configuration
public class CatalogConfiguration {

    @Bean
    AuditLog<AssetCode> assetLog(SpringDataAuditEntryRepository entries, AuditActionCatalog auditActions) {
        return new JpaAuditLog<>(entries, "asset", AssetCode::value, auditActions);
    }

    @Bean
    AssetService assetService(AuditLog<AssetCode> assetLog, AssetRepository assetRepository,
                              KnownJurisdictions knownJurisdictions) {
        return new AssetService(assetLog, assetRepository, knownJurisdictions);
    }

    @Bean
    RegisterAssetUseCase registerAsset(TransactionTemplate tx, AssetRepository assetRepository, AssetService assetService,
                                       Clock clock) {
        return TransactionalUseCases.wrap(RegisterAssetUseCase.class, new RegisterAssetUseCaseImpl(assetRepository, assetService, clock), tx);
    }

    @Bean
    AssetHistoryUseCase assetHistory(TransactionTemplate tx, AuditLog<AssetCode> assetLog) {
        return TransactionalUseCases.wrap(AssetHistoryUseCase.class, new AssetHistoryUseCaseImpl(assetLog), tx);
    }

    @Bean
    FindAssetUseCase findAsset(TransactionTemplate tx, AssetRepository assetRepository) {
        return TransactionalUseCases.wrap(FindAssetUseCase.class, new FindAssetUseCaseImpl(assetRepository), tx);
    }

    @Bean
    ListAssetsUseCase listAssets(TransactionTemplate tx, AssetRepository assetRepository) {
        return TransactionalUseCases.wrap(ListAssetsUseCase.class, new ListAssetsUseCaseImpl(assetRepository), tx);
    }
}
