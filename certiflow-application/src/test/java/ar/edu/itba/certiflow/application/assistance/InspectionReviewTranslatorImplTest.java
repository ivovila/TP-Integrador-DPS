package ar.edu.itba.certiflow.application.assistance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import ar.edu.itba.certiflow.application.support.Samples;
import ar.edu.itba.certiflow.assistance.model.CriterionUnderReview;
import ar.edu.itba.certiflow.assistance.model.EvidenceUnderReview;
import ar.edu.itba.certiflow.assistance.model.InspectionUnderReview;
import ar.edu.itba.certiflow.inspection.model.Inspection;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.inspection.model.exceptions.InspectionNotFoundException;
import ar.edu.itba.certiflow.inspection.repository.InspectionRepository;
import ar.edu.itba.certiflow.schema.model.CriterionCode;
import ar.edu.itba.certiflow.schema.model.Outcome;
import ar.edu.itba.certiflow.shared.model.Stamp;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InspectionReviewTranslatorImplTest {

    @Mock
    private InspectionRepository inspectionRepository;
    @InjectMocks
    private InspectionReviewTranslatorImpl translator;

    private InspectionUnderReview translated(Inspection inspection) {
        when(inspectionRepository.findById(inspection.id())).thenReturn(Optional.of(inspection));
        return translator.translate(inspection.id());
    }

    private CriterionUnderReview criterion(InspectionUnderReview inspection, String code) {
        return inspection.criteria().stream().filter(criterion -> criterion.is(new CriterionCode(code))).findFirst().orElseThrow();
    }

    @Test
    void eachCriterionCarriesItsRuleAndAnswerInWordsAndItsOutcome() {
        InspectionUnderReview inspection = translated(Samples.answeredInspection("5.00"));

        CriterionUnderReview pressure = criterion(inspection, "EXT-01");
        assertEquals("Gauge pressure within range", pressure.text());
        assertEquals("numeric-range (unit: bar; minimum: 6.00; maximum: 8.00)", pressure.rule());
        assertEquals("5.00", pressure.answer());
        assertEquals(Outcome.REJECTED, pressure.outcome());
        assertEquals("option-in-list (options: REFLECTIVE, VISIBLE)", criterion(inspection, "EXT-03").rule());
        assertEquals(3, inspection.criteria().size());
    }

    @Test
    void theFreeTextTheRuleDoesNotReadReachesTheAssistant() {
        Inspection inspection = Samples.answeredInspection("7.00");
        Samples.inspectionService().addObservation(inspection, new CriterionCode("EXT-02"), "The pin is loose",
                new Stamp(Samples.INSPECTOR, Samples.NOW));

        InspectionUnderReview underReview = translated(inspection);

        assertEquals(List.of("The pin is loose"), criterion(underReview, "EXT-02").observations());
        assertEquals(List.of(new EvidenceUnderReview("PHOTO", "Gauge")), criterion(underReview, "EXT-01").evidence());
        assertFalse(underReview.closed());
        assertEquals(1, underReview.schemaVersion());
        assertEquals(Samples.EXTINGUISHER, underReview.assetType());
    }

    @Test
    void anUnansweredCriterionSaysSo() {
        Inspection inspection = Samples.inspectionService().assign(new InspectionId("I-2"), Samples.ASSET,
                Samples.answeredInspection("7.00").assignment(), Samples.schema().latestPublishedVersion(),
                new Stamp(Samples.PLANNER, Samples.NOW));

        CriterionUnderReview seal = criterion(translated(inspection), "EXT-02");

        assertEquals("not answered", seal.answer());
        assertEquals(Outcome.PENDING, seal.outcome());
    }

    @Test
    void aClosedInspectionIsTranslatedAsClosed() {
        Inspection inspection = Samples.answeredInspection("7.00");
        Samples.inspectionService().close(inspection, new Stamp(Samples.INSPECTOR, Samples.NOW));

        assertTrue(translated(inspection).closed());
    }

    @Test
    void anUnknownInspectionIsNotFound() {
        InspectionId unknown = new InspectionId("I-404");
        when(inspectionRepository.findById(unknown)).thenReturn(Optional.empty());

        assertThrows(InspectionNotFoundException.class, () -> translator.translate(unknown));
    }
}
