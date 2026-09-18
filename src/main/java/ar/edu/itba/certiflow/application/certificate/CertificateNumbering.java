package ar.edu.itba.certiflow.application.certificate;

import ar.edu.itba.certiflow.domain.certificate.CertificateNumber;

/** De dónde sale el próximo número es un detalle, igual que el reloj. Cada llamada debe devolver un número nuevo. */
public interface CertificateNumbering {

    CertificateNumber next();
}
