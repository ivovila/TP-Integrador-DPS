package ar.edu.itba.certiflow.web.security;

import ar.edu.itba.certiflow.infrastructure.security.SpringDataRevokedTokenRepository;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

public class NotRevokedValidator implements OAuth2TokenValidator<Jwt> {

    private static final OAuth2Error REVOKED = new OAuth2Error("invalid_token", "The token was revoked", null);

    private final SpringDataRevokedTokenRepository revokedTokens;

    public NotRevokedValidator(SpringDataRevokedTokenRepository revokedTokens) {
        this.revokedTokens = revokedTokens;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        boolean revoked = jwt.getId() == null || revokedTokens.existsById(jwt.getId());
        return revoked ? OAuth2TokenValidatorResult.failure(REVOKED) : OAuth2TokenValidatorResult.success();
    }
}
