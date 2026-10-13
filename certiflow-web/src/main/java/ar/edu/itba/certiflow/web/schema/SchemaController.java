package ar.edu.itba.certiflow.web.schema;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import ar.edu.itba.certiflow.catalog.model.AssetType;
import ar.edu.itba.certiflow.catalog.model.SubsystemCode;
import ar.edu.itba.certiflow.schema.model.InspectionSchema;
import ar.edu.itba.certiflow.schema.model.SchemaVersion;
import ar.edu.itba.certiflow.schema.usecase.CreateInspectionSchemaCommand;
import ar.edu.itba.certiflow.schema.usecase.CreateInspectionSchemaUseCase;
import ar.edu.itba.certiflow.schema.usecase.FindEffectiveSchemaVersionUseCase;
import ar.edu.itba.certiflow.schema.usecase.FindSchemaUseCase;
import ar.edu.itba.certiflow.schema.usecase.PublishSchemaVersionCommand;
import ar.edu.itba.certiflow.schema.usecase.PublishSchemaVersionUseCase;
import ar.edu.itba.certiflow.shared.model.Person;
import ar.edu.itba.certiflow.shared.usecase.AuditHistoryUseCase;
import ar.edu.itba.certiflow.shared.usecase.AuditSubject;
import ar.edu.itba.certiflow.web.support.ActingPerson;
import ar.edu.itba.certiflow.web.support.AuditEntryResponse;
import ar.edu.itba.certiflow.web.support.ResourceNotFoundException;
import ar.edu.itba.certiflow.web.support.Uris;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;

@RestController
@RequestMapping("/api/v1/schemas")
public class SchemaController {

    private final CreateInspectionSchemaUseCase createInspectionSchema;
    private final PublishSchemaVersionUseCase publishSchemaVersion;
    private final FindSchemaUseCase findSchema;
    private final AuditHistoryUseCase auditHistory;
    private final Clock clock;
    private final FindEffectiveSchemaVersionUseCase findEffective;

    public SchemaController(CreateInspectionSchemaUseCase createInspectionSchema, PublishSchemaVersionUseCase publishSchemaVersion,
                            FindSchemaUseCase findSchema, AuditHistoryUseCase auditHistory, Clock clock, FindEffectiveSchemaVersionUseCase findEffective) {
        this.createInspectionSchema = createInspectionSchema;
        this.publishSchemaVersion = publishSchemaVersion;
        this.findSchema = findSchema;
        this.auditHistory = auditHistory;
        this.clock = clock;
        this.findEffective = findEffective;
    }

    @PostMapping
    public ResponseEntity<SchemaResponse> create(@RequestBody SchemaRequest request, @ActingPerson Person createdBy) {
        InspectionSchema schema = createInspectionSchema.execute(new CreateInspectionSchemaCommand(request.name(),
                new AssetType(request.assetType()), SchemaPayloads.toSections(request.sections()), createdBy, request.requiredSubsystems().stream().map(SubsystemCode::new).collect(Collectors.toSet())));
        SchemaResponse response = resource(schema);
        return ResponseEntity.created(response.getRequiredLink("self").toUri()).body(response);
    }

    @GetMapping("/{assetType}")
    public ResponseEntity<SchemaResponse> find(@PathVariable String assetType) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(resource(findSchema.execute(new AssetType(assetType))));
    }

    @PostMapping("/{assetType}/versions")
    public ResponseEntity<VersionResponse> publish(@PathVariable String assetType, @RequestBody VersionRequest request,
                                                   @ActingPerson Person publishedBy) {
        SchemaVersion version = publishSchemaVersion.execute(new PublishSchemaVersionCommand(new AssetType(assetType),
                SchemaPayloads.toSections(request.sections()), publishedBy, Optional.ofNullable(request.effectiveFrom()), request.requiredSubsystems().stream().map(SubsystemCode::new).collect(Collectors.toSet())));
        VersionResponse response = versionResource(assetType, version);
        return ResponseEntity.created(response.getRequiredLink("self").toUri()).body(response);
    }

    @GetMapping("/{assetType}/effective-version")
    public ResponseEntity<VersionResponse> effective(@PathVariable String assetType,
            @RequestParam Instant at) {
        VersionResponse response = versionResource(assetType,
                findEffective.execute(new AssetType(assetType), at));
        response.resolvedAt = clock.instant();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(response);
    }

    @GetMapping("/{assetType}/versions")
    public CollectionModel<VersionResponse> versions(@PathVariable String assetType) {
        return CollectionModel.of(findSchema.execute(new AssetType(assetType)).versions().stream()
                        .map(version -> versionResource(assetType, version)).toList(),
                linkTo(methodOn(SchemaController.class).versions(assetType)).withSelfRel());
    }

    @GetMapping("/{assetType}/versions/{number}")
    public ResponseEntity<VersionResponse> version(@PathVariable String assetType, @PathVariable int number, WebRequest request) {
        InspectionSchema schema = findSchema.execute(new AssetType(assetType));
        String etag = Uris.etag("schema", assetType, String.valueOf(number));
        return schema.versions().stream()
                .filter(version -> version.number() == number)
                .findFirst()
                .map(version -> request.checkNotModified(etag)
                        ? ResponseEntity.status(304).eTag(etag).<VersionResponse>build()
                        : ResponseEntity.ok().eTag(etag).cacheControl(CacheControl.maxAge(Duration.ofDays(1)))
                                .body(versionResource(assetType, version)))
                .orElseThrow(() -> new ResourceNotFoundException("Schema " + assetType + " has no version " + number));
    }

    @GetMapping("/{assetType}/audit-entries")
    public CollectionModel<AuditEntryResponse> history(@PathVariable String assetType) {
        return CollectionModel.of(auditHistory.execute(new AuditSubject.OfSchema(new AssetType(assetType))).stream()
                        .map(AuditEntryResponse::of).toList(),
                linkTo(methodOn(SchemaController.class).history(assetType)).withSelfRel());
    }

    private SchemaResponse resource(InspectionSchema schema) {
        String assetType = schema.assetType().name();
        Instant now = clock.instant();
        SchemaResponse response = new SchemaResponse(schema, now);
        response.add(linkTo(methodOn(SchemaController.class).find(assetType)).withSelfRel(),
                linkTo(methodOn(SchemaController.class).versions(assetType)).withRel("versions"),
                linkTo(methodOn(SchemaController.class).version(assetType, schema.versionAt(now).number(), null))
                        .withRel("current-version"),
                linkTo(methodOn(SchemaController.class).history(assetType)).withRel("audit-entries"),
                linkTo(methodOn(SchemaController.class).version(assetType, schema.latestPublishedVersion().number(), null)).withRel("latest-published-version"));
        return response;
    }

    private VersionResponse versionResource(String assetType, SchemaVersion version) {
        VersionResponse response = new VersionResponse(version);
        response.add(linkTo(methodOn(SchemaController.class).version(assetType, version.number(), null)).withSelfRel(),
                linkTo(methodOn(SchemaController.class).find(assetType)).withRel("schema"));
        return response;
    }
}
