package ar.edu.itba.certiflow.infrastructure.certification.persistence;

import ar.edu.itba.certiflow.certification.model.CertificateNumber;
import ar.edu.itba.certiflow.certification.repository.CertificateNumbering;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

@Component
public class SequenceCertificateNumbering implements CertificateNumbering {

    private static final String NUMBER_FORMAT = "CERT-%05d";

    private final EntityManager entityManager;

    public SequenceCertificateNumbering(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public CertificateNumber next() {
        Number next = (Number) entityManager.createNativeQuery("select nextval('certificate_number_seq')").getSingleResult();
        return new CertificateNumber(NUMBER_FORMAT.formatted(next.longValue()));
    }
}
