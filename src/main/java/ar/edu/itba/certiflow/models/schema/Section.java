package ar.edu.itba.certiflow.models.schema;

import ar.edu.itba.certiflow.models.evaluation.Criterion;
import java.util.List;

public record Section(String title, List<Criterion<?>> criteria) {
}
