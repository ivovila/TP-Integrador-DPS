package ar.edu.itba.certiflow.infrastructure.support;

import ar.edu.itba.certiflow.infrastructure.certification.persistence.DecisionDocumentMapper;
import ar.edu.itba.certiflow.infrastructure.inspection.persistence.EvaluationDocumentMapper;
import ar.edu.itba.certiflow.infrastructure.schema.persistence.SectionsDocumentMapper;
import ar.edu.itba.certiflow.infrastructure.schema.persistence.StoredSchemaVersions;
import ar.edu.itba.certiflow.infrastructure.shared.persistence.CodesDocumentMapper;
import ar.edu.itba.certiflow.infrastructure.shared.persistence.StoredJson;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;

/** Los colaboradores que los repositorios JPA reciben y que {@code @DataJpaTest} no registra por sí solo. */
@TestConfiguration
@Import({StoredJson.class, CodesDocumentMapper.class, SectionsDocumentMapper.class, EvaluationDocumentMapper.class,
        DecisionDocumentMapper.class, StoredSchemaVersions.class})
public class PersistenceMappers {
}
