package ar.edu.itba.certiflow.schema.model.exceptions;

import ar.edu.itba.certiflow.shared.model.DomainException;

public class SectionNotInSchemaException extends DomainException {

    public SectionNotInSchemaException(String sectionTitle) {
        super("Section '" + sectionTitle + "' does not belong to the schema version bound to this inspection");
    }
}
