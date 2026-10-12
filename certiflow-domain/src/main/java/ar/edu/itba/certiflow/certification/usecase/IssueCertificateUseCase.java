package ar.edu.itba.certiflow.certification.usecase;

import ar.edu.itba.certiflow.certification.model.CertificateNumber;

public interface IssueCertificateUseCase {

    CertificateNumber execute(IssueCertificateCommand command);
}
