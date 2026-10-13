package ar.edu.itba.certiflow.infrastructure.certification.persistence;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.certification.model.Certificate;
import ar.edu.itba.certiflow.certification.model.CertificateFactory;
import ar.edu.itba.certiflow.certification.model.CertificateNumber;
import ar.edu.itba.certiflow.certification.model.CertificateSnapshot;
import ar.edu.itba.certiflow.certification.repository.CertificateRepository;
import ar.edu.itba.certiflow.infrastructure.shared.persistence.CodesDocumentMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaCertificateRepository implements CertificateRepository {

    private final SpringDataCertificateRepository certificates;
    private final CertificateFactory factory = new CertificateFactory();

    private final DecisionDocumentMapper decisionDocuments;
    private final CodesDocumentMapper codeDocuments;

    public JpaCertificateRepository(SpringDataCertificateRepository certificates,
                                    DecisionDocumentMapper decisionDocuments, CodesDocumentMapper codeDocuments) {
        this.certificates = certificates;
        this.decisionDocuments = decisionDocuments;
        this.codeDocuments = codeDocuments;
    }

    @Override
    public void save(Certificate certificate) {
        CertificateSnapshot snapshot = certificate.snapshot();
        certificates.findById(snapshot.number().value()).ifPresentOrElse(
                existing -> existing.copyFrom(snapshot, decisionDocuments, codeDocuments),
                () -> certificates.save(new CertificateEntity(snapshot, decisionDocuments, codeDocuments)));
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
        return factory.restore(entity.toSnapshot(decisionDocuments, codeDocuments));
    }
}
