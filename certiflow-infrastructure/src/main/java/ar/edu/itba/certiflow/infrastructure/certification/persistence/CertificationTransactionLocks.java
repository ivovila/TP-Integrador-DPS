package ar.edu.itba.certiflow.infrastructure.certification.persistence;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.catalog.repository.AssetLock;
import ar.edu.itba.certiflow.certification.usecase.*;
import ar.edu.itba.certiflow.inspection.usecase.*;
import java.util.function.Consumer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class CertificationTransactionLocks implements Consumer<Object[]> {
    private final AssetLock locks;
    private final JdbcTemplate jdbc;
    public CertificationTransactionLocks(AssetLock locks, JdbcTemplate jdbc) { this.locks = locks; this.jdbc = jdbc; }

    @Override
    public void accept(Object[] arguments) {
        if (arguments == null || arguments.length == 0) { return; }
        String code = switch (arguments[0]) {
            case IssueCertificateCommand command -> assetFor("inspection", "id", command.inspectionId().value());
            case RenewCertificateCommand command -> assetFor("inspection", "id", command.inspectionId().value());
            case SuspendCertificateCommand command -> assetFor("certificate", "number", command.number().value());
            case CloseInspectionCommand command -> assetFor("inspection", "id", command.inspectionId().value());
            case RectifyInspectionCommand command -> assetFor("inspection", "id", command.inspectionId().value());
            case VerifyCorrectiveActionCommand command -> assetFor("finding", "id", command.findingId().value());
            case IssueGlobalCertificateCommand command -> command.assetCode().value();
            case RenewGlobalCertificateCommand command -> command.assetCode().value();
            default -> null;
        };
        if (code != null) { locks.lock(new AssetCode(code)); }
    }

    private String assetFor(String table, String field, String id) {
        var rows = jdbc.queryForList("select asset_code from " + table + " where " + field + " = ?", String.class, id);
        return rows.isEmpty() ? null : rows.getFirst();
    }
}
