package ar.edu.itba.certiflow.models.schema.exceptions;

import ar.edu.itba.certiflow.models.shared.DomainException;

public class SectionNotInSchemaException extends DomainException {

    public SectionNotInSchemaException(String sectionTitle) {
        super("Section '" + sectionTitle + "' does not belong to the schema version bound to this inspection");
    }
}
