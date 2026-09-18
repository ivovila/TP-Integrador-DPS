package ar.edu.itba.certiflow.domain.certificate;

import ar.edu.itba.certiflow.domain.asset.Asset;
import ar.edu.itba.certiflow.domain.inspection.Inspection;
import ar.edu.itba.certiflow.domain.shared.Person;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Se obtiene únicamente a través de AuditedCertificateGenerator o renovando un certificado existente. */
public interface Certificate {

    void suspend(String reason, Person by, Instant at);

    Certificate renew(CertificateNumber number, ValidityPeriod validity, Inspection basedOn, Person by, Instant at);

    CertificateStatus statusOn(LocalDate date);

    /** Vigente aunque esté suspendido: ocupa el lugar del único certificado del activo. */
    boolean isCurrentOn(LocalDate date);

    boolean certifies(Asset asset);

    CertificateNumber number();

    Asset asset();

    Inspection basedOn();

    ValidityPeriod validity();

    Person issuedBy();

    Instant issuedAt();

    List<Suspension> suspensions();
}
