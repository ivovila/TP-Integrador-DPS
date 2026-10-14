package ar.edu.itba.certiflow.infrastructure.catalog.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.itba.certiflow.catalog.model.Asset;
import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.catalog.model.AssetType;
import ar.edu.itba.certiflow.catalog.model.Characteristic;
import ar.edu.itba.certiflow.catalog.model.Characteristics;
import ar.edu.itba.certiflow.catalog.model.JurisdictionCode;
import ar.edu.itba.certiflow.catalog.model.Location;
import ar.edu.itba.certiflow.catalog.model.exceptions.AssetAlreadyRegisteredException;
import ar.edu.itba.certiflow.infrastructure.support.PersistenceMappers;
import ar.edu.itba.certiflow.shared.model.Person;
import ar.edu.itba.certiflow.shared.model.PersonId;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({JpaAssetRepository.class, PersistenceMappers.class})
class JpaAssetRepositoryTest {

    @Autowired
    private JpaAssetRepository repository;
    @Autowired
    private EntityManager entityManager;

    private final Asset extinguisher = new Asset(new AssetCode("EXT-0042"), "Lobby extinguisher", new AssetType("Fire extinguisher"), new Location("Lobby"), new Person(new PersonId("EMP-003"), "Raul"), new Characteristics(List.of(new Characteristic("capacity", "10 kg"), new Characteristic("agent", "ABC"))), new JurisdictionCode("DEMO-STANDARD"), List.of());

    @Test
    void anAssetIsReadBackEqualToWhatWasSaved() {
        repository.add(extinguisher);
        entityManager.flush();
        entityManager.clear();

        assertEquals(extinguisher, repository.findByCode(extinguisher.code()).orElseThrow());
        assertEquals(extinguisher.responsible().name(),
                repository.findByCode(extinguisher.code()).orElseThrow().responsible().name());
    }

    /** Guardar un activo es darlo de alta: si el código ya existe falla, no pisa al que estaba. */
    @Test
    void aSecondAssetWithACodeThatIsAlreadyStoredIsRejectedAndTheFirstOneIsKept() {
        repository.add(extinguisher);
        entityManager.flush();
        entityManager.clear();
        Asset sameCodeOtherName = new Asset(extinguisher.code(), "Another extinguisher", extinguisher.type(),
                extinguisher.location(), extinguisher.responsible(), new Characteristics(), extinguisher.jurisdictionCode(),
                List.of());

        assertThrows(AssetAlreadyRegisteredException.class, () -> repository.add(sameCodeOtherName));

        entityManager.clear();
        assertEquals("Lobby extinguisher", repository.findByCode(extinguisher.code()).orElseThrow().name());
    }

    @Test
    void anUnknownCodeFindsNothingAndAllListsWhatWasSaved() {
        repository.add(extinguisher);

        assertTrue(repository.findByCode(new AssetCode("NOPE")).isEmpty());
        assertEquals(List.of(extinguisher), repository.findAll());
    }
}
