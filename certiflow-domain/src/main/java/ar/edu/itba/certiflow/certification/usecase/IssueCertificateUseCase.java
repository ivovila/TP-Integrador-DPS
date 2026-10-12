package ar.edu.itba.certiflow.certification.usecase;

import ar.edu.itba.certiflow.certification.model.CertificateNumber;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.shared.model.Person;
import java.time.LocalDate;

public interface IssueCertificateUseCase {

    CertificateNumber execute(InspectionId inspectionId, LocalDate validUntil, Person issuedBy);
}
