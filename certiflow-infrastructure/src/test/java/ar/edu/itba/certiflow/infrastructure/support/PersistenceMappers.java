package ar.edu.itba.certiflow.infrastructure.support;

import ar.edu.itba.certiflow.infrastructure.certification.persistence.DecisionDocumentMapper;
import ar.edu.itba.certiflow.infrastructure.inspection.persistence.EvaluationDocumentMapper;
import ar.edu.itba.certiflow.infrastructure.schema.persistence.SectionsDocumentMapper;
import ar.edu.itba.certiflow.infrastructure.schema.persistence.StoredSchemaVersions;
import ar.edu.itba.certiflow.infrastructure.shared.persistence.CodesDocumentMapper;
import ar.edu.itba.certiflow.infrastructure.shared.persistence.StoredJson;
import ar.edu.itba.certiflow.schema.model.ApprovalRuleKindCatalog;
import ar.edu.itba.certiflow.schema.model.EvidenceKindCatalog;
import ar.edu.itba.certiflow.schema.model.NumericRangeRuleKind;
import ar.edu.itba.certiflow.schema.model.OptionInListRuleKind;
import ar.edu.itba.certiflow.schema.model.SeverityCatalog;
import ar.edu.itba.certiflow.schema.model.StandardEvidenceKind;
import ar.edu.itba.certiflow.schema.model.StandardSeverity;
import ar.edu.itba.certiflow.schema.model.YesNoRuleKind;
import java.util.List;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/** Los colaboradores que los repositorios JPA reciben y que {@code @DataJpaTest} no registra por sí solo. */
@TestConfiguration
@Import({StoredJson.class, CodesDocumentMapper.class, SectionsDocumentMapper.class, EvaluationDocumentMapper.class,
        DecisionDocumentMapper.class, StoredSchemaVersions.class})
public class PersistenceMappers {

    @Bean
    SeverityCatalog severityCatalog() {
        return new SeverityCatalog(List.of(StandardSeverity.values()));
    }

    @Bean
    ApprovalRuleKindCatalog approvalRuleKindCatalog() {
        return new ApprovalRuleKindCatalog(List.of(new NumericRangeRuleKind(), new YesNoRuleKind(),
                new OptionInListRuleKind()));
    }

    @Bean
    EvidenceKindCatalog evidenceKindCatalog() {
        return new EvidenceKindCatalog(List.of(StandardEvidenceKind.values()));
    }
}
