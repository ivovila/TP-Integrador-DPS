package ar.edu.itba.certiflow.demo;

import ar.edu.itba.certiflow.application.CatalogService;
import ar.edu.itba.certiflow.application.CertificationService;
import ar.edu.itba.certiflow.application.CorrectiveActionService;
import ar.edu.itba.certiflow.application.InspectionService;
import ar.edu.itba.certiflow.application.ReportService;
import ar.edu.itba.certiflow.domain.Answer;
import ar.edu.itba.certiflow.domain.Criterion;
import ar.edu.itba.certiflow.domain.DomainException;
import ar.edu.itba.certiflow.domain.Evidence;
import ar.edu.itba.certiflow.domain.Section;
import ar.edu.itba.certiflow.domain.Severity;
import ar.edu.itba.certiflow.domain.Submission;
import ar.edu.itba.certiflow.domain.rule.NumericRangeRule;
import ar.edu.itba.certiflow.infrastructure.memory.InMemoryActionRepository;
import ar.edu.itba.certiflow.infrastructure.memory.InMemoryAssetRepository;
import ar.edu.itba.certiflow.infrastructure.memory.InMemoryCertificateRepository;
import ar.edu.itba.certiflow.infrastructure.memory.InMemoryInspectionRepository;
import ar.edu.itba.certiflow.infrastructure.memory.InMemorySchemeRepository;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Composition root and executable example. No framework or external services required. */
public final class CertiflowDemo {
    private CertiflowDemo() {}

    public static void main(String[] args) {
        Clock clock = Clock.fixed(Instant.parse("2026-09-16T12:00:00Z"), ZoneOffset.UTC);
        var assets = new InMemoryAssetRepository();
        var schemes = new InMemorySchemeRepository();
        var inspections = new InMemoryInspectionRepository();
        var actions = new InMemoryActionRepository();
        var certificates = new InMemoryCertificateRepository();
        var catalog = new CatalogService(assets, schemes, clock);
        var inspectionService = new InspectionService(assets, schemes, inspections, clock);
        var actionService = new CorrectiveActionService(inspections, actions, clock);
        var certificationService =
                new CertificationService(inspections, actions, certificates, clock);
        var reports = new ReportService(inspections, actions, certificates, clock);

        var asset =
                catalog.registerAsset(
                        "Laboratorio Quimica A", "LAB", "Edificio 1", "Ana", Map.of("piso", "2"));
        var criterion =
                new Criterion(
                        "TEMP",
                        "Temperatura de almacenamiento",
                        Severity.MAJOR,
                        Set.of(Evidence.Kind.PHOTO),
                        new NumericRangeRule(
                                new BigDecimal("0"),
                                new BigDecimal("10"),
                                new BigDecimal("30"),
                                new BigDecimal("50"),
                                "C"));
        var scheme =
                catalog.publish(
                        UUID.randomUUID(),
                        "Inspeccion de laboratorios",
                        "LAB",
                        List.of(new Section("Almacenamiento", List.of(criterion))),
                        365,
                        "Coordinacion");
        var inspection =
                inspectionService.assign(
                        asset.id(),
                        scheme.schemeId(),
                        "Bruno",
                        LocalDate.now(clock),
                        "Almacenamiento",
                        "Coordinacion");
        inspectionService.start(inspection.id(), "Bruno");
        var proof =
                new Evidence(
                        UUID.randomUUID(),
                        Evidence.Kind.PHOTO,
                        URI.create("urn:evidence:termometro"),
                        "Lectura inicial del termometro",
                        "Bruno",
                        clock.instant());
        inspectionService.record(
                inspection.id(),
                "TEMP",
                new Submission(
                        new Answer.Numeric(new BigDecimal("60"), "C"),
                        List.of(proof),
                        "Temperatura excesiva"),
                "Bruno");
        var closed = inspectionService.close(inspection.id(), "Bruno");
        System.out.println("Inspeccion cerrada: " + closed.evaluations().get("TEMP").outcome());
        try {
            certificationService.issue(closed.id(), "Coordinacion");
            throw new IllegalStateException("Expected certification to be blocked");
        } catch (DomainException expected) {
            System.out.println("Certificacion bloqueada: " + expected.getMessage());
        }
        var action =
                actionService.plan(
                        closed.id(),
                        closed.findings().get(0).id(),
                        "Reparar climatizacion",
                        "Carla",
                        LocalDate.now(clock).plusDays(7),
                        "Ana");
        var correctedProof =
                new Evidence(
                        UUID.randomUUID(),
                        Evidence.Kind.PHOTO,
                        URI.create("urn:evidence:retest"),
                        "Nueva medicion: 20 C",
                        "Carla",
                        clock.instant());
        actionService.submit(action.id(), List.of(correctedProof), "Carla");
        actionService.verify(action.id(), true, "Medicion repetida y conforme", "Bruno");
        actionService.close(action.id(), "Bruno");
        var certificate = certificationService.issue(closed.id(), "Coordinacion");
        System.out.println(
                "Hallazgos del acta: " + reports.findings(closed.id()).findings().size());
        System.out.println("Certificado: " + reports.certificate(certificate.id()).status());
        System.out.println("Vencimiento: " + certificate.expiresAt());
        inspectionService.rectify(
                closed.id(), "Corregir descripcion de la observacion", "Coordinacion");
        System.out.println(
                "Tras abrir rectificacion: " + reports.certificate(certificate.id()).status());
    }
}
