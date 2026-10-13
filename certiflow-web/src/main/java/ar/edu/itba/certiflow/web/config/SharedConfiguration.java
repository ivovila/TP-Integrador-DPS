package ar.edu.itba.certiflow.web.config;

import ar.edu.itba.certiflow.catalog.model.AssetAudit;
import ar.edu.itba.certiflow.certification.model.CertificateAudit;
import ar.edu.itba.certiflow.inspection.model.FindingAudit;
import ar.edu.itba.certiflow.inspection.model.InspectionAudit;
import ar.edu.itba.certiflow.schema.model.SchemaAudit;
import ar.edu.itba.certiflow.shared.model.AuditAction;
import ar.edu.itba.certiflow.shared.model.AuditActionCatalog;
import java.time.Clock;
import java.time.Duration;
import java.util.Arrays;
import java.util.stream.Stream;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** El reloj y la bitácora, compartidos por todos los contextos. */
@Configuration
public class SharedConfiguration {

    /** Los instantes se guardan con microsegundos: el reloj ya los entrega con esa precisión, truncando. */
    @Bean
    Clock clock() {
        return Clock.tick(Clock.systemUTC(), Duration.ofNanos(1_000));
    }

    /** El único lugar que nombra los enums de acciones: la persistencia las busca por su código. */
    @Bean
    AuditActionCatalog auditActionCatalog() {
        return new AuditActionCatalog(Stream.<AuditAction[]>of(AssetAudit.values(), SchemaAudit.values(),
                        InspectionAudit.values(), FindingAudit.values(), CertificateAudit.values())
                .flatMap(Arrays::stream)
                .toList());
    }
}
