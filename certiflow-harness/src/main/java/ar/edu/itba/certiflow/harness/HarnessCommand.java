package ar.edu.itba.certiflow.harness;

import ar.edu.itba.certiflow.harness.golden.GoldenSet;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * La línea de comandos del harness (README, "Harness"):
 * <ul>
 *   <li>{@code run}: corre el conjunto dorado con el asistente configurado y escribe el reporte.</li>
 *   <li>{@code compare <base.json> <candidata.json>}: compara dos versiones de prompt y termina con código 1 si
 *       alguna métrica empeoró, para que el workflow falle.</li>
 * </ul>
 */
public final class HarnessCommand {

    private static final String USAGE = "Usage: run | compare <baseline.json> <candidate.json>";

    void main(String[] args) {
        List<String> arguments = List.of(args);
        Map<String, Consumer<List<String>>> commands = Map.of("run", this::run, "compare", this::compare);
        Optional.ofNullable(commands.get(arguments.stream().findFirst().orElse("run")))
                .orElseThrow(() -> new IllegalArgumentException(USAGE))
                .accept(arguments);
    }

    private void run(List<String> arguments) {
        try (AnnotationConfigApplicationContext context = context()) {
            int runs = context.getEnvironment().getRequiredProperty("harness.runs", Integer.class);
            HarnessReport report = context.getBean(HarnessRun.class).run(new GoldenSet().cases(), runs);
            Path written = context.getBean(ReportFiles.class).write(report);
            System.out.println(new ReportMarkdown().render(report));
            System.out.println("Report written to " + written.toAbsolutePath());
        }
    }

    private void compare(List<String> arguments) {
        if (arguments.size() != 3) {
            throw new IllegalArgumentException(USAGE);
        }
        Path candidate = Path.of(arguments.get(2)).toAbsolutePath();
        ReportFiles files = new ReportFiles(candidate.getParent().getParent());
        ReportComparison comparison = new ReportComparison(files.read(Path.of(arguments.get(1))), files.read(candidate));
        Path written = files.write(comparison);
        System.out.println(comparison.markdown());
        System.out.println("Comparison written to " + written);
        System.exit(comparison.regressions().isEmpty() ? 0 : 1);
    }

    private AnnotationConfigApplicationContext context() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.getBeanFactory().setConversionService(ApplicationConversionService.getSharedInstance());
        context.register(HarnessConfiguration.class);
        context.refresh();
        return context;
    }
}
