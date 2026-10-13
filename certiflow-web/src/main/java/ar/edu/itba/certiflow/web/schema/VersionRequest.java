package ar.edu.itba.certiflow.web.schema;

import ar.edu.itba.certiflow.schema.usecase.EffectiveFrom;
import ar.edu.itba.certiflow.schema.usecase.EffectiveOnPublication;
import ar.edu.itba.certiflow.schema.usecase.RequestedEffectiveDate;
import ar.edu.itba.certiflow.web.support.RequestFields;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public record VersionRequest(List<SectionPayload> sections, Instant effectiveFrom, Set<String> requiredSubsystems) {

    public VersionRequest {
        requiredSubsystems = requiredSubsystems == null ? Set.of() : Set.copyOf(requiredSubsystems);
        sections = RequestFields.requireElements(sections, "sections");
    }

    /** Sin fecha de vigencia pedida, la versión rige desde que se publica. */
    public RequestedEffectiveDate requestedEffectiveDate() {
        return Optional.ofNullable(effectiveFrom)
                .<RequestedEffectiveDate>map(EffectiveFrom::new)
                .orElseGet(EffectiveOnPublication::new);
    }
}
