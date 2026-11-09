package ar.edu.itba.certiflow.harness;

import ar.edu.itba.certiflow.assistance.model.InspectionAssistant;
import ar.edu.itba.certiflow.harness.golden.CaseInspections;
import ar.edu.itba.certiflow.infrastructure.assistance.SpringAiAssistantConfiguration;
import ar.edu.itba.certiflow.infrastructure.assistance.StubInspectionAssistant;
import ar.edu.itba.certiflow.schema.model.SeverityCatalog;
import ar.edu.itba.certiflow.schema.model.StandardSeverity;
import java.nio.file.Path;
import java.time.Clock;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

/**
 * Compone el asistente con la misma configuración que la aplicación ({@link SpringAiAssistantConfiguration} y las
 * mismas properties), así el harness mide lo que corre en producción. Sin Spring Boot: no levanta base de datos ni
 * servidor, solo el asistente.
 */
@Configuration(proxyBeanMethods = false)
@Import(SpringAiAssistantConfiguration.class)
@PropertySource("classpath:harness.properties")
public class HarnessConfiguration {

    @Bean
    SeverityCatalog severityCatalog() {
        return new SeverityCatalog(List.of(StandardSeverity.values()));
    }

    @Bean
    @ConditionalOnProperty(name = "certiflow.assistant.provider", havingValue = "stub")
    InspectionAssistant stubInspectionAssistant() {
        return new StubInspectionAssistant();
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    HarnessRun harnessRun(InspectionAssistant assistant, Clock clock) {
        return new HarnessRun(assistant, new CaseInspections(clock), clock, System.out::println);
    }

    @Bean
    ReportFiles reportFiles(@Value("${harness.results}") Path resultsDirectory) {
        return new ReportFiles(resultsDirectory);
    }
}
