package ar.edu.itba.certiflow.models.asset;

import ar.edu.itba.certiflow.models.asset.exceptions.DuplicateCharacteristicException;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public record Characteristics(List<Characteristic> entries) {

    public Characteristics {
        entries = List.copyOf(entries);
        Set<String> names = new HashSet<>();
        entries.stream()
                .map(Characteristic::name)
                .filter(name -> !names.add(name))
                .findFirst()
                .ifPresent(duplicatedName -> {
                    throw new DuplicateCharacteristicException(duplicatedName);
                });
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
