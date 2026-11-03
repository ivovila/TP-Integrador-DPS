package ar.edu.itba.certiflow.web.schema;

import ar.edu.itba.certiflow.web.support.PersonPayload;
import java.time.Instant;

public record VersionCancellationView(PersonPayload cancelledBy, Instant cancelledAt, String reason) {
}
