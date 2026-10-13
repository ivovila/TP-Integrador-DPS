package ar.edu.itba.certiflow.certification.usecase;

import ar.edu.itba.certiflow.certification.model.CertificateNumber;
import ar.edu.itba.certiflow.shared.model.Person;
import java.util.List;

public record RenewGlobalCertificateCommand(CertificateNumber currentNumber, int schemaVersion,
                                            List<CertificateNumber> partialCertificateNumbers, Person issuedBy) {

    public RenewGlobalCertificateCommand {
        partialCertificateNumbers = List.copyOf(partialCertificateNumbers);
    }
}
