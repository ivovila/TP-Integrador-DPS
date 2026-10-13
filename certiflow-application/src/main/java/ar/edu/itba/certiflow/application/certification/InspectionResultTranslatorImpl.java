package ar.edu.itba.certiflow.application.certification;

import ar.edu.itba.certiflow.catalog.model.exceptions.AssetNotFoundException;
import ar.edu.itba.certiflow.catalog.repository.AssetRepository;
import ar.edu.itba.certiflow.certification.model.*;
import ar.edu.itba.certiflow.inspection.model.*;
import ar.edu.itba.certiflow.inspection.model.exceptions.InspectionNotFoundException;
import ar.edu.itba.certiflow.inspection.repository.*;
import java.util.Comparator;
import java.util.Optional;

public final class InspectionResultTranslatorImpl implements InspectionResultTranslator {
    private final InspectionRepository inspections;
    private final FindingRepository findings;
    private final AssetRepository assets;

    public InspectionResultTranslatorImpl(InspectionRepository inspections, FindingRepository findings, AssetRepository assets) {
        this.inspections = inspections;
        this.findings = findings;
        this.assets = assets;
    }

    @Override
    public InspectionResult translate(InspectionId id) {
        var inspection = inspections.findById(id).orElseThrow(() -> new InspectionNotFoundException(id));
        var asset = assets.findByCode(inspection.assetCode()).orElseThrow(() -> new AssetNotFoundException(inspection.assetCode()));
        var open = findings.findByAsset(asset.code()).stream().filter(Finding::isOpen)
                .map(finding -> new OpenNonConformity(finding.criterion().code(), finding.severity(), finding.id().value(), finding.subsystemCode())).toList();
        var version = inspection.schemaVersion();
        var coveredCriteria = inspection.assignment().scope().criteriaIn(version);
        var subsystems = version.requiredSubsystems().stream().sorted(Comparator.comparing(code -> code.value()))
                .map(code -> new SubsystemResult(code, coveredCriteria.containsAll(version.criteriaFor(code)),
                        open.stream().filter(finding -> finding.subsystemCode().isEmpty() || finding.subsystemCode().filter(code::equals).isPresent()).toList()))
                .toList();
        return new InspectionResult(inspection.id(), asset.code(),
                inspection.isClosed() ? Optional.of(inspection.originalRevision().sealedAt()) : Optional.empty(),
                inspection.coversWholeSchema(), open, new SchemaReference(asset.type(), version.number()), subsystems,
                new CertificationScope.WholeAsset());
    }
}
