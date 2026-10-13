package ar.edu.itba.certiflow.infrastructure.certification.persistence;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.certification.model.Certificate;
import ar.edu.itba.certiflow.certification.model.CertificateFactory;
import ar.edu.itba.certiflow.certification.model.CertificateNumber;
import ar.edu.itba.certiflow.certification.model.CertificateSnapshot;
import ar.edu.itba.certiflow.certification.repository.CertificateRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaCertificateRepository implements CertificateRepository {

    private final SpringDataCertificateRepository certificates;
    private final CertificateFactory factory = new CertificateFactory();

    public JpaCertificateRepository(SpringDataCertificateRepository certificates) {
        this.certificates = certificates;
    }

    @Override
    public void save(Certificate certificate) {
        CertificateSnapshot snapshot = certificate.snapshot();
        certificates.findById(snapshot.number().value()).ifPresentOrElse(
                existing -> existing.copyFrom(snapshot),
                () -> certificates.save(CertificateEntity.from(snapshot)));
    }

    @Override
    public Optional<Certificate> findByNumber(CertificateNumber number) {
        return certificates.findById(number.value()).map(this::restore);
    }

    @Override
    public List<Certificate> findByAsset(AssetCode assetCode) {
        return certificates.findByAssetCode(assetCode.value()).stream().map(this::restore).toList();
    }

    @Override
    public List<Certificate> findAll() {
        return certificates.findAll().stream().map(this::restore).toList();
    }

    private Certificate restore(CertificateEntity entity) {
        return factory.restore(entity.toSnapshot());
    }
}
