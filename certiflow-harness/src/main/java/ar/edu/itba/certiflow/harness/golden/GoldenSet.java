package ar.edu.itba.certiflow.harness.golden;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Los casos de {@code golden/case-NN.json}, en orden. Un campo desconocido, faltante o nulo es un error: un caso mal
 * escrito no mide nada.
 */
public final class GoldenSet {

    private static final String CASES = "classpath:golden/case-*.json";

    private final JsonMapper jsonMapper = JsonMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES)
            .enable(DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES)
            .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
            .build();

    public List<GoldenCase> cases() {
        try {
            return Arrays.stream(new PathMatchingResourcePatternResolver().getResources(CASES))
                    .sorted(Comparator.comparing(Resource::getFilename))
                    .map(this::read)
                    .toList();
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }

    private GoldenCase read(Resource resource) {
        try (InputStream content = resource.getInputStream()) {
            return jsonMapper.readValue(content, GoldenCase.class);
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }
}
