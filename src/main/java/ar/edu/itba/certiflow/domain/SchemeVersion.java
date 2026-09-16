package ar.edu.itba.certiflow.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Published snapshot. It has no editing operations. New rules require a new version. */
public record SchemeVersion(
        UUID schemeId,
        int number,
        String name,
        String assetType,
        List<Section> sections,
        int certificateValidityDays,
        Instant publishedAt,
        String publishedBy) {
    public SchemeVersion {
        Objects.requireNonNull(schemeId);
        Checks.require(number > 0, "Version must be positive");
        name = Checks.text(name, "name");
        assetType = Checks.text(assetType, "assetType");
        sections = List.copyOf(sections);
        Checks.require(!sections.isEmpty(), "Scheme needs sections");
        var all = sections.stream().flatMap(s -> s.criteria().stream()).toList();
        Checks.require(
                all.stream().map(Criterion::code).distinct().count() == all.size(),
                "Criterion codes must be unique in a version");
        Checks.require(certificateValidityDays > 0, "Validity must be positive");
        Objects.requireNonNull(publishedAt);
        publishedBy = Checks.text(publishedBy, "publishedBy");
    }

    public List<Criterion> criteria() {
        return sections.stream().flatMap(s -> s.criteria().stream()).toList();
    }

    public Criterion criterion(String code) {
        return criteria().stream()
                .filter(c -> c.code().equals(code))
                .findFirst()
                .orElseThrow(() -> new DomainException("Unknown criterion: " + code));
    }
}
