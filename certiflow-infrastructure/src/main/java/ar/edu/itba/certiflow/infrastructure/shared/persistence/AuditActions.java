package ar.edu.itba.certiflow.infrastructure.shared.persistence;

import ar.edu.itba.certiflow.catalog.model.AssetAudit;
import ar.edu.itba.certiflow.certification.model.CertificateAudit;
import ar.edu.itba.certiflow.inspection.model.FindingAudit;
import ar.edu.itba.certiflow.inspection.model.InspectionAudit;
import ar.edu.itba.certiflow.schema.model.SchemaAudit;
import ar.edu.itba.certiflow.shared.model.AuditAction;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

final class AuditActions {

    private static final Map<String, AuditAction> BY_NAME = Stream.<Enum<?>[]>of(AssetAudit.values(), SchemaAudit.values(),
                    InspectionAudit.values(), FindingAudit.values(), CertificateAudit.values())
            .flatMap(Stream::of)
            .collect(Collectors.toMap(AuditActions::name, constant -> (AuditAction) constant));

    private AuditActions() {
    }

    static String nameOf(AuditAction action) {
        return name((Enum<?>) action);
    }

    static AuditAction valueOf(String name) {
        AuditAction action = BY_NAME.get(name);
        if (action == null) {
            throw new IllegalStateException("Unknown audit action " + name);
        }
        return action;
    }

    private static String name(Enum<?> constant) {
        return constant.getDeclaringClass().getSimpleName() + "." + constant.name();
    }
}
