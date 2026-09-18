package ar.edu.itba.certiflow;

import ar.edu.itba.certiflow.application.CatalogService;
import ar.edu.itba.certiflow.application.CertificationService;
import ar.edu.itba.certiflow.application.CorrectiveActionService;
import ar.edu.itba.certiflow.application.InspectionService;
import ar.edu.itba.certiflow.application.ReportService;
import ar.edu.itba.certiflow.domain.Answer;
import ar.edu.itba.certiflow.domain.Asset;
import ar.edu.itba.certiflow.domain.CorrectiveAction;
import ar.edu.itba.certiflow.domain.Criterion;
import ar.edu.itba.certiflow.domain.Evidence;
import ar.edu.itba.certiflow.domain.Inspection;
import ar.edu.itba.certiflow.details.rules.BooleanRule;
import ar.edu.itba.certiflow.details.rules.DocumentaryRule;
import ar.edu.itba.certiflow.details.rules.NumericRangeRule;
import ar.edu.itba.certiflow.infrastructure.memory.InMemoryActionRepository;
import ar.edu.itba.certiflow.infrastructure.memory.InMemoryAssetRepository;
import ar.edu.itba.certiflow.infrastructure.memory.InMemoryCertificateRepository;
import ar.edu.itba.certiflow.infrastructure.memory.InMemoryInspectionRepository;
import ar.edu.itba.certiflow.infrastructure.memory.InMemorySchemeRepository;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

final class Fixture {
    static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-09-16T12:00:00Z");

        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        public Clock withZone(ZoneId zone) {
            return Clock.fixed(now, zone);
        }

        public Instant instant() {
            return now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }
    }

    final MutableClock clock = new MutableClock();
    final InMemoryAssetRepository assetRepo = new InMemoryAssetRepository();
    final InMemorySchemeRepository schemeRepo = new InMemorySchemeRepository();
    final InMemoryInspectionRepository inspectionRepo = new InMemoryInspectionRepository();
    final InMemoryActionRepository actionRepo = new InMemoryActionRepository();
    final InMemoryCertificateRepository certificateRepo = new InMemoryCertificateRepository();
    final CatalogService catalog = new CatalogService(assetRepo, schemeRepo, clock);
    final InspectionService inspections =
            new InspectionService(assetRepo, schemeRepo, inspectionRepo, clock);
    final CorrectiveActionService actions =
            new CorrectiveActionService(inspectionRepo, actionRepo, clock);
    final CertificationService certificates =
            new CertificationService(inspectionRepo, actionRepo, certificateRepo, clock);
    final ReportService reports =
            new ReportService(inspectionRepo, actionRepo, certificateRepo, clock);
    final UUID schemeId = UUID.randomUUID();
    final Asset asset =
            catalog.registerAsset(
                    "Laboratorio A", "LAB", "Edificio 1", "owner", Map.of("floor", "1"));

    Fixture() {
        publish("50");
    }

    SchemeVersion publish(String max) {
        return catalog.publish(
                schemeId,
                "Laboratorios",
                "LAB",
                List.of(
                        new Section(
                                "Seguridad",
                                List.of(
                                        new Criterion(
                                                "TEMP",
                                                "Temperatura",
                                                Severity.CRITICAL,
                                                Set.of(Evidence.Kind.PHOTO),
                                                new NumericRangeRule(
                                                        new BigDecimal("0"),
                                                        new BigDecimal("10"),
                                                        new BigDecimal("30"),
                                                        new BigDecimal(max),
                                                        "C")),
                                        new Criterion(
                                                "EXIT",
                                                "Salida despejada",
                                                Severity.MAJOR,
                                                Set.of(),
                                                new BooleanRule(true, Outcome.REJECTED)),
                                        new Criterion(
                                                "DOC",
                                                "Manual",
                                                Severity.MINOR,
                                                Set.of(Evidence.Kind.DOCUMENT),
                                                new DocumentaryRule())))),
                365,
                "author");
    }

    Evidence evidence(Evidence.Kind kind) {
        return new Evidence(
                UUID.randomUUID(),
                kind,
                URI.create("urn:evidence:" + UUID.randomUUID()),
                "Prueba",
                "inspector",
                clock.instant());
    }

    InspectorSubmission numeric(String value) {
        return new InspectorSubmission(
                new Answer.Numeric(new BigDecimal(value), "C"),
                List.of(evidence(Evidence.Kind.PHOTO)),
                "");
    }

    Inspection assigned() {
        return inspections.assign(
                asset.id(),
                schemeId,
                "inspector",
                LocalDate.now(clock),
                "Todos los criterios",
                "coordinator");
    }

    Inspection started() {
        return inspections.start(assigned().id(), "inspector");
    }

    Inspection closed(String measurement) {
        var i = started();
        inspections.record(i.id(), "TEMP", numeric(measurement), "inspector");
        inspections.record(
                i.id(), "EXIT", new InspectorSubmission(new Answer.YesNo(true), List.of(), ""), "inspector");
        inspections.record(
                i.id(),
                "DOC",
                new InspectorSubmission(
                        new Answer.Documentary(), List.of(evidence(Evidence.Kind.DOCUMENT)), ""),
                "inspector");
        return inspections.close(i.id(), "inspector");
    }

    CorrectiveAction plan(Inspection i) {
        return actions.plan(
                i.id(),
                i.findings().get(0).id(),
                "Reparar ventilacion",
                "technician",
                LocalDate.now(clock).plusDays(2),
                "owner");
    }

    CorrectiveAction resolve(Inspection i) {
        var a = plan(i);
        actions.submit(a.id(), List.of(evidence(Evidence.Kind.PHOTO)), "technician");
        actions.verify(a.id(), true, "Medicion verificada", "inspector");
        return actions.close(a.id(), "inspector");
    }
}
