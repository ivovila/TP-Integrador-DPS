package ar.edu.itba.certiflow.web.security;

import ar.edu.itba.certiflow.infrastructure.security.RevokedTokenEntity;
import ar.edu.itba.certiflow.infrastructure.security.SpringDataRevokedTokenRepository;
import ar.edu.itba.certiflow.web.support.InvalidRequestException;
import java.time.Clock;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tokens")
public class TokenController {

    private final SpringDataRevokedTokenRepository revokedTokens;
    private final Clock clock;

    public TokenController(SpringDataRevokedTokenRepository revokedTokens, Clock clock) {
        this.revokedTokens = revokedTokens;
        this.clock = clock;
    }

    @DeleteMapping("/current")
    @Transactional
    public ResponseEntity<Void> revokeCurrent(Authentication authentication) {
        if (!(authentication instanceof JwtAuthenticationToken bearer)) {
            throw new InvalidRequestException("Only a bearer token can be revoked: log in first and send the token you received");
        }
        revokedTokens.deleteByExpiresAtBefore(clock.instant());
        revokedTokens.save(new RevokedTokenEntity(bearer.getToken().getId(), bearer.getToken().getExpiresAt()));
        return ResponseEntity.noContent().build();
    }
}
