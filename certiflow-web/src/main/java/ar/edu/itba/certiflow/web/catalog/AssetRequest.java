package ar.edu.itba.certiflow.web.catalog;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.catalog.model.AssetType;
import ar.edu.itba.certiflow.catalog.model.Characteristic;
import ar.edu.itba.certiflow.catalog.model.Characteristics;
import ar.edu.itba.certiflow.catalog.model.JurisdictionCode;
import ar.edu.itba.certiflow.catalog.model.Location;
import ar.edu.itba.certiflow.catalog.model.Subsystem;
import ar.edu.itba.certiflow.catalog.model.SubsystemCode;
import ar.edu.itba.certiflow.catalog.usecase.RegisterAssetCommand;
import ar.edu.itba.certiflow.shared.model.Person;
import ar.edu.itba.certiflow.web.support.PersonPayload;
import ar.edu.itba.certiflow.web.support.RequestFields;
import java.util.List;

public record AssetRequest(String code, String name, String type, String location, PersonPayload responsible,
                           List<CharacteristicPayload> characteristics, String jurisdictionCode, List<SubsystemPayload> subsystems) {

    public record SubsystemPayload(String code, String name) {
        public SubsystemPayload { RequestFields.require(code, "subsystems.code"); RequestFields.require(name, "subsystems.name"); }
    }

    public record CharacteristicPayload(String name, String value) {

        public CharacteristicPayload {
            RequestFields.require(name, "characteristics.name");
            RequestFields.require(value, "characteristics.value");
        }
    }

    public AssetRequest {
        RequestFields.require(jurisdictionCode, "jurisdictionCode");
        subsystems = RequestFields.requireElements(subsystems, "subsystems");
        RequestFields.require(code, "code");
        RequestFields.require(name, "name");
        RequestFields.require(type, "type");
        RequestFields.require(location, "location");
        RequestFields.require(responsible, "responsible");
        characteristics = RequestFields.requireElements(characteristics, "characteristics");
    }

    RegisterAssetCommand toCommand(Person registeredBy) {
        return new RegisterAssetCommand(new AssetCode(code), name, new AssetType(type), new Location(location), responsible.toDomain(),
                new Characteristics(characteristics.stream().map(c -> new Characteristic(c.name(), c.value())).toList()),
                registeredBy, new JurisdictionCode(jurisdictionCode), subsystems.stream().map(item -> new Subsystem(new SubsystemCode(item.code()), item.name())).toList());
    }
}
