package ar.edu.itba.certiflow.application.certification;

import ar.edu.itba.certiflow.catalog.model.Asset;
import ar.edu.itba.certiflow.catalog.model.SubsystemCode;
import ar.edu.itba.certiflow.catalog.model.exceptions.AssetNotFoundException;
import ar.edu.itba.certiflow.catalog.repository.AssetRepository;
import ar.edu.itba.certiflow.certification.model.ClosedAt;
import ar.edu.itba.certiflow.certification.model.InspectionClosure;
import ar.edu.itba.certiflow.certification.model.InspectionResult;
import ar.edu.itba.certiflow.certification.model.InspectionResultTranslator;
import ar.edu.itba.certiflow.certification.model.OpenNonConformity;
import ar.edu.itba.certiflow.certification.model.SchemaReference;
import ar.edu.itba.certiflow.certification.model.StillOpen;
import ar.edu.itba.certiflow.certification.model.SubsystemResult;
import ar.edu.itba.certiflow.certification.model.WholeAssetScope;
import ar.edu.itba.certiflow.inspection.model.Finding;
import ar.edu.itba.certiflow.inspection.model.Inspection;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.inspection.model.exceptions.InspectionNotFoundException;
import ar.edu.itba.certiflow.inspection.repository.FindingRepository;
import ar.edu.itba.certiflow.inspection.repository.InspectionRepository;
import ar.edu.itba.certiflow.schema.model.SchemaVersion;
import java.util.Comparator;
import java.util.List;

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
        InspectionClosure closure = inspection.closedAt().<InspectionClosure>map(ClosedAt::new).orElseGet(StillOpen::new);
        return new InspectionResult(inspection.id(), asset.code(), closure,
                inspection.coversWholeSchema(), open, new SchemaReference(asset.type(), version.number()),
                resultsBySubsystem(inspection, version, open), new WholeAssetScope());
    }

    /** Los hallazgos abiertos del activo, incluidos los de otras inspecciones, con la severidad de su criterio. */
    private List<OpenNonConformity> openNonConformitiesOf(Asset asset) {
        return findingRepository.findByAsset(asset.code()).stream().filter(Finding::isOpen)
                .map(finding -> new OpenNonConformity(finding.criterion().code(), finding.severity(),
                        finding.id().value(), finding.subsystem()))
                .toList();
    }

    private List<SubsystemResult> resultsBySubsystem(Inspection inspection, SchemaVersion version,
                                                            List<OpenNonConformity> open) {
        return version.requiredSubsystems().stream().sorted(Comparator.comparing(SubsystemCode::value))
                .map(code -> new SubsystemResult(code, inspection.covers(code),
                        open.stream().filter(nonConformity -> nonConformity.affects(code)).toList()))
                .toList();
    }
}
