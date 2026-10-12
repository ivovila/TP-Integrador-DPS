package ar.edu.itba.certiflow.certification.usecase;

import ar.edu.itba.certiflow.certification.model.Certificate;
import ar.edu.itba.certiflow.certification.model.CertificateNumber;

public interface FindCertificateUseCase {

    Certificate execute(CertificateNumber number);
}
