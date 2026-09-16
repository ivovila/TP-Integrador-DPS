package ar.edu.itba.certiflow.application.port;

import ar.edu.itba.certiflow.domain.Inspection;

import java.util.Optional;
import java.util.UUID;

/** save replaces the snapshot identified by id; missing lookups return Optional.empty(). */
public interface InspectionRepository {
    Optional<Inspection> findById(UUID id);

    void save(Inspection value);
}
