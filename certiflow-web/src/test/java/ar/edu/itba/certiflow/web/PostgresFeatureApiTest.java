package ar.edu.itba.certiflow.web;

import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.test.context.SpringBootTest;

@EnabledIfSystemProperty(named = "certiflow.postgres", matches = "true")
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://localhost:55433/certiflow",
        "spring.datasource.username=postgres", "spring.datasource.password=certiflow-test"})
class PostgresFeatureApiTest extends DeliveryFeaturesApiTest { }
