package ar.edu.itba.certiflow.models.evaluation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class EvidenceRequirementTest {

    private enum LabEvidenceKind implements EvidenceKind {
        CALIBRATION_CERTIFICATE;

        @Override
        public String label() {
            return "Calibration certificate";
        }
    }

    private final Evidence photoEvidence = new Evidence(StandardEvidenceKind.PHOTO, "photos/gauge.jpg", "Gauge");
    private final Evidence documentEvidence = new Evidence(StandardEvidenceKind.DOCUMENT, "docs/manual.pdf", "Manual");

    @Test
    void onlyEvidenceOfTheRequiredKindCounts() {
        EvidenceRequirement onePhoto = new EvidenceRequirement(StandardEvidenceKind.PHOTO, 1);

        assertFalse(onePhoto.isSatisfiedBy(List.of(documentEvidence)));
        assertTrue(onePhoto.isSatisfiedBy(List.of(documentEvidence, photoEvidence)));
    }

    @Test
    void requirementIsMetExactlyAtItsMinimum() {
        EvidenceRequirement twoPhotos = new EvidenceRequirement(StandardEvidenceKind.PHOTO, 2);

        assertFalse(twoPhotos.isSatisfiedBy(List.of(photoEvidence)));
        assertTrue(twoPhotos.isSatisfiedBy(List.of(photoEvidence, photoEvidence)));
    }

    @Test
    void schemaDefinedEvidenceKindsWorkWithoutTouchingTheStandardOnes() {
        EvidenceRequirement calibration = new EvidenceRequirement(LabEvidenceKind.CALIBRATION_CERTIFICATE, 1);
        Evidence calibrationCertificate = new Evidence(LabEvidenceKind.CALIBRATION_CERTIFICATE, "docs/calibration.pdf", "2026");

        assertFalse(calibration.isSatisfiedBy(List.of(photoEvidence)));
        assertTrue(calibration.isSatisfiedBy(List.of(calibrationCertificate)));
    }

    @Test
    void requirementAskingForNoEvidenceIsAnInvalidConfiguration() {
        assertThrows(InvalidRuleConfigurationException.class,
                () -> new EvidenceRequirement(StandardEvidenceKind.PHOTO, 0));
    }
}
