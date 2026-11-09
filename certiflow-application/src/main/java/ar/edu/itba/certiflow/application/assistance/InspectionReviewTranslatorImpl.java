package ar.edu.itba.certiflow.application.assistance;

import ar.edu.itba.certiflow.assistance.model.CriterionUnderReview;
import ar.edu.itba.certiflow.assistance.model.EvidenceUnderReview;
import ar.edu.itba.certiflow.assistance.model.InspectionReviewTranslator;
import ar.edu.itba.certiflow.assistance.model.InspectionUnderReview;
import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.inspection.model.AttachedEvidence;
import ar.edu.itba.certiflow.inspection.model.CriterionResponse;
import ar.edu.itba.certiflow.inspection.model.Inspection;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.inspection.model.Observation;
import ar.edu.itba.certiflow.inspection.model.exceptions.InspectionNotFoundException;
import ar.edu.itba.certiflow.inspection.repository.InspectionRepository;
import ar.edu.itba.certiflow.schema.model.AnswerText;

/**
 * El único lugar que conoce a la vez la inspección y lo que lee el asistente (D16). Deja afuera quién escribió
 * cada cosa y cuándo, y la severidad configurada: el modelo no necesita lo primero y no debe ver lo segundo.
 */
public final class InspectionReviewTranslatorImpl implements InspectionReviewTranslator {

    private static final String NOT_ANSWERED = "not answered";

    private final InspectionRepository inspectionRepository;

    public InspectionReviewTranslatorImpl(InspectionRepository inspectionRepository) {
        this.inspectionRepository = inspectionRepository;
    }

    @Override
    public AssetCode assetCodeOf(InspectionId inspectionId) {
        return inspectionRepository.assetCodeOf(inspectionId)
                .orElseThrow(() -> new InspectionNotFoundException(inspectionId));
    }

    @Override
    public InspectionUnderReview translate(InspectionId inspectionId) {
        Inspection inspection = inspectionRepository.findById(inspectionId)
                .orElseThrow(() -> new InspectionNotFoundException(inspectionId));
        return new InspectionUnderReview(inspection.id(), inspection.assetType(), inspection.schemaVersion().number(),
                inspection.isClosed(), inspection.evaluate().responses().stream().map(this::underReview).toList());
    }

    private CriterionUnderReview underReview(CriterionResponse<?> response) {
        return new CriterionUnderReview(response.criterion().code(), response.criterion().text(),
                response.criterion().rule().describedBy(new RuleInWords()), answerOf(response), response.outcome(),
                response.observations().stream().map(Observation::text).toList(),
                response.attachments().stream().map(AttachedEvidence::evidence)
                        .map(evidence -> new EvidenceUnderReview(evidence.kind().code(), evidence.description()))
                        .toList());
    }

    private String answerOf(CriterionResponse<?> response) {
        return response.given()
                .map(given -> given.value().describedBy(new AnswerText()))
                .orElse(NOT_ANSWERED);
    }
}
