package ar.edu.itba.certiflow.scenario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.catalog.model.AssetType;
import ar.edu.itba.certiflow.catalog.model.exceptions.AssetNotFoundException;
import ar.edu.itba.certiflow.inspection.model.FindingId;
import ar.edu.itba.certiflow.inspection.model.InspectionAudit;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.scenario.support.CertiflowFixture;
import ar.edu.itba.certiflow.scenario.support.ScenarioTest;
import ar.edu.itba.certiflow.shared.model.AuditEntry;
import java.util.List;
import org.junit.jupiter.api.Test;

class QueriesTest extends ScenarioTest {

    @Test
    void assetsAreFoundByCodeAndListedByType() {
        assertEquals(fixture.extinguisher, fixture.findAsset.execute(fixture.extinguisher.code()));
        assertEquals(List.of(fixture.extinguisher), fixture.listAssets.execute(CertiflowFixture.EXTINGUISHER));
        assertEquals(List.of(), fixture.listAssets.execute(new AssetType("Boiler")));
        assertThrows(AssetNotFoundException.class, () -> fixture.findAsset.execute(new AssetCode("NOPE")));
    }

    @Test
    void findingsAreListedByAssetAndHistoryIsReadBySubject() {
        InspectionId inspection = fixture.assignedInspection();
        FindingId finding = fixture.majorFindingOf(inspection);

        assertEquals(List.of(finding), fixture.listFindings.execute(fixture.extinguisher.code()).stream()
                .map(f -> f.id()).toList());
        assertEquals(finding, fixture.findFinding.execute(finding).id());
        assertEquals(List.of(InspectionAudit.ASSIGNED, InspectionAudit.ANSWER_RECORDED),
                fixture.inspectionHistory.execute(inspection).stream()
                        .map(AuditEntry::action).distinct().limit(2).toList());
    }

    @Test
    void certificatesAreListedByAsset() {
        var number = fixture.issue(fixture.closedCompliantInspection(), fixture.certifier);

        assertEquals(List.of(number), fixture.listCertificates.execute(fixture.extinguisher.code()).stream()
                .map(view -> view.certificate().number()).toList());
        assertEquals(number, fixture.findCertificate.execute(number).certificate().number());
    }
}
