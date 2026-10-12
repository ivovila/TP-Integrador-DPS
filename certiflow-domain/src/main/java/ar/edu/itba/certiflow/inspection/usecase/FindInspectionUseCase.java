package ar.edu.itba.certiflow.inspection.usecase;

import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.inspection.model.InspectionView;

public interface FindInspectionUseCase {

    InspectionView execute(InspectionId id);
}
