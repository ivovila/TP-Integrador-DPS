package ar.edu.itba.certiflow.web.config;

import ar.edu.itba.certiflow.catalog.model.AssetAudit;
import ar.edu.itba.certiflow.assistance.model.AssistanceAudit;
import ar.edu.itba.certiflow.certification.model.CertificateAudit;
import ar.edu.itba.certiflow.inspection.model.FindingAudit;
import ar.edu.itba.certiflow.inspection.model.InspectionAudit;
import ar.edu.itba.certiflow.schema.model.SchemaAudit;
import ar.edu.itba.certiflow.shared.model.AuditAction;
import ar.edu.itba.certiflow.shared.model.AuditActionCatalog;
import java.time.Clock;
import java.util.Arrays;
import java.util.stream.Stream;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** El reloj y la bitácora, compartidos por todos los contextos. */
@Configuration
public class SharedConfiguration {

    /** Sellos ordenados, con la misma precisión de microsegundos que la persistencia. */
    @Bean
    Clock clock() {
        return new OrderedClock(Clock.systemUTC());
    }

    /** El único lugar que nombra los enums de acciones: la persistencia las busca por su código. */
    @Bean
    AuditActionCatalog auditActionCatalog() {
        return new AuditActionCatalog(Stream.<AuditAction[]>of(AssetAudit.values(), SchemaAudit.values(),
                        InspectionAudit.values(), FindingAudit.values(), CertificateAudit.values(), AssistanceAudit.values())
                .flatMap(Arrays::stream)
                .toList());
    }
}
