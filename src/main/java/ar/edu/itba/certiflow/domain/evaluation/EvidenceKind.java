package ar.edu.itba.certiflow.domain.evaluation;

/** Enum extensible: un esquema puede aportar sus propios tipos de evidencia sin tocar los existentes. */
public interface EvidenceKind {

    String label();
}
