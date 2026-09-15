package ar.edu.itba.certiflow.domain.model.shared;

import static ar.edu.itba.certiflow.support.Fixtures.photo;
import static ar.edu.itba.certiflow.support.Fixtures.pressure;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

class ResponseTest {

    private final Measurement temperature = new Measurement("temperatura", new BigDecimal("35"), "C");

    @Test
    void measurementsOfDifferentMagnitudesCoexist() {
        Response response = Response.empty().with(pressure("11")).with(temperature);

        assertEquals(List.of(pressure("11"), temperature), response.all(Measurement.class));
    }

    @Test
    void newMeasurementOfTheSameMagnitudeReplacesThePreviousOne() {
        Response response = Response.empty().with(pressure("14")).with(temperature).with(pressure("11"));

        assertEquals(List.of(temperature, pressure("11")), response.all(Measurement.class));
    }

    @Test
    void newAnswerReplacesThePreviousOne() {
        Response response = Response.empty().with(new YesNo(true)).with(new YesNo(false));

        assertEquals(List.of(new YesNo(false)), response.all(YesNo.class));
    }

    @Test
    void valuesOfEachTypeAreQueriedSeparately() {
        Response response = Response.empty()
                .with(pressure("11"))
                .with(new SelectedOption("rojo"))
                .with(new YesNo(true))
                .withEvidence(photo());

        assertEquals(List.of(new SelectedOption("rojo")), response.all(SelectedOption.class));
        assertEquals(3, response.values().size());
        assertEquals(1, response.evidences().size());
    }
}
