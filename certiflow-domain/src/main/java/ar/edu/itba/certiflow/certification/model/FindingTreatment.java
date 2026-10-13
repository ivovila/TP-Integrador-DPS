package ar.edu.itba.certiflow.certification.model;

/** Qué hace una política con un hallazgo abierto según su severidad. */
public enum FindingTreatment {

    BLOCKS {
        @Override
        void applyTo(OpenNonConformity finding, PolicyAssessment assessment) {
            assessment.block(IneligibilityReason.BLOCKING_FINDINGS, finding);
        }
    },
    REQUIRES_CONDITION {
        @Override
        void applyTo(OpenNonConformity finding, PolicyAssessment assessment) {
            assessment.requireCondition(finding);
        }
    },
    TOLERATED {
        @Override
        void applyTo(OpenNonConformity finding, PolicyAssessment assessment) {
        }
    },
    /** Una severidad que la política no conoce nunca aprueba por omisión. */
    UNSUPPORTED {
        @Override
        void applyTo(OpenNonConformity finding, PolicyAssessment assessment) {
            assessment.block(IneligibilityReason.UNSUPPORTED_SEVERITY, finding);
        }
    };

    abstract void applyTo(OpenNonConformity finding, PolicyAssessment assessment);
}
