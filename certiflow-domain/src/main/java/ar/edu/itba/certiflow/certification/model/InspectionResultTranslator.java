package ar.edu.itba.certiflow.certification.model;

import ar.edu.itba.certiflow.inspection.model.InspectionId;

public interface InspectionResultTranslator {

    InspectionResult translate(InspectionId inspectionId);
}
