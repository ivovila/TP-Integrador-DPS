package ar.edu.itba.certiflow.domain.evaluation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class EvidenceRequirementTest {

    /** Un esquema puede aportar sus propios tipos de evidencia sin modificar StandardEvidenceKind. */
    private enum LabEvidenceKind implements EvidenceKind {
        CALIBRATION_CERTIFICATE;

        @Override
        public String label() {
            return "Calibration certificate";
        }
    }

    private final Evidence photo = new Evidence(StandardEvidenceKind.PHOTO, "photos/gauge.jpg", "Gauge");
    private final Evidence document = new Evidence(StandardEvidenceKind.DOCUMENT, "docs/manual.pdf", "Manual");

    @Test
    void onlyEvidenceOfTheRequiredKindCounts() {
        EvidenceRequirement onePhoto = new EvidenceRequirement(StandardEvidenceKind.PHOTO, 1);

        assertFalse(onePhoto.isSatisfiedBy(List.of(document)));
        assertTrue(onePhoto.isSatisfiedBy(List.of(document, photo)));
    }

    @Test
    void requirementIsMetExactlyAtItsMinimum() {
        EvidenceRequirement twoPhotos = new EvidenceRequirement(StandardEvidenceKind.PHOTO, 2);

        assertFalse(twoPhotos.isSatisfiedBy(List.of(photo)));
        assertTrue(twoPhotos.isSatisfiedBy(List.of(photo, photo)));
    }

    @Test
    void schemaDefinedEvidenceKindsWorkWithoutTouchingTheStandardOnes() {
        EvidenceRequirement calibration = new EvidenceRequirement(LabEvidenceKind.CALIBRATION_CERTIFICATE, 1);
        Evidence certificate = new Evidence(LabEvidenceKind.CALIBRATION_CERTIFICATE, "docs/calibration.pdf", "2026");

        assertFalse(calibration.isSatisfiedBy(List.of(photo)));
        assertTrue(calibration.isSatisfiedBy(List.of(certificate)));
    }

    @Test
    void requirementAskingForNoEvidenceIsAnInvalidConfiguration() {
        assertThrows(InvalidRuleConfigurationException.class,
                () -> new EvidenceRequirement(StandardEvidenceKind.PHOTO, 0));
    }
}
