package ar.edu.itba.certiflow.web.security;

import java.time.Instant;

public record IssuedToken(String value, Instant expiresAt) {
}
