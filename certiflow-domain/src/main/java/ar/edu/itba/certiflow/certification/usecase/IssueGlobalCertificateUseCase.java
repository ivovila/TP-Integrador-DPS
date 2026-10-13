package ar.edu.itba.certiflow.certification.usecase;

import ar.edu.itba.certiflow.certification.model.CertificateNumber;

public interface IssueGlobalCertificateUseCase {

    CertificateNumber execute(IssueGlobalCertificateCommand command);
}
