package ar.edu.itba.certiflow.assistance.usecase;

import ar.edu.itba.certiflow.assistance.model.AssistantReview;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import java.util.List;

public interface ListAssistantReviewsUseCase {

    List<AssistantReview> execute(InspectionId inspectionId);
}