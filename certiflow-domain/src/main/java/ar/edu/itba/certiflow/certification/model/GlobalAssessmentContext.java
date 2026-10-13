package ar.edu.itba.certiflow.certification.model;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.catalog.model.JurisdictionCode;
import ar.edu.itba.certiflow.catalog.model.SubsystemCode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/** Lo que una política global evalúa: los parciales propuestos para un activo y versión de esquema. */
public record GlobalAssessmentContext(AssetCode assetCode, SchemaReference schemaReference, Set<SubsystemCode> requiredSubsystems,
                                      List<Certificate> partials, JurisdictionCode jurisdictionCode, LocalDate issuanceDate,
                                      Instant evaluatedAt) {

    public GlobalAssessmentContext {
        requiredSubsystems = Set.copyOf(requiredSubsystems);
        partials = List.copyOf(partials);
    }
}
