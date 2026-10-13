package ar.edu.itba.certiflow.web.certification;

import ar.edu.itba.certiflow.certification.model.CertificateBasisDescriber;
import ar.edu.itba.certiflow.certification.model.CertificateNumber;
import ar.edu.itba.certiflow.certification.model.InspectionReference;
import ar.edu.itba.certiflow.certification.model.PolicyCode;
import ar.edu.itba.certiflow.web.certification.CertificationPayloads.BasisPayload;
import java.time.Instant;
import java.util.List;

final class BasisPayloadDescriber implements CertificateBasisDescriber<BasisPayload> {

    @Override
    public BasisPayload inspection(InspectionReference inspection) {
        return new BasisPayload("INSPECTION", inspection.id().value(), inspection.closedAt(), List.of(), null);
    }

    @Override
    public BasisPayload partialCertificates(List<CertificateNumber> partialCertificateNumbers, PolicyCode globalPolicyCode,
                                            Instant latestInspectionClosedAt) {
        return new BasisPayload("PARTIAL_CERTIFICATES", null, latestInspectionClosedAt,
                partialCertificateNumbers.stream().map(CertificateNumber::value).toList(), globalPolicyCode.value());
    }
}
