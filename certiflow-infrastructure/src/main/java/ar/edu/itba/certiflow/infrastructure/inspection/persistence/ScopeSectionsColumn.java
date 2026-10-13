package ar.edu.itba.certiflow.infrastructure.inspection.persistence;

import ar.edu.itba.certiflow.infrastructure.shared.persistence.CodesDocumentMapper;
import ar.edu.itba.certiflow.inspection.model.InspectionScopeDescriber;
import java.util.Optional;
import java.util.Set;

/** Lo que se guarda del alcance de una inspección: los títulos de sus secciones, o nada si es completo. */
final class ScopeSectionsColumn implements InspectionScopeDescriber<Optional<String>> {

    private final CodesDocumentMapper codeDocuments;

    ScopeSectionsColumn(CodesDocumentMapper codeDocuments) {
        this.codeDocuments = codeDocuments;
    }

    @Override
    public Optional<String> full() {
        return Optional.empty();
    }

    @Override
    public Optional<String> sections(Set<String> sectionTitles) {
        return Optional.of(codeDocuments.write(sectionTitles.stream().sorted().toList()));
    }
}
