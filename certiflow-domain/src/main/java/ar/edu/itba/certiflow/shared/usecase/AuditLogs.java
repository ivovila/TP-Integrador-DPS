package ar.edu.itba.certiflow.shared.usecase;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.catalog.model.AssetType;
import ar.edu.itba.certiflow.certification.model.CertificateNumber;
import ar.edu.itba.certiflow.inspection.model.FindingId;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.shared.model.AuditLog;

public record AuditLogs(AuditLog<AssetCode> assets, AuditLog<AssetType> schemas, AuditLog<InspectionId> inspections,
                        AuditLog<FindingId> findings, AuditLog<CertificateNumber> certificates) {
}
