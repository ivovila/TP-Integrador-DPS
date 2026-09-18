package ar.edu.itba.certiflow.domain.certificate;

public record CertificateNumber(String value) {

    public CertificateNumber {
        if (value.isBlank()) {
            throw new IllegalArgumentException("A certificate number must not be blank");
        }
    }
}
