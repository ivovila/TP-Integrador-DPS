package ar.edu.itba.certiflow.application.inspection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.itba.certiflow.application.support.Samples;
import ar.edu.itba.certiflow.catalog.model.exceptions.AssetNotFoundException;
import ar.edu.itba.certiflow.catalog.repository.AssetRepository;
import ar.edu.itba.certiflow.inspection.model.FullScope;
import ar.edu.itba.certiflow.inspection.model.Inspection;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.inspection.repository.InspectionRepository;
import ar.edu.itba.certiflow.inspection.usecase.AssignInspectionCommand;
import ar.edu.itba.certiflow.schema.model.InspectionSchema;
import ar.edu.itba.certiflow.schema.model.exceptions.NoInspectionSchemaForAssetTypeException;
import ar.edu.itba.certiflow.schema.repository.InspectionSchemaRepository;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AssignInspectionUseCaseImplTest {

    private static final AssignInspectionCommand COMMAND = new AssignInspectionCommand(Samples.ASSET.code(), Samples.INSPECTOR,
            LocalDate.of(2026, 3, 13), new FullScope(), Samples.PLANNER);

    @Mock
    private AssetRepository assetRepository;
    @Mock
    private InspectionSchemaRepository schemaRepository;
    @Mock
    private InspectionRepository inspectionRepository;
    private AssignInspectionUseCaseImpl assignInspection;

    @BeforeEach
    void setUp() {
        assignInspection = new AssignInspectionUseCaseImpl(assetRepository, schemaRepository, inspectionRepository,
                Samples.inspectionService(), Samples.CLOCK);
    }

    @Test
    void theInspectionFollowsTheVersionInForceWhenItIsAssignedNotOneScheduledForLater() {
        InspectionSchema schema = Samples.schema().withNewVersion(Samples.sections(), Samples.NOW, Samples.NOW.plusSeconds(86_400),
                Set.of());
        when(assetRepository.findByCode(Samples.ASSET.code())).thenReturn(Optional.of(Samples.ASSET));
        when(schemaRepository.findByAssetType(Samples.EXTINGUISHER)).thenReturn(Optional.of(schema));
        when(inspectionRepository.nextId()).thenReturn(new InspectionId("I-7"));

        assertEquals(new InspectionId("I-7"), assignInspection.execute(COMMAND));

        ArgumentCaptor<Inspection> saved = ArgumentCaptor.forClass(Inspection.class);
        verify(inspectionRepository).save(saved.capture());
        assertEquals(1, saved.getValue().schemaVersion().number());
    }

    @Test
    void anUnknownAssetIsRejectedAndNothingIsSaved() {
        when(assetRepository.findByCode(Samples.ASSET.code())).thenReturn(Optional.empty());

        assertThrows(AssetNotFoundException.class, () -> assignInspection.execute(COMMAND));
        verify(inspectionRepository, never()).save(any());
    }

    @Test
    void anAssetTypeWithoutSchemaCannotBeInspected() {
        when(assetRepository.findByCode(Samples.ASSET.code())).thenReturn(Optional.of(Samples.ASSET));
        when(schemaRepository.findByAssetType(Samples.EXTINGUISHER)).thenReturn(Optional.empty());

        assertThrows(NoInspectionSchemaForAssetTypeException.class, () -> assignInspection.execute(COMMAND));
        verify(inspectionRepository, never()).save(any());
    }
}
