package ar.edu.itba.certiflow.domain.certificate;

import ar.edu.itba.certiflow.domain.asset.Asset;
import ar.edu.itba.certiflow.domain.inspection.InspectionView;
import ar.edu.itba.certiflow.domain.shared.Person;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public interface Certificate {

    void suspend(String reason, Person by, Instant at);

    Certificate renew(CertificateNumber number, ValidityPeriod validity, InspectionView basedOn, Person by,
                      Instant at);

    CertificateStatus statusOn(LocalDate date);

    boolean isCurrentOn(LocalDate date);

    boolean certifies(Asset asset);

    CertificateNumber number();

    Asset asset();

    InspectionView basedOn();

    ValidityPeriod validity();

    Person issuedBy();

    Instant issuedAt();

    List<Suspension> suspensions();
}
