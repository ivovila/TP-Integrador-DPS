package ar.edu.itba.certiflow.certification.usecase;

import ar.edu.itba.certiflow.shared.model.Stamp;
import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.certification.model.CertificateBasis;
import ar.edu.itba.certiflow.certification.model.CertificateNumber;
import ar.edu.itba.certiflow.certification.model.CertificateStatus;
import ar.edu.itba.certiflow.certification.model.CertificationDecision;
import ar.edu.itba.certiflow.certification.model.CertificationScope;
import ar.edu.itba.certiflow.certification.model.Suspension;
import ar.edu.itba.certiflow.certification.model.ValidityPeriod;
import ar.edu.itba.certiflow.shared.model.Person;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record CertificateDocument(CertificateNumber number, AssetCode assetCode, String assetName,
                                  ValidityPeriod validity, LocalDate statusDate, CertificateStatus status,
                                  Stamp issued, List<Suspension> suspensions, CertificationDecision decision, CertificationScope scope, CertificateBasis basis) {

    public CertificateDocument {
        suspensions = List.copyOf(suspensions);
    }

    public Person issuedBy() {
        return issued.by();
    }

    public Instant issuedAt() {
        return issued.at();
    }
}
