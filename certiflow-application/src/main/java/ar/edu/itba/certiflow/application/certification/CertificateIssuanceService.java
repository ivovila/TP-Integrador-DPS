package ar.edu.itba.certiflow.application.certification;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.certification.model.CertificateNumber;
import ar.edu.itba.certiflow.certification.model.CertificationDecision;
import ar.edu.itba.certiflow.certification.model.CertificationScope;
import ar.edu.itba.certiflow.certification.model.GrantedDecision;
import ar.edu.itba.certiflow.certification.model.InspectionResult;
import ar.edu.itba.certiflow.certification.model.InspectionResultTranslator;
import ar.edu.itba.certiflow.certification.model.exceptions.AssetAlreadyCertifiedException;
import ar.edu.itba.certiflow.certification.model.exceptions.NotEligibleForCertificationException;
import ar.edu.itba.certiflow.certification.repository.CertificateRepository;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import java.time.LocalDate;
import java.util.function.Predicate;
import ar.edu.itba.certiflow.certification.model.Certificate;

/**
 * Lo que comparten consultar, emitir y renovar una certificación: traducir la inspección al alcance pedido
 * y decidir con la política de la jurisdicción. La diferencia es qué hace cada uno con un rechazo, y eso es
 * lo único que cambia entre {@link #evaluate} y {@link #decide}.
 */
public final class CertificateIssuanceService {

    private final InspectionResultTranslator inspectionResultTranslator;
    private final CertificationAssessmentService certificationAssessment;
    private final CertificateRepository certificateRepository;

    public CertificateIssuanceService(InspectionResultTranslator inspectionResultTranslator,
                                      CertificationAssessmentService certificationAssessment,
                                      CertificateRepository certificateRepository) {
        this.inspectionResultTranslator = inspectionResultTranslator;
        this.certificationAssessment = certificationAssessment;
        this.certificateRepository = certificateRepository;
    }

    /** Previsualizar: la decisión se devuelve tal cual, también rechazada, y no reserva nada. */
    public CertificationDecision evaluate(InspectionId inspectionId, CertificationScope scope) {
        return certificationAssessment.evaluate(resultFor(inspectionId, scope));
    }

    /** Emitir o renovar: una decisión rechazada es un conflicto, no una respuesta. */
    public DecidedCertification decide(InspectionId inspectionId, CertificationScope scope) {
        InspectionResult result = resultFor(inspectionId, scope);
        CertificationDecision decision = certificationAssessment.evaluate(result);
        GrantedDecision grantedDecision = decision.granted()
                .orElseThrow(() -> new NotEligibleForCertificationException(decision));
        return new DecidedCertification(result, grantedDecision);
    }

    /**
     * Un certificado suspendido dentro de su vigencia también ocupa su alcance: no se evade una suspensión
     * emitiendo otro.
     */
    public void ensureScopeAvailable(AssetCode assetCode, CertificationScope scope, LocalDate on) {
        ensureNoOtherOccupant(assetCode, scope, on, occupant -> true);
    }

    /** Al renovar, el certificado que se reemplaza no cuenta como ocupante de su propio alcance. */
    public void ensureScopeAvailableToReplace(CertificateNumber beingReplaced, AssetCode assetCode, CertificationScope scope,
                                              LocalDate on) {
        ensureNoOtherOccupant(assetCode, scope, on, occupant -> !occupant.number().equals(beingReplaced));
    }

    private void ensureNoOtherOccupant(AssetCode assetCode, CertificationScope scope, LocalDate on, Predicate<Certificate> counts) {
        certificateRepository.findByAsset(assetCode).stream()
                .filter(certificate -> certificate.isCurrentFor(scope, on))
                .filter(counts)
                .findFirst()
                .ifPresent(current -> {
                    throw new AssetAlreadyCertifiedException(assetCode, current.number());
                });
    }

    private InspectionResult resultFor(InspectionId inspectionId, CertificationScope scope) {
        return inspectionResultTranslator.translate(inspectionId).forScope(scope);
    }
}
