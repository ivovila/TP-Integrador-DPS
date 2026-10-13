package ar.edu.itba.certiflow.certification.usecase;

import ar.edu.itba.certiflow.certification.model.CertificateNumber;

public interface RenewGlobalCertificateUseCase {

    CertificateNumber execute(RenewGlobalCertificateCommand command);
}
