package ar.edu.itba.certiflow.assistance.model;

/** Lo que el asistente sabe de una evidencia: su tipo y lo que el inspector describió. El archivo no lo ve. */
public record EvidenceUnderReview(String kind, String description) {
}
