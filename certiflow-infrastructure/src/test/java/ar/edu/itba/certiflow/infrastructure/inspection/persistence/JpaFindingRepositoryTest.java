package ar.edu.itba.certiflow.infrastructure.inspection.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ar.edu.itba.certiflow.infrastructure.schema.persistence.JpaInspectionSchemaRepository;
import ar.edu.itba.certiflow.infrastructure.support.DomainSamples;
import ar.edu.itba.certiflow.inspection.model.Finding;
import ar.edu.itba.certiflow.inspection.model.Inspection;
import ar.edu.itba.certiflow.schema.model.InspectionSchema;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({JpaFindingRepository.class, JpaInspectionRepository.class, JpaInspectionSchemaRepository.class})
class JpaFindingRepositoryTest {

    @Autowired
    private JpaFindingRepository repository;
    @Autowired
    private JpaInspectionRepository inspections;
    @Autowired
    private JpaInspectionSchemaRepository schemas;
    @Autowired
    private EntityManager entityManager;

    private Finding savedFinding() {
        InspectionSchema schema = DomainSamples.schema();
        schemas.save(schema);
        Inspection inspection = DomainSamples.answeredInspection(schema, "5.00");
        var closure = DomainSamples.close(inspection);
        inspections.save(inspection);
        Finding finding = DomainSamples.findingWithTwoActions(closure);
        repository.saveAll(List.of(finding));
        entityManager.flush();
        entityManager.clear();
        return finding;
    }

    @Test
    void aFindingIsReadBackWithItsActionsVerificationsAndCancellations() {
        Finding finding = savedFinding();

        assertEquals(finding.snapshot(), repository.findById(finding.id()).orElseThrow().snapshot());
    }

    @Test
    void findingsAreFoundByAssetAndNextIdsAreUnique() {
        Finding finding = savedFinding();

        assertEquals(List.of(finding.id()), repository.findByAsset(DomainSamples.ASSET.code()).stream().map(Finding::id).toList());
        assertEquals(List.of(finding.id()), repository.findAll().stream().map(Finding::id).toList());
        org.junit.jupiter.api.Assertions.assertNotEquals(repository.nextId(), repository.nextId());
    }
}
