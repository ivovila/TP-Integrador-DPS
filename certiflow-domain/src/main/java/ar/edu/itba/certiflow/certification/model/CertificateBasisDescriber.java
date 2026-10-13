package ar.edu.itba.certiflow.certification.model;

import java.time.Instant;
import java.util.List;

public interface CertificateBasisDescriber<T> {

    T inspection(InspectionReference inspection);

    T partialCertificates(List<CertificateNumber> partialCertificateNumbers, PolicyCode globalPolicyCode,
                          Instant latestInspectionClosedAt);
}
