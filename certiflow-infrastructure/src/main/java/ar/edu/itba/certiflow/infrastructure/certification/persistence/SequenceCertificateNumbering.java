package ar.edu.itba.certiflow.infrastructure.certification.persistence;

import ar.edu.itba.certiflow.certification.model.CertificateNumber;
import ar.edu.itba.certiflow.certification.repository.CertificateNumbering;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class SequenceCertificateNumbering implements CertificateNumbering {

    private static final String NUMBER_FORMAT = "CERT-%05d";

    private final JdbcTemplate jdbcTemplate;

    public SequenceCertificateNumbering(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public CertificateNumber next() {
        Long nextValue = jdbcTemplate.queryForObject("select nextval('certificate_number_seq')", Long.class);
        return new CertificateNumber(NUMBER_FORMAT.formatted(nextValue));
    }
}
