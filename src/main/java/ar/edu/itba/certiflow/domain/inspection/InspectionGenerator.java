package ar.edu.itba.certiflow.domain.inspection;

import ar.edu.itba.certiflow.domain.asset.Asset;
import ar.edu.itba.certiflow.domain.schema.SchemaVersion;
import ar.edu.itba.certiflow.domain.shared.Person;
import java.time.Instant;

public interface InspectionGenerator {

    Inspection generate(Asset asset, InspectionAssignment assignment, SchemaVersion schemaVersion, Person by,
                        Instant at);
}
