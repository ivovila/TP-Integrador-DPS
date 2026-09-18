package ar.edu.itba.certiflow.domain.certificate;

/** Estado a una fecha dada. Nunca se almacena: EXPIRED se deriva de la vigencia. */
public enum CertificateStatus {
    ACTIVE,
    EXPIRED,
    SUSPENDED,
    RENEWED
}
