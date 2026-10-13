package ar.edu.itba.certiflow.infrastructure.inspection.persistence;

import ar.edu.itba.certiflow.infrastructure.shared.persistence.CodesDocumentMapper;
import ar.edu.itba.certiflow.inspection.model.FullScope;
import ar.edu.itba.certiflow.inspection.model.InspectionScope;
import ar.edu.itba.certiflow.inspection.model.SectionsScope;
import ar.edu.itba.certiflow.schema.model.SectionTitle;
import java.util.Optional;
import java.util.stream.Collectors;

/** El alcance como está guardado: los títulos de las secciones, o nada si la inspección es completa. */
record StoredInspectionScope(String scopeSections) {

    InspectionScope scope(CodesDocumentMapper codeDocuments) {
        return Optional.ofNullable(scopeSections)
                .<InspectionScope>map(storedSections -> new SectionsScope(codeDocuments.read(storedSections).stream()
                        .map(SectionTitle::new)
                        .collect(Collectors.toSet())))
                .orElseGet(FullScope::new);
    }
}
