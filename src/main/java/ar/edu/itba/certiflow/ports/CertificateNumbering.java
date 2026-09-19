package ar.edu.itba.certiflow.ports;

import ar.edu.itba.certiflow.models.certificate.CertificateNumber;

public interface CertificateNumbering {

    CertificateNumber next();
}
