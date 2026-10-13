package ar.edu.itba.certiflow.web.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("certiflow.security.jwt")
public record JwtProperties(String secret, Duration ttl) {

    private static final int MINIMUM_SECRET_BYTES = 32;

    public JwtProperties {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MINIMUM_SECRET_BYTES) {
            throw new IllegalStateException("certiflow.security.jwt.secret must have at least " + MINIMUM_SECRET_BYTES + " bytes");
        }
        if (ttl == null || ttl.isNegative() || ttl.isZero()) {
            throw new IllegalStateException("certiflow.security.jwt.ttl must be a positive duration");
        }
    }
}
