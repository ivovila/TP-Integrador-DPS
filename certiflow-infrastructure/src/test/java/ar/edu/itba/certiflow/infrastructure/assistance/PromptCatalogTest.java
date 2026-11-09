package ar.edu.itba.certiflow.infrastructure.assistance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.itba.certiflow.assistance.model.PromptVersion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Si este test falla porque cambió la suma de una versión, no se corrige la suma: se deshace la edición y se
 * publica una versión nueva. Una revisión guardada con "v1" tiene que seguir significando el mismo texto.
 */
class PromptCatalogTest {

    private final PromptCatalog catalog = new PromptCatalog();

    @ParameterizedTest
    @ValueSource(strings = {"v1", "v2"})
    void everyPublishedVersionLoadsUnchangedAndReceivesTheInspection(String version) {
        ActivePrompt prompt = catalog.published(new PromptVersion(version));

        assertEquals(new PromptVersion(version), prompt.version());
        assertTrue(prompt.template().contains("{inspection}"));
    }

    @Test
    void aVersionThatWasNeverPublishedDoesNotLoad() {
        assertThrows(InvalidPromptException.class, () -> catalog.published(new PromptVersion("v99")));
    }
}
