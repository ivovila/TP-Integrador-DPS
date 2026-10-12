package ar.edu.itba.certiflow.web.certification;

import ar.edu.itba.certiflow.web.support.RequestFields;
import java.time.LocalDate;

public record CertificateRequest(String inspectionId, LocalDate validUntil) {

    public CertificateRequest {
        RequestFields.require(inspectionId, "inspectionId");
        RequestFields.require(validUntil, "validUntil");
    }
}
