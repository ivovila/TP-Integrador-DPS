package ar.edu.itba.certiflow.certification.model;

public enum StandardFindingTreatment implements FindingTreatment {

    BLOCKS {
        @Override
        public void applyTo(OpenNonConformity finding, PolicyAssessment assessment) {
            assessment.block(StandardIneligibilityReason.BLOCKING_FINDINGS, finding);
        }
    },
    /** La política acepta el hallazgo tal como está: no bloquea ni condiciona. */
    TOLERATED {
        @Override
        public void applyTo(OpenNonConformity finding, PolicyAssessment assessment) {
        }
    },
    /** Una severidad que la política no conoce nunca aprueba por omisión. */
    UNSUPPORTED {
        @Override
        public void applyTo(OpenNonConformity finding, PolicyAssessment assessment) {
            assessment.block(StandardIneligibilityReason.UNSUPPORTED_SEVERITY, finding);
        }
    }
}
