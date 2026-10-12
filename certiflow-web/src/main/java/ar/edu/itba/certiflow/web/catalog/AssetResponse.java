package ar.edu.itba.certiflow.web.catalog;

import ar.edu.itba.certiflow.catalog.model.Asset;
import ar.edu.itba.certiflow.web.support.PersonPayload;
import java.util.List;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

@Relation(collectionRelation = "assets", itemRelation = "asset")
public class AssetResponse extends RepresentationModel<AssetResponse> {

    public final String code;
    public final String name;
    public final String type;
    public final String location;
    public final PersonPayload responsible;
    public final List<AssetRequest.CharacteristicPayload> characteristics;

    AssetResponse(Asset asset) {
        this.code = asset.code().value();
        this.name = asset.name();
        this.type = asset.type().name();
        this.location = asset.location().description();
        this.responsible = PersonPayload.of(asset.responsible());
        this.characteristics = asset.characteristics().entries().stream()
                .map(c -> new AssetRequest.CharacteristicPayload(c.name(), c.value())).toList();
    }
}
