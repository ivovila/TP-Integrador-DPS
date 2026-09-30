package ar.edu.itba.certiflow.usecases.certificate;

import ar.edu.itba.certiflow.models.certificate.CertificateNumber;
import ar.edu.itba.certiflow.models.inspection.InspectionId;
import ar.edu.itba.certiflow.models.shared.Person;
import java.time.LocalDate;

public interface IssueCertificate {

    CertificateNumber execute(InspectionId inspectionId, LocalDate validUntil, Person issuedBy);
}
