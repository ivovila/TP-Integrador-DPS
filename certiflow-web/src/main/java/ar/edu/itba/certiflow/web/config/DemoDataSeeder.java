package ar.edu.itba.certiflow.web.config;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.catalog.model.AssetType;
import ar.edu.itba.certiflow.catalog.model.Characteristics;
import ar.edu.itba.certiflow.catalog.model.JurisdictionCode;
import ar.edu.itba.certiflow.catalog.model.Location;
import ar.edu.itba.certiflow.catalog.model.Subsystem;
import ar.edu.itba.certiflow.catalog.model.SubsystemCode;
import ar.edu.itba.certiflow.catalog.usecase.ListAssetsUseCase;
import ar.edu.itba.certiflow.catalog.usecase.RegisterAssetCommand;
import ar.edu.itba.certiflow.catalog.usecase.RegisterAssetUseCase;
import ar.edu.itba.certiflow.certification.model.CertificateNumber;
import ar.edu.itba.certiflow.certification.model.SubsystemScope;
import ar.edu.itba.certiflow.certification.model.WholeAssetScope;
import ar.edu.itba.certiflow.certification.usecase.IssueCertificateCommand;
import ar.edu.itba.certiflow.certification.usecase.IssueCertificateUseCase;
import ar.edu.itba.certiflow.certification.usecase.IssueGlobalCertificateCommand;
import ar.edu.itba.certiflow.certification.usecase.IssueGlobalCertificateUseCase;
import ar.edu.itba.certiflow.certification.usecase.SuspendCertificateCommand;
import ar.edu.itba.certiflow.certification.usecase.SuspendCertificateUseCase;
import ar.edu.itba.certiflow.inspection.model.FullScope;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.inspection.usecase.AssignInspectionCommand;
import ar.edu.itba.certiflow.inspection.usecase.AssignInspectionUseCase;
import ar.edu.itba.certiflow.inspection.usecase.CloseInspectionCommand;
import ar.edu.itba.certiflow.inspection.usecase.CloseInspectionUseCase;
import ar.edu.itba.certiflow.inspection.usecase.RecordAnswerCommand;
import ar.edu.itba.certiflow.inspection.usecase.RecordAnswerUseCase;
import ar.edu.itba.certiflow.schema.model.Criterion;
import ar.edu.itba.certiflow.schema.model.Section;
import ar.edu.itba.certiflow.schema.model.StandardSeverity;
import ar.edu.itba.certiflow.schema.model.YesNoAnswer;
import ar.edu.itba.certiflow.schema.model.YesNoRule;
import ar.edu.itba.certiflow.schema.usecase.CreateInspectionSchemaCommand;
import ar.edu.itba.certiflow.schema.usecase.CreateInspectionSchemaUseCase;
import ar.edu.itba.certiflow.schema.usecase.PublishSchemaVersionCommand;
import ar.edu.itba.certiflow.schema.usecase.PublishSchemaVersionUseCase;
import ar.edu.itba.certiflow.shared.model.Person;
import ar.edu.itba.certiflow.shared.model.PersonId;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Datos de ejemplo para recorrer F1, F2 y F3 desde un cliente. Solo con el perfil {@code demo} y sobre una base vacía;
 * pasa por los casos de uso, así que deja la misma auditoría que dejaría un usuario.
 */
@Component
@Profile("demo")
public class DemoDataSeeder implements ApplicationRunner {

    private static final AssetType EXTINGUISHER = new AssetType("Fire extinguisher");
    private static final AssetType PRESSURE_VESSEL = new AssetType("Pressure vessel");
    private static final SubsystemCode ELECTRICAL = new SubsystemCode("ELECTRICAL");
    private static final SubsystemCode PRESSURE = new SubsystemCode("PRESSURE");
    private static final List<Subsystem> VESSEL_SUBSYSTEMS = List.of(
            new Subsystem(ELECTRICAL, "Electrical system"), new Subsystem(PRESSURE, "Pressure system"));

    private final Person coordinator = new Person(new PersonId("EMP-001"), "Paula Rios");
    private final Person inspector = new Person(new PersonId("EMP-002"), "Ines Duarte");
    private final Person assetResponsible = new Person(new PersonId("EMP-003"), "Raul Mendez");
    private final Person certifier = new Person(new PersonId("EMP-005"), "Carlos Vidal");

    private final ListAssetsUseCase listAssets;
    private final RegisterAssetUseCase registerAsset;
    private final CreateInspectionSchemaUseCase createSchema;
    private final PublishSchemaVersionUseCase publishVersion;
    private final AssignInspectionUseCase assignInspection;
    private final RecordAnswerUseCase recordAnswer;
    private final CloseInspectionUseCase closeInspection;
    private final IssueCertificateUseCase issueCertificate;
    private final IssueGlobalCertificateUseCase issueGlobalCertificate;
    private final SuspendCertificateUseCase suspendCertificate;
    private final Clock clock;

    public DemoDataSeeder(ListAssetsUseCase listAssets, RegisterAssetUseCase registerAsset, CreateInspectionSchemaUseCase createSchema,
                          PublishSchemaVersionUseCase publishVersion, AssignInspectionUseCase assignInspection,
                          RecordAnswerUseCase recordAnswer, CloseInspectionUseCase closeInspection,
                          IssueCertificateUseCase issueCertificate, IssueGlobalCertificateUseCase issueGlobalCertificate,
                          SuspendCertificateUseCase suspendCertificate, Clock clock) {
        this.listAssets = listAssets;
        this.registerAsset = registerAsset;
        this.createSchema = createSchema;
        this.publishVersion = publishVersion;
        this.assignInspection = assignInspection;
        this.recordAnswer = recordAnswer;
        this.closeInspection = closeInspection;
        this.issueCertificate = issueCertificate;
        this.issueGlobalCertificate = issueGlobalCertificate;
        this.suspendCertificate = suspendCertificate;
        this.clock = clock;
    }

    @Override
    public void run(ApplicationArguments arguments) {
        if (!listAssets.execute().isEmpty()) {
            return;
        }
        seedSchemas();
        seedAssets();
        seedExtinguisherInspections();
        seedBoilerInspections();
    }

    /** Un extintor con una versión programada a 30 días y un recipiente a presión con dos subsistemas. */
    private void seedSchemas() {
        createSchema.execute(new CreateInspectionSchemaCommand("Portable extinguisher inspection", EXTINGUISHER,
                extinguisherSections(false), coordinator, Set.of()));
        publishVersion.execute(new PublishSchemaVersionCommand(EXTINGUISHER, extinguisherSections(true), coordinator,
                clock.instant().plus(Duration.ofDays(30)), Set.of()));
        createSchema.execute(new CreateInspectionSchemaCommand("Pressure vessel inspection", PRESSURE_VESSEL,
                vesselSections(), coordinator, Set.of(ELECTRICAL, PRESSURE)));
    }

    /** Un activo por política de jurisdicción. */
    private void seedAssets() {
        register("EXT-0042", "Lobby extinguisher", EXTINGUISHER, "DEMO-STRICT", List.of());
        register("EXT-0043", "Server room extinguisher", EXTINGUISHER, "DEMO-STANDARD", List.of());
        register("BLR-0007", "Boiler B", PRESSURE_VESSEL, "DEMO-STRICT", VESSEL_SUBSYSTEMS);
        register("BLR-0012", "Boiler C", PRESSURE_VESSEL, "DEMO-CONDITIONAL", VESSEL_SUBSYSTEMS);
    }

    /** Uno rechazado por la política estricta y otro aprobado y certificado. */
    private void seedExtinguisherInspections() {
        inspectAndClose("EXT-0042", answers("SEAL", YesNoAnswer.NO, "SIGNAGE", YesNoAnswer.YES));
        InspectionId serverRoom = inspectAndClose("EXT-0043", answers("SEAL", YesNoAnswer.YES, "SIGNAGE", YesNoAnswer.YES));
        issueCertificate.execute(new IssueCertificateCommand(serverRoom, certifier, new WholeAssetScope()));
    }

    /** Parciales por subsistema: uno suspendido en la caldera B y uno condicional en la caldera C. */
    private void seedBoilerInspections() {
        InspectionId boilerB = inspectAndClose("BLR-0007", answers("RELIEF-VALVE", YesNoAnswer.YES, "INSULATION", YesNoAnswer.YES,
                "GROUNDING", YesNoAnswer.YES));
        List<CertificateNumber> boilerBPartials = certifyEverySubsystem("BLR-0007", boilerB);
        suspendCertificate.execute(new SuspendCertificateCommand(boilerBPartials.get(1), "Manufacturer recall of the relief valve",
                certifier));

        InspectionId boilerC = inspectAndClose("BLR-0012", answers("RELIEF-VALVE", YesNoAnswer.YES, "INSULATION", YesNoAnswer.NO,
                "GROUNDING", YesNoAnswer.YES));
        certifyEverySubsystem("BLR-0012", boilerC);
    }

    private static Map<String, YesNoAnswer> answers(Object... codesAndAnswers) {
        Map<String, YesNoAnswer> answers = new LinkedHashMap<>();
        for (int index = 0; index < codesAndAnswers.length; index += 2) {
            answers.put((String) codesAndAnswers[index], (YesNoAnswer) codesAndAnswers[index + 1]);
        }
        return answers;
    }

    private List<Section> extinguisherSections(boolean withAccess) {
        List<Criterion<?>> criteria = new ArrayList<>(List.of(
                yesNo("SEAL", "Safety seal intact", StandardSeverity.MAJOR),
                yesNo("SIGNAGE", "Signage visible from the corridor", StandardSeverity.MINOR)));
        if (withAccess) {
            criteria.add(yesNo("ACCESS", "Access to the extinguisher unobstructed", StandardSeverity.MAJOR));
        }
        return List.of(new Section("Condition", criteria));
    }

    private List<Section> vesselSections() {
        return List.of(
                new Section("Pressure system", List.of(
                        yesNo("RELIEF-VALVE", "Relief valve opens at its set pressure", StandardSeverity.CRITICAL),
                        yesNo("INSULATION", "Insulation in good condition", StandardSeverity.MINOR)),
                        PRESSURE),
                new Section("Electrical system", List.of(
                        yesNo("GROUNDING", "Burner grounding verified", StandardSeverity.MAJOR)),
                        ELECTRICAL));
    }

    private static Criterion<YesNoAnswer> yesNo(String code, String text, StandardSeverity severity) {
        return new Criterion<>(code, text, new YesNoRule(YesNoAnswer.YES), severity, List.of());
    }

    private void register(String code, String name, AssetType type, String jurisdiction, List<Subsystem> subsystems) {
        registerAsset.execute(new RegisterAssetCommand(new AssetCode(code), name, type, new Location("Main plant"),
                assetResponsible, Characteristics.none(), coordinator, new JurisdictionCode(jurisdiction), subsystems));
    }

    private InspectionId inspectAndClose(String assetCode, Map<String, YesNoAnswer> answers) {
        InspectionId inspection = assignInspection.execute(new AssignInspectionCommand(new AssetCode(assetCode), inspector,
                LocalDate.now(clock), new FullScope(), coordinator));
        answers.forEach((criterionCode, answer) ->
                recordAnswer.execute(new RecordAnswerCommand(inspection, criterionCode, answer.raw(), inspector)));
        closeInspection.execute(new CloseInspectionCommand(inspection, inspector));
        return inspection;
    }

    private List<CertificateNumber> certifyEverySubsystem(String assetCode, InspectionId inspection) {
        List<CertificateNumber> partials = VESSEL_SUBSYSTEMS.stream()
                .map(subsystem -> issueCertificate.execute(new IssueCertificateCommand(inspection, certifier,
                        new SubsystemScope(subsystem.code()))))
                .toList();
        issueGlobalCertificate.execute(new IssueGlobalCertificateCommand(new AssetCode(assetCode), 1, partials, certifier));
        return partials;
    }
}
