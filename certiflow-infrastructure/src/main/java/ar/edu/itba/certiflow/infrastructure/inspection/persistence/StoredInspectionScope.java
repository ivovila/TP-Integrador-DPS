package ar.edu.itba.certiflow.infrastructure.inspection.persistence;

import ar.edu.itba.certiflow.infrastructure.shared.persistence.CodesJson;
import ar.edu.itba.certiflow.inspection.model.FullScope;
import ar.edu.itba.certiflow.inspection.model.InspectionScope;
import ar.edu.itba.certiflow.inspection.model.SectionsScope;
import java.util.Optional;
import java.util.Set;

/** El alcance como está guardado: los títulos de las secciones, o nada si la inspección es completa. */
record StoredInspectionScope(String scopeSections) {

    InspectionScope scope() {
        return Optional.ofNullable(scopeSections)
                .<InspectionScope>map(storedSections -> new SectionsScope(Set.copyOf(CodesJson.read(storedSections))))
                .orElseGet(FullScope::new);
    }
}
