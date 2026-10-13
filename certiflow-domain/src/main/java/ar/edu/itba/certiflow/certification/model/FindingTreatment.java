package ar.edu.itba.certiflow.certification.model;

/** Qué hace una política con un hallazgo abierto según su severidad. */
public interface FindingTreatment {

    void applyTo(OpenNonConformity finding, PolicyAssessment assessment);
}
