package ar.edu.itba.certiflow.application.certification;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.certification.repository.CertificateRepository;
import ar.edu.itba.certiflow.certification.usecase.CertificateView;
import ar.edu.itba.certiflow.certification.usecase.ListCertificatesUseCase;
import java.time.Clock;
import java.util.List;

public final class ListCertificatesUseCaseImpl implements ListCertificatesUseCase {

    private final CertificateRepository certificateRepository;
    private final CertificateStatuses statuses;

    public ListCertificatesUseCaseImpl(CertificateRepository certificateRepository, Clock clock) {
        this.certificateRepository = certificateRepository;
        this.statuses = new CertificateStatuses(certificateRepository, clock);
    }

    @Override
    public List<CertificateView> execute() {
        return statuses.viewsOf(certificateRepository.findAll());
    }

    @Override
    public List<CertificateView> execute(AssetCode asset) {
        return statuses.viewsOfOneAsset(certificateRepository.findByAsset(asset));
    }
}
