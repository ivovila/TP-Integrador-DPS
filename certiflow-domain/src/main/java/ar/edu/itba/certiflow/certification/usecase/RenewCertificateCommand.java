package ar.edu.itba.certiflow.certification.usecase;

import ar.edu.itba.certiflow.certification.model.CertificateNumber;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.shared.model.Person;
import java.time.LocalDate;

public record RenewCertificateCommand(CertificateNumber currentNumber, InspectionId inspectionId, LocalDate validUntil, Person renewedBy) {
}
