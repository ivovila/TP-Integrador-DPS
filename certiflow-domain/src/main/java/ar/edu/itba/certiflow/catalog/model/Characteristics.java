package ar.edu.itba.certiflow.catalog.model;

import ar.edu.itba.certiflow.catalog.model.exceptions.DuplicateCharacteristicException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

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

    public static Characteristics none() {
        return new Characteristics(List.of());
    }

    public Optional<String> valueOf(String name) {
        return entries.stream()
                .filter(characteristic -> characteristic.name().equals(name))
                .map(Characteristic::value)
                .findFirst();
    }
}
