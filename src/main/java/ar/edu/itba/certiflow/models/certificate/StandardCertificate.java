package ar.edu.itba.certiflow.models.certificate;

import ar.edu.itba.certiflow.models.asset.Asset;
import ar.edu.itba.certiflow.models.certificate.exceptions.CertificateAlreadyRenewedException;
import ar.edu.itba.certiflow.models.certificate.exceptions.CertificateNotActiveException;
import ar.edu.itba.certiflow.models.certificate.exceptions.InspectionOfAnotherAssetException;
import ar.edu.itba.certiflow.models.inspection.InspectionView;
import ar.edu.itba.certiflow.models.inspection.exceptions.InspectionNotClosedException;
import ar.edu.itba.certiflow.models.shared.Person;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

final class StandardCertificate implements Certificate {

    private final CertificateNumber number;
    private final Asset asset;
    private final InspectionView basedOn;
    private final ValidityPeriod validity;
    private final Person issuedBy;
    private final Instant issuedAt;
    private final List<Suspension> suspensions = new ArrayList<>();
    private CertificateLifecycle lifecycle = CertificateLifecycle.ISSUED;

    StandardCertificate(CertificateNumber number, Asset asset, InspectionView basedOn, ValidityPeriod validity,
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
    public void suspend(String reason, Person suspendedBy, Instant suspendedAt) {
        if (lifecycle != CertificateLifecycle.ISSUED) {
            throw new CertificateNotActiveException(number);
        }
        suspensions.add(new Suspension(reason, suspendedBy, suspendedAt));
        lifecycle = CertificateLifecycle.SUSPENDED;
    }

    @Override
    public Certificate renew(CertificateNumber newNumber, ValidityPeriod newValidity, InspectionView newBasis,
                             Person renewedBy, Instant renewedAt) {
        if (lifecycle == CertificateLifecycle.RENEWED) {
            throw new CertificateAlreadyRenewedException(number);
        }
        Certificate renewedCertificate = new StandardCertificate(newNumber, asset, newBasis, newValidity, renewedBy, renewedAt);
        lifecycle = CertificateLifecycle.RENEWED;
        return renewedCertificate;
    }

    @Override
    public CertificateStatus statusOn(LocalDate referenceDate) {
        return lifecycle.statusOn(validity, referenceDate);
    }

    @Override
    public boolean isCurrentOn(LocalDate referenceDate) {
        return lifecycle != CertificateLifecycle.RENEWED && validity.covers(referenceDate);
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
    public InspectionView basedOn() {
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
