package ar.edu.itba.certiflow.harness;

import ar.edu.itba.certiflow.assistance.model.InspectionUnderReview;
import ar.edu.itba.certiflow.harness.golden.GoldenCase;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import tools.jackson.databind.json.JsonMapper;

final class EvaluationFingerprint {
    private final JsonMapper json = JsonMapper.builder().build();

    String of(InspectionUnderReview inspection, GoldenCase goldenCase) {
        String document = json.writeValueAsString(List.of(inspection.assetType(), inspection.schemaVersion(),
                inspection.criteria(), goldenCase.expected()));
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(document.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException failure) {
            throw new IllegalStateException("SHA-256 is required", failure);
        }
    }
}
