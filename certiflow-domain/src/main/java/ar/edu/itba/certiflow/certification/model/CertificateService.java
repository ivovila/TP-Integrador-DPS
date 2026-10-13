package ar.edu.itba.certiflow.certification.model;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.catalog.model.SubsystemCode;
import ar.edu.itba.certiflow.shared.model.AuditLog;
import ar.edu.itba.certiflow.shared.model.Person;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;

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

    public Certificate issue(CertificateNumber number, InspectionResult basedOn, CertificationDecision decision, Person issuedBy,
                             Instant issuedAt) {
        Certificate certificate = certificateFactory.create(number, basedOn, decision, issuedBy, issuedAt);
        recorded(certificate);
        return certificate;
    }

    /** Se consulta antes de pedir un número nuevo, para que una renovación rechazada no consuma ninguno. */
    public void ensureRenewableWith(Certificate currentCertificate, InspectionResult newBasis) {
        currentCertificate.ensureRenewableWith(newBasis);
    }

    public Certificate renew(Certificate currentCertificate, CertificateNumber newNumber, CertificationDecision newDecision,
                             InspectionResult newBasis, Person renewedBy, Instant renewedAt) {
        Certificate renewedCertificate = currentCertificate.renew(newNumber, newDecision, newBasis, renewedBy, renewedAt);
        recorded(currentCertificate);
        recorded(renewedCertificate);
        return renewedCertificate;
    }

    public void suspend(Certificate certificate, String reason, Person suspendedBy, Instant suspendedAt, LocalDate suspendedOn) {
        certificate.suspend(reason, suspendedBy, suspendedAt, suspendedOn);
        recorded(certificate);
    }

    public Certificate issueGlobal(CertificateNumber number, AssetCode assetCode,
            CertificateBasis.PartialCertificatesBasis basis, SchemaReference schemaReference,
            Set<SubsystemCode> requiredSubsystems,
            CertificationDecision decision, Person by, Instant at) {
        Certificate certificate = certificateFactory.createGlobal(number, assetCode, basis, schemaReference,
                requiredSubsystems, decision, by, at);
        recorded(certificate);
        return certificate;
    }

    public void markRenewed(Certificate current, Certificate replacement, Person by, Instant at) {
        current.renewedBy(replacement, by, at);
        recorded(current);
    }

    private void recorded(Certificate certificate) {
        certificateLog.recordAll(certificate.number(), certificate.pullAuditEntries());
    }
}
