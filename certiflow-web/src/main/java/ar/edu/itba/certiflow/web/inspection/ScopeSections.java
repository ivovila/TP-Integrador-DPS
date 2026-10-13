package ar.edu.itba.certiflow.web.inspection;

import ar.edu.itba.certiflow.inspection.model.InspectionScopeDescriber;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/** Las secciones que publica una inspección de alcance parcial; una completa no publica ninguna. */
final class ScopeSections implements InspectionScopeDescriber<Optional<Set<String>>> {

    @Override
    public Optional<Set<String>> full() {
        return Optional.empty();
    }

    @Override
    public Optional<Set<String>> sections(Set<String> sectionTitles) {
        return Optional.of(new TreeSet<>(sectionTitles));
    }
}
