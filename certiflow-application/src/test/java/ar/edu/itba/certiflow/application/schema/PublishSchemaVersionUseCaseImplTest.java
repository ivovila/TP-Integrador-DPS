package ar.edu.itba.certiflow.application.schema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.itba.certiflow.application.support.Samples;
import ar.edu.itba.certiflow.schema.model.InspectionSchemaService;
import ar.edu.itba.certiflow.schema.model.SchemaVersion;
import ar.edu.itba.certiflow.schema.model.exceptions.NoInspectionSchemaForAssetTypeException;
import ar.edu.itba.certiflow.schema.repository.InspectionSchemaRepository;
import ar.edu.itba.certiflow.schema.usecase.PublishSchemaVersionCommand;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PublishSchemaVersionUseCaseImplTest {

    @Mock
    private InspectionSchemaRepository schemaRepository;
    private PublishSchemaVersionUseCaseImpl publishVersion;

    @BeforeEach
    void setUp() {
        publishVersion = new PublishSchemaVersionUseCaseImpl(schemaRepository, new InspectionSchemaService(Samples.auditLog()),
                Samples.CLOCK);
    }

    @Test
    void withoutARequestedDateTheVersionIsInForceFromItsPublication() {
        when(schemaRepository.findForUpdate(Samples.EXTINGUISHER)).thenReturn(Optional.of(Samples.schema()));

        SchemaVersion version = publishVersion.execute(new PublishSchemaVersionCommand(Samples.EXTINGUISHER, Samples.sections(),
                Samples.PLANNER));

        assertEquals(2, version.number());
        assertEquals(Samples.NOW, version.effectiveFrom());
        verify(schemaRepository).save(any());
    }

    @Test
    void aRequestedDateSchedulesTheVersion() {
        Instant later = Samples.NOW.plusSeconds(86_400);
        when(schemaRepository.findForUpdate(Samples.EXTINGUISHER)).thenReturn(Optional.of(Samples.schema()));

        SchemaVersion version = publishVersion.execute(new PublishSchemaVersionCommand(Samples.EXTINGUISHER, Samples.sections(),
                Samples.PLANNER, later, Set.of()));

        assertEquals(later, version.effectiveFrom());
        assertEquals(Samples.NOW, version.publishedAt());
    }

    @Test
    void aVersionNeedsAnExistingSchema() {
        when(schemaRepository.findForUpdate(Samples.EXTINGUISHER)).thenReturn(Optional.empty());

        assertThrows(NoInspectionSchemaForAssetTypeException.class, () -> publishVersion.execute(
                new PublishSchemaVersionCommand(Samples.EXTINGUISHER, Samples.sections(), Samples.PLANNER)));
        verify(schemaRepository, never()).save(any());
    }
}
