package ar.edu.itba.certiflow.catalog.model;

import ar.edu.itba.certiflow.catalog.model.exceptions.DuplicateCharacteristicException;
import java.util.Collections;
import java.util.List;

public record Characteristics(List<Characteristic> entries) {

    public Characteristics {
        entries = List.copyOf(entries);
        List<String> names = entries.stream().map(Characteristic::name).toList();
        List<String> duplicatedNames = names.stream()
                .filter(name -> Collections.frequency(names, name) > 1)
                .distinct()
                .toList();
        if (!duplicatedNames.isEmpty()) {
            throw new DuplicateCharacteristicException(duplicatedNames.getFirst());
        }
    }

    public Characteristics() {
        this(List.of());
    }
}
