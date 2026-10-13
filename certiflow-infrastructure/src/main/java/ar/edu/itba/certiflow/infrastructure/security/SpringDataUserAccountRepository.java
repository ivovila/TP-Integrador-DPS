package ar.edu.itba.certiflow.infrastructure.security;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataUserAccountRepository extends JpaRepository<UserAccountEntity, String> {
}
