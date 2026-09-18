package ar.edu.itba.certiflow.application.certificate;

import ar.edu.itba.certiflow.application.finding.FindingRepository;
import ar.edu.itba.certiflow.domain.finding.Finding;
import ar.edu.itba.certiflow.domain.inspection.Inspection;
import ar.edu.itba.certiflow.domain.inspection.exceptions.InspectionNotClosedException;
import java.util.List;
import java.util.Objects;

/**
 * Regla que cruza agregados: un activo es certificable si la inspección que lo respalda está cerrada
 * y no le quedan hallazgos abiertos cuya severidad bloquee la certificación.
 */
public final class CertificationEligibility {

    private final FindingRepository findings;

    public CertificationEligibility(FindingRepository findings) {
        this.findings = Objects.requireNonNull(findings, "findings");
    }

    public void ensureEligible(Inspection inspection) {
        if (!inspection.isClosed()) {
            throw new InspectionNotClosedException();
        }
        List<Finding> blocking = findings.findByAsset(inspection.asset()).stream()
                .filter(Finding::blocksCertification)
                .toList();
        if (!blocking.isEmpty()) {
            throw new BlockingFindingsPreventCertificationException(inspection.asset(), blocking.size());
        }
    }
}
