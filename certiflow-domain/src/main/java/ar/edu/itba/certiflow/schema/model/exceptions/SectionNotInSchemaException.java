package ar.edu.itba.certiflow.schema.model.exceptions;

import ar.edu.itba.certiflow.schema.model.SectionTitle;
import ar.edu.itba.certiflow.shared.model.InvalidValueException;

public class SectionNotInSchemaException extends InvalidValueException {

    public SectionNotInSchemaException(SectionTitle sectionTitle) {
        super("Section '" + sectionTitle.value() + "' does not belong to the schema version bound to this inspection");
    }
}
