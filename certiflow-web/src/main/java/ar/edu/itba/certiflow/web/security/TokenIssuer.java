package ar.edu.itba.certiflow.web.security;

import ar.edu.itba.certiflow.infrastructure.security.UserAccountEntity;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

public class TokenIssuer {

    static final String NAME_CLAIM = "name";
    static final String ROLE_CLAIM = "role";

    private final JwtEncoder jwtEncoder;
    private final JwtProperties properties;
    private final Clock clock;

    public TokenIssuer(JwtEncoder jwtEncoder, JwtProperties properties, Clock clock) {
        this.jwtEncoder = jwtEncoder;
        this.properties = properties;
        this.clock = clock;
    }

    public IssuedToken issueFor(UserAccountEntity account) {
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(properties.ttl());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(account.personId())
                .claim(NAME_CLAIM, account.name())
                .claim(ROLE_CLAIM, account.role())
                .id(UUID.randomUUID().toString())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .build();
        String value = jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new IssuedToken(value, expiresAt);
    }
}
