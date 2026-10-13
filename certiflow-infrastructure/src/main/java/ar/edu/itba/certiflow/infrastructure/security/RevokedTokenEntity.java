package ar.edu.itba.certiflow.infrastructure.security;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "revoked_token")
public class RevokedTokenEntity {

    @Id
    private String jti;
    @Column(name = "expires_at")
    private Instant expiresAt;

    protected RevokedTokenEntity() {
    }

    public RevokedTokenEntity(String jti, Instant expiresAt) {
        this.jti = jti;
        this.expiresAt = expiresAt;
    }

    public String jti() {
        return jti;
    }

    public Instant expiresAt() {
        return expiresAt;
    }
}
