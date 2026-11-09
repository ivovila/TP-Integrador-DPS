package ar.edu.itba.certiflow.harness.golden;

import ar.edu.itba.certiflow.application.assistance.InspectionReviewTranslatorImpl;
import ar.edu.itba.certiflow.assistance.model.InspectionReviewTranslator;
import ar.edu.itba.certiflow.assistance.model.InspectionUnderReview;
import ar.edu.itba.certiflow.harness.golden.DemoAssets.DemoAsset;
import ar.edu.itba.certiflow.harness.golden.GoldenCase.GoldenCriterion;
import ar.edu.itba.certiflow.harness.golden.GoldenCase.GoldenEvidence;
import ar.edu.itba.certiflow.inspection.model.FullScope;
import ar.edu.itba.certiflow.inspection.model.Inspection;
import ar.edu.itba.certiflow.inspection.model.InspectionAssignment;
import ar.edu.itba.certiflow.inspection.model.InspectionFactory;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.inspection.model.InspectionService;
import ar.edu.itba.certiflow.inspection.repository.InspectionRepository;
import ar.edu.itba.certiflow.schema.model.CriterionCode;
import ar.edu.itba.certiflow.schema.model.Evidence;
import ar.edu.itba.certiflow.schema.model.EvidenceKind;
import ar.edu.itba.certiflow.schema.model.EvidenceKindCatalog;
import ar.edu.itba.certiflow.schema.model.RawAnswer;
import ar.edu.itba.certiflow.schema.model.StandardEvidenceKind;
import ar.edu.itba.certiflow.shared.model.Person;
import ar.edu.itba.certiflow.shared.model.PersonId;
import ar.edu.itba.certiflow.shared.model.Stamp;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Convierte un caso en lo que el asistente lee. No arma {@code InspectionUnderReview} a mano: carga el caso en una
 * inspección del dominio, con las mismas operaciones que usa el inspector, y la traduce con el traductor de la
 * aplicación. Así la regla en palabras, la respuesta y el resultado son los que vería el modelo en producción.
 */
public final class CaseInspections {

    private static final Person PLANNER = new Person(new PersonId("HARNESS-PLANNER"), "Golden set planner");
    private static final Person INSPECTOR = new Person(new PersonId("HARNESS-INSPECTOR"), "Golden set inspector");

    private final DemoAssets demoAssets = new DemoAssets();
    private final EvidenceKindCatalog evidenceKinds = new EvidenceKindCatalog(List.of(StandardEvidenceKind.values()));
    private final InspectionRepository inspections = new InMemoryInspectionRepository();
    private final InspectionService inspectionService = new InspectionService(new InspectionFactory(),
            new UnrecordedAuditLog<InspectionId>());
    private final InspectionReviewTranslator translator = new InspectionReviewTranslatorImpl(inspections);
    private final Clock clock;

    public CaseInspections(Clock clock) {
        this.clock = clock;
    }

    public InspectionUnderReview underReview(GoldenCase goldenCase) {
        DemoAsset demo = demoAssets.of(goldenCase.assetType());
        Inspection inspection = inspectionService.assign(inspections.nextId(), demo.asset(),
                new InspectionAssignment(INSPECTOR, LocalDate.now(clock), new FullScope()),
                demo.schema().version(goldenCase.schemaVersion()), new Stamp(PLANNER, clock.instant()));
        Stamp inspected = new Stamp(INSPECTOR, clock.instant());
        goldenCase.criteria().forEach(criterion -> load(inspection, criterion, inspected));
        inspections.save(inspection);
        return translator.translate(inspection.id());
    }

    private void load(Inspection inspection, GoldenCriterion criterion, Stamp stamp) {
        CriterionCode code = new CriterionCode(criterion.code());
        inspectionService.recordAnswer(inspection, code, new RawAnswer(criterion.answer()), stamp);
        criterion.observations().forEach(text -> inspectionService.addObservation(inspection, code, text, stamp));
        IntStream.range(0, criterion.evidence().size()).forEach(index -> inspectionService.attachEvidence(inspection, code,
                evidence(criterion, index), stamp));
    }

    private Evidence evidence(GoldenCriterion criterion, int index) {
        GoldenEvidence evidence = criterion.evidence().get(index);
        return new Evidence(kind(evidence.kind()), "golden/" + criterion.code() + "-" + (index + 1), evidence.description());
    }

    private EvidenceKind kind(String code) {
        return evidenceKinds.withCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Unknown evidence kind in a golden case: " + code));
    }
}
