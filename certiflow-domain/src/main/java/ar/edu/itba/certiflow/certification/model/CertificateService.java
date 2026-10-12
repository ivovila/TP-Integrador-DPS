package ar.edu.itba.certiflow.certification.model;

import ar.edu.itba.certiflow.shared.model.AuditLog;
import ar.edu.itba.certiflow.shared.model.Person;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Único punto de entrada a los comandos de un certificado. Emitir y renovar pasan por acá,
 * de modo que todo certificado nace de la misma forma y con su entrada de bitácora.
 */
public final class CertificateService {

    private final CertificateFactory certificateFactory;
    private final AuditLog<CertificateNumber> certificateLog;

    public CertificateService(CertificateFactory certificateFactory, AuditLog<CertificateNumber> certificateLog) {
        this.certificateFactory = certificateFactory;
        this.certificateLog = certificateLog;
    }

    public Certificate issue(CertificateNumber number, InspectionResult basedOn, ValidityPeriod validity, Person issuedBy,
                             Instant issuedAt) {
        Certificate certificate = certificateFactory.create(number, basedOn, validity, issuedBy, issuedAt);
        recorded(certificate);
        return certificate;
    }

    /** Se consulta antes de pedir un número nuevo, para que una renovación rechazada no consuma ninguno. */
    public void ensureRenewableWith(Certificate currentCertificate, InspectionResult newBasis) {
        currentCertificate.ensureRenewableWith(newBasis);
    }

    public Certificate renew(Certificate currentCertificate, CertificateNumber newNumber, ValidityPeriod newValidity,
                             InspectionResult newBasis, Person renewedBy, Instant renewedAt) {
        Certificate renewedCertificate = currentCertificate.renew(newNumber, newValidity, newBasis, renewedBy, renewedAt);
        recorded(currentCertificate);
        recorded(renewedCertificate);
        return renewedCertificate;
    }

    public void suspend(Certificate certificate, String reason, Person suspendedBy, Instant suspendedAt, LocalDate suspendedOn) {
        certificate.suspend(reason, suspendedBy, suspendedAt, suspendedOn);
        recorded(certificate);
    }

    private void recorded(Certificate certificate) {
        certificateLog.recordAll(certificate.number(), certificate.pullAuditEntries());
    }
}
