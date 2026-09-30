package ar.edu.itba.certiflow.models.asset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.itba.certiflow.models.asset.exceptions.DuplicateCharacteristicException;
import ar.edu.itba.certiflow.models.shared.RequiredValueMissingException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CharacteristicsTest {

    @Test
    void characteristicIsLookedUpByName() {
        Characteristics characteristics = new Characteristics(List.of(new Characteristic("capacity", "10 kg")));

        assertEquals(Optional.of("10 kg"), characteristics.valueOf("capacity"));
        assertTrue(characteristics.valueOf("agent").isEmpty());
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
