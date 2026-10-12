package ar.edu.itba.certiflow.web.inspection;

import ar.edu.itba.certiflow.inspection.model.VerificationResult;
import ar.edu.itba.certiflow.shared.model.RequiredValueMissingException;
import ar.edu.itba.certiflow.web.support.RequestFields;
import java.util.Arrays;

public record VerificationRequest(String result, String notes) {

    public VerificationRequest {
        RequestFields.require(result, "result");
        notes = notes == null ? "" : notes;
    }

    VerificationResult verificationResult() {
        return Arrays.stream(VerificationResult.values())
                .filter(candidate -> candidate.name().equals(result))
                .findFirst()
                .orElseThrow(() -> new RequiredValueMissingException("Unknown verification result " + result));
    }
}
