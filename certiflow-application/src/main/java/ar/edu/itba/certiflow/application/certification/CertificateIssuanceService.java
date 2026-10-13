package ar.edu.itba.certiflow.application.certification;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.certification.model.CertificateNumber;
import ar.edu.itba.certiflow.certification.model.CertificationDecision;
import ar.edu.itba.certiflow.certification.model.CertificationScope;
import ar.edu.itba.certiflow.certification.model.InspectionResult;
import ar.edu.itba.certiflow.certification.model.InspectionResultTranslator;
import ar.edu.itba.certiflow.certification.model.exceptions.AssetAlreadyCertifiedException;
import ar.edu.itba.certiflow.certification.model.exceptions.NotEligibleForCertificationException;
import ar.edu.itba.certiflow.certification.repository.CertificateRepository;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import java.time.LocalDate;
import java.util.Optional;

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
        return assess(inspectionId, scope).decision();
    }

    /** Emitir o renovar: una decisión rechazada es un conflicto, no una respuesta. */
    public DecidedCertification decide(InspectionId inspectionId, CertificationScope scope) {
        DecidedCertification assessed = assess(inspectionId, scope);
        if (!assessed.decision().isEligible()) {
            throw new NotEligibleForCertificationException(assessed.decision());
        }
        return assessed;
    }

    /**
     * Un certificado suspendido dentro de su vigencia también ocupa su alcance: no se evade una suspensión
     * emitiendo otro. Al renovar, el que se está reemplazando no cuenta como ocupante.
     */
    public void ensureScopeAvailable(AssetCode assetCode, CertificationScope scope, LocalDate on,
                                     Optional<CertificateNumber> beingRenewed) {
        certificateRepository.findCurrentFor(assetCode, scope, on)
                .filter(current -> beingRenewed.filter(current.number()::equals).isEmpty())
                .ifPresent(current -> {
                    throw new AssetAlreadyCertifiedException(assetCode, current.number());
                });
    }

    private DecidedCertification assess(InspectionId inspectionId, CertificationScope scope) {
        InspectionResult result = inspectionResultTranslator.translate(inspectionId).forScope(scope);
        return new DecidedCertification(result, certificationAssessment.evaluate(result));
    }
}
