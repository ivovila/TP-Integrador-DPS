package ar.edu.itba.certiflow.shared.usecase;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.catalog.model.AssetType;
import ar.edu.itba.certiflow.certification.model.CertificateNumber;
import ar.edu.itba.certiflow.inspection.model.FindingId;
import ar.edu.itba.certiflow.inspection.model.InspectionId;

public sealed interface AuditSubject {

    record OfAsset(AssetCode code) implements AuditSubject {
    }

    record OfSchema(AssetType type) implements AuditSubject {
    }

    record OfInspection(InspectionId id) implements AuditSubject {
    }

    record OfFinding(FindingId id) implements AuditSubject {
    }

    record OfCertificate(CertificateNumber number) implements AuditSubject {
    }
}
