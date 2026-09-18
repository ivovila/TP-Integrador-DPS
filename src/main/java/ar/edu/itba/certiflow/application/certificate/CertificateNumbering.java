package ar.edu.itba.certiflow.application.certificate;

import ar.edu.itba.certiflow.domain.certificate.CertificateNumber;

public interface CertificateNumbering {

    CertificateNumber next();
}
