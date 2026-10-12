package ar.edu.itba.certiflow.certification.repository;

import ar.edu.itba.certiflow.certification.model.CertificateNumber;

public interface CertificateNumbering {

    CertificateNumber next();
}
