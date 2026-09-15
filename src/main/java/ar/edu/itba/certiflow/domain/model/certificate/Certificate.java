package ar.edu.itba.certiflow.domain.model.certificate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import ar.edu.itba.certiflow.domain.model.asset.AssetId;
import ar.edu.itba.certiflow.domain.model.finding.Finding;
import ar.edu.itba.certiflow.domain.model.inspection.InspectionEvaluation;
import ar.edu.itba.certiflow.domain.model.inspection.InspectionId;
import ar.edu.itba.certiflow.domain.model.shared.AggregateRoot;
import ar.edu.itba.certiflow.domain.model.shared.DomainException;
import ar.edu.itba.certiflow.domain.model.shared.InvalidTransitionException;
import lombok.Getter;

@Getter
public class Certificate extends AggregateRoot {

    public static final String OVERDUE_ACTIONS = "Accion correctiva vencida sin verificar";

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

    public static Certificate issue(CertificateId id, InspectionEvaluation evaluation, List<Finding> inspectionFindings,
                                    CertificationPolicy policy, ValidityPeriod validity, LocalDateTime now) {
        if (inspectionFindings.stream().anyMatch(finding -> !finding.getInspection().equals(evaluation.inspection()))) {
            throw new IllegalArgumentException("Los hallazgos recibidos no son de esta inspeccion");
        }
        if (!policy.allows(evaluation, inspectionFindings)) {
            throw new DomainException("La inspeccion no cumple la politica de certificacion");
        }
        Certificate certificate = new Certificate(id, evaluation.asset(), evaluation.inspection(), validity);
        certificate.recordEvent(new CertificateIssued(id, evaluation.asset(), validity, now));
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

    public Certificate renew(CertificateId newId, InspectionEvaluation renewal, List<Finding> renewalFindings,
                             CertificationPolicy policy, ValidityPeriod newValidity, LocalDateTime now) {
        if (!renewal.asset().equals(asset)) {
            throw new DomainException("La inspeccion de renovacion corresponde a otro activo");
        }
        Certificate renewed = issue(newId, renewal, renewalFindings, policy, newValidity, now);
        transitionTo(CertificateStatus.RENEWED, null, now);
        renewedBy = newId;
        return renewed;
    }

    public boolean suspendIfActionsOverdue(List<Finding> assetFindings, LocalDateTime now) {
        if (assetFindings.stream().anyMatch(finding -> !finding.getAsset().equals(asset))) {
            throw new IllegalArgumentException("Los hallazgos recibidos no son de este activo");
        }
        boolean overdue = assetFindings.stream()
                .anyMatch(finding -> finding.isOpen() && finding.hasOverdueActions(now.toLocalDate()));
        if (!overdue || !isValidOn(now.toLocalDate())) {
            return false;
        }
        suspend(OVERDUE_ACTIONS, now);
        return true;
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
