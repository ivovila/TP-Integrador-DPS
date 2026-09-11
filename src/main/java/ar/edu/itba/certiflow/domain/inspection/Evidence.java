package ar.edu.itba.certiflow.domain.inspection;

import ar.edu.itba.certiflow.domain.schema.EvidenceType;

public record Evidence(EvidenceType type, String file) {
}
