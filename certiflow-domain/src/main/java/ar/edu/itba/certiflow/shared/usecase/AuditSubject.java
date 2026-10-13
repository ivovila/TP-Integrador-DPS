package ar.edu.itba.certiflow.shared.usecase;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.catalog.model.AssetType;
import ar.edu.itba.certiflow.certification.model.CertificateNumber;
import ar.edu.itba.certiflow.inspection.model.FindingId;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.shared.model.AuditEntry;
import java.util.List;

/** Cada sujeto sabe en qué bitácora está su historia. */
public interface AuditSubject {

    List<AuditEntry> historyIn(AuditLogs logs);

    record OfAsset(AssetCode code) implements AuditSubject {
        @Override
        public List<AuditEntry> historyIn(AuditLogs logs) {
            return logs.assets().historyOf(code);
        }
    }

    record OfSchema(AssetType type) implements AuditSubject {
        @Override
        public List<AuditEntry> historyIn(AuditLogs logs) {
            return logs.schemas().historyOf(type);
        }
    }

    record OfInspection(InspectionId id) implements AuditSubject {
        @Override
        public List<AuditEntry> historyIn(AuditLogs logs) {
            return logs.inspections().historyOf(id);
        }
    }

    record OfFinding(FindingId id) implements AuditSubject {
        @Override
        public List<AuditEntry> historyIn(AuditLogs logs) {
            return logs.findings().historyOf(id);
        }
    }

    record OfCertificate(CertificateNumber number) implements AuditSubject {
        @Override
        public List<AuditEntry> historyIn(AuditLogs logs) {
            return logs.certificates().historyOf(number);
        }
    }
}
