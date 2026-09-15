package ar.edu.itba.certiflow.domain.model.finding;

import java.time.LocalDateTime;

import ar.edu.itba.certiflow.domain.model.shared.PersonId;

public record Verification(PersonId verifier, LocalDateTime verifiedAt) {
}
