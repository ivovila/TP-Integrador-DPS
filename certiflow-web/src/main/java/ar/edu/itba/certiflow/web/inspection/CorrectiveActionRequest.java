package ar.edu.itba.certiflow.web.inspection;

import ar.edu.itba.certiflow.web.support.PersonPayload;
import ar.edu.itba.certiflow.web.support.RequestFields;
import java.time.LocalDate;

public record CorrectiveActionRequest(String description, PersonPayload responsible, LocalDate dueDate) {

    public CorrectiveActionRequest {
        RequestFields.require(description, "description");
        RequestFields.require(responsible, "responsible");
        RequestFields.require(dueDate, "dueDate");
    }
}
