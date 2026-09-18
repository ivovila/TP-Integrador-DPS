package ar.edu.itba.certiflow.application.report;

import ar.edu.itba.certiflow.domain.asset.AssetCode;
import ar.edu.itba.certiflow.domain.certificate.CertificateNumber;
import ar.edu.itba.certiflow.domain.certificate.CertificateStatus;
import ar.edu.itba.certiflow.domain.certificate.Suspension;
import ar.edu.itba.certiflow.domain.certificate.ValidityPeriod;
import ar.edu.itba.certiflow.domain.shared.Person;
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
