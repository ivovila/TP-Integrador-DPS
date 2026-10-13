package ar.edu.itba.certiflow.certification.model;

import ar.edu.itba.certiflow.catalog.model.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public interface GlobalCertificationPolicy {
    String code();
    CertificationDecision evaluate(AssetCode asset, SchemaReference schema, Set<SubsystemCode> required,
            List<Certificate> partials, JurisdictionCode jurisdiction, LocalDate date, Instant evaluatedAt);
}
