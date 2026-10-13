package ar.edu.itba.certiflow.certification.model;

import ar.edu.itba.certiflow.shared.model.Stamp;
import ar.edu.itba.certiflow.shared.model.AuditLog;
import ar.edu.itba.certiflow.shared.model.Person;
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

    public Certificate issue(CertificateNumber number, InspectionResult basedOn, CertificationDecision decision, Stamp issued) {
        Certificate certificate = certificateFactory.create(number, basedOn, decision, issued);
        recorded(certificate);
        return certificate;
    }

    /** Se consulta antes de pedir un número nuevo, para que una renovación rechazada no consuma ninguno. */
    public void ensureRenewableWith(Certificate currentCertificate, InspectionResult newBasis) {
        currentCertificate.ensureRenewableWith(newBasis);
    }

    public Certificate renew(Certificate currentCertificate, CertificateNumber newNumber, CertificationDecision newDecision,
                             InspectionResult newBasis, Stamp renewed) {
        Certificate renewedCertificate = currentCertificate.renew(newNumber, newDecision, newBasis, renewed);
        recorded(currentCertificate);
        recorded(renewedCertificate);
        return renewedCertificate;
    }

    public void suspend(Certificate certificate, String reason, Stamp suspended, LocalDate suspendedOn) {
        certificate.suspend(reason, suspended, suspendedOn);
        recorded(certificate);
    }

    public Certificate issueGlobal(CertificateNumber number, DecidedGlobalCertification decided, Person issuedBy) {
        Certificate certificate = certificateFactory.createGlobal(number, decided, issuedBy);
        recorded(certificate);
        return certificate;
    }

    public void markRenewed(Certificate current, Certificate replacement, Stamp renewed) {
        current.renewedBy(replacement, renewed);
        recorded(current);
    }

    private void recorded(Certificate certificate) {
        certificateLog.recordAll(certificate.number(), certificate.pullAuditEntries());
    }
}
