package ar.edu.itba.certiflow.domain.ports;

import java.time.LocalDate;
import java.time.LocalDateTime;

public interface Clock {

    LocalDateTime now();

    default LocalDate today() {
        return now().toLocalDate();
    }
}
