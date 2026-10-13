package ar.edu.itba.certiflow.certification.usecase;

import ar.edu.itba.certiflow.certification.model.CertificateNumber;
import ar.edu.itba.certiflow.certification.model.CertificationScope;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.shared.model.Person;

public record RenewCertificateCommand(CertificateNumber currentNumber, InspectionId inspectionId, Person renewedBy, CertificationScope scope) {
}
