package ar.edu.itba.certiflow.infrastructure.security;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_account")
public class UserAccountEntity {

    @Id
    @Column(name = "person_id")
    private String personId;
    private String username;
    private String name;
    private String role;
    @Column(name = "password_hash")
    private String passwordHash;

    protected UserAccountEntity() {
    }

    public UserAccountEntity(String personId, String username, String name, String role, String passwordHash) {
        this.personId = personId;
        this.username = username;
        this.name = name;
        this.role = role;
        this.passwordHash = passwordHash;
    }

    public String personId() {
        return personId;
    }

    public String username() {
        return username;
    }

    public String name() {
        return name;
    }

    public String role() {
        return role;
    }

    public String passwordHash() {
        return passwordHash;
    }
}
