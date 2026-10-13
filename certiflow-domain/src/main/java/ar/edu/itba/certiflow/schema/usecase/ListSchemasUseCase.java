package ar.edu.itba.certiflow.schema.usecase;

import ar.edu.itba.certiflow.schema.model.InspectionSchema;
import java.util.List;

public interface ListSchemasUseCase {

    List<InspectionSchema> execute();
}
