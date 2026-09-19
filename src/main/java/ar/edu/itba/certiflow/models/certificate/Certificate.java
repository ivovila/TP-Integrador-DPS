package ar.edu.itba.certiflow.models.certificate;

import ar.edu.itba.certiflow.models.asset.Asset;
import ar.edu.itba.certiflow.models.inspection.InspectionView;
import ar.edu.itba.certiflow.models.shared.Person;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public interface Certificate {

    void suspend(String reason, Person suspendedBy, Instant suspendedAt);

    Certificate renew(CertificateNumber renewalNumber, ValidityPeriod renewalValidity, InspectionView basedOn, Person renewedBy,
                      Instant renewedAt);

    CertificateStatus statusOn(LocalDate referenceDate);

    boolean isCurrentOn(LocalDate referenceDate);

    boolean certifies(Asset asset);

    CertificateNumber number();

    Asset asset();

    InspectionView basedOn();

    ValidityPeriod validity();

    Person issuedBy();

    Instant issuedAt();

    List<Suspension> suspensions();
}
