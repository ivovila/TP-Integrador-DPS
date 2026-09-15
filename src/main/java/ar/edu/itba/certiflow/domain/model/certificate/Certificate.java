package ar.edu.itba.certiflow.domain.model.certificate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import ar.edu.itba.certiflow.domain.model.asset.AssetId;
import ar.edu.itba.certiflow.domain.model.finding.Finding;
import ar.edu.itba.certiflow.domain.model.inspection.Inspection;
import ar.edu.itba.certiflow.domain.model.inspection.InspectionId;
import ar.edu.itba.certiflow.domain.model.shared.AggregateRoot;
import ar.edu.itba.certiflow.domain.model.shared.DomainException;
import ar.edu.itba.certiflow.domain.model.shared.InvalidTransitionException;
import lombok.Getter;

@Getter
public class Certificate extends AggregateRoot {

    private final CertificateId id;
    private final AssetId asset;
    private final InspectionId inspection;
    private final ValidityPeriod validity;
    private CertificateStatus status = CertificateStatus.ISSUED;
    private CertificateId renewedBy;

    private Certificate(CertificateId id, AssetId asset, InspectionId inspection, ValidityPeriod validity) {
        this.id = id;
        this.asset = asset;
        this.inspection = inspection;
        this.validity = validity;
    }

    public static Certificate issue(CertificateId id, Inspection inspection, List<Finding> findings,
                                    CertificationPolicy policy, ValidityPeriod validity, LocalDateTime now) {
        List<Finding> inspectionFindings = findings.stream()
                .filter(finding -> finding.getInspection().equals(inspection.getId()))
                .toList();
        if (!policy.allows(inspection.evaluate(), inspectionFindings)) {
            throw new DomainException("La inspeccion no cumple la politica de certificacion");
        }
        Certificate certificate = new Certificate(id, inspection.getAsset(), inspection.getId(), validity);
        certificate.recordEvent(new CertificateIssued(id, inspection.getAsset(), validity, now));
        return certificate;
    }

    public void suspend(String reason, LocalDateTime now) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("La suspension requiere un motivo");
        }
        transitionTo(CertificateStatus.SUSPENDED, reason, now);
    }

    public void reinstate(LocalDateTime now) {
        transitionTo(CertificateStatus.ISSUED, null, now);
    }

    public void expire(LocalDateTime now) {
        if (!validity.hasEndedBy(now.toLocalDate())) {
            throw new DomainException("El certificado sigue vigente hasta " + validity.until());
        }
        transitionTo(CertificateStatus.EXPIRED, null, now);
    }

    public Certificate renew(CertificateId newId, Inspection renewalInspection, List<Finding> findings,
                             CertificationPolicy policy, ValidityPeriod newValidity, LocalDateTime now) {
        if (!renewalInspection.getAsset().equals(asset)) {
            throw new DomainException("La inspeccion de renovacion corresponde a otro activo");
        }
        Certificate renewed = issue(newId, renewalInspection, findings, policy, newValidity, now);
        transitionTo(CertificateStatus.RENEWED, null, now);
        renewedBy = newId;
        return renewed;
    }

    public boolean isValidOn(LocalDate date) {
        return status.isValid() && validity.contains(date);
    }

    public Optional<CertificateId> getRenewedBy() {
        return Optional.ofNullable(renewedBy);
    }

    private void transitionTo(CertificateStatus target, String reason, LocalDateTime now) {
        if (!status.next().contains(target)) {
            throw new InvalidTransitionException("El certificado no puede pasar de " + status + " a " + target);
        }
        CertificateStatus previous = status;
        status = target;
        recordEvent(new CertificateStatusChanged(id, previous, target, reason, now));
    }
}
