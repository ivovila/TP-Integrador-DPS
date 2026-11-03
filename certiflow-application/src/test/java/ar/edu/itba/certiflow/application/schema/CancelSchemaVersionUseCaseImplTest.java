package ar.edu.itba.certiflow.application.schema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.itba.certiflow.application.support.Samples;
import ar.edu.itba.certiflow.schema.model.InspectionSchema;
import ar.edu.itba.certiflow.schema.model.InspectionSchemaService;
import ar.edu.itba.certiflow.schema.model.SchemaVersion;
import ar.edu.itba.certiflow.schema.model.exceptions.NoInspectionSchemaForAssetTypeException;
import ar.edu.itba.certiflow.schema.model.exceptions.SchemaVersionAlreadyInForceException;
import ar.edu.itba.certiflow.schema.repository.InspectionSchemaRepository;
import ar.edu.itba.certiflow.schema.usecase.CancelSchemaVersionCommand;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CancelSchemaVersionUseCaseImplTest {

    @Mock
    private InspectionSchemaRepository schemaRepository;
    private CancelSchemaVersionUseCaseImpl cancelVersion;

    @BeforeEach
    void setUp() {
        cancelVersion = new CancelSchemaVersionUseCaseImpl(schemaRepository, new InspectionSchemaService(Samples.auditLog()),
                Samples.CLOCK);
    }

    @Test
    void aPendingVersionIsCancelledAndSaved() {
        InspectionSchema schema = Samples.schema().withNewVersion(Samples.sections(), Samples.NOW.minusSeconds(60),
                Samples.NOW.plusSeconds(86_400), Set.of());
        when(schemaRepository.findForUpdate(Samples.EXTINGUISHER)).thenReturn(Optional.of(schema));

        SchemaVersion cancelled = cancelVersion.execute(new CancelSchemaVersionCommand(Samples.EXTINGUISHER, 2,
                "Urgent correction", Samples.PLANNER));

        assertTrue(cancelled.isCancelled());
        ArgumentCaptor<InspectionSchema> saved = ArgumentCaptor.forClass(InspectionSchema.class);
        verify(schemaRepository).save(saved.capture());
        assertEquals(1, saved.getValue().versionAt(Samples.NOW.plusSeconds(86_400)).number());
    }

    @Test
    void aVersionInForceIsNotCancelledNorSaved() {
        when(schemaRepository.findForUpdate(Samples.EXTINGUISHER)).thenReturn(Optional.of(Samples.schema()));

        assertThrows(SchemaVersionAlreadyInForceException.class, () -> cancelVersion.execute(
                new CancelSchemaVersionCommand(Samples.EXTINGUISHER, 1, "Too late", Samples.PLANNER)));
        verify(schemaRepository, never()).save(any());
    }

    @Test
    void cancellingNeedsAnExistingSchema() {
        when(schemaRepository.findForUpdate(Samples.EXTINGUISHER)).thenReturn(Optional.empty());

        assertThrows(NoInspectionSchemaForAssetTypeException.class, () -> cancelVersion.execute(
                new CancelSchemaVersionCommand(Samples.EXTINGUISHER, 2, "Missing", Samples.PLANNER)));
    }
}
