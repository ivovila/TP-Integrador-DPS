package ar.edu.itba.certiflow.infrastructure.security;

import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

public interface SpringDataRevokedTokenRepository extends JpaRepository<RevokedTokenEntity, String> {

    @Transactional
    @Modifying
    void deleteByExpiresAtBefore(Instant instant);
}
