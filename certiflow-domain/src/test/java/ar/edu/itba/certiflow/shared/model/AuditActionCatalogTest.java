package ar.edu.itba.certiflow.shared.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.itba.certiflow.catalog.model.AssetAudit;
import ar.edu.itba.certiflow.certification.model.CertificateAudit;
import ar.edu.itba.certiflow.inspection.model.FindingAudit;
import ar.edu.itba.certiflow.inspection.model.InspectionAudit;
import ar.edu.itba.certiflow.schema.model.SchemaAudit;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class AuditActionCatalogTest {

    private final List<AuditAction> everyAction = Stream.<AuditAction[]>of(AssetAudit.values(), SchemaAudit.values(),
                    InspectionAudit.values(), FindingAudit.values(), CertificateAudit.values())
            .flatMap(Arrays::stream)
            .toList();
    private final AuditActionCatalog catalog = new AuditActionCatalog(everyAction);

    @Test
    void everyActionIsFoundAgainByItsCode() {
        everyAction.forEach(action -> assertSame(action, catalog.withCode(action.code()).orElseThrow()));
    }

    @Test
    void theCodeIsTheTextAlreadyStoredInTheAuditLog() {
        assertEquals("AssetAudit.REGISTERED", AssetAudit.REGISTERED.code());
        assertEquals("SchemaAudit.VERSION_ADDED", SchemaAudit.VERSION_ADDED.code());
        assertEquals("InspectionAudit.CLOSED", InspectionAudit.CLOSED.code());
        assertEquals("FindingAudit.RAISED", FindingAudit.RAISED.code());
        assertEquals("CertificateAudit.ISSUED", CertificateAudit.ISSUED.code());
    }

    @Test
    void actionsOfDifferentContextsWithTheSameNameKeepDifferentCodes() {
        assertEquals(everyAction.size(), everyAction.stream().map(AuditAction::code).distinct().count());
    }

    @Test
    void anUnknownCodeIsNotFound() {
        assertTrue(catalog.withCode("InspectionAudit.ARCHIVED").isEmpty());
    }
}
