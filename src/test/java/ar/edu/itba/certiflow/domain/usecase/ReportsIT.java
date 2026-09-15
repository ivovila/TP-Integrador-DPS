package ar.edu.itba.certiflow.domain.usecase;

import static ar.edu.itba.certiflow.support.Fixtures.PRESSURE;
import static ar.edu.itba.certiflow.support.Fixtures.SIGNAGE;
import static ar.edu.itba.certiflow.support.Fixtures.photo;
import static ar.edu.itba.certiflow.support.Fixtures.pressure;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ar.edu.itba.certiflow.domain.model.asset.Asset;
import ar.edu.itba.certiflow.domain.model.certificate.Certificate;
import ar.edu.itba.certiflow.domain.model.certificate.ValidityPeriod;
import ar.edu.itba.certiflow.domain.model.finding.Finding;
import ar.edu.itba.certiflow.domain.model.finding.FindingDetails;
import ar.edu.itba.certiflow.domain.model.finding.Severity;
import ar.edu.itba.certiflow.domain.model.inspection.Inspection;
import ar.edu.itba.certiflow.domain.model.inspection.InspectionId;
import ar.edu.itba.certiflow.domain.model.report.CertificateReport;
import ar.edu.itba.certiflow.domain.model.report.FindingsSummary;
import ar.edu.itba.certiflow.domain.model.report.InspectionReport;
import ar.edu.itba.certiflow.domain.model.schema.InspectionSchema;
import ar.edu.itba.certiflow.domain.model.shared.InvalidTransitionException;
import ar.edu.itba.certiflow.domain.model.shared.PersonId;
import ar.edu.itba.certiflow.domain.rules.CriterionOutcome;
import ar.edu.itba.certiflow.support.Fixtures;
import ar.edu.itba.certiflow.support.TestContext;

class ReportsIT {

    private TestContext ctx;
    private Asset asset;
    private InspectionSchema schema;
    private final PersonId responsible = PersonId.generate();

    @BeforeEach
    void setUp() {
        ctx = new TestContext();
        Asset template = Fixtures.extinguisher();
        asset = ctx.registerAsset.execute(template.getType(), template.getLocation(), responsible,
                template.getCharacteristics());
        schema = Fixtures.extinguisherSchema();
        ctx.schemas.save(schema);
        ctx.publishSchema.execute(schema.getId());
    }

    private InspectionId inspect(String bar) {
        Inspection inspection = ctx.assignInspection.execute(asset.getId(), schema.getId(), PersonId.generate(),
                ctx.clock.today(), "Relevamiento completo");
        InspectionId id = inspection.getId();
        ctx.startInspection.execute(id);
        ctx.registerResponse.execute(id, PRESSURE,
                r -> r.withMeasurement(pressure(bar)).withObservation("Manometro nuevo"));
        ctx.registerResponse.execute(id, SIGNAGE, r -> r.withAnswer(true).withEvidence(photo()));
        return id;
    }

    @Test
    void inspectionReportShowsEachSectionWithItsCriteria() {
        InspectionId id = inspect("12.5");
        ctx.closeInspection.execute(id);

        InspectionReport report = ctx.inspectionReport.execute(id);

        assertEquals("Matafuegos", report.schemaName());
        assertEquals(1, report.schemaVersion());
        assertEquals(CriterionOutcome.OBSERVED, report.overall());
        assertEquals(List.of("Presion", "Senalizacion"),
                report.sections().stream().map(InspectionReport.SectionLine::name).toList());
        InspectionReport.CriterionLine pressureLine = report.sections().getFirst().criteria().getFirst();
        assertEquals(CriterionOutcome.OBSERVED, pressureLine.outcome());
        assertEquals(List.of("Manometro nuevo"), pressureLine.observations());
    }

    @Test
    void inspectionReportIncludesRectifications() {
        InspectionId id = inspect("14");
        ctx.closeInspection.execute(id);
        ctx.rectifyInspection.execute(id, PersonId.generate(), "Manometro descalibrado",
                Map.of(PRESSURE, Fixtures.pressureResponse("11")));

        InspectionReport report = ctx.inspectionReport.execute(id);

        assertEquals(CriterionOutcome.APPROVED, report.overall());
        assertEquals(1, report.rectifications().size());
    }

    @Test
    void inspectionReportRequiresAClosedInspection() {
        InspectionId id = inspect("11");

        assertThrows(InvalidTransitionException.class, () -> ctx.inspectionReport.execute(id));
    }

    @Test
    void findingsSummaryCountsOpenBySeverityAndOverdueActions() {
        InspectionId id = inspect("14");
        ctx.registerResponse.execute(id, SIGNAGE, r -> r.withAnswer(false));
        ctx.closeInspection.execute(id);
        Finding pressureFinding = ctx.raiseFinding.execute(id,
                new FindingDetails(PRESSURE, Severity.MAJOR, "Presion alta", responsible));
        ctx.raiseFinding.execute(id, new FindingDetails(SIGNAGE, Severity.MINOR, "Cartel caido", responsible));
        ctx.planAction.execute(pressureFinding.getId(), "Recalibrar", responsible, ctx.clock.today().plusDays(5));
        ctx.clock.advanceDays(6);

        FindingsSummary summary = ctx.findingsSummary.execute(id);

        assertEquals(2, summary.open());
        assertEquals(Map.of(Severity.MAJOR, 1L, Severity.MINOR, 1L), summary.openBySeverity());
        assertEquals(0, summary.closed());
        assertEquals(1, summary.findings().stream().mapToLong(FindingsSummary.FindingLine::overdueActions).sum());
    }

    @Test
    void certificateReportShowsAssetAndValidity() {
        InspectionId id = inspect("11");
        ctx.closeInspection.execute(id);
        Certificate certificate = ctx.issueCertificate.execute(id, ValidityPeriod.ofYears(ctx.clock.today(), 1));

        CertificateReport valid = ctx.certificateReport.execute(certificate.getId());
        ctx.clock.advanceDays(400);
        CertificateReport later = ctx.certificateReport.execute(certificate.getId());

        assertEquals(asset.getLocation(), valid.location());
        assertEquals(responsible, valid.responsible());
        assertTrue(valid.valid());
        assertFalse(later.valid());
    }
}
