package ar.edu.itba.certiflow.application.certification;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ar.edu.itba.certiflow.application.support.Samples;
import ar.edu.itba.certiflow.catalog.model.JurisdictionCode;
import ar.edu.itba.certiflow.catalog.repository.AssetLock;
import ar.edu.itba.certiflow.certification.model.CertificateFactory;
import ar.edu.itba.certiflow.certification.model.CertificateNumber;
import ar.edu.itba.certiflow.certification.model.CertificateService;
import ar.edu.itba.certiflow.certification.model.CertificationDecision;
import ar.edu.itba.certiflow.certification.model.CertificationScope;
import ar.edu.itba.certiflow.certification.model.ClosedAt;
import ar.edu.itba.certiflow.certification.model.DecidedCertification;
import ar.edu.itba.certiflow.certification.model.GrantedDecision;
import ar.edu.itba.certiflow.certification.model.Ineligibility;
import ar.edu.itba.certiflow.certification.model.InspectionResult;
import ar.edu.itba.certiflow.certification.model.PolicyCode;
import ar.edu.itba.certiflow.certification.model.RejectedDecision;
import ar.edu.itba.certiflow.certification.model.SchemaReference;
import ar.edu.itba.certiflow.certification.model.StandardIneligibilityReason;
import ar.edu.itba.certiflow.certification.model.ValidityPeriod;
import ar.edu.itba.certiflow.certification.model.WholeAssetScope;
import ar.edu.itba.certiflow.certification.model.exceptions.AssetAlreadyCertifiedException;
import ar.edu.itba.certiflow.certification.model.exceptions.NotEligibleForCertificationException;
import ar.edu.itba.certiflow.certification.repository.CertificateNumbering;
import ar.edu.itba.certiflow.certification.repository.CertificateRepository;
import ar.edu.itba.certiflow.certification.usecase.IssueCertificateCommand;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.inspection.model.exceptions.InspectionNotFoundException;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Un número de certificado solo se pide cuando ya se sabe que se va a emitir. */
@ExtendWith(MockitoExtension.class)
class IssueCertificateUseCaseImplTest {

    private static final InspectionId INSPECTION = new InspectionId("I-1");
    private static final CertificationScope WHOLE = new WholeAssetScope();
    private static final IssueCertificateCommand COMMAND = new IssueCertificateCommand(INSPECTION, Samples.PLANNER, WHOLE);

    @Mock
    private CertificateIssuanceService issuance;
    @Mock
    private CertificateRepository certificateRepository;
    @Mock
    private CertificateNumbering numbering;
    @Mock
    private AssetLock assetLock;
    private IssueCertificateUseCaseImpl issueCertificate;

    @BeforeEach
    void setUp() {
        issueCertificate = new IssueCertificateUseCaseImpl(issuance, certificateRepository, numbering,
                new CertificateService(new CertificateFactory(), Samples.auditLog()), assetLock);
        when(issuance.assetCodeOf(INSPECTION)).thenReturn(Samples.ASSET.code());
    }

    @Test
    void theAssetIsLockedBeforeDeciding() {
        when(issuance.decide(INSPECTION, WHOLE)).thenThrow(new InspectionNotFoundException(INSPECTION));

        assertThrows(InspectionNotFoundException.class, () -> issueCertificate.execute(COMMAND));

        InOrder order = inOrder(assetLock, issuance);
        order.verify(assetLock).lock(Samples.ASSET.code());
        order.verify(issuance).decide(INSPECTION, WHOLE);
    }

    @Test
    void aRejectedDecisionConsumesNoNumberAndSavesNothing() {
        CertificationDecision rejected = new RejectedDecision(
                List.of(new Ineligibility(StandardIneligibilityReason.BLOCKING_FINDINGS, "Open finding")),
                new PolicyCode("STANDARD"), new JurisdictionCode("DEMO-STANDARD"), Samples.NOW, WHOLE);
        when(issuance.decide(INSPECTION, WHOLE)).thenThrow(new NotEligibleForCertificationException(rejected));

        assertThrows(NotEligibleForCertificationException.class, () -> issueCertificate.execute(COMMAND));
        verifyNoInteractions(numbering);
        verify(certificateRepository, never()).save(any());
    }

    @Test
    void anOccupiedScopeConsumesNoNumberAndSavesNothing() {
        LocalDate today = LocalDate.of(2026, 3, 10);
        InspectionResult result = new InspectionResult(INSPECTION, Samples.ASSET.code(), new ClosedAt(Samples.NOW), true, List.of(),
                new SchemaReference(Samples.EXTINGUISHER, 1), List.of(), WHOLE);
        GrantedDecision approved = new GrantedDecision(List.of(),
                new ValidityPeriod(today, today.plusYears(1).minusDays(1)), new PolicyCode("STANDARD"),
                Samples.ASSET.jurisdictionCode(), Samples.NOW, WHOLE);
        when(issuance.decide(INSPECTION, WHOLE)).thenReturn(new DecidedCertification(result, approved));
        doThrow(new AssetAlreadyCertifiedException(Samples.ASSET.code(), new CertificateNumber("CERT-00001")))
                .when(issuance).ensureScopeAvailable(Samples.ASSET.code(), WHOLE, today);

        assertThrows(AssetAlreadyCertifiedException.class, () -> issueCertificate.execute(COMMAND));
        verifyNoInteractions(numbering);
        verify(certificateRepository, never()).save(any());
    }
}
