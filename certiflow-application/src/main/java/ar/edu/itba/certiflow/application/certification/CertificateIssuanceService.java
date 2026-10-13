package ar.edu.itba.certiflow.application.certification;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.certification.model.AssetCertificates;
import ar.edu.itba.certiflow.certification.model.Certificate;
import ar.edu.itba.certiflow.certification.model.CertificationDecision;
import ar.edu.itba.certiflow.certification.model.CertificationScope;
import ar.edu.itba.certiflow.certification.model.DecidedCertification;
import ar.edu.itba.certiflow.certification.model.GrantedDecision;
import ar.edu.itba.certiflow.certification.model.InspectionResult;
import ar.edu.itba.certiflow.certification.model.InspectionResultTranslator;
import ar.edu.itba.certiflow.certification.model.exceptions.NotEligibleForCertificationException;
import ar.edu.itba.certiflow.certification.repository.CertificateRepository;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import java.time.LocalDate;

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

    /** El activo que hay que bloquear antes de decidir sobre una inspección. */
    public AssetCode assetCodeOf(InspectionId inspectionId) {
        return inspectionResultTranslator.assetCodeOf(inspectionId);
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

    public void ensureScopeAvailable(AssetCode assetCode, CertificationScope scope, LocalDate date) {
        certificatesOf(assetCode).ensureScopeAvailable(scope, date);
    }

    public void ensureScopeAvailableToReplace(Certificate replacedCertificate, CertificationScope scope, LocalDate date) {
        certificatesOf(replacedCertificate.assetCode()).ensureScopeAvailableToReplace(replacedCertificate, scope, date);
    }

    private AssetCertificates certificatesOf(AssetCode assetCode) {
        return new AssetCertificates(assetCode, certificateRepository.findByAsset(assetCode));
    }

    private InspectionResult resultFor(InspectionId inspectionId, CertificationScope scope) {
        return inspectionResultTranslator.translate(inspectionId).forScope(scope);
    }
}
