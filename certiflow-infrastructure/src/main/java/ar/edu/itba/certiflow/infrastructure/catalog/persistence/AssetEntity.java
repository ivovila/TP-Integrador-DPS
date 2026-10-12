package ar.edu.itba.certiflow.infrastructure.catalog.persistence;

import ar.edu.itba.certiflow.catalog.model.Asset;
import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.catalog.model.AssetType;
import ar.edu.itba.certiflow.catalog.model.Characteristic;
import ar.edu.itba.certiflow.catalog.model.Characteristics;
import ar.edu.itba.certiflow.catalog.model.Location;
import ar.edu.itba.certiflow.infrastructure.shared.persistence.PersonColumns;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "asset")
public class AssetEntity {

    @Id
    private String code;
    private String name;
    private String type;
    private String location;
    @Column(name = "responsible_id")
    private String responsibleId;
    @Column(name = "responsible_name")
    private String responsibleName;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "asset_characteristic", joinColumns = @JoinColumn(name = "asset_code"))
    @OrderColumn(name = "position")
    private List<AssetCharacteristicEmbeddable> characteristics = new ArrayList<>();

    protected AssetEntity() {
    }

    static AssetEntity from(Asset asset) {
        AssetEntity entity = new AssetEntity();
        entity.code = asset.code().value();
        entity.name = asset.name();
        entity.type = asset.type().name();
        entity.location = asset.location().description();
        PersonColumns responsible = PersonColumns.of(asset.responsible());
        entity.responsibleId = responsible.id();
        entity.responsibleName = responsible.name();
        asset.characteristics().entries().forEach(characteristic -> entity.characteristics.add(
                new AssetCharacteristicEmbeddable(characteristic.name(), characteristic.value())));
        return entity;
    }

    Asset toDomain() {
        return new Asset(new AssetCode(code), name, new AssetType(type), new Location(location),
                new PersonColumns(responsibleId, responsibleName).toDomain(),
                new Characteristics(characteristics.stream()
                        .map(characteristic -> new Characteristic(characteristic.name(), characteristic.value()))
                        .toList()));
    }
}
