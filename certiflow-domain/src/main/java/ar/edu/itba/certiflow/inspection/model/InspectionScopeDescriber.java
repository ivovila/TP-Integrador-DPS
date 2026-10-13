package ar.edu.itba.certiflow.inspection.model;

import java.util.Set;

public interface InspectionScopeDescriber<T> {

    T full();

    T sections(Set<String> sectionTitles);
}
