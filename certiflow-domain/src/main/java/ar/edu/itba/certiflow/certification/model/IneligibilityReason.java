package ar.edu.itba.certiflow.certification.model;

/** Por qué una decisión no permite certificar. Una política nueva puede traer sus propios motivos. */
public interface IneligibilityReason {

    /** Identificador estable con el que se guarda y se publica. */
    String code();
}
