package ar.edu.itba.certiflow.application.port;

import ar.edu.itba.certiflow.domain.SchemeVersion;

import java.util.List;
import java.util.UUID;

public interface SchemeRepository {
    List<SchemeVersion> versions(UUID schemeId);

    /** Append only. Duplicate version numbers must fail; published snapshots cannot be replaced. */
    void publish(SchemeVersion version);
}
