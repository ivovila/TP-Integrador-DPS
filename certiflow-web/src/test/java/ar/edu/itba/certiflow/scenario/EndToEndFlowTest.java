package ar.edu.itba.certiflow.scenario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.itba.certiflow.catalog.model.AssetAudit;
import ar.edu.itba.certiflow.catalog.model.JurisdictionCode;
import ar.edu.itba.certiflow.catalog.model.exceptions.AssetAlreadyRegisteredException;
import ar.edu.itba.certiflow.catalog.usecase.RegisterAssetCommand;
import ar.edu.itba.certiflow.certification.model.CertificateNumber;
import ar.edu.itba.certiflow.certification.model.CertificateStatus;
import ar.edu.itba.certiflow.certification.model.StandardIneligibilityReason;
import ar.edu.itba.certiflow.certification.usecase.CertificateDocument;
import ar.edu.itba.certiflow.inspection.model.CorrectiveActionId;
import ar.edu.itba.certiflow.inspection.model.FindingId;
import ar.edu.itba.certiflow.inspection.model.InspectionAudit;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.inspection.model.VerificationResult;
import ar.edu.itba.certiflow.inspection.model.exceptions.InspectionNotClosedException;
import ar.edu.itba.certiflow.inspection.usecase.ActLine;
import ar.edu.itba.certiflow.inspection.usecase.AddObservationCommand;
import ar.edu.itba.certiflow.inspection.usecase.FindingsSummary;
import ar.edu.itba.certiflow.inspection.usecase.InspectionAct;
import ar.edu.itba.certiflow.scenario.support.CertiflowFixture;
import ar.edu.itba.certiflow.scenario.support.ScenarioTest;
import ar.edu.itba.certiflow.schema.model.OptionAnswer;
import ar.edu.itba.certiflow.schema.model.Outcome;
import ar.edu.itba.certiflow.schema.model.YesNoAnswer;
import ar.edu.itba.certiflow.shared.model.AuditEntry;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class EndToEndFlowTest extends ScenarioTest {

    @Test
    void assetGoesFromInspectionToCertificateAndEveryReportTellsTheSameStory() {
        InspectionId inspection = fixture.assignedInspection();
        fixture.answer(inspection, fixture.pressureCriterion, fixture.numericAnswer("5.20"), fixture.inspector);
        fixture.attach(inspection, fixture.pressureCriterion, fixture.gaugePhoto(), fixture.inspector);
        fixture.addObservation.execute(new AddObservationCommand(inspection, fixture.pressureCriterion.code(), "Needle sits below the green band", fixture.inspector));
        fixture.answer(inspection, fixture.sealIntactCriterion, YesNoAnswer.YES, fixture.inspector);
        fixture.answer(inspection, fixture.signageCriterion, new OptionAnswer("FADED"), fixture.inspector);

        List<FindingId> findings = fixture.close(inspection, fixture.inspector);

        InspectionAct inspectionAct = fixture.generateInspectionAct.execute(inspection);
        assertEquals("EXT-0042", inspectionAct.assetCode().value());
        assertEquals(fixture.inspector, inspectionAct.assignment().inspector());
        assertEquals(1, inspectionAct.schemaVersion());
        assertEquals(List.of(Outcome.REJECTED, Outcome.APPROVED, Outcome.OBSERVED),
                inspectionAct.lines().stream().map(ActLine::outcome).toList());
        ActLine pressureLine = inspectionAct.lines().getFirst();
        assertEquals(fixture.numericAnswer("5.20"), pressureLine.answer());
        assertEquals(fixture.inspector, pressureLine.answeredBy());
        assertEquals(1, pressureLine.evidenceCount());
        assertEquals(List.of("Needle sits below the green band"), pressureLine.observations());
        assertEquals(List.of(InspectionAudit.ASSIGNED, InspectionAudit.ANSWER_RECORDED,
                        InspectionAudit.EVIDENCE_ATTACHED, InspectionAudit.OBSERVATION_ADDED,
                        InspectionAudit.ANSWER_RECORDED, InspectionAudit.ANSWER_RECORDED, InspectionAudit.CLOSED),
                inspectionAct.history().stream().map(AuditEntry::action).toList());

        FindingsSummary summaryAfterClosing = fixture.generateFindingsSummary.execute(fixture.extinguisher.code());
        assertEquals(2, summaryAfterClosing.openFindings());
        assertFalse(fixture.evaluate(inspection).isEligible());
        CertiflowFixture.assertNotEligible(StandardIneligibilityReason.BLOCKING_FINDINGS, () -> fixture.issue(inspection, fixture.certifier));

        FindingId lowPressureFinding = findings.getFirst();
        CorrectiveActionId rechargeAction = fixture.plan(lowPressureFinding, "Recharge the extinguisher", fixture.maintenanceTechnician, CertiflowFixture.TODAY.plusDays(10), fixture.assetResponsible);
        fixture.clock.advanceDays(7);
        fixture.verify(lowPressureFinding, rechargeAction, fixture.verifier, VerificationResult.ACCEPTED, "Gauge reads 7 bar after recharge");

        FindingsSummary summaryAfterVerification = fixture.generateFindingsSummary.execute(fixture.extinguisher.code());
        assertEquals(1, summaryAfterVerification.openFindings());
        assertTrue(fixture.evaluate(inspection).isEligible());

        LocalDate validUntil = LocalDate.now(fixture.clock).plusMonths(12).minusDays(1);
        CertificateNumber certificate = fixture.issue(inspection, fixture.certifier);

        CertificateDocument certificateDocument = fixture.generateCertificateDocument.execute(certificate);
        assertEquals(certificate, certificateDocument.number());
        assertEquals("EXT-0042", certificateDocument.assetCode().value());
        assertEquals(CertificateStatus.ACTIVE, certificateDocument.status());
        assertEquals(validUntil, certificateDocument.validity().validUntil());
        assertEquals(fixture.certifier, certificateDocument.issuedBy());
        assertEquals(inspection, fixture.inspectionBacking(certificate));
        assertTrue(fixture.currentWholeAssetCertificate(fixture.extinguisher.code(), LocalDate.now(fixture.clock)).isPresent());
    }

    @Test
    void actIsOnlyIssuedForClosedInspections() {
        InspectionId inspectionInProgress = fixture.assignedInspection();

        assertThrows(InspectionNotClosedException.class, () -> fixture.generateInspectionAct.execute(inspectionInProgress));
    }

    @Test
    void catalogRejectsASecondAssetWithTheSameCode() {
        assertThrows(AssetAlreadyRegisteredException.class, () -> fixture.registerAsset.execute(new RegisterAssetCommand(fixture.extinguisher.code(), fixture.extinguisher.name(), fixture.extinguisher.type(), fixture.extinguisher.location(), fixture.extinguisher.responsible(), fixture.extinguisher.characteristics(), fixture.planner, new JurisdictionCode("DEMO-STANDARD"), List.of())));
        assertEquals(fixture.extinguisher, fixture.assetRepository.findByCode(fixture.extinguisher.code()).orElseThrow());
    }

    @Test
    void registeringAnAssetIsAudited() {
        AuditEntry registration = fixture.assetLog.historyOf(fixture.extinguisher.code()).getFirst();

        assertEquals(AssetAudit.REGISTERED, registration.action());
        assertEquals(fixture.planner, registration.performedBy());
        assertEquals(fixture.extinguisher.name(), registration.detail());
    }
}
