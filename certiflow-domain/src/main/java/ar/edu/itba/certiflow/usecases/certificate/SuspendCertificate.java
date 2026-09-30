package ar.edu.itba.certiflow.usecases.certificate;

import ar.edu.itba.certiflow.models.certificate.CertificateNumber;
import ar.edu.itba.certiflow.models.shared.Person;

public interface SuspendCertificate {

    void execute(CertificateNumber number, String reason, Person suspendedBy);
}
