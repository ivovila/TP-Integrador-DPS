package ar.edu.itba.certiflow.inspection.usecase;

import ar.edu.itba.certiflow.inspection.model.FindingId;
import java.util.List;

public interface RectifyInspectionUseCase {

    List<FindingId> execute(RectifyInspectionCommand command);
}
