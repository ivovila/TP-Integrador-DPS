package ar.edu.itba.certiflow.application.certification;

import ar.edu.itba.certiflow.certification.model.CertificateNumber;
import ar.edu.itba.certiflow.certification.usecase.CertificateHistoryUseCase;
import ar.edu.itba.certiflow.shared.model.AuditEntry;
import ar.edu.itba.certiflow.shared.model.AuditLog;
import java.util.List;

public final class CertificateHistoryUseCaseImpl implements CertificateHistoryUseCase {

    private final AuditLog<CertificateNumber> certificateLog;

    public CertificateHistoryUseCaseImpl(AuditLog<CertificateNumber> certificateLog) {
        this.certificateLog = certificateLog;
    }

    @Override
    public List<AuditEntry> execute(CertificateNumber certificateNumber) {
        return certificateLog.historyOf(certificateNumber);
    }
}
