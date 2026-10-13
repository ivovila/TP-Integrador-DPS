package ar.edu.itba.certiflow.web.support;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.web.util.UriUtils;

public final class Uris {

    private Uris() {
    }

    public static String path(String... segments) {
        return "/api/v1/" + Arrays.stream(segments)
                .map(segment -> UriUtils.encodePathSegment(segment, StandardCharsets.UTF_8))
                .collect(Collectors.joining("/"));
    }

    public static Optional<String> query(String name, Optional<String> value) {
        return value.map(present -> name + "=" + UriUtils.encodeQueryParam(present, StandardCharsets.UTF_8));
    }

    public static String etag(String... parts) {
        return "\"" + UUID.nameUUIDFromBytes(String.join("\u0000", parts).getBytes(StandardCharsets.UTF_8)) + "\"";
    }
}
