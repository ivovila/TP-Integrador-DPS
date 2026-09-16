package ar.edu.itba.certiflow.application.port;

import ar.edu.itba.certiflow.domain.CorrectiveAction;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** save replaces the snapshot identified by id; missing lookups return Optional.empty(). */
public interface ActionRepository {
    Optional<CorrectiveAction> findById(UUID id);

    void save(CorrectiveAction value);

    List<CorrectiveAction> findByInspection(UUID inspectionId);
}
