package ar.edu.itba.certiflow.web.inspection;

import ar.edu.itba.certiflow.inspection.model.InspectionScopeDescriber;
import ar.edu.itba.certiflow.schema.model.SectionTitle;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/** Las secciones que publica una inspección de alcance parcial; una completa no publica ninguna. */
final class ScopeSections implements InspectionScopeDescriber<Optional<Set<String>>> {

    @Override
    public Optional<Set<String>> full() {
        return Optional.empty();
    }

    @Override
    public Optional<Set<String>> sections(Set<SectionTitle> sectionTitles) {
        return Optional.of(sectionTitles.stream().map(SectionTitle::value).collect(Collectors.toCollection(TreeSet::new)));
    }
}
