# Certiflow · Entrega 1

Módulo de dominio para inspeccionar activos, preservar esquemas versionados, evaluar criterios, gestionar hallazgos y acciones correctivas y emitir certificados. Java 17, Maven y JUnit 5. Sin dependencias de producción externas.

## Ejecutar

Requisitos: JDK 17 o posterior y Maven 3.9.x. Maven puede necesitar descargar plugins y JUnit en la primera ejecución.

```bash
mvn clean verify
java -jar target/certiflow-1.0.0-SNAPSHOT.jar
```

En IntelliJ: abrir `pom.xml` como proyecto Maven, seleccionar un JDK compatible y recargar Maven. El ejemplo generado inicialmente en `src/Main.java` se reemplaza por `CertiflowDemo`.

La demostración realiza una inspección con un incumplimiento, muestra el bloqueo de certificación, resuelve una acción con verificación independiente, emite el certificado y demuestra su suspensión al abrir una rectificación. Usa una fecha fija para que el resultado sea reproducible; las aplicaciones pueden inyectar `Clock.systemUTC()`.

## Pruebas

```bash
mvn test                         # Toda la suite
mvn test -Dgroups=unit           # Reglas y límites del negocio
mvn test -Dgroups=integration    # Casos de uso con objetos y repositorios reales en memoria
mvn -Pcoverage verify           # Informe HTML: target/site/jacoco/index.html
```

Las pruebas de integración no necesitan una base de datos: integran servicios de aplicación, entidades, reglas y adaptadores de repositorio. No se prueban getters ni detalles triviales para inflar cobertura. Se cubren reglas históricas, rangos, evidencia faltante, estados, auditoría, correcciones, vencimiento y renovación.

## Estructura y lectura sugerida

- `domain`: objetos de negocio, invariantes y transiciones. `Inspection`, `CorrectiveAction` y `Certificate` son agregados inmutables.
- `domain/rule`: estrategias de evaluación numérica, booleana y documental.
- `application`: casos de uso y consultas de informes.
- `application/port`: contratos de repositorios propios.
- `infrastructure/memory`: implementaciones sin persistencia externa.
- `demo/CertiflowDemo`: composición manual de dependencias y escenario ejecutable.
- `src/test/java`: pruebas unitarias y de integración.

Primero leer [DESIGN.md](DESIGN.md), después `CertiflowDemo`, `InspectionWorkflowTest` y `Inspection`. Las decisiones, supuestos, alternativas descartadas y límites están explicitados en el documento de diseño.

## Alcance de la consigna

| Capacidad | Implementación |
|---|---|
| Alta y consulta de activos | `CatalogService.registerAsset`, `CatalogService.assets` |
| Plantillas por secciones y criterios | `Section`, `Criterion`, `CatalogService.publish` |
| Versionado histórico | `SchemeVersion`, publicación append-only, selección al iniciar |
| Asignación de inspector, fecha y alcance | `InspectionService.assign` |
| Registro progresivo | `InspectionService.record` permite completar o reemplazar la respuesta de un criterio mientras está abierto |
| Evaluación automática | `InspectionService.evaluate`, evaluación obligatoria al cerrar |
| Hallazgos con severidad, evidencia y responsable | Generación por criterio no conforme durante `Inspection.close` |
| Acciones correctivas | `CorrectiveActionService.plan/submit/verify/close`, consulta `isOverdue` |
| Certificación | `CertificationService.issue/suspend/renew/status` |
| Auditoría y rectificación | Historial de transiciones y decisiones; revisiones cerradas preservadas |
| Informes | `ReportService.inspectionAct/findings/certificate`, resultados estructurados inmutables |

El acta incluye activo, inspector, alcance, versión, respuestas, resultados, hallazgos e historial. Los informes son datos de negocio listos para un futuro renderizador, no archivos PDF. La consigna no prescribe formato; esta interpretación está documentada para validarla con la cátedra.

## Estilo de código

Código y nombres de API en inglés; documentación de diseño en español. Formato Java uniforme con `google-java-format` 1.24.0 en modo AOSP (cuatro espacios). `.editorconfig` define espacios, UTF-8 y finales de línea. El dominio no tiene dependencias de producción externas.

## Preparación de la entrega

La consigna también requiere **URL de GitHub y hash del commit entregado**. Repositorio: https://github.com/ivovila/TP-Integrador-DPS. La implementación se entrega en la rama `main`. Para obtener el hash exacto del commit elegido, ejecutar `git rev-parse HEAD`.

Antes de presentar, validar con la cátedra los supuestos de `DESIGN.md`, especialmente la política de certificación, la cobertura del alcance y el formato de informes. Las decisiones están implementadas y probadas; no son reglas textuales que aparezcan completas en el enunciado.
