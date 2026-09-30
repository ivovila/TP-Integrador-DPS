package ar.edu.itba.certiflow.usecases.report;

import ar.edu.itba.certiflow.usecases.report.CertificateDocument;
import ar.edu.itba.certiflow.models.certificate.CertificateNumber;

public interface GenerateCertificateDocument {

    CertificateDocument execute(CertificateNumber number);
}
