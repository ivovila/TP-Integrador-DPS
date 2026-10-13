package ar.edu.itba.certiflow.infrastructure.certification.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface SpringDataCertificateRepository extends JpaRepository<CertificateEntity, String> {

    List<CertificateEntity> findByAssetCode(String assetCode);

    @Query("select certificate.assetCode from CertificateEntity certificate where certificate.number = :number")
    Optional<String> findAssetCodeByNumber(@Param("number") String number);
}
