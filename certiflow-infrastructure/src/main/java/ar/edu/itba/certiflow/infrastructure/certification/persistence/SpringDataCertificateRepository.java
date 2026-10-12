package ar.edu.itba.certiflow.infrastructure.certification.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataCertificateRepository extends JpaRepository<CertificateEntity, String> {

    List<CertificateEntity> findByAssetCode(String assetCode);
}
