package ar.edu.itba.certiflow.web.catalog;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import ar.edu.itba.certiflow.web.support.Uris;
import ar.edu.itba.certiflow.catalog.model.Asset;
import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.catalog.model.AssetType;
import ar.edu.itba.certiflow.catalog.usecase.FindAssetUseCase;
import ar.edu.itba.certiflow.catalog.usecase.ListAssetsUseCase;
import ar.edu.itba.certiflow.catalog.usecase.RegisterAssetUseCase;
import ar.edu.itba.certiflow.shared.model.Person;
import ar.edu.itba.certiflow.shared.usecase.AuditHistoryUseCase;
import ar.edu.itba.certiflow.shared.usecase.AuditSubject;
import ar.edu.itba.certiflow.web.support.ActingPerson;
import ar.edu.itba.certiflow.web.support.AuditEntryResponse;
import ar.edu.itba.certiflow.web.support.PageLinks;
import java.util.Optional;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/assets")
public class AssetController {

    private final RegisterAssetUseCase registerAsset;
    private final FindAssetUseCase findAsset;
    private final ListAssetsUseCase listAssets;
    private final AuditHistoryUseCase auditHistory;

    public AssetController(RegisterAssetUseCase registerAsset, FindAssetUseCase findAsset, ListAssetsUseCase listAssets,
                           AuditHistoryUseCase auditHistory) {
        this.registerAsset = registerAsset;
        this.findAsset = findAsset;
        this.listAssets = listAssets;
        this.auditHistory = auditHistory;
    }

    @PostMapping
    public ResponseEntity<AssetResponse> register(@RequestBody AssetRequest request, @ActingPerson Person registeredBy) {
        AssetResponse response = resource(registerAsset.execute(request.toCommand(registeredBy)));
        return ResponseEntity.created(response.getRequiredLink("self").toUri()).body(response);
    }

    @GetMapping("/{code}")
    public AssetResponse find(@PathVariable String code) {
        return resource(findAsset.execute(new AssetCode(code)));
    }

    @GetMapping
    public PagedModel<AssetResponse> list(@RequestParam Optional<String> type, @RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "20") int size) {
        return PageLinks.page(listAssets.execute(type.map(AssetType::new)).stream().map(this::resource).toList(), page, size,
                "/api/v1/assets", Uris.query("type", type).orElse(""));
    }

    @GetMapping("/{code}/audit-entries")
    public CollectionModel<AuditEntryResponse> history(@PathVariable String code) {
        return CollectionModel.of(auditHistory.execute(new AuditSubject.OfAsset(new AssetCode(code))).stream()
                        .map(AuditEntryResponse::of).toList(),
                linkTo(methodOn(AssetController.class).history(code)).withSelfRel());
    }

    private AssetResponse resource(Asset asset) {
        String code = asset.code().value();
        AssetResponse response = new AssetResponse(asset);
        response.add(linkTo(methodOn(AssetController.class).find(code)).withSelfRel(),
                Link.of(Uris.path("assets", code, "findings-summary")).withRel("findings-summary"),
                linkTo(methodOn(AssetController.class).history(code)).withRel("audit-entries"),
                Link.of("/api/v1/certificates?" + Uris.query("asset", Optional.of(code)).orElseThrow()).withRel("certificates"),
                Link.of("/api/v1/findings?" + Uris.query("asset", Optional.of(code)).orElseThrow()).withRel("findings"));
        return response;
    }
}
