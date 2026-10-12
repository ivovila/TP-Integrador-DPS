package ar.edu.itba.certiflow.certification.usecase;

import ar.edu.itba.certiflow.certification.model.CertificateNumber;
import ar.edu.itba.certiflow.shared.model.Person;

public interface SuspendCertificateUseCase {

    void execute(CertificateNumber number, String reason, Person suspendedBy);
}
