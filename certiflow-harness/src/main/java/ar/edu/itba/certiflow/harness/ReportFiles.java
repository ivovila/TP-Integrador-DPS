package ar.edu.itba.certiflow.harness;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Dónde quedan los resultados: {@code <resultados>/<versión de prompt>/<modelo>.json} y su resumen en Markdown,
 * versionados en el repositorio para que el cambio de un prompt muestre su comparación en el pull request.
 */
public final class ReportFiles {

    private final JsonMapper jsonMapper = JsonMapper.builder().enable(SerializationFeature.INDENT_OUTPUT).build();
    private final ReportMarkdown markdown = new ReportMarkdown();
    private final Path resultsDirectory;

    public ReportFiles(Path resultsDirectory) {
        this.resultsDirectory = resultsDirectory;
    }

    public Path write(HarnessReport report) {
        Path json = resultsDirectory.resolve(report.promptVersion()).resolve(fileNameOf(report.model()) + ".json");
        writeText(json, jsonMapper.writeValueAsString(report) + "\n");
        writeText(json.resolveSibling(fileNameOf(report.model()) + ".md"), markdown.render(report));
        return json;
    }

    public Path write(ReportComparison comparison) {
        Path file = resultsDirectory.resolve(comparison.candidate().promptVersion())
                .resolve("comparison-with-" + comparison.baseline().promptVersion() + ".md");
        writeText(file, comparison.markdown());
        return file;
    }

    public HarnessReport read(Path report) {
        return jsonMapper.readValue(report.toFile(), HarnessReport.class);
    }

    /** Un modelo como {@code nvidia/nemotron} no puede ser un nombre de archivo tal cual. */
    private String fileNameOf(String model) {
        return model.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private void writeText(Path file, String text) {
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, text);
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }
}
