package ar.edu.itba.certiflow.inspection.model;

import ar.edu.itba.certiflow.schema.model.SectionTitle;
import java.util.Set;

public interface InspectionScopeDescriber<T> {

    T full();

    T sections(Set<SectionTitle> sectionTitles);
}
