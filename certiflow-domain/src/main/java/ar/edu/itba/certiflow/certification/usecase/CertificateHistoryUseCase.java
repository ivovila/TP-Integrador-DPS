package ar.edu.itba.certiflow.certification.usecase;

import ar.edu.itba.certiflow.certification.model.CertificateNumber;
import ar.edu.itba.certiflow.shared.model.AuditEntry;
import java.util.List;

public interface CertificateHistoryUseCase {

    List<AuditEntry> execute(CertificateNumber certificateNumber);
}
