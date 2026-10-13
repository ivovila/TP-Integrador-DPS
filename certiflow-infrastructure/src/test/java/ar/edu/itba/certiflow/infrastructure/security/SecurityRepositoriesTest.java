package ar.edu.itba.certiflow.infrastructure.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

@DataJpaTest
class SecurityRepositoriesTest {

    @Autowired
    private SpringDataUserAccountRepository userAccounts;
    @Autowired
    private SpringDataRevokedTokenRepository revokedTokens;

    @Test
    void aUserAccountIsReadBackByUsername() {
        userAccounts.save(new UserAccountEntity("EMP-002", "ines.duarte", "Ines Duarte", "INSPECTOR", "{bcrypt}hash"));

        UserAccountEntity found = userAccounts.findByUsername("ines.duarte").orElseThrow();

        assertEquals("EMP-002", found.personId());
        assertEquals("Ines Duarte", found.name());
        assertEquals("INSPECTOR", found.role());
        assertEquals("{bcrypt}hash", found.passwordHash());
    }

    @Test
    void deletingExpiredRevocationsKeepsTheOnesStillValid() {
        Instant now = Instant.parse("2026-10-13T12:00:00Z");
        revokedTokens.save(new RevokedTokenEntity("expired", now.minusSeconds(1)));
        revokedTokens.save(new RevokedTokenEntity("valid", now.plusSeconds(3600)));

        revokedTokens.deleteByExpiresAtBefore(now);

        assertFalse(revokedTokens.existsById("expired"));
        assertTrue(revokedTokens.existsById("valid"));
    }
}
