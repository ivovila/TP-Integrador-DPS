package ar.edu.itba.certiflow.web.schema;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ar.edu.itba.certiflow.schema.usecase.EffectiveFrom;
import ar.edu.itba.certiflow.schema.usecase.EffectiveOnPublication;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Set;
import org.junit.jupiter.api.Test;

class VersionRequestTest {

    @Test
    void theRequestedEffectiveDateIsTruncatedToThePrecisionThatIsStored() {
        VersionRequest request = new VersionRequest(new ArrayList<>(), Instant.parse("2026-11-01T03:00:00.123456789Z"), Set.of());

        assertEquals(new EffectiveFrom(Instant.parse("2026-11-01T03:00:00.123456Z")), request.requestedEffectiveDate());
    }

    @Test
    void withoutARequestedDateTheVersionIsEffectiveOnPublication() {
        VersionRequest request = new VersionRequest(new ArrayList<>(), null, Set.of());

        assertEquals(new EffectiveOnPublication(), request.requestedEffectiveDate());
    }
}
