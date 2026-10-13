package ar.edu.itba.certiflow.certification.usecase;

import ar.edu.itba.certiflow.certification.model.Certificate;
import ar.edu.itba.certiflow.certification.model.CertificateStatus;

/** Un certificado con su estado a hoy, que puede depender de otros certificados del mismo activo. */
public record CertificateView(Certificate certificate, CertificateStatus status) {
}
