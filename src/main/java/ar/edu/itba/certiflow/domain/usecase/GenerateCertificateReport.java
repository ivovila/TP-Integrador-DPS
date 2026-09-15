package ar.edu.itba.certiflow.domain.usecase;

import ar.edu.itba.certiflow.domain.model.asset.Asset;
import ar.edu.itba.certiflow.domain.model.certificate.Certificate;
import ar.edu.itba.certiflow.domain.model.certificate.CertificateId;
import ar.edu.itba.certiflow.domain.model.report.CertificateReport;
import ar.edu.itba.certiflow.domain.ports.AssetRepository;
import ar.edu.itba.certiflow.domain.ports.CertificateRepository;
import ar.edu.itba.certiflow.domain.ports.Clock;

public class GenerateCertificateReport {

    private final CertificateRepository certificates;
    private final AssetRepository assets;
    private final Clock clock;

    public GenerateCertificateReport(CertificateRepository certificates, AssetRepository assets, Clock clock) {
        this.certificates = certificates;
        this.assets = assets;
        this.clock = clock;
    }

    public CertificateReport execute(CertificateId certificateId) {
        Certificate certificate = certificates.getById(certificateId);
        Asset asset = assets.getById(certificate.getAsset());
        return CertificateReport.of(certificate, asset, clock.today());
    }
}
