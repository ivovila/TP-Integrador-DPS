package ar.edu.itba.certiflow.assistance.model;

import ar.edu.itba.certiflow.schema.model.CriterionCode;
import java.util.Set;

/**
 * Por qué una propuesta del modelo no llega a ser sugerencia. Son controles deterministas sobre lo que el modelo
 * tuvo a la vista; el orden de las constantes es el orden en que se aplican y decide qué motivo se registra.
 */
public enum GuardrailRejection {

    OUT_OF_SCOPE {
        @Override
        boolean rejects(ProposedFinding proposal, InspectionUnderReview inspection, Set<CriterionCode> admitted) {
            return !inspection.includes(proposal.criterion());
        }
    },
    ALREADY_NONCONFORMING {
        @Override
        boolean rejects(ProposedFinding proposal, InspectionUnderReview inspection, Set<CriterionCode> admitted) {
            return inspection.isNonConforming(proposal.criterion());
        }
    },
    /** Sin citas, o con alguna que no aparece en la inspección: no se apoya en nada que el inspector escribió. */
    UNSUPPORTED_QUOTE {
        @Override
        boolean rejects(ProposedFinding proposal, InspectionUnderReview inspection, Set<CriterionCode> admitted) {
            return proposal.quotes().isEmpty() || !proposal.quotes().stream().allMatch(inspection::mentions);
        }
    },
    /** Solo la primera propuesta válida de un criterio puede recibir una decisión. */
    DUPLICATE_CRITERION {
        @Override
        boolean rejects(ProposedFinding proposal, InspectionUnderReview inspection, Set<CriterionCode> admitted) {
            return admitted.contains(proposal.criterion());
        }
    };

    abstract boolean rejects(ProposedFinding proposal, InspectionUnderReview inspection, Set<CriterionCode> admitted);
}
