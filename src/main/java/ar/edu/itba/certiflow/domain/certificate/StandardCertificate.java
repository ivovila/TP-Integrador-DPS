package ar.edu.itba.certiflow.domain.certificate;

import ar.edu.itba.certiflow.domain.asset.Asset;
import ar.edu.itba.certiflow.domain.inspection.InspectionNotClosedException;
import ar.edu.itba.certiflow.domain.inspection.InspectionRecord;
import ar.edu.itba.certiflow.domain.shared.Person;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

final class StandardCertificate implements Certificate {

    private final CertificateNumber number;
    private final Asset asset;
    private final InspectionRecord basedOn;
    private final ValidityPeriod validity;
    private final Person issuedBy;
    private final Instant issuedAt;
    private final List<Suspension> suspensions = new ArrayList<>();
    private CertificateLifecycle lifecycle = CertificateLifecycle.ISSUED;

    StandardCertificate(CertificateNumber number, Asset asset, InspectionRecord basedOn, ValidityPeriod validity,
                        Person issuedBy, Instant issuedAt) {
        this.number = number;
        this.asset = asset;
        this.basedOn = basedOn;
        this.validity = validity;
        this.issuedBy = issuedBy;
        this.issuedAt = issuedAt;
        if (!basedOn.isClosed()) {
            throw new InspectionNotClosedException();
        }
        if (!basedOn.asset().equals(asset)) {
            throw new InspectionOfAnotherAssetException(asset);
        }
    }

    @Override
    public void suspend(String reason, Person by, Instant at) {
        if (lifecycle != CertificateLifecycle.ISSUED) {
            throw new CertificateNotActiveException(number);
        }
        suspensions.add(new Suspension(reason, by, at));
        lifecycle = CertificateLifecycle.SUSPENDED;
    }

    @Override
    public Certificate renew(CertificateNumber newNumber, ValidityPeriod newValidity, InspectionRecord newBasis,
                             Person by, Instant at) {
        if (lifecycle == CertificateLifecycle.RENEWED) {
            throw new CertificateAlreadyRenewedException(number);
        }
        Certificate renewal = new StandardCertificate(newNumber, asset, newBasis, newValidity, by, at);
        lifecycle = CertificateLifecycle.RENEWED;
        return renewal;
    }

    @Override
    public CertificateStatus statusOn(LocalDate date) {
        return lifecycle.statusOn(validity, date);
    }

    @Override
    public boolean isCurrentOn(LocalDate date) {
        return lifecycle != CertificateLifecycle.RENEWED && validity.covers(date);
    }

    @Override
    public boolean certifies(Asset candidate) {
        return asset.equals(candidate);
    }

    @Override
    public CertificateNumber number() {
        return number;
    }

    @Override
    public Asset asset() {
        return asset;
    }

    @Override
    public InspectionRecord basedOn() {
        return basedOn;
    }

    @Override
    public ValidityPeriod validity() {
        return validity;
    }

    @Override
    public Person issuedBy() {
        return issuedBy;
    }

    @Override
    public Instant issuedAt() {
        return issuedAt;
    }

    @Override
    public List<Suspension> suspensions() {
        return List.copyOf(suspensions);
    }
}
