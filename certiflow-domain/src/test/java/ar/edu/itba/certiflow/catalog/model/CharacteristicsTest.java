package ar.edu.itba.certiflow.catalog.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ar.edu.itba.certiflow.catalog.model.exceptions.DuplicateCharacteristicException;
import ar.edu.itba.certiflow.shared.model.RequiredValueMissingException;
import java.util.List;
import org.junit.jupiter.api.Test;

class CharacteristicsTest {

    @Test
    void characteristicsKeepTheOrderInWhichTheyWereDeclared() {
        Characteristic capacity = new Characteristic("capacity", "10 kg");
        Characteristic agent = new Characteristic("agent", "ABC powder");

        assertEquals(List.of(capacity, agent), new Characteristics(List.of(capacity, agent)).entries());
        assertEquals(List.of(), new Characteristics().entries());
    }

    @Test
    void sameCharacteristicCannotBeDeclaredTwice() {
        assertThrows(DuplicateCharacteristicException.class, () -> new Characteristics(List.of(
                new Characteristic("capacity", "10 kg"), new Characteristic("capacity", "5 kg"))));
    }

    @Test
    void characteristicNeedsNameAndValue() {
        assertThrows(RequiredValueMissingException.class, () -> new Characteristic(" ", "10 kg"));
        assertThrows(RequiredValueMissingException.class, () -> new Characteristic("capacity", " "));
    }
}
