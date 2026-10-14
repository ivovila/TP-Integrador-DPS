package ar.edu.itba.certiflow.scenario.support;

import ar.edu.itba.certiflow.web.support.TestDatabase;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Escenarios de negocio sobre la aplicación real: casos de uso con sus transacciones, repositorios JPA y H2.
 * Cada escenario parte de una base vacía y de un reloj que el test puede adelantar.
 */
@SpringBootTest(properties = {TestDatabase.URL, TestDatabase.USER, TestDatabase.PASSWORD})
@Import(ScenarioTest.AdjustableClock.class)
public abstract class ScenarioTest {

    private static final Instant START = Instant.parse("2026-03-10T09:00:00Z");

    @TestConfiguration
    static class AdjustableClock {
        @Bean
        @Primary
        MutableClock mutableClock() {
            return new MutableClock(START);
        }
    }

    @Autowired
    private ApplicationContext context;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private MutableClock clock;

    protected CertiflowFixture fixture;

    @BeforeEach
    void startFromAnEmptyApplication() {
        TestDatabase.empty(jdbcTemplate);
        clock.reset(START);
        fixture = new CertiflowFixture(context, clock);
    }

    /**
     * También al terminar: los tests de API comparten esta misma base H2 y la vacían después de cada uno,
     * así que un escenario que dejara sus filas rompería el primer test de la clase de API que siguiera.
     */
    @AfterEach
    void leaveTheApplicationEmpty() {
        TestDatabase.empty(jdbcTemplate);
    }
}
