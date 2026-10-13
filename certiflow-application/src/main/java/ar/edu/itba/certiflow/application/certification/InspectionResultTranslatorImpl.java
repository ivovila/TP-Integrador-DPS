package ar.edu.itba.certiflow.application.certification;

import ar.edu.itba.certiflow.catalog.model.Asset;
import ar.edu.itba.certiflow.catalog.model.SubsystemCode;
import ar.edu.itba.certiflow.catalog.model.exceptions.AssetNotFoundException;
import ar.edu.itba.certiflow.catalog.repository.AssetRepository;
import ar.edu.itba.certiflow.certification.model.CertificationScope;
import ar.edu.itba.certiflow.certification.model.InspectionResult;
import ar.edu.itba.certiflow.certification.model.InspectionResultTranslator;
import ar.edu.itba.certiflow.certification.model.OpenNonConformity;
import ar.edu.itba.certiflow.certification.model.SchemaReference;
import ar.edu.itba.certiflow.certification.model.SubsystemResult;
import ar.edu.itba.certiflow.inspection.model.Finding;
import ar.edu.itba.certiflow.inspection.model.Inspection;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.inspection.model.exceptions.InspectionNotFoundException;
import ar.edu.itba.certiflow.inspection.repository.FindingRepository;
import ar.edu.itba.certiflow.inspection.repository.InspectionRepository;
import ar.edu.itba.certiflow.schema.model.Criterion;
import ar.edu.itba.certiflow.schema.model.SchemaVersion;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class InspectionResultTranslatorImpl implements InspectionResultTranslator {

    private final InspectionRepository inspectionRepository;
    private final FindingRepository findingRepository;
    private final AssetRepository assetRepository;

    public InspectionResultTranslatorImpl(InspectionRepository inspectionRepository, FindingRepository findingRepository,
                                          AssetRepository assetRepository) {
        this.inspectionRepository = inspectionRepository;
        this.findingRepository = findingRepository;
        this.assetRepository = assetRepository;
    }

    @Override
    public InspectionResult translate(InspectionId inspectionId) {
        Inspection inspection = inspectionRepository.findById(inspectionId)
                .orElseThrow(() -> new InspectionNotFoundException(inspectionId));
        Asset asset = assetRepository.findByCode(inspection.assetCode())
                .orElseThrow(() -> new AssetNotFoundException(inspection.assetCode()));
        List<OpenNonConformity> open = openNonConformitiesOf(asset);
        SchemaVersion version = inspection.schemaVersion();
        return new InspectionResult(inspection.id(), asset.code(), closingInstantOf(inspection),
                inspection.coversWholeSchema(), open, new SchemaReference(asset.type(), version.number()),
                resultsBySubsystem(inspection, version, open), new CertificationScope.WholeAsset());
    }

    /** Los hallazgos abiertos del activo, incluidos los de otras inspecciones, con la severidad de su criterio. */
    private List<OpenNonConformity> openNonConformitiesOf(Asset asset) {
        return findingRepository.findByAsset(asset.code()).stream().filter(Finding::isOpen)
                .map(finding -> new OpenNonConformity(finding.criterion().code(), finding.severity(),
                        finding.id().value(), finding.subsystemCode()))
                .toList();
    }

    private static Optional<Instant> closingInstantOf(Inspection inspection) {
        return inspection.isClosed() ? Optional.of(inspection.originalRevision().sealedAt()) : Optional.empty();
    }

    /**
     * Un subsistema está cubierto solo si el alcance de la inspección incluye todos los criterios que la
     * versión le asigna: no alcanza con haber inspeccionado una sección si otra también le aporta criterios.
     */
    private static List<SubsystemResult> resultsBySubsystem(Inspection inspection, SchemaVersion version,
                                                            List<OpenNonConformity> open) {
        List<Criterion<?>> coveredCriteria = inspection.assignment().scope().criteriaIn(version);
        return version.requiredSubsystems().stream().sorted(Comparator.comparing(SubsystemCode::value))
                .map(code -> new SubsystemResult(code, coveredCriteria.containsAll(version.criteriaFor(code)),
                        affecting(code, open)))
                .toList();
    }

    /** Una no conformidad sin subsistema viene de una inspección sin subsistemas, así que afecta a todos. */
    private static List<OpenNonConformity> affecting(SubsystemCode code, List<OpenNonConformity> open) {
        return open.stream().filter(nonConformity -> nonConformity.subsystemCode().isEmpty()
                || nonConformity.subsystemCode().filter(code::equals).isPresent()).toList();
    }
}
