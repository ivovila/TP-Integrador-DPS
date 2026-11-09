package ar.edu.itba.certiflow.infrastructure.assistance;

import ar.edu.itba.certiflow.assistance.model.PromptVersion;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Los prompts son configuración versionada: cada versión es un archivo inmutable en
 * {@code prompts/inspection-review/}. Cargar una versión verifica que esté publicada y que su texto siga siendo el
 * que se publicó, así una revisión guardada con "v1" siempre significa el mismo texto.
 */
public final class PromptCatalog {

    private static final String LOCATION = "prompts/inspection-review/";
    private static final String EXTENSION = ".st";

    public ActivePrompt published(PromptVersion version) {
        PublishedPrompt prompt = Arrays.stream(PublishedPrompt.values())
                .filter(published -> published.is(version))
                .findFirst()
                .orElseThrow(() -> new InvalidPromptException("Prompt version " + version.value() + " is not published"));
        String template = textOf(version);
        if (!prompt.matches(checksumOf(template))) {
            throw new InvalidPromptException("Prompt version " + version.value()
                    + " was edited after it was published: publish a new version instead");
        }
        return new ActivePrompt(version, template);
    }

    private String textOf(PromptVersion version) {
        String location = LOCATION + version.value() + EXTENSION;
        try (InputStream stream = Optional.ofNullable(getClass().getClassLoader().getResourceAsStream(location))
                .orElseThrow(() -> new InvalidPromptException("Prompt file " + location + " is missing"))) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8).replace("\r\n", "\n");
        } catch (IOException failure) {
            throw new InvalidPromptException("Prompt file " + location + " cannot be read", failure);
        }
    }

    private String checksumOf(String template) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(template.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException failure) {
            throw new InvalidPromptException("SHA-256 is not available", failure);
        }
    }
}
