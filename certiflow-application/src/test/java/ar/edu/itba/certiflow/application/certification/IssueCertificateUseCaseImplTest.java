package ar.edu.itba.certiflow.application.certification;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ar.edu.itba.certiflow.application.support.Samples;
import ar.edu.itba.certiflow.catalog.model.JurisdictionCode;
import ar.edu.itba.certiflow.certification.model.CertificateFactory;
import ar.edu.itba.certiflow.certification.model.CertificateNumber;
import ar.edu.itba.certiflow.certification.model.CertificateService;
import ar.edu.itba.certiflow.certification.model.CertificationDecision;
import ar.edu.itba.certiflow.certification.model.CertificationOutcome;
import ar.edu.itba.certiflow.certification.model.CertificationScope;
import ar.edu.itba.certiflow.certification.model.Ineligibility;
import ar.edu.itba.certiflow.certification.model.StandardIneligibilityReason;
import ar.edu.itba.certiflow.certification.model.exceptions.AssetAlreadyCertifiedException;
import ar.edu.itba.certiflow.certification.model.exceptions.NotEligibleForCertificationException;
import ar.edu.itba.certiflow.certification.repository.CertificateNumbering;
import ar.edu.itba.certiflow.certification.repository.CertificateRepository;
import ar.edu.itba.certiflow.certification.usecase.IssueCertificateCommand;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ar.edu.itba.certiflow.certification.model.InspectionResult;
import ar.edu.itba.certiflow.certification.model.SchemaReference;
import ar.edu.itba.certiflow.certification.model.ValidityPeriod;

/** Un número de certificado solo se pide cuando ya se sabe que se va a emitir. */
@ExtendWith(MockitoExtension.class)
class IssueCertificateUseCaseImplTest {

    private static final InspectionId INSPECTION = new InspectionId("I-1");
    private static final CertificationScope WHOLE = new CertificationScope.WholeAsset();
    private static final IssueCertificateCommand COMMAND = new IssueCertificateCommand(INSPECTION, Samples.PLANNER, WHOLE);

    @Mock
    private CertificateIssuanceService issuance;
    @Mock
    private CertificateRepository certificateRepository;
    @Mock
    private CertificateNumbering numbering;
    private IssueCertificateUseCaseImpl issueCertificate;

    @BeforeEach
    void setUp() {
        issueCertificate = new IssueCertificateUseCaseImpl(issuance, certificateRepository, numbering,
                new CertificateService(new CertificateFactory(), Samples.auditLog()));
    }

    @Test
    void aRejectedDecisionConsumesNoNumberAndSavesNothing() {
        CertificationDecision rejected = new CertificationDecision(CertificationOutcome.REJECTED,
                List.of(new Ineligibility(StandardIneligibilityReason.BLOCKING_FINDINGS, "Open finding")), List.of(),
                Optional.empty(), "STANDARD", new JurisdictionCode("DEMO-STANDARD"), Samples.NOW, WHOLE);
        when(issuance.decide(INSPECTION, WHOLE)).thenThrow(new NotEligibleForCertificationException(rejected));

        assertThrows(NotEligibleForCertificationException.class, () -> issueCertificate.execute(COMMAND));
        verifyNoInteractions(numbering);
        verify(certificateRepository, never()).save(any());
    }

    @Test
    void anOccupiedScopeConsumesNoNumberAndSavesNothing() {
        LocalDate today = LocalDate.of(2026, 3, 10);
        InspectionResult result = new InspectionResult(INSPECTION, Samples.ASSET.code(), Optional.of(Samples.NOW), true, List.of(),
                new SchemaReference(Samples.EXTINGUISHER, 1), List.of(), WHOLE);
        CertificationDecision approved = new CertificationDecision(CertificationOutcome.APPROVED, List.of(), List.of(),
                Optional.of(new ValidityPeriod(today, today.plusYears(1).minusDays(1))), "STANDARD",
                Samples.ASSET.jurisdictionCode(), Samples.NOW, WHOLE);
        when(issuance.decide(INSPECTION, WHOLE)).thenReturn(new DecidedCertification(result, approved));
        doThrow(new AssetAlreadyCertifiedException(Samples.ASSET.code(), new CertificateNumber("CERT-00001")))
                .when(issuance).ensureScopeAvailable(Samples.ASSET.code(), WHOLE, today);

        assertThrows(AssetAlreadyCertifiedException.class, () -> issueCertificate.execute(COMMAND));
        verifyNoInteractions(numbering);
        verify(certificateRepository, never()).save(any());
    }
}
