package ar.edu.itba.certiflow.application.report;

import ar.edu.itba.certiflow.models.asset.AssetCode;
import ar.edu.itba.certiflow.models.certificate.CertificateNumber;
import ar.edu.itba.certiflow.models.certificate.CertificateStatus;
import ar.edu.itba.certiflow.models.certificate.Suspension;
import ar.edu.itba.certiflow.models.certificate.ValidityPeriod;
import ar.edu.itba.certiflow.models.shared.Person;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record CertificateDocument(CertificateNumber number, AssetCode assetCode, String assetName,
                                  ValidityPeriod validity, LocalDate statusDate, CertificateStatus status,
                                  Person issuedBy, Instant issuedAt, List<Suspension> suspensions) {

    public CertificateDocument {
        suspensions = List.copyOf(suspensions);
    }
}
