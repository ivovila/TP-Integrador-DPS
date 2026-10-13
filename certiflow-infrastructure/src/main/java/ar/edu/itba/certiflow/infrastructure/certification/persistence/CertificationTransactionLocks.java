package ar.edu.itba.certiflow.infrastructure.certification.persistence;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.catalog.repository.AssetLock;
import ar.edu.itba.certiflow.shared.usecase.AssetLockKey;
import ar.edu.itba.certiflow.shared.usecase.LocksAsset;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Antes de un comando que cambia algo de un activo, bloquea ese activo hasta el fin de la transacción.
 * Un comando nuevo solo implementa {@link LocksAsset}; una forma nueva de llegar al activo es una entrada más.
 */
@Component
public class CertificationTransactionLocks implements Consumer<Object[]> {

    private final AssetLock locks;
    private final Map<String, Function<String, Optional<String>>> assetCodeByOwner;

    public CertificationTransactionLocks(AssetLock locks, JdbcTemplate jdbc) {
        this.locks = locks;
        this.assetCodeByOwner = Map.of(
                "asset", Optional::of,
                "inspection", id -> assetCode(jdbc, "select asset_code from inspection where id = ?", id),
                "certificate", number -> assetCode(jdbc, "select asset_code from certificate where number = ?", number),
                "finding", id -> assetCode(jdbc, "select asset_code from finding where id = ?", id));
    }

    @Override
    public void accept(Object[] arguments) {
        Optional.ofNullable(arguments).stream().flatMap(Arrays::stream)
                .filter(LocksAsset.class::isInstance)
                .map(LocksAsset.class::cast)
                .findFirst()
                .flatMap(command -> assetCodeOf(command.assetLockKey()))
                .ifPresent(code -> locks.lock(new AssetCode(code)));
    }

    private Optional<String> assetCodeOf(AssetLockKey key) {
        return Optional.ofNullable(assetCodeByOwner.get(key.owner()))
                .orElseThrow(() -> new IllegalStateException("Unknown lock owner " + key.owner()))
                .apply(key.id());
    }

    private static Optional<String> assetCode(JdbcTemplate jdbc, String query, String id) {
        return jdbc.queryForList(query, String.class, id).stream().findFirst();
    }
}
