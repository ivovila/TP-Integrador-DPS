package ar.edu.itba.certiflow.application.certification;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.certification.model.Certificate;
import ar.edu.itba.certiflow.certification.repository.CertificateRepository;
import ar.edu.itba.certiflow.certification.usecase.ListCertificatesUseCase;
import java.util.List;
import java.util.Optional;

public final class ListCertificatesUseCaseImpl implements ListCertificatesUseCase {

    private final CertificateRepository certificateRepository;

    public ListCertificatesUseCaseImpl(CertificateRepository certificateRepository) {
        this.certificateRepository = certificateRepository;
    }

    @Override
    public List<Certificate> execute(Optional<AssetCode> asset) {
        return asset.map(certificateRepository::findByAsset).orElseGet(certificateRepository::findAll);
    }
}
