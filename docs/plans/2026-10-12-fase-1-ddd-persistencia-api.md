# Fase 1 · DDD, persistencia, API REST y CI · Plan de implementación

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Reorganizar CertiFlow en bounded contexts y capas DDD, persistir los agregados con JPA, exponer una API REST de nivel 3 y correr los tests en CI, sin cambiar el comportamiento de negocio.

**Architecture:** Cuatro módulos Maven (`domain`, `application`, `infrastructure`, `web`) con paquetes por contexto (`catalog`, `schema`, `inspection`, `certification`, `shared`). Spring Boot, JPA y Jackson solo en `infrastructure` y `web`. Los agregados se reconstruyen desde snapshots (Memento); Certificación decide sobre un `InspectionResult` armado por un ACL.

**Tech Stack:** Java 25, Maven 3.9, JUnit 5.12, Spring Boot 4.1.1 (webmvc, data-jpa, flyway, hateoas), H2 (tests), PostgreSQL 17 (al correr), Mockito, GitHub Actions.

**Spec:** [docs/specs/2026-10-12-fase-1-ddd-persistencia-api.md](../specs/2026-10-12-fase-1-ddd-persistencia-api.md)

## Global Constraints

- Paquete base `ar.edu.itba.certiflow`. Contextos: `catalog`, `schema`, `inspection`, `certification`, `shared`.
- Dominio: `ar.edu.itba.certiflow.<ctx>.model` (+ `.model.exceptions`), `.<ctx>.repository`, `.<ctx>.usecase`.
- Aplicación: `ar.edu.itba.certiflow.application.<ctx>`, clases `XxxUseCaseImpl implements XxxUseCase`.
- Infraestructura: `ar.edu.itba.certiflow.infrastructure.<ctx>.persistence`, `ar.edu.itba.certiflow.infrastructure.inmemory`, `ar.edu.itba.certiflow.infrastructure.shared.persistence`.
- Web: `ar.edu.itba.certiflow.web.<ctx>`, `ar.edu.itba.certiflow.web.config`, `ar.edu.itba.certiflow.web.support`; clase de arranque `ar.edu.itba.certiflow.CertiflowApplication`.
- `certiflow-domain` y `certiflow-application` no importan `org.springframework`, `jakarta.persistence` ni `tools.jackson`/`com.fasterxml`.
- Sin comentarios explicativos en el código (preferencia del usuario). Los javadoc existentes se mueven tal cual.
- Commits sin `Co-Authored-By` ni ninguna atribución a Claude (preferencia del usuario, prevalece sobre el harness). Un commit por tarea, mensaje en inglés, en imperativo, como los anteriores del repo.
- Cada tarea termina con `mvn -q clean verify` en verde desde la raíz.
- API: base `/api/v1`, HAL (`application/hal+json`), errores `application/problem+json`, persona en headers `X-Person-Id` y `X-Person-Name`.
- Cada cambio se anota en `CHANGES.md` en la tarea 17 (una entrada por tarea).

## Review Focus

1. **Un agregado leído de la base se comporta igual que el original**: una inspección cerrada y recargada sigue rechazando respuestas; un hallazgo recargado numera la próxima acción como `n+1`. Test en la tarea 9.
2. **Una respuesta JSON del tipo equivocado** (`{"value":"abc"}` en un criterio numérico, o `{"value":5}` en uno sí/no) responde 400 con `AnswerTypeMismatchException`, nunca 500. Test en la tarea 13.
3. **Una operación rechazada no deja auditoría en la base**: cerrar con criterios pendientes hace rollback y `audit_entry` no gana filas. Test en la tarea 13.
4. **Un código de criterio inexistente en la URL** responde 404, no 409. Test en la tarea 13.
5. **La escala de un decimal sobrevive al viaje** JSON → dominio → base → dominio: `7.00` vuelve como `7.00` y el acta lo muestra igual. Test en la tarea 9.

---

## Mapa de archivos

| Módulo | Qué se crea o cambia |
|---|---|
| `certiflow-domain` | Renombre de paquetes (T1). `RawAnswer` y `answerFrom` (T2). `XxxCommand` por caso de uso (T3). `AuditLog` por id (T4). `InspectionResult`, `OpenNonConformity`, `InspectionResultTranslator` (T5). `XxxSnapshot` y `restore` (T6). Casos de uso de lectura (T7). |
| `certiflow-application` | Renombre `Interactor` → `UseCaseImpl` (T1); comandos (T3); traductor ACL (T5); consultas (T7). |
| `certiflow-infrastructure` | Antes `certiflow-details`. Repositorios en memoria (T1). JPA, Flyway, mappers, `JpaAuditLog`, numeración por secuencia (T8–T10). |
| `certiflow-web` | Nuevo. Arranque, composición, proxy transaccional, persona, errores (T11); controladores y tests de API (T12–T14). |
| raíz | `pom.xml` (T8, T11), `docker-compose.yml` y `.github/workflows/ci.yml` (T16), `DESIGN.md`, `README.md`, `CHANGES.md` (T17). |

---

### Task 1: Paquetes por bounded context y módulos DDD

Movimiento mecánico, sin cambiar comportamiento. Los 116 tests siguen pasando.

**Files:**
- Move: todo `certiflow-domain/src/{main,test}/java/ar/edu/itba/certiflow/{models,ports,usecases}/**`
- Move: `certiflow-application/src/{main,test}/java/**`
- Move: módulo `certiflow-details` → `certiflow-infrastructure`
- Modify: `pom.xml`, `certiflow-application/pom.xml`, `certiflow-infrastructure/pom.xml`
- Script de movimiento: vive en el scratchpad de la sesión (`$SCRATCH`) y no se commitea.

**Interfaces:**
- Produces: los nombres de paquete y de clase de la tabla de abajo; todas las tareas siguientes los usan.

| Origen | Destino |
|---|---|
| `models.asset` | `catalog.model` |
| `models.asset.exceptions` | `catalog.model.exceptions` |
| `models.schema`, `models.evaluation` | `schema.model` |
| `models.schema.exceptions` | `schema.model.exceptions` |
| `models.inspection`, `models.finding` | `inspection.model` |
| `models.inspection.exceptions`, `models.finding.exceptions` | `inspection.model.exceptions` |
| `models.certificate`, `models.certification` | `certification.model` |
| `models.certificate.exceptions`, `models.certification.exceptions` | `certification.model.exceptions` |
| `models.shared`, `models.audit` | `shared.model` |
| `ports.AssetRepository` | `catalog.repository` |
| `ports.InspectionSchemaRepository` | `schema.repository` |
| `ports.InspectionRepository`, `ports.FindingRepository` | `inspection.repository` |
| `ports.CertificateRepository`, `ports.CertificateNumbering` | `certification.repository` |
| `usecases.asset` | `catalog.usecase` |
| `usecases.schema` | `schema.usecase` |
| `usecases.inspection`, `usecases.finding` | `inspection.usecase` |
| `usecases.report.{InspectionAct, ActLine, GenerateInspectionAct, FindingsSummary, FindingLine, GenerateFindingsSummary}` | `inspection.usecase` |
| `usecases.certificate`, `usecases.report.{CertificateDocument, GenerateCertificateDocument}` | `certification.usecase` |
| `application.asset` | `application.catalog` |
| `application.schema` | `application.schema` |
| `application.inspection`, `application.finding`, `application.report.{GenerateInspectionAct,GenerateFindingsSummary}Interactor` | `application.inspection` |
| `application.certificate`, `application.report.GenerateCertificateDocumentInteractor` | `application.certification` |
| `details.inmemory` | `infrastructure.inmemory` |
| tests `usecases.*Test` (módulo application) | `application.*Test` |
| tests `models.shared.RequiredValueTest` (módulo application) | `application.RequiredValueTest` |
| tests `support.*` | `application.support` |
| tests de dominio `models.<x>` | el paquete destino de `<x>` |

Renombres de clase: cada interfaz de caso de uso `Xxx` → `XxxUseCase`; cada `XxxInteractor` → `XxxUseCaseImpl`. Los 18 casos de uso: `RegisterAsset`, `CreateInspectionSchema`, `PublishSchemaVersion`, `AssignInspection`, `RecordAnswer`, `AttachEvidence`, `AddObservation`, `CloseInspection`, `RectifyInspection`, `PlanCorrectiveAction`, `VerifyCorrectiveAction`, `GenerateInspectionAct`, `GenerateFindingsSummary`, `GenerateCertificateDocument`, `IssueCertificate`, `EvaluateCertification`, `SuspendCertificate`, `RenewCertificate`.

- [ ] **Step 1: Verificar el punto de partida**

Run: `mvn -q clean test 2>&1 | tail -5; grep -h "Tests run" */target/surefire-reports/*.txt | awk -F'[ ,]+' '{s+=$3} END {print s}'`
Expected: build verde, `116`.

- [ ] **Step 2: Renombrar el módulo**

```bash
git mv certiflow-details certiflow-infrastructure
sed -i '' 's|<module>certiflow-details</module>|<module>certiflow-infrastructure</module>|; s|<artifactId>certiflow-details</artifactId>|<artifactId>certiflow-infrastructure</artifactId>|' pom.xml certiflow-application/pom.xml
sed -i '' 's|<artifactId>certiflow-details</artifactId>|<artifactId>certiflow-infrastructure</artifactId>|; s|<name>.*</name>|<name>Certiflow - infraestructura (adaptadores de los repositorios)</name>|' certiflow-infrastructure/pom.xml
sed -i '' 's|<name>.*</name>|<name>Certiflow - dominio (model, repository, usecase por contexto)</name>|' certiflow-domain/pom.xml
```

- [ ] **Step 3: Escribir el script de movimiento**

Guardar en `$SCRATCH/restructure.py` (el scratchpad de la sesión, fuera del repo):

```python
import os, re, subprocess, sys

ROOT = sys.argv[1]
BASE = "ar/edu/itba/certiflow"
PKG = "ar.edu.itba.certiflow"

PACKAGE_MAP = {
    "models.asset.exceptions": "catalog.model.exceptions",
    "models.asset": "catalog.model",
    "models.schema.exceptions": "schema.model.exceptions",
    "models.schema": "schema.model",
    "models.evaluation": "schema.model",
    "models.inspection.exceptions": "inspection.model.exceptions",
    "models.inspection": "inspection.model",
    "models.finding.exceptions": "inspection.model.exceptions",
    "models.finding": "inspection.model",
    "models.certificate.exceptions": "certification.model.exceptions",
    "models.certificate": "certification.model",
    "models.certification.exceptions": "certification.model.exceptions",
    "models.certification": "certification.model",
    "models.shared": "shared.model",
    "models.audit": "shared.model",
    "usecases.asset": "catalog.usecase",
    "usecases.schema": "schema.usecase",
    "usecases.inspection": "inspection.usecase",
    "usecases.finding": "inspection.usecase",
    "usecases.certificate": "certification.usecase",
    "application.asset": "application.catalog",
    "application.schema": "application.schema",
    "application.inspection": "application.inspection",
    "application.finding": "application.inspection",
    "application.certificate": "application.certification",
    "details.inmemory": "infrastructure.inmemory",
    "support": "application.support",
    "usecases": "application",
}

CLASS_PACKAGE = {
    "ports.AssetRepository": "catalog.repository",
    "ports.InspectionSchemaRepository": "schema.repository",
    "ports.InspectionRepository": "inspection.repository",
    "ports.FindingRepository": "inspection.repository",
    "ports.CertificateRepository": "certification.repository",
    "ports.CertificateNumbering": "certification.repository",
    "usecases.report.InspectionAct": "inspection.usecase",
    "usecases.report.ActLine": "inspection.usecase",
    "usecases.report.GenerateInspectionAct": "inspection.usecase",
    "usecases.report.FindingsSummary": "inspection.usecase",
    "usecases.report.FindingLine": "inspection.usecase",
    "usecases.report.GenerateFindingsSummary": "inspection.usecase",
    "usecases.report.CertificateDocument": "certification.usecase",
    "usecases.report.GenerateCertificateDocument": "certification.usecase",
    "application.report.GenerateInspectionActInteractor": "application.inspection",
    "application.report.GenerateFindingsSummaryInteractor": "application.inspection",
    "application.report.GenerateCertificateDocumentInteractor": "application.certification",
    "models.shared.RequiredValueTest@application": "application",
}

USE_CASES = ["RegisterAsset", "CreateInspectionSchema", "PublishSchemaVersion", "AssignInspection", "RecordAnswer",
             "AttachEvidence", "AddObservation", "CloseInspection", "RectifyInspection", "PlanCorrectiveAction",
             "VerifyCorrectiveAction", "GenerateInspectionAct", "GenerateFindingsSummary",
             "GenerateCertificateDocument", "IssueCertificate", "EvaluateCertification", "SuspendCertificate",
             "RenewCertificate"]

def java_files():
    for module in ["certiflow-domain", "certiflow-application", "certiflow-infrastructure"]:
        for scope in ["main", "test"]:
            src = os.path.join(ROOT, module, "src", scope, "java")
            for dirpath, _, files in os.walk(src):
                for f in files:
                    if f.endswith(".java"):
                        yield module, src, os.path.join(dirpath, f)

def target_package(module, old_pkg, cls):
    rel = old_pkg[len(PKG) + 1:]
    key = rel + "." + cls
    if key + "@" + module.split("-")[1] in CLASS_PACKAGE:
        return PKG + "." + CLASS_PACKAGE[key + "@" + module.split("-")[1]]
    if key in CLASS_PACKAGE:
        return PKG + "." + CLASS_PACKAGE[key]
    for old in sorted(PACKAGE_MAP, key=len, reverse=True):
        if rel == old:
            return PKG + "." + PACKAGE_MAP[old]
    raise SystemExit("unmapped: " + key)

moves = []
fqcn = {}
for module, src, path in java_files():
    text = open(path).read()
    old_pkg = re.search(r"^package ([\w.]+);", text, re.M).group(1)
    cls = os.path.basename(path)[:-5]
    new_pkg = target_package(module, old_pkg, cls)
    new_cls = cls
    if cls.endswith("Interactor"):
        new_cls = cls[:-len("Interactor")] + "UseCaseImpl"
    elif cls in USE_CASES:
        new_cls = cls + "UseCase"
    moves.append((path, os.path.join(src, new_pkg.replace(".", "/"), new_cls + ".java"), new_pkg))
    fqcn[old_pkg + "." + cls] = new_pkg + "." + new_cls

simple_renames = {u + "Interactor": u + "UseCaseImpl" for u in USE_CASES}
simple_renames.update({u: u + "UseCase" for u in USE_CASES})

for old_path, new_path, new_pkg in moves:
    text = open(old_path).read()
    text = re.sub(r"^package [\w.]+;", "package " + new_pkg + ";", text, count=1, flags=re.M)
    for old, new in sorted(fqcn.items(), key=lambda kv: len(kv[0]), reverse=True):
        text = re.sub(r"\bimport (static )?" + re.escape(old) + r"\b", lambda m: "import " + (m.group(1) or "") + new, text)
    for old, new in simple_renames.items():
        text = re.sub(r"(?<![\w.])" + old + r"(?!\w)", new, text)
    open(old_path, "w").write(text)
    os.makedirs(os.path.dirname(new_path), exist_ok=True)
    if old_path != new_path:
        subprocess.run(["git", "mv", old_path, new_path], cwd=ROOT, check=True)

for module, src, path in java_files():
    text = open(path).read()
    own_pkg = re.search(r"^package ([\w.]+);", text, re.M).group(1)
    lines = text.split("\n")
    seen, out = set(), []
    for line in lines:
        m = re.match(r"import ([\w.]+)\.(\w+);", line)
        if m and m.group(1) == own_pkg:
            continue
        if line.startswith("import ") and line in seen:
            continue
        seen.add(line)
        out.append(line)
    open(path, "w").write("\n".join(out))

for module, src, _ in set((m, s, None) for m, s, _ in java_files()):
    for dirpath, dirs, files in os.walk(src, topdown=False):
        if not os.listdir(dirpath):
            os.rmdir(dirpath)
```

- [ ] **Step 4: Correr el script y agregar los imports que faltan**

Run: `python3 -I $SCRATCH/restructure.py "$PWD" && mvn -q clean test-compile 2>&1 | grep "ERROR.*cannot find symbol" -A2 | head -40`

Al juntar paquetes (por ejemplo `models.inspection` con `models.finding`), clases que antes estaban en el mismo paquete pueden quedar en paquetes distintos de otro módulo, y viceversa. Para cada `cannot find symbol` agregar el `import` del paquete destino según la tabla. Repetir hasta que compile.

Expected final: `mvn -q clean test-compile` sin salida.

- [ ] **Step 5: Verificar que el comportamiento no cambió**

Run: `mvn -q clean test 2>&1 | tail -5; grep -h "Tests run" */target/surefire-reports/*.txt | awk -F'[ ,]+' '{s+=$3} END {print s}'`
Expected: verde, `116`.

- [ ] **Step 6: Verificar que no quedan nombres viejos**

Run: `grep -rn "models\.\|\.ports\.\|usecases\.\|Interactor\|details\." --include=*.java certiflow-*/src | head`
Expected: sin salida.

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "Organize the code by bounded context and DDD layer"
```

---

### Task 2: Respuesta cruda y criterio por código

**Files:**
- Create: `certiflow-domain/src/main/java/ar/edu/itba/certiflow/schema/model/RawAnswer.java`
- Create: `certiflow-domain/src/main/java/ar/edu/itba/certiflow/schema/model/AnswerTypeMismatchException.java`
- Modify: `schema/model/Answer.java`, `ApprovalRule.java`, `NumericRangeRule.java`, `YesNoRule.java`, `OptionInListRule.java`, `NumericAnswer.java`, `YesNoAnswer.java`, `OptionAnswer.java`, `Criterion.java`, `SchemaVersion.java`
- Modify: `inspection/model/InspectionService.java`
- Test: `certiflow-domain/src/test/java/ar/edu/itba/certiflow/schema/model/RawAnswerTest.java`

**Interfaces:**
- Produces: `sealed interface RawAnswer permits RawAnswer.Number, RawAnswer.YesNo, RawAnswer.Option`; `RawAnswer.Number(BigDecimal value)`, `RawAnswer.YesNo(boolean value)`, `RawAnswer.Option(String value)`.
- Produces: `Answer.raw(): RawAnswer`; `ApprovalRule<A>.answerFrom(RawAnswer): A`; `Criterion<A>.answerFrom(RawAnswer): A`; `SchemaVersion.criterion(String code): Criterion<?>` (lanza `CriterionNotInSchemaException(String)`).
- Produces: `InspectionService.recordAnswer(Inspection, String criterionCode, RawAnswer, Person, Instant)`, `attachEvidence(Inspection, String criterionCode, Evidence, Person, Instant)`, `addObservation(Inspection, String criterionCode, String, Person, Instant)`, `rectify(Inspection, String criterionCode, RawAnswer, String reason, Person, Instant)`.

- [ ] **Step 1: Test que falla**

```java
package ar.edu.itba.certiflow.schema.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ar.edu.itba.certiflow.schema.model.exceptions.CriterionNotInSchemaException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.Test;

class RawAnswerTest {

    private static final ApprovalRule<NumericAnswer> NUMERIC =
            new NumericRangeRule(new Unit("bar"), new Range(new BigDecimal("6"), new BigDecimal("8")));
    private static final ApprovalRule<YesNoAnswer> YES_NO = new YesNoRule(YesNoAnswer.YES);
    private static final ApprovalRule<OptionAnswer> OPTION = new OptionInListRule(Set.of("VISIBLE"));

    static Stream<Arguments> acceptedAnswers() {
        return Stream.of(
                Arguments.of(NUMERIC, new RawAnswer.Number(new BigDecimal("7.00")), new NumericAnswer(new BigDecimal("7.00"))),
                Arguments.of(YES_NO, new RawAnswer.YesNo(true), YesNoAnswer.YES),
                Arguments.of(YES_NO, new RawAnswer.YesNo(false), YesNoAnswer.NO),
                Arguments.of(OPTION, new RawAnswer.Option("VISIBLE"), new OptionAnswer("VISIBLE")));
    }

    static Stream<Arguments> mismatchedAnswers() {
        return Stream.of(
                Arguments.of(NUMERIC, new RawAnswer.YesNo(true)),
                Arguments.of(NUMERIC, new RawAnswer.Option("7")),
                Arguments.of(YES_NO, new RawAnswer.Number(BigDecimal.ONE)),
                Arguments.of(YES_NO, new RawAnswer.Option("YES")),
                Arguments.of(OPTION, new RawAnswer.Number(BigDecimal.ONE)),
                Arguments.of(OPTION, new RawAnswer.YesNo(true)));
    }

    @ParameterizedTest
    @MethodSource("acceptedAnswers")
    void eachRuleTurnsTheRawValueItAcceptsIntoItsTypedAnswer(ApprovalRule<?> rule, RawAnswer raw, Answer expected) {
        assertEquals(expected, rule.answerFrom(raw));
    }

    @ParameterizedTest
    @MethodSource("mismatchedAnswers")
    void eachRuleRejectsARawValueOfAnotherType(ApprovalRule<?> rule, RawAnswer raw) {
        assertThrows(AnswerTypeMismatchException.class, () -> rule.answerFrom(raw));
    }

    @ParameterizedTest
    @MethodSource("acceptedAnswers")
    void aTypedAnswerGoesBackToTheRawValueItCameFrom(ApprovalRule<?> rule, RawAnswer raw, Answer typed) {
        assertEquals(raw, typed.raw());
    }

    @Test
    void aVersionFindsItsCriterionByCodeAndRejectsAnUnknownOne() {
        Criterion<YesNoAnswer> seal = new Criterion<>("EXT-02", "Seal intact", YES_NO, StandardSeverity.CRITICAL, List.of());
        SchemaVersion version = new SchemaVersion(1, List.of(new Section("Seal", List.of(seal))), java.time.Instant.EPOCH);

        assertEquals(seal, version.criterion("EXT-02"));
        assertThrows(CriterionNotInSchemaException.class, () -> version.criterion("EXT-99"));
    }
}
```

- [ ] **Step 2: Ver que falla**

Run: `mvn -q -pl certiflow-domain test -Dtest=RawAnswerTest 2>&1 | grep -m3 ERROR`
Expected: `cannot find symbol ... RawAnswer`.

- [ ] **Step 3: Implementar**

`RawAnswer.java`:

```java
package ar.edu.itba.certiflow.schema.model;

import java.math.BigDecimal;

public sealed interface RawAnswer {

    record Number(BigDecimal value) implements RawAnswer {
    }

    record YesNo(boolean value) implements RawAnswer {
    }

    record Option(String value) implements RawAnswer {
    }
}
```

`AnswerTypeMismatchException.java`:

```java
package ar.edu.itba.certiflow.schema.model;

import ar.edu.itba.certiflow.shared.model.DomainException;

public final class AnswerTypeMismatchException extends DomainException {

    public AnswerTypeMismatchException(String expected, RawAnswer received) {
        super("Expected " + expected + " answer but received " + received);
    }
}
```

Antes de escribirla, abrir `shared/model/DomainException.java` y usar su constructor tal como lo usan las demás excepciones (por ejemplo `InvalidRuleConfigurationException`).

`Answer.java`:

```java
package ar.edu.itba.certiflow.schema.model;

public interface Answer {

    RawAnswer raw();
}
```

`ApprovalRule.java`:

```java
package ar.edu.itba.certiflow.schema.model;

public interface ApprovalRule<A extends Answer> {

    boolean isSatisfiedBy(A answer);

    A answerFrom(RawAnswer raw);
}
```

`NumericRangeRule.java`, agregar:

```java
    @Override
    public NumericAnswer answerFrom(RawAnswer raw) {
        if (raw instanceof RawAnswer.Number(java.math.BigDecimal value)) {
            return new NumericAnswer(value);
        }
        throw new AnswerTypeMismatchException("numeric", raw);
    }
```

`YesNoRule.java`, agregar:

```java
    @Override
    public YesNoAnswer answerFrom(RawAnswer raw) {
        if (raw instanceof RawAnswer.YesNo(boolean value)) {
            return value ? YesNoAnswer.YES : YesNoAnswer.NO;
        }
        throw new AnswerTypeMismatchException("yes/no", raw);
    }
```

`OptionInListRule.java`, agregar:

```java
    @Override
    public OptionAnswer answerFrom(RawAnswer raw) {
        if (raw instanceof RawAnswer.Option(String value)) {
            return new OptionAnswer(value);
        }
        throw new AnswerTypeMismatchException("option", raw);
    }
```

`NumericAnswer.java`: `public RawAnswer raw() { return new RawAnswer.Number(value); }` con `@Override`.
`OptionAnswer.java`: `public RawAnswer raw() { return new RawAnswer.Option(option); }` con `@Override`.
`YesNoAnswer.java`:

```java
public enum YesNoAnswer implements Answer {
    YES,
    NO;

    @Override
    public RawAnswer raw() {
        return new RawAnswer.YesNo(this == YES);
    }
}
```

`Criterion.java`, agregar:

```java
    public A answerFrom(RawAnswer raw) {
        return rule.answerFrom(raw);
    }
```

`SchemaVersion.java`, agregar:

```java
    public Criterion<?> criterion(String code) {
        return criteria().stream()
                .filter(criterion -> criterion.code().equals(code))
                .findFirst()
                .orElseThrow(() -> new CriterionNotInSchemaException(code));
    }
```

Abrir `CriterionNotInSchemaException` y agregarle un constructor `(String criterionCode)` con el mismo mensaje que el constructor existente arma a partir del criterio.

`InspectionService.java`: agregar las sobrecargas por código. Cada una resuelve el criterio en la versión de la inspección y delega en la existente; el método genérico auxiliar captura el comodín:

```java
    public void recordAnswer(Inspection inspection, String criterionCode, RawAnswer answer, Person answeredBy, Instant answeredAt) {
        recordTyped(inspection, inspection.schemaVersion().criterion(criterionCode), answer, answeredBy, answeredAt);
    }

    public void attachEvidence(Inspection inspection, String criterionCode, Evidence evidence, Person attachedBy, Instant attachedAt) {
        attachEvidence(inspection, inspection.schemaVersion().criterion(criterionCode), evidence, attachedBy, attachedAt);
    }

    public void addObservation(Inspection inspection, String criterionCode, String observationText, Person observedBy, Instant observedAt) {
        addObservation(inspection, inspection.schemaVersion().criterion(criterionCode), observationText, observedBy, observedAt);
    }

    public InspectionRectified rectify(Inspection inspection, String criterionCode, RawAnswer answer, String reason,
                                       Person rectifiedBy, Instant rectifiedAt) {
        return rectifyTyped(inspection, inspection.schemaVersion().criterion(criterionCode), answer, reason, rectifiedBy, rectifiedAt);
    }

    private <A extends Answer> void recordTyped(Inspection inspection, Criterion<A> criterion, RawAnswer answer, Person answeredBy,
                                                Instant answeredAt) {
        recordAnswer(inspection, criterion, criterion.answerFrom(answer), answeredBy, answeredAt);
    }

    private <A extends Answer> InspectionRectified rectifyTyped(Inspection inspection, Criterion<A> criterion, RawAnswer answer,
                                                                String reason, Person rectifiedBy, Instant rectifiedAt) {
        return rectify(inspection, criterion, criterion.answerFrom(answer), reason, rectifiedBy, rectifiedAt);
    }
```

- [ ] **Step 4: Ver que pasa todo**

Run: `mvn -q clean test`
Expected: verde.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "Let each approval rule build its typed answer from a raw value"
```

---

### Task 3: Casos de uso con comandos

**Files:**
- Create en `certiflow-domain/src/main/java/ar/edu/itba/certiflow/<ctx>/usecase/`: un `XxxCommand` por caso de uso de escritura (lista abajo).
- Modify: las 18 interfaces `XxxUseCase` y sus 18 `XxxUseCaseImpl`.
- Modify: `certiflow-application/src/test/java/ar/edu/itba/certiflow/application/support/CertiflowFixture.java` y los 11 tests de `application`.

**Interfaces:**
- Produces (todos records, en el paquete `usecase` de su contexto):

| Command | Campos | `execute` devuelve |
|---|---|---|
| `catalog.usecase.RegisterAssetCommand` | `AssetCode code, String name, AssetType type, Location location, Person responsible, Characteristics characteristics, Person registeredBy` | `Asset` |
| `schema.usecase.CreateInspectionSchemaCommand` | `String name, AssetType assetType, List<Section> sections, Person createdBy` | `InspectionSchema` |
| `schema.usecase.PublishSchemaVersionCommand` | `AssetType assetType, List<Section> sections, Person publishedBy` | `SchemaVersion` |
| `inspection.usecase.AssignInspectionCommand` | `AssetCode assetCode, Person inspector, LocalDate plannedDate, InspectionScope scope, Person assignedBy` | `InspectionId` |
| `inspection.usecase.RecordAnswerCommand` | `InspectionId inspectionId, String criterionCode, RawAnswer answer, Person answeredBy` | `void` |
| `inspection.usecase.AttachEvidenceCommand` | `InspectionId inspectionId, String criterionCode, Evidence evidence, Person attachedBy` | `void` |
| `inspection.usecase.AddObservationCommand` | `InspectionId inspectionId, String criterionCode, String text, Person observedBy` | `void` |
| `inspection.usecase.CloseInspectionCommand` | `InspectionId inspectionId, Person closedBy` | `List<FindingId>` |
| `inspection.usecase.RectifyInspectionCommand` | `InspectionId inspectionId, String criterionCode, RawAnswer answer, String reason, Person rectifiedBy` | `List<FindingId>` |
| `inspection.usecase.PlanCorrectiveActionCommand` | `FindingId findingId, String description, Person responsible, LocalDate dueDate, Person plannedBy` | `CorrectiveActionId` |
| `inspection.usecase.VerifyCorrectiveActionCommand` | `FindingId findingId, CorrectiveActionId actionId, Person verifier, VerificationResult result, String notes` | `Verification` |
| `certification.usecase.IssueCertificateCommand` | `InspectionId inspectionId, LocalDate validUntil, Person issuedBy` | `CertificateNumber` |
| `certification.usecase.SuspendCertificateCommand` | `CertificateNumber number, String reason, Person suspendedBy` | `void` |
| `certification.usecase.RenewCertificateCommand` | `CertificateNumber currentNumber, InspectionId inspectionId, LocalDate validUntil, Person renewedBy` | `CertificateNumber` |

Los cuatro de lectura (`GenerateInspectionAct`, `GenerateFindingsSummary`, `GenerateCertificateDocument`, `EvaluateCertification`) mantienen su parámetro único (un id), que ya es su comando.

- [ ] **Step 1: Crear los comandos**

Un archivo por fila de la tabla, todos con esta forma (ejemplo completo del más largo):

```java
package ar.edu.itba.certiflow.catalog.usecase;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.catalog.model.AssetType;
import ar.edu.itba.certiflow.catalog.model.Characteristics;
import ar.edu.itba.certiflow.catalog.model.Location;
import ar.edu.itba.certiflow.shared.model.Person;

public record RegisterAssetCommand(AssetCode code, String name, AssetType type, Location location, Person responsible,
                                   Characteristics characteristics, Person registeredBy) {
}
```

`AssignInspectionCommand` agrega un método que arma el VO existente:

```java
    public InspectionAssignment assignment() {
        return new InspectionAssignment(inspector, plannedDate, scope);
    }
```

- [ ] **Step 2: Cambiar las interfaces**

Cada `XxxUseCase` de escritura pasa a tener un único método `Retorno execute(XxxCommand command);` con el retorno de la tabla. Ejemplo:

```java
public interface RecordAnswerUseCase {

    void execute(RecordAnswerCommand command);
}
```

- [ ] **Step 3: Cambiar las implementaciones**

Cada `XxxUseCaseImpl` lee los campos del comando. Los que reciben un criterio usan las sobrecargas por código de `InspectionService` (tarea 2). Ejemplos:

```java
    @Override
    public void execute(RecordAnswerCommand command) {
        Inspection inspection = inspectionRepository.findById(command.inspectionId())
                .orElseThrow(() -> new InspectionNotFoundException(command.inspectionId()));
        inspectionService.recordAnswer(inspection, command.criterionCode(), command.answer(), command.answeredBy(), clock.instant());
        inspectionRepository.save(inspection);
    }
```

```java
    @Override
    public Asset execute(RegisterAssetCommand command) {
        Asset asset = new Asset(command.code(), command.name(), command.type(), command.location(), command.responsible(),
                command.characteristics());
        Asset registeredAsset = assetService.register(asset, assetRepository.findByCode(asset.code()), command.registeredBy(),
                clock.instant());
        assetRepository.save(registeredAsset);
        return registeredAsset;
    }
```

Antes de editar `RegisterAssetUseCaseImpl`, leer su cuerpo actual y conservar el mismo orden de llamadas; el ejemplo solo cambia de dónde salen los datos.

- [ ] **Step 4: Adaptar el fixture**

En `CertiflowFixture`:
- `extinguisher` se registra con `registerAsset.execute(new RegisterAssetCommand(new AssetCode("EXT-0042"), "Lobby extinguisher", EXTINGUISHER, new Location("Main building, ground floor lobby"), assetResponsible, new Characteristics(List.of(...)), planner))`.
- El constructor usa `new CreateInspectionSchemaCommand("Portable extinguisher inspection", EXTINGUISHER, firstVersionSections(), planner)`.
- `assignedInspection(scope)` usa `new AssignInspectionCommand(extinguisher.code(), inspector, TODAY.plusDays(3), scope, planner)`.
- Agregar helpers para que los tests sigan leyéndose en el lenguaje del dominio:

```java
    public void answer(InspectionId inspection, Criterion<?> criterion, Answer answer, Person by) {
        recordAnswer.execute(new RecordAnswerCommand(inspection, criterion.code(), answer.raw(), by));
    }

    public void attach(InspectionId inspection, Criterion<?> criterion, Evidence evidence, Person by) {
        attachEvidence.execute(new AttachEvidenceCommand(inspection, criterion.code(), evidence, by));
    }

    public List<FindingId> close(InspectionId inspection, Person by) {
        return closeInspection.execute(new CloseInspectionCommand(inspection, by));
    }

    public List<FindingId> rectify(InspectionId inspection, Criterion<?> criterion, Answer answer, String reason, Person by) {
        return rectifyInspection.execute(new RectifyInspectionCommand(inspection, criterion.code(), answer.raw(), reason, by));
    }

    public CorrectiveActionId plan(FindingId finding, String description, Person responsible, LocalDate dueDate, Person by) {
        return planCorrectiveAction.execute(new PlanCorrectiveActionCommand(finding, description, responsible, dueDate, by));
    }

    public Verification verify(FindingId finding, CorrectiveActionId action, Person verifier, VerificationResult result, String notes) {
        return verifyCorrectiveAction.execute(new VerifyCorrectiveActionCommand(finding, action, verifier, result, notes));
    }

    public CertificateNumber issue(InspectionId inspection, LocalDate validUntil, Person by) {
        return issueCertificate.execute(new IssueCertificateCommand(inspection, validUntil, by));
    }

    public CertificateNumber renew(CertificateNumber current, InspectionId inspection, LocalDate validUntil, Person by) {
        return renewCertificate.execute(new RenewCertificateCommand(current, inspection, validUntil, by));
    }

    public void suspend(CertificateNumber number, String reason, Person by) {
        suspendCertificate.execute(new SuspendCertificateCommand(number, reason, by));
    }
```

`answerAll`, `closedCompliantInspection`, `closeFinding` y `majorFindingOf` pasan a usar estos helpers.

- [ ] **Step 5: Adaptar los tests**

Reemplazar cada llamada vieja por el helper equivalente, sin cambiar qué verifica el test. Las equivalencias:

| Antes | Después |
|---|---|
| `fixture.recordAnswer.execute(i, c, a, p)` | `fixture.answer(i, c, a, p)` |
| `fixture.attachEvidence.execute(i, c, e, p)` | `fixture.attach(i, c, e, p)` |
| `fixture.addObservation.execute(i, c, t, p)` | `fixture.addObservation.execute(new AddObservationCommand(i, c.code(), t, p))` |
| `fixture.closeInspection.execute(i, p)` | `fixture.close(i, p)` |
| `fixture.rectifyInspection.execute(i, c, a, r, p)` | `fixture.rectify(i, c, a, r, p)` |
| `fixture.planCorrectiveAction.execute(f, d, r, due, p)` | `fixture.plan(f, d, r, due, p)` |
| `fixture.verifyCorrectiveAction.execute(f, a, v, res, n)` | `fixture.verify(f, a, v, res, n)` |
| `fixture.issueCertificate.execute(i, u, p)` | `fixture.issue(i, u, p)` |
| `fixture.renewCertificate.execute(n, i, u, p)` | `fixture.renew(n, i, u, p)` |
| `fixture.suspendCertificate.execute(n, r, p)` | `fixture.suspend(n, r, p)` |
| `fixture.publishSchemaVersion.execute(t, s, p)` | `fixture.publishSchemaVersion.execute(new PublishSchemaVersionCommand(t, s, p))` |
| `fixture.createInspectionSchema.execute(n, t, s, p)` | `fixture.createInspectionSchema.execute(new CreateInspectionSchemaCommand(n, t, s, p))` |
| `fixture.assignInspection.execute(code, assignment, p)` | `fixture.assignInspection.execute(new AssignInspectionCommand(code, assignment.inspector(), assignment.plannedDate(), assignment.scope(), p))` |
| `fixture.registerAsset.execute(asset, p)` | `fixture.registerAsset.execute(new RegisterAssetCommand(asset.code(), asset.name(), asset.type(), asset.location(), asset.responsible(), asset.characteristics(), p))` |

Run después de cada archivo: `mvn -q -pl certiflow-application -am test-compile`.

- [ ] **Step 6: Test nuevo: código de criterio desconocido**

En `InspectionExecutionTest`:

```java
    @Test
    void answeringAnUnknownCriterionCodeIsRejectedAndChangesNothing() {
        InspectionId inspection = fixture.assignedInspection();
        int entriesBefore = fixture.inspectionLog.historyOf(fixture.inspection(inspection)).size();

        assertThrows(CriterionNotInSchemaException.class, () -> fixture.recordAnswer.execute(
                new RecordAnswerCommand(inspection, "EXT-99", new RawAnswer.YesNo(true), fixture.inspector)));

        assertEquals(entriesBefore, fixture.inspectionLog.historyOf(fixture.inspection(inspection)).size());
    }

    @Test
    void answeringWithTheWrongTypeOfValueIsRejectedAndChangesNothing() {
        InspectionId inspection = fixture.assignedInspection();

        assertThrows(AnswerTypeMismatchException.class, () -> fixture.recordAnswer.execute(
                new RecordAnswerCommand(inspection, "EXT-01", new RawAnswer.YesNo(true), fixture.inspector)));

        assertEquals(Outcome.PENDING, fixture.inspection(inspection).evaluate().outcomeOf(fixture.pressureCriterion));
    }
```

(La tarea 4 cambia `historyOf(fixture.inspection(inspection))` por `historyOf(inspection)`.)

- [ ] **Step 7: Verificar y commitear**

Run: `mvn -q clean test`
Expected: verde; los 116 originales más los nuevos.

```bash
git add -A
git commit -m "Pass commands with ids and criterion codes to every use case"
```

---

### Task 4: Auditoría indexada por id

**Files:**
- Modify: `inspection/model/InspectionService.java`, `inspection/model/FindingService.java`, `certification/model/CertificateService.java`
- Modify: `application/inspection/GenerateInspectionActUseCaseImpl.java`
- Modify: `CertiflowFixture.java` y los tests que llaman `historyOf(...)`

**Interfaces:**
- Consumes: `Inspection.id(): InspectionId`, `Finding.id(): FindingId`, `Certificate.number(): CertificateNumber`.
- Produces: `InspectionService(InspectionFactory, AuditLog<InspectionId>)`, `FindingService(FindingFactory, AuditLog<FindingId>)`, `CertificateService(CertificateFactory, AuditLog<CertificateNumber>)`, `GenerateInspectionActUseCaseImpl(InspectionRepository, AssetRepository, AuditLog<InspectionId>)`.

- [ ] **Step 1: Test que falla**

En `AggregateIdentityTest` (módulo application):

```java
    @Test
    void historyIsFoundByIdentityEvenFromAnotherInstanceOfTheSameAggregate() {
        InspectionId inspection = fixture.assignedInspection();

        assertEquals(List.of(InspectionAudit.ASSIGNED),
                fixture.inspectionLog.historyOf(inspection).stream().map(AuditEntry::action).toList());
    }
```

Run: `mvn -q -pl certiflow-application -am test-compile 2>&1 | grep -m1 ERROR`
Expected: error de tipos: `historyOf(InspectionId)` no aplica a `AuditLog<Inspection>`.

- [ ] **Step 2: Implementar**

En los tres servicios, cambiar el tipo del log y el volcado:

```java
    private final AuditLog<InspectionId> inspectionLog;

    private Inspection recorded(Inspection inspection) {
        inspectionLog.recordAll(inspection.id(), inspection.pullAuditEntries());
        return inspection;
    }
```

```java
    private void recorded(Finding finding) {
        findingLog.recordAll(finding.id(), finding.pullAuditEntries());
    }
```

```java
    private void recorded(Certificate certificate) {
        certificateLog.recordAll(certificate.number(), certificate.pullAuditEntries());
    }
```

`GenerateInspectionActUseCaseImpl`: `AuditLog<InspectionId>` e `inspectionLog.historyOf(inspection.id())`.

En el fixture: `AuditLog<InspectionId> inspectionLog`, `AuditLog<FindingId> findingLog`, `AuditLog<CertificateNumber> certificateLog`. En los tests: `historyOf(fixture.inspection(x))` → `historyOf(x)`, `historyOf(fixture.finding(x))` → `historyOf(x)`, `historyOf(fixture.certificate(x))` → `historyOf(x)`.

Run: `grep -rn "historyOf(fixture\.\(inspection\|finding\|certificate\)(" certiflow-application/src/test`
Expected: sin salida.

- [ ] **Step 3: Verificar y commitear**

Run: `mvn -q clean test`
Expected: verde.

```bash
git add -A
git commit -m "Index audit history by aggregate identity"
```

---

### Task 5: Capa anticorrupción entre Inspección y Certificación

**Files:**
- Create: `certiflow-domain/src/main/java/ar/edu/itba/certiflow/certification/model/InspectionResult.java`
- Create: `.../certification/model/OpenNonConformity.java`
- Create: `.../certification/model/InspectionResultTranslator.java`
- Create: `certiflow-application/src/main/java/ar/edu/itba/certiflow/application/certification/InspectionResultTranslatorImpl.java`
- Modify: `certification/model/CertificationEligibility.java`, `NoBlockingFindingsEligibility.java`, `Certificate.java`, `CertificateFactory.java`, `CertificateService.java`
- Modify: `application/certification/{Issue,Renew}CertificateUseCaseImpl.java`, `EvaluateCertificationUseCaseImpl.java`
- Modify: `CertiflowFixture.java`
- Test: `certiflow-domain/src/test/java/ar/edu/itba/certiflow/certification/model/NoBlockingFindingsEligibilityTest.java`
- Test: `certiflow-application/src/test/java/ar/edu/itba/certiflow/application/certification/InspectionResultTranslatorImplTest.java`
- Modify: `certiflow-application/pom.xml` (Mockito, scope test)

**Interfaces:**
- Produces:
  - `record InspectionResult(InspectionId inspectionId, AssetCode assetCode, Optional<Instant> closedAt, boolean coversWholeSchema, List<OpenNonConformity> openNonConformities)` con `isClosed()`, `reference(): InspectionReference` (lanza `InspectionNotClosedException` si no está cerrada), `blockingNonConformities(): List<OpenNonConformity>`.
  - `record OpenNonConformity(String criterionCode, Severity severity)` con `blocksCertification()`.
  - `interface InspectionResultTranslator { InspectionResult translate(InspectionId inspectionId); }`
  - `CertificationEligibility.evaluate(InspectionResult)`, `ensureEligible(InspectionResult)`.
  - `CertificateFactory.create(CertificateNumber, InspectionResult, ValidityPeriod, Person, Instant)`; `CertificateService.issue(CertificateNumber, InspectionResult, ValidityPeriod, Person, Instant)`, `ensureRenewableWith(Certificate, InspectionResult)`, `renew(Certificate, CertificateNumber, ValidityPeriod, InspectionResult, Person, Instant)`.
  - `InspectionResultTranslatorImpl(InspectionRepository, FindingRepository)`.
- `certification.model` deja de importar `inspection.model.Finding` e `inspection.model.InspectionView`. Sigue importando `InspectionId` (la identidad viaja entre contextos) e `InspectionNotClosedException`.

- [ ] **Step 1: Test unitario de la política, que falla**

```java
package ar.edu.itba.certiflow.certification.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.schema.model.StandardSeverity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class NoBlockingFindingsEligibilityTest {

    private static final Optional<Instant> CLOSED = Optional.of(Instant.parse("2026-03-10T10:00:00Z"));

    private static InspectionResult result(Optional<Instant> closedAt, boolean wholeSchema, OpenNonConformity... open) {
        return new InspectionResult(new InspectionId("I-1"), new AssetCode("EXT-0042"), closedAt, wholeSchema, List.of(open));
    }

    static Stream<Arguments> results() {
        return Stream.of(
                Arguments.of(result(CLOSED, true), List.of()),
                Arguments.of(result(CLOSED, true, new OpenNonConformity("EXT-03", StandardSeverity.MINOR)), List.of()),
                Arguments.of(result(CLOSED, true, new OpenNonConformity("EXT-01", StandardSeverity.MAJOR)),
                        List.of(IneligibilityReason.BLOCKING_FINDINGS)),
                Arguments.of(result(CLOSED, true, new OpenNonConformity("EXT-02", StandardSeverity.CRITICAL)),
                        List.of(IneligibilityReason.BLOCKING_FINDINGS)),
                Arguments.of(result(Optional.empty(), true), List.of(IneligibilityReason.INSPECTION_NOT_CLOSED)),
                Arguments.of(result(CLOSED, false), List.of(IneligibilityReason.PARTIAL_INSPECTION)),
                Arguments.of(result(Optional.empty(), false, new OpenNonConformity("EXT-01", StandardSeverity.MAJOR)),
                        List.of(IneligibilityReason.INSPECTION_NOT_CLOSED, IneligibilityReason.PARTIAL_INSPECTION,
                                IneligibilityReason.BLOCKING_FINDINGS)));
    }

    @ParameterizedTest
    @MethodSource("results")
    void decisionListsEveryReasonTheResultCannotBeCertified(InspectionResult result, List<IneligibilityReason> expected) {
        CertificationDecision decision = new NoBlockingFindingsEligibility().evaluate(result);

        assertEquals(expected, decision.ineligibilities().stream().map(Ineligibility::reason).toList());
    }
}
```

Run: `mvn -q -pl certiflow-domain test -Dtest=NoBlockingFindingsEligibilityTest 2>&1 | grep -m1 ERROR`
Expected: `cannot find symbol ... InspectionResult`.

- [ ] **Step 2: Implementar el modelo de Certificación**

```java
package ar.edu.itba.certiflow.certification.model;

import ar.edu.itba.certiflow.schema.model.Severity;

public record OpenNonConformity(String criterionCode, Severity severity) {

    public boolean blocksCertification() {
        return severity.blocksCertification();
    }
}
```

```java
package ar.edu.itba.certiflow.certification.model;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.inspection.model.exceptions.InspectionNotClosedException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public record InspectionResult(InspectionId inspectionId, AssetCode assetCode, Optional<Instant> closedAt,
                               boolean coversWholeSchema, List<OpenNonConformity> openNonConformities) {

    public InspectionResult {
        openNonConformities = List.copyOf(openNonConformities);
    }

    public boolean isClosed() {
        return closedAt.isPresent();
    }

    public InspectionReference reference() {
        return new InspectionReference(inspectionId, closedAt.orElseThrow(InspectionNotClosedException::new));
    }

    public List<OpenNonConformity> blockingNonConformities() {
        return openNonConformities.stream().filter(OpenNonConformity::blocksCertification).toList();
    }
}
```

```java
package ar.edu.itba.certiflow.certification.model;

import ar.edu.itba.certiflow.inspection.model.InspectionId;

public interface InspectionResultTranslator {

    InspectionResult translate(InspectionId inspectionId);
}
```

`CertificationEligibility`:

```java
public interface CertificationEligibility {

    CertificationDecision evaluate(InspectionResult result);

    default void ensureEligible(InspectionResult result) {
        CertificationDecision decision = evaluate(result);
        if (!decision.isEligible()) {
            throw new NotEligibleForCertificationException(decision);
        }
    }
}
```

(Conservar el javadoc existente sobre `ensureEligible`.)

`NoBlockingFindingsEligibility.evaluate(InspectionResult result)`: mismo cuerpo que hoy, cambiando las fuentes: `result.isClosed()`, `result.coversWholeSchema()`, `result.blockingNonConformities().size()`, `result.inspectionId().value()`, `result.assetCode().value()`.

`Certificate`: el constructor recibe `(CertificateNumber number, InspectionResult basedOn, ValidityPeriod validity, Person issuedBy, Instant issuedAt)`; `assetCode = basedOn.assetCode()`; `this.basedOn = basedOn.reference()` (que lanza `InspectionNotClosedException` si no está cerrada, igual que hoy). Se elimina el chequeo `InspectionOfAnotherAssetException` del constructor porque el activo sale del resultado. `ensureRenewableWith(InspectionResult newBasis)`: `newBasis.assetCode()` y `newBasis.reference().closedAt()` en lugar de `originalRevision().sealedAt()`. `renew(..., InspectionResult newBasis, ...)` igual. Ajustar `CertificateFactory` y `CertificateService` a las firmas de **Interfaces**.

- [ ] **Step 3: Implementar el traductor**

```java
package ar.edu.itba.certiflow.application.certification;

import ar.edu.itba.certiflow.certification.model.InspectionResult;
import ar.edu.itba.certiflow.certification.model.InspectionResultTranslator;
import ar.edu.itba.certiflow.certification.model.OpenNonConformity;
import ar.edu.itba.certiflow.inspection.model.Finding;
import ar.edu.itba.certiflow.inspection.model.Inspection;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.inspection.model.exceptions.InspectionNotFoundException;
import ar.edu.itba.certiflow.inspection.repository.FindingRepository;
import ar.edu.itba.certiflow.inspection.repository.InspectionRepository;
import java.util.Optional;

public final class InspectionResultTranslatorImpl implements InspectionResultTranslator {

    private final InspectionRepository inspectionRepository;
    private final FindingRepository findingRepository;

    public InspectionResultTranslatorImpl(InspectionRepository inspectionRepository, FindingRepository findingRepository) {
        this.inspectionRepository = inspectionRepository;
        this.findingRepository = findingRepository;
    }

    @Override
    public InspectionResult translate(InspectionId inspectionId) {
        Inspection inspection = inspectionRepository.findById(inspectionId)
                .orElseThrow(() -> new InspectionNotFoundException(inspectionId));
        return new InspectionResult(inspection.id(), inspection.assetCode(),
                inspection.isClosed() ? Optional.of(inspection.originalRevision().sealedAt()) : Optional.empty(),
                inspection.coversWholeSchema(),
                findingRepository.findByAsset(inspection.assetCode()).stream()
                        .filter(Finding::isOpen)
                        .map(finding -> new OpenNonConformity(finding.criterion().code(), finding.severity()))
                        .toList());
    }
}
```

`IssueCertificateUseCaseImpl` pasa a recibir `(InspectionResultTranslator, CertificateRepository, CertificationEligibility, CertificateNumbering, CertificateService, Clock)`:

```java
    @Override
    public CertificateNumber execute(IssueCertificateCommand command) {
        InspectionResult result = inspectionResultTranslator.translate(command.inspectionId());
        LocalDate today = LocalDate.now(clock);
        certificationEligibility.ensureEligible(result);
        certificateRepository.findCurrentFor(result.assetCode(), today).ifPresent(currentCertificate -> {
            throw new AssetAlreadyCertifiedException(result.assetCode(), currentCertificate.number());
        });
        ValidityPeriod validity = new ValidityPeriod(today, command.validUntil());
        Certificate certificate = certificateService.issue(certificateNumbering.next(), result, validity, command.issuedBy(),
                clock.instant());
        certificateRepository.save(certificate);
        return certificate.number();
    }
```

`RenewCertificateUseCaseImpl` igual: `(InspectionResultTranslator, CertificateRepository, CertificationEligibility, CertificateNumbering, CertificateService, Clock)`, con el mismo orden de validaciones que hoy (elegibilidad, vigencia, `ensureRenewableWith`, recién después `certificateNumbering.next()`).

`EvaluateCertificationUseCaseImpl(InspectionResultTranslator, CertificationEligibility)`: `return certificationEligibility.evaluate(inspectionResultTranslator.translate(inspectionId));`

En el fixture: `InspectionResultTranslator inspectionResultTranslator = new InspectionResultTranslatorImpl(inspectionRepository, findingRepository);` y los constructores nuevos.

- [ ] **Step 4: Test unitario del traductor, con mocks**

Agregar a `certiflow-application/pom.xml`:

```xml
    <dependency>
      <groupId>org.mockito</groupId>
      <artifactId>mockito-junit-jupiter</artifactId>
      <version>5.20.0</version>
      <scope>test</scope>
    </dependency>
```

Antes de fijar la versión, correr `curl -s https://repo.maven.apache.org/maven2/org/mockito/mockito-junit-jupiter/maven-metadata.xml | grep -o "<release>.*</release>"` y usar esa.

```java
package ar.edu.itba.certiflow.application.certification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

import ar.edu.itba.certiflow.application.support.CertiflowFixture;
import ar.edu.itba.certiflow.certification.model.InspectionResult;
import ar.edu.itba.certiflow.certification.model.OpenNonConformity;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.inspection.model.exceptions.InspectionNotFoundException;
import ar.edu.itba.certiflow.inspection.repository.FindingRepository;
import ar.edu.itba.certiflow.inspection.repository.InspectionRepository;
import ar.edu.itba.certiflow.schema.model.StandardSeverity;
import ar.edu.itba.certiflow.schema.model.YesNoAnswer;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InspectionResultTranslatorImplTest {

    private final CertiflowFixture fixture = new CertiflowFixture();

    @Mock
    private InspectionRepository inspectionRepository;
    @Mock
    private FindingRepository findingRepository;

    @Test
    void onlyOpenFindingsReachCertificationWithTheirSeverity() {
        InspectionId inspection = fixture.assignedInspection();
        fixture.answerAll(inspection, "5.00", YesNoAnswer.YES, "VISIBLE");
        fixture.close(inspection, fixture.inspector);
        when(inspectionRepository.findById(inspection)).thenReturn(Optional.of(fixture.inspection(inspection)));
        when(findingRepository.findByAsset(fixture.extinguisher.code()))
                .thenReturn(fixture.findingRepository.findByAsset(fixture.extinguisher.code()));

        InspectionResult result = new InspectionResultTranslatorImpl(inspectionRepository, findingRepository).translate(inspection);

        assertEquals(List.of(new OpenNonConformity("EXT-01", StandardSeverity.MAJOR)), result.openNonConformities());
        assertEquals(fixture.inspection(inspection).originalRevision().sealedAt(), result.closedAt().orElseThrow());
    }

    @Test
    void anOpenInspectionTranslatesToAResultWithoutClosingInstant() {
        InspectionId inspection = fixture.assignedInspection();
        when(inspectionRepository.findById(inspection)).thenReturn(Optional.of(fixture.inspection(inspection)));
        when(findingRepository.findByAsset(fixture.extinguisher.code())).thenReturn(List.of());

        assertFalse(new InspectionResultTranslatorImpl(inspectionRepository, findingRepository).translate(inspection).isClosed());
    }

    @Test
    void anUnknownInspectionCannotBeTranslated() {
        InspectionId unknown = new InspectionId("missing");
        when(inspectionRepository.findById(unknown)).thenReturn(Optional.empty());

        org.junit.jupiter.api.Assertions.assertThrows(InspectionNotFoundException.class,
                () -> new InspectionResultTranslatorImpl(inspectionRepository, findingRepository).translate(unknown));
    }
}
```

- [ ] **Step 5: Verificar el corte entre contextos**

Run: `grep -rln "inspection\.model\.\(Finding\|InspectionView\|Inspection;\)" certiflow-domain/src/main/java/ar/edu/itba/certiflow/certification`
Expected: sin salida.

Run: `mvn -q clean test`
Expected: verde. Los tests de `CertificationTest` que esperaban `InspectionOfAnotherAssetException` al **emitir** desaparecen como caso (el activo sale de la inspección); si alguno lo verificaba, cambiarlo para verificar que el certificado queda emitido para el activo de la inspección. Los de **renovación** con otro activo siguen lanzando `InspectionOfAnotherAssetException`.

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "Decide certification on an inspection result built by an anticorruption layer"
```

---

### Task 6: Snapshots y reconstrucción de agregados

**Files:**
- Create en `inspection/model/`: `InspectionSnapshot.java`, `FindingSnapshot.java`, `CorrectiveActionSnapshot.java`
- Create en `certification/model/`: `CertificateSnapshot.java`
- Modify: `Inspection.java`, `InspectionFactory.java`, `Finding.java`, `FindingFactory.java`, `CorrectiveAction.java`, `CorrectiveActions.java`, `Certificate.java`, `CertificateFactory.java`, `CertificateLifecycle.java` (pasa a `public`)
- Test: `certiflow-application/src/test/java/ar/edu/itba/certiflow/application/SnapshotTest.java`

**Interfaces:**
- Produces:
  - `record InspectionSnapshot(InspectionId id, AssetCode assetCode, Person assetResponsible, InspectionAssignment assignment, SchemaVersion schemaVersion, List<CriterionResponse<?>> responses, Revisions revisions)`; `Inspection.snapshot()`; `InspectionFactory.restore(InspectionSnapshot): Inspection`.
  - `record CorrectiveActionSnapshot(CorrectiveActionId id, String description, Person responsible, LocalDate dueDate, Person plannedBy, Instant plannedAt, List<Verification> verifications, Optional<Cancellation> cancellation)`.
  - `record FindingSnapshot(FindingId id, InspectionId inspectionId, AssetCode assetCode, Criterion<?> criterion, List<AttachedEvidence> evidence, Person responsible, List<CorrectiveActionSnapshot> actions)`; `Finding.snapshot()`; `FindingFactory.restore(FindingSnapshot): Finding`.
  - `record CertificateSnapshot(CertificateNumber number, AssetCode assetCode, InspectionReference basedOn, ValidityPeriod validity, Person issuedBy, Instant issuedAt, List<Suspension> suspensions, CertificateLifecycle lifecycle)`; `Certificate.snapshot()`; `CertificateFactory.restore(CertificateSnapshot): Certificate`.
  - `public enum CertificateLifecycle` (sus métodos siguen de paquete).

- [ ] **Step 1: Test que falla**

```java
package ar.edu.itba.certiflow.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.itba.certiflow.application.support.CertiflowFixture;
import ar.edu.itba.certiflow.certification.model.Certificate;
import ar.edu.itba.certiflow.certification.model.CertificateFactory;
import ar.edu.itba.certiflow.certification.model.CertificateNumber;
import ar.edu.itba.certiflow.inspection.model.CorrectiveActionId;
import ar.edu.itba.certiflow.inspection.model.Finding;
import ar.edu.itba.certiflow.inspection.model.FindingFactory;
import ar.edu.itba.certiflow.inspection.model.FindingId;
import ar.edu.itba.certiflow.inspection.model.Inspection;
import ar.edu.itba.certiflow.inspection.model.InspectionFactory;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.inspection.model.exceptions.InspectionAlreadyClosedException;
import ar.edu.itba.certiflow.schema.model.YesNoAnswer;
import org.junit.jupiter.api.Test;

class SnapshotTest {

    private final CertiflowFixture fixture = new CertiflowFixture();

    @Test
    void aRestoredInspectionHasTheSameStateAndNoPendingAuditEntries() {
        InspectionId id = fixture.closedCompliantInspection();
        Inspection original = fixture.inspection(id);

        Inspection restored = new InspectionFactory().restore(original.snapshot());

        assertEquals(original.snapshot(), restored.snapshot());
        assertTrue(restored.isClosed());
        int entriesBefore = fixture.inspectionLog.historyOf(id).size();
        fixture.inspectionRepository.save(restored);
        assertThrows(InspectionAlreadyClosedException.class,
                () -> fixture.answer(id, fixture.sealIntactCriterion, YesNoAnswer.NO, fixture.inspector));
        assertEquals(entriesBefore, fixture.inspectionLog.historyOf(id).size());
    }

    @Test
    void aRestoredFindingKeepsNumberingItsActions() {
        FindingId id = fixture.majorFindingOf(fixture.assignedInspection());
        fixture.plan(id, "Recharge", fixture.maintenanceTechnician, CertiflowFixture.TODAY.plusDays(5), fixture.planner);
        Finding original = fixture.finding(id);

        Finding restored = new FindingFactory().restore(original.snapshot());
        fixture.findingRepository.save(restored);
        CorrectiveActionId next = fixture.plan(id, "Replace", fixture.maintenanceTechnician, CertiflowFixture.TODAY.plusDays(6),
                fixture.planner);

        assertEquals(new CorrectiveActionId(2), next);
        assertEquals(original.snapshot().actions().getFirst(), fixture.finding(id).snapshot().actions().getFirst());
    }

    @Test
    void aRestoredCertificateKeepsItsLifecycle() {
        CertificateNumber number = fixture.issue(fixture.closedCompliantInspection(), CertiflowFixture.TODAY.plusYears(1),
                fixture.certifier);
        fixture.suspend(number, "Seal tampered", fixture.planner);
        Certificate original = fixture.certificate(number);

        Certificate restored = new CertificateFactory().restore(original.snapshot());

        assertEquals(original.snapshot(), restored.snapshot());
        assertEquals(original.statusOn(CertiflowFixture.TODAY), restored.statusOn(CertiflowFixture.TODAY));
    }
}
```

Leer `CorrectiveActionId` antes: si su campo no es `int`, ajustar `new CorrectiveActionId(2)` al tipo real.

Run: `mvn -q -pl certiflow-application -am test-compile 2>&1 | grep -m1 ERROR`
Expected: `cannot find symbol ... snapshot()`.

- [ ] **Step 2: Implementar `Inspection`**

```java
package ar.edu.itba.certiflow.inspection.model;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.schema.model.SchemaVersion;
import ar.edu.itba.certiflow.shared.model.Person;
import java.util.List;

public record InspectionSnapshot(InspectionId id, AssetCode assetCode, Person assetResponsible, InspectionAssignment assignment,
                                 SchemaVersion schemaVersion, List<CriterionResponse<?>> responses, Revisions revisions) {

    public InspectionSnapshot {
        responses = List.copyOf(responses);
    }
}
```

En `Inspection`, constructor de restauración (de paquete) y `snapshot()`:

```java
    Inspection(InspectionSnapshot snapshot) {
        this.id = snapshot.id();
        this.assetCode = snapshot.assetCode();
        this.assetResponsible = snapshot.assetResponsible();
        this.assignment = snapshot.assignment();
        this.schemaVersion = snapshot.schemaVersion();
        snapshot.responses().forEach(response -> responses.put(response.criterion(), response));
        this.revisions = snapshot.revisions();
    }

    public InspectionSnapshot snapshot() {
        return new InspectionSnapshot(id, assetCode, assetResponsible, assignment, schemaVersion, List.copyOf(responses.values()),
                revisions);
    }
```

`InspectionFactory`:

```java
    public Inspection restore(InspectionSnapshot snapshot) {
        return new Inspection(snapshot);
    }
```

- [ ] **Step 3: Implementar `Finding` y sus acciones**

```java
package ar.edu.itba.certiflow.inspection.model;

import ar.edu.itba.certiflow.shared.model.Person;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public record CorrectiveActionSnapshot(CorrectiveActionId id, String description, Person responsible, LocalDate dueDate,
                                       Person plannedBy, Instant plannedAt, List<Verification> verifications,
                                       Optional<Cancellation> cancellation) {

    public CorrectiveActionSnapshot {
        verifications = List.copyOf(verifications);
    }
}
```

```java
package ar.edu.itba.certiflow.inspection.model;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.schema.model.Criterion;
import ar.edu.itba.certiflow.shared.model.Person;
import java.util.List;

public record FindingSnapshot(FindingId id, InspectionId inspectionId, AssetCode assetCode, Criterion<?> criterion,
                              List<AttachedEvidence> evidence, Person responsible, List<CorrectiveActionSnapshot> actions) {

    public FindingSnapshot {
        evidence = List.copyOf(evidence);
        actions = List.copyOf(actions);
    }
}
```

`CorrectiveAction`, constructor de restauración y `snapshot()`:

```java
    CorrectiveAction(CorrectiveActionSnapshot snapshot) {
        this.id = snapshot.id();
        this.description = snapshot.description();
        this.responsible = snapshot.responsible();
        this.dueDate = snapshot.dueDate();
        this.plannedBy = snapshot.plannedBy();
        this.plannedAt = snapshot.plannedAt();
        this.verifications.addAll(snapshot.verifications());
        this.cancellation = snapshot.cancellation().orElse(null);
    }

    CorrectiveActionSnapshot snapshot() {
        return new CorrectiveActionSnapshot(id, description, responsible, dueDate, plannedBy, plannedAt, verifications(),
                cancellation());
    }
```

`CorrectiveActions`:

```java
    CorrectiveActions() {
    }

    CorrectiveActions(List<CorrectiveActionSnapshot> snapshots) {
        snapshots.forEach(snapshot -> actions.add(new CorrectiveAction(snapshot)));
    }

    List<CorrectiveActionSnapshot> snapshot() {
        return actions.stream().map(CorrectiveAction::snapshot).toList();
    }
```

`Finding`: el campo `actions` deja de inicializarse en la declaración; el constructor de creación hace `this.actions = new CorrectiveActions();`. Constructor de restauración y `snapshot()`:

```java
    Finding(FindingSnapshot snapshot) {
        this.id = snapshot.id();
        this.inspectionId = snapshot.inspectionId();
        this.assetCode = snapshot.assetCode();
        this.criterion = snapshot.criterion();
        this.evidence = snapshot.evidence();
        this.responsible = snapshot.responsible();
        this.actions = new CorrectiveActions(snapshot.actions());
    }

    public FindingSnapshot snapshot() {
        return new FindingSnapshot(id, inspectionId, assetCode, criterion, evidence, responsible, actions.snapshot());
    }

    public AssetCode assetCode() {
        return assetCode;
    }
```

`FindingFactory.restore(FindingSnapshot snapshot) { return new Finding(snapshot); }`

- [ ] **Step 4: Implementar `Certificate`**

```java
package ar.edu.itba.certiflow.certification.model;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.shared.model.Person;
import java.time.Instant;
import java.util.List;

public record CertificateSnapshot(CertificateNumber number, AssetCode assetCode, InspectionReference basedOn,
                                  ValidityPeriod validity, Person issuedBy, Instant issuedAt, List<Suspension> suspensions,
                                  CertificateLifecycle lifecycle) {

    public CertificateSnapshot {
        suspensions = List.copyOf(suspensions);
    }
}
```

`CertificateLifecycle`: `public enum`. En `Certificate`:

```java
    Certificate(CertificateSnapshot snapshot) {
        this.number = snapshot.number();
        this.assetCode = snapshot.assetCode();
        this.basedOn = snapshot.basedOn();
        this.validity = snapshot.validity();
        this.issuedBy = snapshot.issuedBy();
        this.issuedAt = snapshot.issuedAt();
        this.suspensions.addAll(snapshot.suspensions());
        this.lifecycle = snapshot.lifecycle();
    }

    public CertificateSnapshot snapshot() {
        return new CertificateSnapshot(number, assetCode, basedOn, validity, issuedBy, issuedAt, suspensions(), lifecycle);
    }
```

`CertificateFactory.restore(CertificateSnapshot snapshot) { return new Certificate(snapshot); }`

- [ ] **Step 5: Verificar y commitear**

Run: `mvn -q clean test`
Expected: verde.

```bash
git add -A
git commit -m "Let aggregates export a snapshot and be restored from it without replaying creation rules"
```

---

### Task 7: Casos de uso de lectura

**Files:**
- Create en `catalog/usecase/`: `FindAssetUseCase.java`, `ListAssetsUseCase.java`
- Create en `schema/usecase/`: `FindSchemaUseCase.java`
- Create en `inspection/usecase/`: `FindInspectionUseCase.java`, `FindFindingUseCase.java`, `ListFindingsUseCase.java`
- Create en `certification/usecase/`: `ListCertificatesUseCase.java`, `FindCertificateUseCase.java`
- Create en `shared/usecase/`: `AuditHistoryUseCase.java`, `AuditSubject.java`
- Create: los 9 `...UseCaseImpl` en `application.<ctx>` (`AuditHistoryUseCaseImpl` en `application.shared`)
- Modify: `AssetRepository` (+ `findAll`), `FindingRepository` (+ `findAll`), `CertificateRepository` (+ `findAll`, `findByAsset`), y los repositorios en memoria
- Test: `certiflow-application/src/test/java/ar/edu/itba/certiflow/application/QueriesTest.java`

**Interfaces:**
- Produces (las consultas devuelven el agregado: sus comandos son de paquete (D14), así que desde afuera solo se puede leer; `Inspection` se devuelve como `InspectionView` (D13)):

| Interfaz | Método |
|---|---|
| `FindAssetUseCase` | `Asset execute(AssetCode code)` (lanza `AssetNotFoundException`) |
| `ListAssetsUseCase` | `List<Asset> execute(Optional<AssetType> type)` |
| `FindSchemaUseCase` | `InspectionSchema execute(AssetType type)` (lanza `NoInspectionSchemaForAssetTypeException`) |
| `FindInspectionUseCase` | `InspectionView execute(InspectionId id)` (lanza `InspectionNotFoundException`) |
| `FindFindingUseCase` | `Finding execute(FindingId id)` (lanza `FindingNotFoundException`) |
| `ListFindingsUseCase` | `List<Finding> execute(Optional<AssetCode> asset)` |
| `ListCertificatesUseCase` | `List<Certificate> execute(Optional<AssetCode> asset)` |
| `FindCertificateUseCase` | `Certificate execute(CertificateNumber number)` (lanza `CertificateNotFoundException`) |
| `AuditHistoryUseCase` | `List<AuditEntry> execute(AuditSubject subject)` |

- `sealed interface AuditSubject` con `record OfAsset(AssetCode code)`, `OfSchema(AssetType type)`, `OfInspection(InspectionId id)`, `OfFinding(FindingId id)`, `OfCertificate(CertificateNumber number)`.
- `AuditHistoryUseCaseImpl(AuditLog<AssetCode>, AuditLog<AssetType>, AuditLog<InspectionId>, AuditLog<FindingId>, AuditLog<CertificateNumber>)`.
- Repositorios: `AssetRepository.findAll(): List<Asset>`; `FindingRepository.findAll(): List<Finding>`; `CertificateRepository.findAll(): List<Certificate>`, `findByAsset(AssetCode): List<Certificate>`.
- Spec §4.5 se ajusta en la tarea 17: las consultas devuelven el agregado de solo lectura en lugar de un `XxxView` nuevo.

- [ ] **Step 1: Test que falla**

```java
package ar.edu.itba.certiflow.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ar.edu.itba.certiflow.application.support.CertiflowFixture;
import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.catalog.model.AssetType;
import ar.edu.itba.certiflow.catalog.model.exceptions.AssetNotFoundException;
import ar.edu.itba.certiflow.inspection.model.FindingId;
import ar.edu.itba.certiflow.inspection.model.InspectionAudit;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.shared.model.AuditEntry;
import ar.edu.itba.certiflow.shared.usecase.AuditSubject;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class QueriesTest {

    private final CertiflowFixture fixture = new CertiflowFixture();

    @Test
    void assetsAreFoundByCodeAndListedByType() {
        assertEquals(fixture.extinguisher, fixture.findAsset.execute(fixture.extinguisher.code()));
        assertEquals(List.of(fixture.extinguisher), fixture.listAssets.execute(Optional.of(CertiflowFixture.EXTINGUISHER)));
        assertEquals(List.of(), fixture.listAssets.execute(Optional.of(new AssetType("Boiler"))));
        assertThrows(AssetNotFoundException.class, () -> fixture.findAsset.execute(new AssetCode("NOPE")));
    }

    @Test
    void findingsAreListedByAssetAndHistoryIsReadBySubject() {
        InspectionId inspection = fixture.assignedInspection();
        FindingId finding = fixture.majorFindingOf(inspection);

        assertEquals(List.of(finding), fixture.listFindings.execute(Optional.of(fixture.extinguisher.code())).stream()
                .map(f -> f.id()).toList());
        assertEquals(finding, fixture.findFinding.execute(finding).id());
        assertEquals(List.of(InspectionAudit.ASSIGNED, InspectionAudit.ANSWER_RECORDED),
                fixture.auditHistory.execute(new AuditSubject.OfInspection(inspection)).stream()
                        .map(AuditEntry::action).distinct().limit(2).toList());
    }

    @Test
    void certificatesAreListedByAsset() {
        var number = fixture.issue(fixture.closedCompliantInspection(), CertiflowFixture.TODAY.plusYears(1), fixture.certifier);

        assertEquals(List.of(number), fixture.listCertificates.execute(Optional.of(fixture.extinguisher.code())).stream()
                .map(c -> c.number()).toList());
        assertEquals(number, fixture.findCertificate.execute(number).number());
    }
}
```

Run: `mvn -q -pl certiflow-application -am test-compile 2>&1 | grep -m1 ERROR`
Expected: `cannot find symbol ... findAsset`.

- [ ] **Step 2: Implementar interfaces, implementaciones y repositorios**

`AuditSubject`:

```java
package ar.edu.itba.certiflow.shared.usecase;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.catalog.model.AssetType;
import ar.edu.itba.certiflow.certification.model.CertificateNumber;
import ar.edu.itba.certiflow.inspection.model.FindingId;
import ar.edu.itba.certiflow.inspection.model.InspectionId;

public sealed interface AuditSubject {

    record OfAsset(AssetCode code) implements AuditSubject {
    }

    record OfSchema(AssetType type) implements AuditSubject {
    }

    record OfInspection(InspectionId id) implements AuditSubject {
    }

    record OfFinding(FindingId id) implements AuditSubject {
    }

    record OfCertificate(CertificateNumber number) implements AuditSubject {
    }
}
```

`AuditHistoryUseCaseImpl.execute`:

```java
    @Override
    public List<AuditEntry> execute(AuditSubject subject) {
        return switch (subject) {
            case AuditSubject.OfAsset(AssetCode code) -> assetLog.historyOf(code);
            case AuditSubject.OfSchema(AssetType type) -> schemaLog.historyOf(type);
            case AuditSubject.OfInspection(InspectionId id) -> inspectionLog.historyOf(id);
            case AuditSubject.OfFinding(FindingId id) -> findingLog.historyOf(id);
            case AuditSubject.OfCertificate(CertificateNumber number) -> certificateLog.historyOf(number);
        };
    }
```

`ListAssetsUseCaseImpl.execute(Optional<AssetType> type)`:

```java
        return assetRepository.findAll().stream()
                .filter(asset -> type.map(asset.type()::equals).orElse(true))
                .toList();
```

`ListFindingsUseCaseImpl` y `ListCertificatesUseCaseImpl`: `asset.map(repository::findByAsset).orElseGet(repository::findAll)`.

Los `Find...UseCaseImpl` hacen `repository.findBy...(id).orElseThrow(() -> new XxxNotFoundException(id))`.

En memoria: `findAll()` devuelve `List.copyOf(map.values())`; `InMemoryCertificateRepository.findByAsset` filtra con `certificate.certifies(assetCode)`.

En el fixture, instanciar las nueve consultas como campos públicos con esos nombres (`findAsset`, `listAssets`, `findSchema`, `findInspection`, `findFinding`, `listFindings`, `listCertificates`, `findCertificate`, `auditHistory`).

- [ ] **Step 3: Verificar y commitear**

Run: `mvn -q clean test`
Expected: verde.

```bash
git add -A
git commit -m "Add read use cases for assets, schemas, inspections, findings, certificates and audit history"
```

---

### Task 8: Persistencia JPA de activos y esquemas

**Files:**
- Modify: `pom.xml` (BOM de Spring Boot), `certiflow-infrastructure/pom.xml`
- Create: `certiflow-infrastructure/src/main/resources/db/migration/V1__init.sql`
- Create en `certiflow-infrastructure/src/main/java/ar/edu/itba/certiflow/infrastructure/`:
  - `shared/persistence/PersonColumns.java`
  - `catalog/persistence/{AssetEntity, AssetCharacteristicEmbeddable, SpringDataAssetRepository, JpaAssetRepository}.java`
  - `schema/persistence/{SectionsJson, InspectionSchemaEntity, SchemaVersionEntity, SpringDataInspectionSchemaRepository, JpaInspectionSchemaRepository}.java`
- Create en `certiflow-infrastructure/src/test/java/ar/edu/itba/certiflow/infrastructure/`: `PersistenceTestApplication.java`, `catalog/persistence/JpaAssetRepositoryTest.java`, `schema/persistence/JpaInspectionSchemaRepositoryTest.java`, `schema/persistence/SectionsJsonTest.java`
- Create: `certiflow-infrastructure/src/test/resources/application.properties`

**Interfaces:**
- Consumes: `AssetRepository`, `InspectionSchemaRepository` (tareas 1 y 7).
- Produces:
  - `@Repository JpaAssetRepository implements AssetRepository`, `@Repository JpaInspectionSchemaRepository implements InspectionSchemaRepository`.
  - `final class SectionsJson { static String write(List<Section>); static List<Section> read(String); static String writeCriterion(Criterion<?>); static Criterion<?> readCriterion(String); }` (lo usa la tarea 9 para el criterio de un hallazgo).
  - `SchemaVersionEntity` con `Long id`, `int number`, `Instant publishedAt`, `String sections`, `toDomain(): SchemaVersion`, `static from(SchemaVersion)`; la tarea 9 la referencia desde `InspectionEntity`.
  - `@Embeddable PersonColumns(String id, String name)` con `toDomain(): Person`, `static of(Person)`.

- [ ] **Step 1: Dependencias**

En `pom.xml` raíz, dentro de `<dependencyManagement><dependencies>`, al principio:

```xml
      <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-dependencies</artifactId>
        <version>4.1.1</version>
        <type>pom</type>
        <scope>import</scope>
      </dependency>
```

y en `<properties>`: `<spring-boot.version>4.1.1</spring-boot.version>`. Quitar `<version>${junit.version}</version>` de la dependencia de JUnit (lo gestiona el BOM) y borrar la propiedad `junit.version`.

`certiflow-infrastructure/pom.xml`, `<dependencies>`:

```xml
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-flyway</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-jackson</artifactId>
    </dependency>
    <dependency>
      <groupId>org.flywaydb</groupId>
      <artifactId>flyway-database-postgresql</artifactId>
    </dependency>
    <dependency>
      <groupId>org.postgresql</groupId>
      <artifactId>postgresql</artifactId>
      <scope>runtime</scope>
    </dependency>
    <dependency>
      <groupId>com.h2database</groupId>
      <artifactId>h2</artifactId>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-data-jpa-test</artifactId>
      <scope>test</scope>
    </dependency>
```

Run: `mvn -q -pl certiflow-infrastructure -am dependency:resolve 2>&1 | grep -i "could not\|missing" | head`
Expected: sin salida. Si un artefacto no existe en 4.1.1, buscar el nombre correcto con `curl -s https://repo.maven.apache.org/maven2/org/springframework/boot/ | grep -o 'spring-boot-starter-[a-z-]*' | sort -u` y corregirlo.

Run: `mvn -q clean test`
Expected: verde (el BOM no cambia la versión efectiva de JUnit de forma incompatible; si cambia y algo falla, fijar `junit.version` de nuevo).

- [ ] **Step 2: Migración Flyway**

`V1__init.sql`, válido en H2 (modo PostgreSQL) y en PostgreSQL:

```sql
create table asset (
    code varchar(64) primary key,
    name varchar(255) not null,
    type varchar(128) not null,
    location varchar(255) not null,
    responsible_id varchar(64) not null,
    responsible_name varchar(255) not null
);

create table asset_characteristic (
    asset_code varchar(64) not null references asset (code),
    position integer not null,
    name varchar(128) not null,
    value varchar(255) not null,
    primary key (asset_code, position)
);

create table inspection_schema (
    asset_type varchar(128) primary key,
    name varchar(255) not null
);

create table schema_version (
    id bigint generated by default as identity primary key,
    asset_type varchar(128) not null references inspection_schema (asset_type),
    number integer not null,
    published_at timestamp with time zone not null,
    sections text not null,
    unique (asset_type, number)
);

create table inspection (
    id varchar(64) primary key,
    asset_code varchar(64) not null,
    asset_responsible_id varchar(64) not null,
    asset_responsible_name varchar(255) not null,
    inspector_id varchar(64) not null,
    inspector_name varchar(255) not null,
    planned_date date not null,
    scope_sections text,
    schema_version_id bigint not null references schema_version (id)
);

create table criterion_response (
    id bigint generated by default as identity primary key,
    inspection_id varchar(64) not null references inspection (id),
    position integer not null,
    criterion_code varchar(64) not null,
    answer_type varchar(16),
    answer_value varchar(255),
    answered_by_id varchar(64),
    answered_by_name varchar(255),
    answered_at timestamp with time zone
);

create table attached_evidence (
    response_id bigint not null references criterion_response (id),
    position integer not null,
    kind varchar(64) not null,
    reference varchar(512) not null,
    description varchar(512) not null,
    attached_by_id varchar(64) not null,
    attached_by_name varchar(255) not null,
    attached_at timestamp with time zone not null,
    primary key (response_id, position)
);

create table observation (
    response_id bigint not null references criterion_response (id),
    position integer not null,
    text varchar(2000) not null,
    observed_by_id varchar(64) not null,
    observed_by_name varchar(255) not null,
    observed_at timestamp with time zone not null,
    primary key (response_id, position)
);

create table revision (
    inspection_id varchar(64) not null references inspection (id),
    number integer not null,
    reason varchar(2000) not null,
    sealed_by_id varchar(64) not null,
    sealed_by_name varchar(255) not null,
    sealed_at timestamp with time zone not null,
    evaluation text not null,
    primary key (inspection_id, number)
);

create table finding (
    id varchar(64) primary key,
    inspection_id varchar(64) not null references inspection (id),
    asset_code varchar(64) not null,
    criterion text not null,
    responsible_id varchar(64) not null,
    responsible_name varchar(255) not null
);
create index finding_asset_code on finding (asset_code);

create table finding_evidence (
    finding_id varchar(64) not null references finding (id),
    position integer not null,
    kind varchar(64) not null,
    reference varchar(512) not null,
    description varchar(512) not null,
    attached_by_id varchar(64) not null,
    attached_by_name varchar(255) not null,
    attached_at timestamp with time zone not null,
    primary key (finding_id, position)
);

create table corrective_action (
    finding_id varchar(64) not null references finding (id),
    number integer not null,
    description varchar(2000) not null,
    responsible_id varchar(64) not null,
    responsible_name varchar(255) not null,
    due_date date not null,
    planned_by_id varchar(64) not null,
    planned_by_name varchar(255) not null,
    planned_at timestamp with time zone not null,
    cancelled_by_id varchar(64),
    cancelled_by_name varchar(255),
    cancelled_at timestamp with time zone,
    primary key (finding_id, number)
);

create table verification (
    finding_id varchar(64) not null,
    action_number integer not null,
    position integer not null,
    verifier_id varchar(64) not null,
    verifier_name varchar(255) not null,
    result varchar(32) not null,
    notes varchar(2000) not null,
    verified_at timestamp with time zone not null,
    primary key (finding_id, action_number, position),
    foreign key (finding_id, action_number) references corrective_action (finding_id, number)
);

create table certificate (
    number varchar(64) primary key,
    asset_code varchar(64) not null,
    inspection_id varchar(64) not null references inspection (id),
    inspection_closed_at timestamp with time zone not null,
    valid_from date not null,
    valid_until date not null,
    issued_by_id varchar(64) not null,
    issued_by_name varchar(255) not null,
    issued_at timestamp with time zone not null,
    lifecycle varchar(16) not null
);
create index certificate_asset_code on certificate (asset_code);

create table suspension (
    certificate_number varchar(64) not null references certificate (number),
    position integer not null,
    reason varchar(2000) not null,
    suspended_by_id varchar(64) not null,
    suspended_by_name varchar(255) not null,
    suspended_at timestamp with time zone not null,
    primary key (certificate_number, position)
);

create sequence certificate_number_seq start with 1 increment by 1;

create table audit_entry (
    id bigint generated by default as identity primary key,
    subject_type varchar(32) not null,
    subject_id varchar(255) not null,
    action varchar(128) not null,
    performed_by_id varchar(64) not null,
    performed_by_name varchar(255) not null,
    occurred_at timestamp with time zone not null,
    detail varchar(2000) not null
);
create index audit_entry_subject on audit_entry (subject_type, subject_id);
```

- [ ] **Step 3: Configuración de test**

`certiflow-infrastructure/src/test/resources/application.properties`:

```properties
spring.datasource.url=jdbc:h2:mem:certiflow;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH
spring.datasource.username=sa
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.open-in-view=false
```

`PersistenceTestApplication.java`:

```java
package ar.edu.itba.certiflow.infrastructure;

import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PersistenceTestApplication {
}
```

- [ ] **Step 4: Tests de repositorio que fallan**

```java
package ar.edu.itba.certiflow.infrastructure.catalog.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.itba.certiflow.catalog.model.Asset;
import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.catalog.model.AssetType;
import ar.edu.itba.certiflow.catalog.model.Characteristic;
import ar.edu.itba.certiflow.catalog.model.Characteristics;
import ar.edu.itba.certiflow.catalog.model.Location;
import ar.edu.itba.certiflow.shared.model.Person;
import ar.edu.itba.certiflow.shared.model.PersonId;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(JpaAssetRepository.class)
class JpaAssetRepositoryTest {

    @Autowired
    private JpaAssetRepository repository;
    @Autowired
    private EntityManager entityManager;

    private final Asset extinguisher = new Asset(new AssetCode("EXT-0042"), "Lobby extinguisher", new AssetType("Fire extinguisher"),
            new Location("Lobby"), new Person(new PersonId("EMP-003"), "Raul"),
            new Characteristics(List.of(new Characteristic("capacity", "10 kg"), new Characteristic("agent", "ABC"))));

    @Test
    void anAssetIsReadBackEqualToWhatWasSaved() {
        repository.save(extinguisher);
        entityManager.flush();
        entityManager.clear();

        assertEquals(extinguisher, repository.findByCode(extinguisher.code()).orElseThrow());
        assertEquals(extinguisher.responsible().name(),
                repository.findByCode(extinguisher.code()).orElseThrow().responsible().name());
    }

    @Test
    void anUnknownCodeFindsNothingAndAllListsWhatWasSaved() {
        repository.save(extinguisher);

        assertTrue(repository.findByCode(new AssetCode("NOPE")).isEmpty());
        assertEquals(List.of(extinguisher), repository.findAll());
    }
}
```

Si `org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest` no resuelve, buscar el paquete real con `unzip -l ~/.m2/repository/org/springframework/boot/spring-boot-data-jpa-test/4.1.1/*.jar | grep DataJpaTest`.

```java
package ar.edu.itba.certiflow.infrastructure.schema.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ar.edu.itba.certiflow.catalog.model.AssetType;
import ar.edu.itba.certiflow.schema.model.Criterion;
import ar.edu.itba.certiflow.schema.model.EvidenceRequirement;
import ar.edu.itba.certiflow.schema.model.InspectionSchema;
import ar.edu.itba.certiflow.schema.model.NumericRangeRule;
import ar.edu.itba.certiflow.schema.model.OptionInListRule;
import ar.edu.itba.certiflow.schema.model.Range;
import ar.edu.itba.certiflow.schema.model.Section;
import ar.edu.itba.certiflow.schema.model.StandardEvidenceKind;
import ar.edu.itba.certiflow.schema.model.StandardSeverity;
import ar.edu.itba.certiflow.schema.model.Unit;
import ar.edu.itba.certiflow.schema.model.YesNoAnswer;
import ar.edu.itba.certiflow.schema.model.YesNoRule;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(JpaInspectionSchemaRepository.class)
class JpaInspectionSchemaRepositoryTest {

    static List<Section> sections() {
        return List.of(
                new Section("Pressure and seal", List.of(
                        new Criterion<>("EXT-01", "Gauge pressure within range",
                                new NumericRangeRule(new Unit("bar"), new Range(new BigDecimal("6.00"), new BigDecimal("8.00"))),
                                StandardSeverity.MAJOR, List.of(new EvidenceRequirement(StandardEvidenceKind.PHOTO, 1))),
                        new Criterion<>("EXT-02", "Seal intact", new YesNoRule(YesNoAnswer.YES), StandardSeverity.CRITICAL,
                                List.of()))),
                new Section("Signage", List.of(new Criterion<>("EXT-03", "Signage condition",
                        new OptionInListRule(Set.of("VISIBLE", "REFLECTIVE")), StandardSeverity.MINOR, List.of()))));
    }

    @Autowired
    private JpaInspectionSchemaRepository repository;
    @Autowired
    private EntityManager entityManager;

    @Test
    void everyVersionIsReadBackEqualToWhatWasPublished() {
        AssetType extinguisher = new AssetType("Fire extinguisher");
        InspectionSchema schema = new InspectionSchema("Extinguisher", extinguisher, sections(), Instant.parse("2026-03-01T00:00:00Z"))
                .withNewVersion(sections().subList(0, 1), Instant.parse("2026-03-05T00:00:00Z"));

        repository.save(schema);
        entityManager.flush();
        entityManager.clear();

        assertEquals(schema, repository.findByAssetType(extinguisher).orElseThrow());
    }

    @Test
    void savingAgainAppendsTheNewVersionWithoutTouchingThePreviousOnes() {
        AssetType extinguisher = new AssetType("Fire extinguisher");
        InspectionSchema first = new InspectionSchema("Extinguisher", extinguisher, sections(), Instant.parse("2026-03-01T00:00:00Z"));
        repository.save(first);
        entityManager.flush();
        Long firstVersionId = repository.versionEntity(extinguisher, 1).getId();

        repository.save(first.withNewVersion(sections(), Instant.parse("2026-03-05T00:00:00Z")));
        entityManager.flush();
        entityManager.clear();

        assertEquals(firstVersionId, repository.versionEntity(extinguisher, 1).getId());
        assertEquals(2, repository.findByAssetType(extinguisher).orElseThrow().versions().size());
    }
}
```

```java
package ar.edu.itba.certiflow.infrastructure.schema.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SectionsJsonTest {

    @Test
    void sectionsSurviveTheRoundTripIncludingDecimalScale() {
        var sections = JpaInspectionSchemaRepositoryTest.sections();

        var read = SectionsJson.read(SectionsJson.write(sections));

        assertEquals(sections, read);
        assertEquals("6.00", ((ar.edu.itba.certiflow.schema.model.NumericRangeRule) read.getFirst().criteria().getFirst().rule())
                .range().minimum().toPlainString());
    }

    @Test
    void aSingleCriterionSurvivesTheRoundTrip() {
        var criterion = JpaInspectionSchemaRepositoryTest.sections().getFirst().criteria().getFirst();

        assertEquals(criterion, SectionsJson.readCriterion(SectionsJson.writeCriterion(criterion)));
    }
}
```

`Criterion` es un record cuya `equals` compara la regla; `NumericRangeRule` compara `Range` con `BigDecimal.equals`, que es sensible a la escala: por eso el test cubre el punto 5 de **Review Focus**.

Run: `mvn -q -pl certiflow-infrastructure -am test-compile 2>&1 | grep -m1 ERROR`
Expected: `cannot find symbol ... JpaAssetRepository`.

- [ ] **Step 5: Implementar `PersonColumns`**

```java
package ar.edu.itba.certiflow.infrastructure.shared.persistence;

import ar.edu.itba.certiflow.shared.model.Person;
import ar.edu.itba.certiflow.shared.model.PersonId;

public record PersonColumns(String id, String name) {

    public static PersonColumns of(Person person) {
        return new PersonColumns(person.id().value(), person.name());
    }

    public Person toDomain() {
        return new Person(new PersonId(id), name);
    }
}
```

Las entidades guardan cada persona en dos columnas `xxx_id` y `xxx_name`, y usan `PersonColumns` para convertir. Leer `PersonId` antes: si su campo no se llama `value`, ajustar.

- [ ] **Step 6: Implementar activos**

```java
package ar.edu.itba.certiflow.infrastructure.catalog.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class AssetCharacteristicEmbeddable {

    @Column(name = "name")
    private String name;
    @Column(name = "value")
    private String value;

    protected AssetCharacteristicEmbeddable() {
    }

    AssetCharacteristicEmbeddable(String name, String value) {
        this.name = name;
        this.value = value;
    }

    String name() {
        return name;
    }

    String value() {
        return value;
    }
}
```

```java
package ar.edu.itba.certiflow.infrastructure.catalog.persistence;

import ar.edu.itba.certiflow.catalog.model.Asset;
import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.catalog.model.AssetType;
import ar.edu.itba.certiflow.catalog.model.Characteristic;
import ar.edu.itba.certiflow.catalog.model.Characteristics;
import ar.edu.itba.certiflow.catalog.model.Location;
import ar.edu.itba.certiflow.infrastructure.shared.persistence.PersonColumns;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "asset")
public class AssetEntity {

    @Id
    private String code;
    private String name;
    private String type;
    private String location;
    @Column(name = "responsible_id")
    private String responsibleId;
    @Column(name = "responsible_name")
    private String responsibleName;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "asset_characteristic", joinColumns = @JoinColumn(name = "asset_code"))
    @OrderColumn(name = "position")
    private List<AssetCharacteristicEmbeddable> characteristics = new ArrayList<>();

    protected AssetEntity() {
    }

    static AssetEntity from(Asset asset) {
        AssetEntity entity = new AssetEntity();
        entity.code = asset.code().value();
        entity.name = asset.name();
        entity.type = asset.type().name();
        entity.location = asset.location().description();
        PersonColumns responsible = PersonColumns.of(asset.responsible());
        entity.responsibleId = responsible.id();
        entity.responsibleName = responsible.name();
        asset.characteristics().entries().forEach(characteristic -> entity.characteristics.add(
                new AssetCharacteristicEmbeddable(characteristic.name(), characteristic.value())));
        return entity;
    }

    Asset toDomain() {
        return new Asset(new AssetCode(code), name, new AssetType(type), new Location(location),
                new PersonColumns(responsibleId, responsibleName).toDomain(),
                new Characteristics(characteristics.stream()
                        .map(characteristic -> new Characteristic(characteristic.name(), characteristic.value()))
                        .toList()));
    }
}
```

Leer `AssetType` y `Location` antes: si sus campos no se llaman `name` y `description`, usar los nombres reales.

```java
package ar.edu.itba.certiflow.infrastructure.catalog.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataAssetRepository extends JpaRepository<AssetEntity, String> {
}
```

```java
package ar.edu.itba.certiflow.infrastructure.catalog.persistence;

import ar.edu.itba.certiflow.catalog.model.Asset;
import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.catalog.repository.AssetRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaAssetRepository implements AssetRepository {

    private final SpringDataAssetRepository assets;

    public JpaAssetRepository(SpringDataAssetRepository assets) {
        this.assets = assets;
    }

    @Override
    public void save(Asset asset) {
        assets.save(AssetEntity.from(asset));
    }

    @Override
    public Optional<Asset> findByCode(AssetCode code) {
        return assets.findById(code.value()).map(AssetEntity::toDomain);
    }

    @Override
    public List<Asset> findAll() {
        return assets.findAll().stream().map(AssetEntity::toDomain).toList();
    }
}
```

- [ ] **Step 7: Implementar `SectionsJson`**

Documentos JSON propios de infraestructura (records), traducidos a mano para no anotar el dominio:

```java
package ar.edu.itba.certiflow.infrastructure.schema.persistence;

import ar.edu.itba.certiflow.schema.model.ApprovalRule;
import ar.edu.itba.certiflow.schema.model.Criterion;
import ar.edu.itba.certiflow.schema.model.EvidenceKind;
import ar.edu.itba.certiflow.schema.model.EvidenceRequirement;
import ar.edu.itba.certiflow.schema.model.NumericRangeRule;
import ar.edu.itba.certiflow.schema.model.OptionInListRule;
import ar.edu.itba.certiflow.schema.model.Range;
import ar.edu.itba.certiflow.schema.model.Section;
import ar.edu.itba.certiflow.schema.model.Severity;
import ar.edu.itba.certiflow.schema.model.StandardEvidenceKind;
import ar.edu.itba.certiflow.schema.model.StandardSeverity;
import ar.edu.itba.certiflow.schema.model.Unit;
import ar.edu.itba.certiflow.schema.model.YesNoAnswer;
import ar.edu.itba.certiflow.schema.model.YesNoRule;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

public final class SectionsJson {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    record SectionDocument(String title, List<CriterionDocument> criteria) {
    }

    record CriterionDocument(String code, String text, RuleDocument rule, String severity, List<EvidenceDocument> requiredEvidence) {
    }

    record RuleDocument(String type, String unit, BigDecimal minimum, BigDecimal maximum, String expected, Set<String> options) {
    }

    record EvidenceDocument(String kind, int minimum) {
    }

    private SectionsJson() {
    }

    public static String write(List<Section> sections) {
        return MAPPER.writeValueAsString(sections.stream()
                .map(section -> new SectionDocument(section.title(), section.criteria().stream().map(SectionsJson::document).toList()))
                .toList());
    }

    public static List<Section> read(String json) {
        List<SectionDocument> documents = MAPPER.readValue(json, new TypeReference<List<SectionDocument>>() {
        });
        return documents.stream()
                .map(document -> new Section(document.title(), document.criteria().stream().<Criterion<?>>map(SectionsJson::criterion).toList()))
                .toList();
    }

    public static String writeCriterion(Criterion<?> criterion) {
        return MAPPER.writeValueAsString(document(criterion));
    }

    public static Criterion<?> readCriterion(String json) {
        return criterion(MAPPER.readValue(json, CriterionDocument.class));
    }

    private static CriterionDocument document(Criterion<?> criterion) {
        return new CriterionDocument(criterion.code(), criterion.text(), document(criterion.rule()), severityName(criterion.severity()),
                criterion.requiredEvidence().stream()
                        .map(requirement -> new EvidenceDocument(evidenceKindName(requirement.kind()), requirement.minimum()))
                        .toList());
    }

    private static RuleDocument document(ApprovalRule<?> rule) {
        return switch (rule) {
            case NumericRangeRule numeric -> new RuleDocument("numeric-range", numeric.unit().symbol(),
                    numeric.range().minimum(), numeric.range().maximum(), null, null);
            case YesNoRule yesNo -> new RuleDocument("yes-no", null, null, null, yesNo.expected().name(), null);
            case OptionInListRule option -> new RuleDocument("option-in-list", null, null, null, null, option.acceptedOptions());
            default -> throw new IllegalArgumentException("No JSON mapping for rule " + rule.getClass().getName());
        };
    }

    private static Criterion<?> criterion(CriterionDocument document) {
        List<EvidenceRequirement> evidence = document.requiredEvidence().stream()
                .map(requirement -> new EvidenceRequirement(evidenceKind(requirement.kind()), requirement.minimum()))
                .toList();
        Severity severity = StandardSeverity.valueOf(document.severity());
        RuleDocument rule = document.rule();
        return switch (rule.type()) {
            case "numeric-range" -> new Criterion<>(document.code(), document.text(),
                    new NumericRangeRule(new Unit(rule.unit()), new Range(rule.minimum(), rule.maximum())), severity, evidence);
            case "yes-no" -> new Criterion<>(document.code(), document.text(),
                    new YesNoRule(YesNoAnswer.valueOf(rule.expected())), severity, evidence);
            case "option-in-list" -> new Criterion<>(document.code(), document.text(),
                    new OptionInListRule(rule.options()), severity, evidence);
            default -> throw new IllegalArgumentException("Unknown rule type " + rule.type());
        };
    }

    private static String severityName(Severity severity) {
        return ((StandardSeverity) severity).name();
    }

    private static String evidenceKindName(EvidenceKind kind) {
        return ((StandardEvidenceKind) kind).name();
    }

    private static EvidenceKind evidenceKind(String name) {
        return StandardEvidenceKind.valueOf(name);
    }
}
```

Si `tools.jackson.databind.json.JsonMapper` no resuelve (Boot trae Jackson 2), usar `com.fasterxml.jackson.databind.json.JsonMapper` y `com.fasterxml.jackson.core.type.TypeReference`; en Jackson 2 `writeValueAsString`/`readValue` lanzan `JsonProcessingException` (checked): envolver en un método privado que la relance como `IllegalStateException`. `BigDecimal` mantiene su escala en Jackson al leer como `BigDecimal` cuando el número se escribe como `6.00`; si el test de escala falla, agregar `.enable(JsonWriteFeature.WRITE_NUMBERS_AS_STRINGS)`.

- [ ] **Step 8: Implementar esquemas**

```java
package ar.edu.itba.certiflow.infrastructure.schema.persistence;

import ar.edu.itba.certiflow.schema.model.SchemaVersion;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "schema_version")
public class SchemaVersionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "asset_type")
    private String assetType;
    private int number;
    @Column(name = "published_at")
    private Instant publishedAt;
    private String sections;

    protected SchemaVersionEntity() {
    }

    static SchemaVersionEntity from(String assetType, SchemaVersion version) {
        SchemaVersionEntity entity = new SchemaVersionEntity();
        entity.assetType = assetType;
        entity.number = version.number();
        entity.publishedAt = version.publishedAt();
        entity.sections = SectionsJson.write(version.sections());
        return entity;
    }

    public SchemaVersion toDomain() {
        return new SchemaVersion(number, SectionsJson.read(sections), publishedAt);
    }

    public Long getId() {
        return id;
    }

    int number() {
        return number;
    }
}
```

```java
package ar.edu.itba.certiflow.infrastructure.schema.persistence;

import ar.edu.itba.certiflow.catalog.model.AssetType;
import ar.edu.itba.certiflow.schema.model.InspectionSchema;
import ar.edu.itba.certiflow.schema.model.SchemaVersionLog;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "inspection_schema")
public class InspectionSchemaEntity {

    @Id
    @Column(name = "asset_type")
    private String assetType;
    private String name;
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "asset_type", referencedColumnName = "asset_type", insertable = false, updatable = false)
    @OrderBy("number")
    private List<SchemaVersionEntity> versions = new ArrayList<>();

    protected InspectionSchemaEntity() {
    }

    InspectionSchemaEntity(InspectionSchema schema) {
        this.assetType = schema.assetType().name();
        this.name = schema.name();
        appendVersionsOf(schema);
    }

    void appendVersionsOf(InspectionSchema schema) {
        schema.versions().stream()
                .filter(version -> version.number() > versions.size())
                .forEach(version -> versions.add(SchemaVersionEntity.from(assetType, version)));
    }

    List<SchemaVersionEntity> versions() {
        return versions;
    }

    InspectionSchema toDomain() {
        return new InspectionSchema(name, new AssetType(assetType),
                new SchemaVersionLog(versions.stream().map(SchemaVersionEntity::toDomain).toList()));
    }
}
```

```java
package ar.edu.itba.certiflow.infrastructure.schema.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataInspectionSchemaRepository extends JpaRepository<InspectionSchemaEntity, String> {
}
```

```java
package ar.edu.itba.certiflow.infrastructure.schema.persistence;

import ar.edu.itba.certiflow.catalog.model.AssetType;
import ar.edu.itba.certiflow.schema.model.InspectionSchema;
import ar.edu.itba.certiflow.schema.repository.InspectionSchemaRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaInspectionSchemaRepository implements InspectionSchemaRepository {

    private final SpringDataInspectionSchemaRepository schemas;

    public JpaInspectionSchemaRepository(SpringDataInspectionSchemaRepository schemas) {
        this.schemas = schemas;
    }

    @Override
    public void save(InspectionSchema schema) {
        schemas.findById(schema.assetType().name()).ifPresentOrElse(
                existing -> existing.appendVersionsOf(schema),
                () -> schemas.save(new InspectionSchemaEntity(schema)));
    }

    @Override
    public Optional<InspectionSchema> findByAssetType(AssetType assetType) {
        return schemas.findById(assetType.name()).map(InspectionSchemaEntity::toDomain);
    }

    public SchemaVersionEntity versionEntity(AssetType assetType, int number) {
        return schemas.findById(assetType.name()).orElseThrow().versions().stream()
                .filter(version -> version.number() == number)
                .findFirst()
                .orElseThrow();
    }
}
```

`versionEntity` lo usa también la tarea 9 para enlazar la inspección con la fila de su versión.

- [ ] **Step 9: Ver que pasa y commitear**

Run: `mvn -q clean verify`
Expected: verde; los 4 tests nuevos de infraestructura pasan.

```bash
git add -A
git commit -m "Persist assets and versioned schemas with JPA and Flyway"
```

---

### Task 9: Persistencia JPA de inspecciones y hallazgos

**Files:**
- Create en `certiflow-infrastructure/src/main/java/ar/edu/itba/certiflow/infrastructure/inspection/persistence/`:
  `AnswerColumns.java`, `EvidenceEmbeddable.java`, `ObservationEmbeddable.java`, `CriterionResponseEntity.java`, `RevisionEntity.java`, `EvaluationJson.java`, `InspectionEntity.java`, `SpringDataInspectionRepository.java`, `JpaInspectionRepository.java`, `VerificationEmbeddable.java`, `CorrectiveActionEntity.java`, `FindingEntity.java`, `SpringDataFindingRepository.java`, `JpaFindingRepository.java`
- Test: `certiflow-infrastructure/src/test/java/ar/edu/itba/certiflow/infrastructure/inspection/persistence/JpaInspectionRepositoryTest.java`, `JpaFindingRepositoryTest.java`
- Test support: `certiflow-infrastructure/src/test/java/ar/edu/itba/certiflow/infrastructure/support/DomainSamples.java`

**Interfaces:**
- Consumes: `InspectionSnapshot`, `FindingSnapshot`, `CorrectiveActionSnapshot`, `InspectionFactory.restore`, `FindingFactory.restore` (T6); `SectionsJson.writeCriterion/readCriterion`, `SchemaVersionEntity`, `JpaInspectionSchemaRepository.versionEntity` (T8); `RawAnswer`, `Answer.raw()`, `Criterion.answerFrom` (T2); `PersonColumns` (T8).
- Produces: `@Repository JpaInspectionRepository implements InspectionRepository` (constructor `(SpringDataInspectionRepository, JpaInspectionSchemaRepository)`), `@Repository JpaFindingRepository implements FindingRepository`.
- Produces: `DomainSamples` (test) con `static Inspection closedInspection()`, `static Finding findingWithTwoActions()`; los usan T9 y T10.

Reglas de mapeo:
- Una `CriterionResponse<A>` se reconstruye con el `Criterion` de la versión de la inspección (`schemaVersion.criterion(code)`) y la respuesta con `criterion.answerFrom(raw)`; el comodín se captura en un método genérico privado.
- `answer_type` es `NUMBER`, `YES_NO` u `OPTION`; `answer_value` es `BigDecimal.toPlainString()`, `"true"/"false"` o el texto. Así la escala viaja intacta (Review Focus 5).
- El alcance: `scope_sections` es `null` para `FullScope` y los títulos unidos por `\n` para `SectionsScope`.
- `Revision.evaluation` se guarda como JSON en `revision.evaluation` (`EvaluationJson`): lista de `{criterionCode, answerType, answerValue, answeredById, answeredByName, answeredAt, evidence[], observations[]}`; se reconstruye contra la misma versión del esquema.
- Un `Finding` guarda su criterio con `SectionsJson.writeCriterion`.
- `EvidenceKind` y `VerificationResult` por `name()`; `StandardEvidenceKind.valueOf` al leer.

- [ ] **Step 1: Muestras de dominio para los tests**

```java
package ar.edu.itba.certiflow.infrastructure.support;

import ar.edu.itba.certiflow.catalog.model.Asset;
import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.catalog.model.AssetType;
import ar.edu.itba.certiflow.catalog.model.Characteristics;
import ar.edu.itba.certiflow.catalog.model.Location;
import ar.edu.itba.certiflow.infrastructure.inmemory.InMemoryAuditLog;
import ar.edu.itba.certiflow.inspection.model.Finding;
import ar.edu.itba.certiflow.inspection.model.FindingFactory;
import ar.edu.itba.certiflow.inspection.model.FindingId;
import ar.edu.itba.certiflow.inspection.model.FindingService;
import ar.edu.itba.certiflow.inspection.model.FullScope;
import ar.edu.itba.certiflow.inspection.model.Inspection;
import ar.edu.itba.certiflow.inspection.model.InspectionAssignment;
import ar.edu.itba.certiflow.inspection.model.InspectionClosed;
import ar.edu.itba.certiflow.inspection.model.InspectionFactory;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.inspection.model.InspectionService;
import ar.edu.itba.certiflow.inspection.model.VerificationResult;
import ar.edu.itba.certiflow.schema.model.Evidence;
import ar.edu.itba.certiflow.schema.model.InspectionSchema;
import ar.edu.itba.certiflow.schema.model.RawAnswer;
import ar.edu.itba.certiflow.schema.model.StandardEvidenceKind;
import ar.edu.itba.certiflow.shared.model.Person;
import ar.edu.itba.certiflow.shared.model.PersonId;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public final class DomainSamples {

    public static final AssetType EXTINGUISHER = new AssetType("Fire extinguisher");
    public static final Instant T0 = Instant.parse("2026-03-10T09:00:00Z");
    public static final Person PLANNER = new Person(new PersonId("EMP-001"), "Paula Planner");
    public static final Person INSPECTOR = new Person(new PersonId("EMP-002"), "Ines Inspector");
    public static final Person RESPONSIBLE = new Person(new PersonId("EMP-003"), "Raul Responsible");
    public static final Person TECHNICIAN = new Person(new PersonId("EMP-004"), "Marta Maintenance");
    public static final Person VERIFIER = new Person(new PersonId("EMP-005"), "Victor Verifier");
    public static final Asset ASSET = new Asset(new AssetCode("EXT-0042"), "Lobby extinguisher", EXTINGUISHER, new Location("Lobby"),
            RESPONSIBLE, Characteristics.none());

    private static final AtomicInteger IDS = new AtomicInteger();

    private DomainSamples() {
    }

    public static InspectionSchema schema() {
        return new InspectionSchema("Extinguisher", EXTINGUISHER,
                ar.edu.itba.certiflow.infrastructure.schema.persistence.JpaInspectionSchemaRepositoryTest.sections(), T0);
    }

    public static InspectionService inspectionService() {
        return new InspectionService(new InspectionFactory(), new InMemoryAuditLog<>());
    }

    public static Inspection answeredInspection(InspectionSchema schema, String pressure) {
        InspectionService service = inspectionService();
        Inspection inspection = service.assign(new InspectionId("I-" + IDS.incrementAndGet()), ASSET,
                new InspectionAssignment(INSPECTOR, LocalDate.of(2026, 3, 13), new FullScope()), schema.currentVersion(), PLANNER, T0);
        service.recordAnswer(inspection, "EXT-01", new RawAnswer.Number(new BigDecimal(pressure)), INSPECTOR, T0.plusSeconds(60));
        service.attachEvidence(inspection, "EXT-01", new Evidence(StandardEvidenceKind.PHOTO, "photos/gauge.jpg", "Gauge"), INSPECTOR,
                T0.plusSeconds(90));
        service.addObservation(inspection, "EXT-01", "Needle trembles", INSPECTOR, T0.plusSeconds(100));
        service.recordAnswer(inspection, "EXT-02", new RawAnswer.YesNo(true), INSPECTOR, T0.plusSeconds(120));
        service.recordAnswer(inspection, "EXT-03", new RawAnswer.Option("VISIBLE"), INSPECTOR, T0.plusSeconds(180));
        return inspection;
    }

    public static InspectionClosed close(Inspection inspection) {
        return inspectionService().close(inspection, INSPECTOR, T0.plusSeconds(600));
    }

    public static Finding findingWithTwoActions(InspectionClosed closure) {
        FindingService service = new FindingService(new FindingFactory(), new InMemoryAuditLog<>());
        Finding finding = service.raiseFrom(closure, () -> new FindingId("F-" + IDS.incrementAndGet())).getFirst();
        LocalDate today = LocalDate.of(2026, 3, 10);
        service.planAction(finding, "Recharge", TECHNICIAN, today.plusDays(5), PLANNER, T0.plusSeconds(700), today);
        service.planAction(finding, "Replace", TECHNICIAN, today.plusDays(9), PLANNER, T0.plusSeconds(710), today);
        service.verifyAction(finding, finding.actions().getFirst().id(), VERIFIER, VerificationResult.REJECTED, "Still low",
                T0.plusSeconds(800));
        service.verifyAction(finding, finding.actions().getFirst().id(), VERIFIER, VerificationResult.ACCEPTED, "Recharged",
                T0.plusSeconds(900));
        return finding;
    }

    public static List<Finding> none() {
        return List.of();
    }
}
```

Mover `JpaInspectionSchemaRepositoryTest.sections()` a `DomainSamples.sections()` si el compilador se queja de la dependencia entre tests; es solo un lugar común.

- [ ] **Step 2: Tests que fallan**

```java
package ar.edu.itba.certiflow.infrastructure.inspection.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.itba.certiflow.infrastructure.schema.persistence.JpaInspectionSchemaRepository;
import ar.edu.itba.certiflow.infrastructure.support.DomainSamples;
import ar.edu.itba.certiflow.inspection.model.Inspection;
import ar.edu.itba.certiflow.inspection.model.exceptions.InspectionAlreadyClosedException;
import ar.edu.itba.certiflow.schema.model.InspectionSchema;
import ar.edu.itba.certiflow.schema.model.NumericAnswer;
import ar.edu.itba.certiflow.schema.model.RawAnswer;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({JpaInspectionRepository.class, JpaInspectionSchemaRepository.class})
class JpaInspectionRepositoryTest {

    @Autowired
    private JpaInspectionRepository repository;
    @Autowired
    private JpaInspectionSchemaRepository schemas;
    @Autowired
    private EntityManager entityManager;
    private InspectionSchema schema;

    @BeforeEach
    void publishSchema() {
        schema = DomainSamples.schema();
        schemas.save(schema);
    }

    private Inspection reloaded(Inspection inspection) {
        repository.save(inspection);
        entityManager.flush();
        entityManager.clear();
        return repository.findById(inspection.id()).orElseThrow();
    }

    @Test
    void anOpenInspectionIsReadBackWithTheSameSnapshot() {
        Inspection inspection = DomainSamples.answeredInspection(schema, "7.00");

        assertEquals(inspection.snapshot(), reloaded(inspection).snapshot());
    }

    @Test
    void aClosedInspectionIsReadBackClosedAndStillRejectsAnswers() {
        Inspection inspection = DomainSamples.answeredInspection(schema, "7.00");
        DomainSamples.close(inspection);

        Inspection restored = reloaded(inspection);

        assertEquals(inspection.snapshot(), restored.snapshot());
        assertTrue(restored.isClosed());
        assertThrows(InspectionAlreadyClosedException.class, () -> DomainSamples.inspectionService()
                .recordAnswer(restored, "EXT-02", new RawAnswer.YesNo(false), DomainSamples.INSPECTOR, DomainSamples.T0));
    }

    @Test
    void decimalScaleSurvivesTheDatabase() {
        Inspection inspection = DomainSamples.answeredInspection(schema, "7.00");

        NumericAnswer answer = (NumericAnswer) reloaded(inspection).evaluate().responses().getFirst().given().orElseThrow().value();

        assertEquals("7.00", answer.value().toPlainString());
    }

    @Test
    void savingTwiceUpdatesInsteadOfDuplicating() {
        Inspection inspection = DomainSamples.answeredInspection(schema, "7.00");
        repository.save(inspection);
        entityManager.flush();
        DomainSamples.close(inspection);

        assertEquals(inspection.snapshot(), reloaded(inspection).snapshot());
    }
}
```

```java
package ar.edu.itba.certiflow.infrastructure.inspection.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ar.edu.itba.certiflow.infrastructure.schema.persistence.JpaInspectionSchemaRepository;
import ar.edu.itba.certiflow.infrastructure.support.DomainSamples;
import ar.edu.itba.certiflow.inspection.model.Finding;
import ar.edu.itba.certiflow.inspection.model.Inspection;
import ar.edu.itba.certiflow.schema.model.InspectionSchema;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({JpaFindingRepository.class, JpaInspectionRepository.class, JpaInspectionSchemaRepository.class})
class JpaFindingRepositoryTest {

    @Autowired
    private JpaFindingRepository repository;
    @Autowired
    private JpaInspectionRepository inspections;
    @Autowired
    private JpaInspectionSchemaRepository schemas;
    @Autowired
    private EntityManager entityManager;

    private Finding savedFinding() {
        InspectionSchema schema = DomainSamples.schema();
        schemas.save(schema);
        Inspection inspection = DomainSamples.answeredInspection(schema, "5.00");
        var closure = DomainSamples.close(inspection);
        inspections.save(inspection);
        Finding finding = DomainSamples.findingWithTwoActions(closure);
        repository.saveAll(List.of(finding));
        entityManager.flush();
        entityManager.clear();
        return finding;
    }

    @Test
    void aFindingIsReadBackWithItsActionsVerificationsAndCancellations() {
        Finding finding = savedFinding();

        assertEquals(finding.snapshot(), repository.findById(finding.id()).orElseThrow().snapshot());
    }

    @Test
    void findingsAreFoundByAssetAndNextIdsAreUnique() {
        Finding finding = savedFinding();

        assertEquals(List.of(finding.id()), repository.findByAsset(DomainSamples.ASSET.code()).stream().map(Finding::id).toList());
        assertEquals(List.of(finding.id()), repository.findAll().stream().map(Finding::id).toList());
        org.junit.jupiter.api.Assertions.assertNotEquals(repository.nextId(), repository.nextId());
    }
}
```

`Optional` y records con listas comparan por valor; `Person` compara por id. Si `Verification`, `Cancellation`, `AttachedEvidence` u `Observation` no fueran records, comparar sus campos uno a uno en lugar de los snapshots.

Run: `mvn -q -pl certiflow-infrastructure -am test-compile 2>&1 | grep -m1 ERROR`
Expected: `cannot find symbol ... JpaInspectionRepository`.

- [ ] **Step 3: Implementar respuestas**

```java
package ar.edu.itba.certiflow.infrastructure.inspection.persistence;

import ar.edu.itba.certiflow.schema.model.RawAnswer;
import java.math.BigDecimal;

record AnswerColumns(String type, String value) {

    static AnswerColumns of(RawAnswer raw) {
        return switch (raw) {
            case RawAnswer.Number(BigDecimal number) -> new AnswerColumns("NUMBER", number.toPlainString());
            case RawAnswer.YesNo(boolean yes) -> new AnswerColumns("YES_NO", Boolean.toString(yes));
            case RawAnswer.Option(String option) -> new AnswerColumns("OPTION", option);
        };
    }

    RawAnswer toRaw() {
        return switch (type) {
            case "NUMBER" -> new RawAnswer.Number(new BigDecimal(value));
            case "YES_NO" -> new RawAnswer.YesNo(Boolean.parseBoolean(value));
            case "OPTION" -> new RawAnswer.Option(value);
            default -> throw new IllegalStateException("Unknown answer type " + type);
        };
    }
}
```

`EvidenceEmbeddable` (`@Embeddable`, columnas `kind`, `reference`, `description`, `attached_by_id`, `attached_by_name`, `attached_at`) con `static EvidenceEmbeddable of(AttachedEvidence)` y `AttachedEvidence toDomain()` (`new AttachedEvidence(new Evidence(StandardEvidenceKind.valueOf(kind), reference, description), new PersonColumns(byId, byName).toDomain(), attachedAt)`; al escribir, `((StandardEvidenceKind) evidence.kind()).name()`).

`ObservationEmbeddable` (`@Embeddable`, columnas `text`, `observed_by_id`, `observed_by_name`, `observed_at`) con `of(Observation)` y `toDomain()`.

`CriterionResponseEntity`:

```java
package ar.edu.itba.certiflow.infrastructure.inspection.persistence;

import ar.edu.itba.certiflow.infrastructure.shared.persistence.PersonColumns;
import ar.edu.itba.certiflow.inspection.model.AttachedEvidence;
import ar.edu.itba.certiflow.inspection.model.CriterionResponse;
import ar.edu.itba.certiflow.inspection.model.GivenAnswer;
import ar.edu.itba.certiflow.schema.model.Answer;
import ar.edu.itba.certiflow.schema.model.Criterion;
import ar.edu.itba.certiflow.schema.model.SchemaVersion;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Entity
@Table(name = "criterion_response")
public class CriterionResponseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "criterion_code")
    private String criterionCode;
    @Column(name = "answer_type")
    private String answerType;
    @Column(name = "answer_value")
    private String answerValue;
    @Column(name = "answered_by_id")
    private String answeredById;
    @Column(name = "answered_by_name")
    private String answeredByName;
    @Column(name = "answered_at")
    private Instant answeredAt;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "attached_evidence", joinColumns = @JoinColumn(name = "response_id"))
    @OrderColumn(name = "position")
    private List<EvidenceEmbeddable> evidence = new ArrayList<>();
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "observation", joinColumns = @JoinColumn(name = "response_id"))
    @OrderColumn(name = "position")
    private List<ObservationEmbeddable> observations = new ArrayList<>();

    protected CriterionResponseEntity() {
    }

    String criterionCode() {
        return criterionCode;
    }

    void copyFrom(CriterionResponse<?> response) {
        criterionCode = response.criterion().code();
        response.given().ifPresentOrElse(given -> {
            AnswerColumns answer = AnswerColumns.of(given.value().raw());
            PersonColumns by = PersonColumns.of(given.answeredBy());
            answerType = answer.type();
            answerValue = answer.value();
            answeredById = by.id();
            answeredByName = by.name();
            answeredAt = given.answeredAt();
        }, () -> {
            answerType = null;
            answerValue = null;
            answeredById = null;
            answeredByName = null;
            answeredAt = null;
        });
        evidence.clear();
        response.attachments().forEach(attached -> evidence.add(EvidenceEmbeddable.of(attached)));
        observations.clear();
        response.observations().forEach(observation -> observations.add(ObservationEmbeddable.of(observation)));
    }

    static CriterionResponseEntity from(CriterionResponse<?> response) {
        CriterionResponseEntity entity = new CriterionResponseEntity();
        entity.copyFrom(response);
        return entity;
    }

    CriterionResponse<?> toDomain(SchemaVersion version) {
        return typed(version.criterion(criterionCode));
    }

    private <A extends Answer> CriterionResponse<A> typed(Criterion<A> criterion) {
        Optional<GivenAnswer<A>> given = Optional.ofNullable(answerType).map(type -> new GivenAnswer<>(
                criterion.answerFrom(new AnswerColumns(answerType, answerValue).toRaw()),
                new PersonColumns(answeredById, answeredByName).toDomain(), answeredAt));
        List<AttachedEvidence> attachments = evidence.stream().map(EvidenceEmbeddable::toDomain).toList();
        return new CriterionResponse<>(criterion, given, attachments,
                observations.stream().map(ObservationEmbeddable::toDomain).toList());
    }
}
```

- [ ] **Step 4: Implementar revisiones**

`EvaluationJson` (en el mismo paquete) escribe y lee `Evaluation` con Jackson usando documentos propios: `ResponseDocument(String criterionCode, String answerType, String answerValue, String answeredById, String answeredByName, Instant answeredAt, List<EvidenceDocument> evidence, List<ObservationDocument> observations)`, `EvidenceDocument(String kind, String reference, String description, String attachedById, String attachedByName, Instant attachedAt)`, `ObservationDocument(String text, String observedById, String observedByName, Instant observedAt)`. `static String write(Evaluation)` y `static Evaluation read(String json, SchemaVersion version)`; la reconstrucción de cada respuesta usa el mismo método genérico `typed` que `CriterionResponseEntity` (extraerlo a un método estático compartido `ResponseRebuilder.rebuild(Criterion<A>, AnswerColumns, PersonColumns, Instant, List<AttachedEvidence>, List<Observation>)` y usarlo en los dos lugares). El `JsonMapper` necesita el módulo de `java.time`: en Jackson 3 viene incluido; en Jackson 2 agregar `.addModule(new JavaTimeModule())`.

```java
package ar.edu.itba.certiflow.infrastructure.inspection.persistence;

import ar.edu.itba.certiflow.infrastructure.shared.persistence.PersonColumns;
import ar.edu.itba.certiflow.inspection.model.Revision;
import ar.edu.itba.certiflow.schema.model.SchemaVersion;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.Instant;

@Embeddable
public class RevisionEntity {

    private int number;
    private String reason;
    @Column(name = "sealed_by_id")
    private String sealedById;
    @Column(name = "sealed_by_name")
    private String sealedByName;
    @Column(name = "sealed_at")
    private Instant sealedAt;
    private String evaluation;

    protected RevisionEntity() {
    }

    static RevisionEntity from(Revision revision) {
        RevisionEntity entity = new RevisionEntity();
        PersonColumns by = PersonColumns.of(revision.sealedBy());
        entity.number = revision.number();
        entity.reason = revision.reason();
        entity.sealedById = by.id();
        entity.sealedByName = by.name();
        entity.sealedAt = revision.sealedAt();
        entity.evaluation = EvaluationJson.write(revision.evaluation());
        return entity;
    }

    Revision toDomain(SchemaVersion version) {
        return new Revision(number, reason, EvaluationJson.read(evaluation, version),
                new PersonColumns(sealedById, sealedByName).toDomain(), sealedAt);
    }
}
```

La tabla `revision` tiene PK `(inspection_id, number)`: se mapea como `@ElementCollection` de `RevisionEntity` con `@CollectionTable(name = "revision", joinColumns = @JoinColumn(name = "inspection_id"))`, sin `@OrderColumn` (el orden lo da `@OrderBy("number")`).

- [ ] **Step 5: Implementar `InspectionEntity` y el repositorio**

```java
package ar.edu.itba.certiflow.infrastructure.inspection.persistence;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.infrastructure.schema.persistence.SchemaVersionEntity;
import ar.edu.itba.certiflow.infrastructure.shared.persistence.PersonColumns;
import ar.edu.itba.certiflow.inspection.model.CriterionResponse;
import ar.edu.itba.certiflow.inspection.model.FullScope;
import ar.edu.itba.certiflow.inspection.model.InspectionAssignment;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.inspection.model.InspectionScope;
import ar.edu.itba.certiflow.inspection.model.InspectionSnapshot;
import ar.edu.itba.certiflow.inspection.model.Revisions;
import ar.edu.itba.certiflow.inspection.model.SectionsScope;
import ar.edu.itba.certiflow.schema.model.SchemaVersion;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

@Entity
@Table(name = "inspection")
public class InspectionEntity {

    @Id
    private String id;
    @Column(name = "asset_code")
    private String assetCode;
    @Column(name = "asset_responsible_id")
    private String assetResponsibleId;
    @Column(name = "asset_responsible_name")
    private String assetResponsibleName;
    @Column(name = "inspector_id")
    private String inspectorId;
    @Column(name = "inspector_name")
    private String inspectorName;
    @Column(name = "planned_date")
    private LocalDate plannedDate;
    @Column(name = "scope_sections")
    private String scopeSections;
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "schema_version_id")
    private SchemaVersionEntity schemaVersion;
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "inspection_id", nullable = false)
    @OrderColumn(name = "position")
    private List<CriterionResponseEntity> responses = new ArrayList<>();
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "revision", joinColumns = @JoinColumn(name = "inspection_id"))
    @OrderBy("number")
    private List<RevisionEntity> revisions = new ArrayList<>();

    protected InspectionEntity() {
    }

    InspectionEntity(InspectionSnapshot snapshot, SchemaVersionEntity schemaVersion) {
        this.id = snapshot.id().value();
        this.schemaVersion = schemaVersion;
        copyFrom(snapshot);
    }

    void copyFrom(InspectionSnapshot snapshot) {
        PersonColumns responsible = PersonColumns.of(snapshot.assetResponsible());
        PersonColumns inspector = PersonColumns.of(snapshot.assignment().inspector());
        assetCode = snapshot.assetCode().value();
        assetResponsibleId = responsible.id();
        assetResponsibleName = responsible.name();
        inspectorId = inspector.id();
        inspectorName = inspector.name();
        plannedDate = snapshot.assignment().plannedDate();
        scopeSections = snapshot.assignment().scope() instanceof SectionsScope(Set<String> titles)
                ? String.join("\n", new TreeSet<>(titles)) : null;
        List<CriterionResponse<?>> current = snapshot.responses();
        for (int index = 0; index < current.size(); index++) {
            if (index < responses.size()) {
                responses.get(index).copyFrom(current.get(index));
            } else {
                responses.add(CriterionResponseEntity.from(current.get(index)));
            }
        }
        revisions.clear();
        snapshot.revisions().asList().forEach(revision -> revisions.add(RevisionEntity.from(revision)));
    }

    InspectionSnapshot toSnapshot() {
        SchemaVersion version = schemaVersion.toDomain();
        InspectionScope scope = scopeSections == null ? new FullScope() : new SectionsScope(Set.of(scopeSections.split("\n")));
        return new InspectionSnapshot(new InspectionId(id), new AssetCode(assetCode),
                new PersonColumns(assetResponsibleId, assetResponsibleName).toDomain(),
                new InspectionAssignment(new PersonColumns(inspectorId, inspectorName).toDomain(), plannedDate, scope),
                version,
                responses.stream().<CriterionResponse<?>>map(response -> response.toDomain(version)).toList(),
                new Revisions(revisions.stream().map(revision -> revision.toDomain(version)).toList()));
    }
}
```

`InspectionScope` es una interfaz con dos records (`FullScope`, `SectionsScope`): el `instanceof` con patrón es el adaptador traduciendo a columnas, no una decisión de negocio (queda en D19). El `Arrays` import se borra si no se usa.

```java
package ar.edu.itba.certiflow.infrastructure.inspection.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataInspectionRepository extends JpaRepository<InspectionEntity, String> {
}
```

```java
package ar.edu.itba.certiflow.infrastructure.inspection.persistence;

import ar.edu.itba.certiflow.infrastructure.schema.persistence.JpaInspectionSchemaRepository;
import ar.edu.itba.certiflow.inspection.model.Inspection;
import ar.edu.itba.certiflow.inspection.model.InspectionFactory;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.inspection.model.InspectionSnapshot;
import ar.edu.itba.certiflow.inspection.repository.InspectionRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class JpaInspectionRepository implements InspectionRepository {

    private final SpringDataInspectionRepository inspections;
    private final JpaInspectionSchemaRepository schemas;
    private final InspectionFactory factory = new InspectionFactory();

    public JpaInspectionRepository(SpringDataInspectionRepository inspections, JpaInspectionSchemaRepository schemas) {
        this.inspections = inspections;
        this.schemas = schemas;
    }

    @Override
    public InspectionId nextId() {
        return new InspectionId(UUID.randomUUID().toString());
    }

    @Override
    public void save(Inspection inspection) {
        InspectionSnapshot snapshot = inspection.snapshot();
        inspections.findById(snapshot.id().value()).ifPresentOrElse(
                existing -> existing.copyFrom(snapshot),
                () -> inspections.save(new InspectionEntity(snapshot, schemas.versionEntityFor(snapshot.schemaVersion()))));
    }

    @Override
    public Optional<Inspection> findById(InspectionId id) {
        return inspections.findById(id.value()).map(entity -> factory.restore(entity.toSnapshot()));
    }
}
```

La inspección no guarda el `AssetType`, y la `SchemaVersion` tampoco lo conoce. Para enlazar la fila de su versión, agregar en `infrastructure.schema.persistence`:

```java
package ar.edu.itba.certiflow.infrastructure.schema.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataSchemaVersionRepository extends JpaRepository<SchemaVersionEntity, Long> {

    List<SchemaVersionEntity> findByNumberAndSections(int number, String sections);
}
```

y en `JpaInspectionSchemaRepository` (que la recibe como segundo parámetro del constructor):

```java
    public SchemaVersionEntity versionEntityFor(SchemaVersion version) {
        return versions.findByNumberAndSections(version.number(), SectionsJson.write(version.sections())).stream()
                .filter(entity -> entity.toDomain().equals(version))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Schema version " + version.number() + " was never saved"));
    }
```

`SectionsJson.write` es determinista para una misma versión (las listas conservan su orden; los `Set` de opciones se escriben ordenados: en `SectionsJson.document(ApprovalRule)` pasar `new TreeSet<>(option.acceptedOptions())`), así que la igualdad de texto encuentra la fila. Si dos esquemas tuvieran una versión idéntica, el filtro devuelve cualquiera de las dos: su contenido es el mismo y la inspección solo la lee.

- [ ] **Step 6: Implementar hallazgos**

`VerificationEmbeddable` (`@Embeddable`, columnas `verifier_id`, `verifier_name`, `result`, `notes`, `verified_at`) con `of(Verification)` y `toDomain()` (`VerificationResult.valueOf(result)`).

`CorrectiveActionEntity`: `@Embeddable` no sirve porque tiene su propia colección; usar `@Entity @Table(name = "corrective_action") @IdClass(CorrectiveActionEntity.Key.class)` con `@Id @Column(name = "finding_id") String findingId`, `@Id int number`, las columnas de la tabla, y

```java
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "verification", joinColumns = {
            @JoinColumn(name = "finding_id", referencedColumnName = "finding_id"),
            @JoinColumn(name = "action_number", referencedColumnName = "number")})
    @OrderColumn(name = "position")
    private List<VerificationEmbeddable> verifications = new ArrayList<>();

    public record Key(String findingId, int number) implements java.io.Serializable {
    }
```

con `static CorrectiveActionEntity from(String findingId, CorrectiveActionSnapshot)`, `void copyFrom(CorrectiveActionSnapshot)` (reescribe verificaciones y cancelación) y `CorrectiveActionSnapshot toSnapshot()`. Si Hibernate rechaza un record como `@IdClass`, usar una clase con constructor vacío, getters, `equals` y `hashCode`.

`FindingEntity`:

```java
@Entity
@Table(name = "finding")
public class FindingEntity {

    @Id
    private String id;
    @Column(name = "inspection_id")
    private String inspectionId;
    @Column(name = "asset_code")
    private String assetCode;
    private String criterion;
    @Column(name = "responsible_id")
    private String responsibleId;
    @Column(name = "responsible_name")
    private String responsibleName;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "finding_evidence", joinColumns = @JoinColumn(name = "finding_id"))
    @OrderColumn(name = "position")
    private List<EvidenceEmbeddable> evidence = new ArrayList<>();
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "finding_id", referencedColumnName = "id", insertable = false, updatable = false)
    @OrderBy("number")
    private List<CorrectiveActionEntity> actions = new ArrayList<>();

    protected FindingEntity() {
    }

    FindingEntity(FindingSnapshot snapshot) {
        this.id = snapshot.id().value();
        this.inspectionId = snapshot.inspectionId().value();
        this.assetCode = snapshot.assetCode().value();
        this.criterion = SectionsJson.writeCriterion(snapshot.criterion());
        PersonColumns responsible = PersonColumns.of(snapshot.responsible());
        this.responsibleId = responsible.id();
        this.responsibleName = responsible.name();
        snapshot.evidence().forEach(attached -> evidence.add(EvidenceEmbeddable.of(attached)));
        copyActionsFrom(snapshot);
    }

    void copyActionsFrom(FindingSnapshot snapshot) {
        List<CorrectiveActionSnapshot> current = snapshot.actions();
        for (int index = 0; index < current.size(); index++) {
            if (index < actions.size()) {
                actions.get(index).copyFrom(current.get(index));
            } else {
                actions.add(CorrectiveActionEntity.from(id, current.get(index)));
            }
        }
    }

    FindingSnapshot toSnapshot() {
        return new FindingSnapshot(new FindingId(id), new InspectionId(inspectionId), new AssetCode(assetCode),
                SectionsJson.readCriterion(criterion), evidence.stream().map(EvidenceEmbeddable::toDomain).toList(),
                new PersonColumns(responsibleId, responsibleName).toDomain(),
                actions.stream().map(CorrectiveActionEntity::toSnapshot).toList());
    }
}
```

(Con los imports correspondientes; `SectionsJson` es `infrastructure.schema.persistence.SectionsJson`.)

`SpringDataFindingRepository extends JpaRepository<FindingEntity, String>` con `List<FindingEntity> findByAssetCode(String assetCode)`.

`JpaFindingRepository`: `nextId()` con `UUID`; `save` = `findById(...).ifPresentOrElse(e -> e.copyActionsFrom(snapshot), () -> repo.save(new FindingEntity(snapshot)))`; `saveAll` = `forEach(this::save)`; `findById`, `findByAsset`, `findAll` restauran con `new FindingFactory().restore(entity.toSnapshot())`.

- [ ] **Step 7: Ver que pasa y commitear**

Run: `mvn -q clean verify`
Expected: verde, con los 6 tests nuevos.

```bash
git add -A
git commit -m "Persist inspections and findings through their snapshots"
```

---

### Task 10: Persistencia de certificados, numeración y auditoría

**Files:**
- Create en `infrastructure/certification/persistence/`: `SuspensionEmbeddable.java`, `CertificateEntity.java`, `SpringDataCertificateRepository.java`, `JpaCertificateRepository.java`, `SequenceCertificateNumbering.java`
- Create en `infrastructure/shared/persistence/`: `AuditEntryEntity.java`, `SpringDataAuditEntryRepository.java`, `JpaAuditLog.java`, `AuditActions.java`
- Test: `infrastructure/certification/persistence/JpaCertificateRepositoryTest.java`, `infrastructure/shared/persistence/JpaAuditLogTest.java`

**Interfaces:**
- Produces:
  - `@Repository JpaCertificateRepository implements CertificateRepository`.
  - `@Component SequenceCertificateNumbering implements CertificateNumbering` (formato `CERT-%05d`, igual que el en memoria).
  - `class JpaAuditLog<S> implements AuditLog<S>`, constructor `(SpringDataAuditEntryRepository entries, String subjectType, Function<S, String> idOf)`. No es `@Component`: la web lo instancia cinco veces (tarea 11).
  - `final class AuditActions { static String nameOf(AuditAction); static AuditAction valueOf(String); }` usando `"<SimpleNameDelEnum>.<CONSTANTE>"` y los enums `AssetAudit`, `SchemaAudit`, `InspectionAudit`, `FindingAudit`, `CertificateAudit`.

- [ ] **Step 1: Tests que fallan**

```java
package ar.edu.itba.certiflow.infrastructure.certification.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.itba.certiflow.certification.model.Certificate;
import ar.edu.itba.certiflow.certification.model.CertificateFactory;
import ar.edu.itba.certiflow.certification.model.InspectionResult;
import ar.edu.itba.certiflow.certification.model.ValidityPeriod;
import ar.edu.itba.certiflow.infrastructure.inspection.persistence.JpaInspectionRepository;
import ar.edu.itba.certiflow.infrastructure.schema.persistence.JpaInspectionSchemaRepository;
import ar.edu.itba.certiflow.infrastructure.support.DomainSamples;
import ar.edu.itba.certiflow.inspection.model.Inspection;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({JpaCertificateRepository.class, SequenceCertificateNumbering.class, JpaInspectionRepository.class,
        JpaInspectionSchemaRepository.class})
class JpaCertificateRepositoryTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 3, 10);

    @Autowired
    private JpaCertificateRepository repository;
    @Autowired
    private SequenceCertificateNumbering numbering;
    @Autowired
    private JpaInspectionRepository inspections;
    @Autowired
    private JpaInspectionSchemaRepository schemas;
    @Autowired
    private EntityManager entityManager;

    private Certificate issued() {
        var schema = DomainSamples.schema();
        schemas.save(schema);
        Inspection inspection = DomainSamples.answeredInspection(schema, "7.00");
        DomainSamples.close(inspection);
        inspections.save(inspection);
        InspectionResult result = new InspectionResult(inspection.id(), inspection.assetCode(),
                Optional.of(inspection.originalRevision().sealedAt()), true, List.of());
        return new CertificateFactory().create(numbering.next(), result, new ValidityPeriod(TODAY, TODAY.plusYears(1)),
                DomainSamples.PLANNER, DomainSamples.T0);
    }

    @Test
    void aCertificateIsReadBackWithTheSameSnapshotAndIsCurrent() {
        Certificate certificate = issued();
        repository.save(certificate);
        entityManager.flush();
        entityManager.clear();

        assertEquals(certificate.snapshot(), repository.findByNumber(certificate.number()).orElseThrow().snapshot());
        assertEquals(certificate.number(), repository.findCurrentFor(certificate.assetCode(), TODAY).orElseThrow().number());
        assertTrue(repository.findCurrentFor(certificate.assetCode(), TODAY.plusYears(2)).isEmpty());
        assertEquals(1, repository.findByAsset(certificate.assetCode()).size());
    }

    @Test
    void numbersAreConsecutiveAndFormatted() {
        String first = numbering.next().value();
        String second = numbering.next().value();

        assertTrue(first.matches("CERT-\\d{5}"));
        assertEquals(Integer.parseInt(first.substring(5)) + 1, Integer.parseInt(second.substring(5)));
    }
}
```

Para probar la suspensión persistida, `CertificateService.suspend` es el único camino: agregar al test un caso que construya `new CertificateService(new CertificateFactory(), new InMemoryAuditLog<>())`, emita, suspenda con `suspendedOn = TODAY`, guarde, recargue y compare snapshots.

```java
package ar.edu.itba.certiflow.infrastructure.shared.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ar.edu.itba.certiflow.inspection.model.InspectionAudit;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.infrastructure.support.DomainSamples;
import ar.edu.itba.certiflow.shared.model.AuditEntry;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

@DataJpaTest
class JpaAuditLogTest {

    @Autowired
    private SpringDataAuditEntryRepository entries;

    @Test
    void historyIsReadBackInOrderAndOnlyForItsSubject() {
        JpaAuditLog<InspectionId> log = new JpaAuditLog<>(entries, "inspection", InspectionId::value);
        AuditEntry assigned = new AuditEntry(InspectionAudit.ASSIGNED, DomainSamples.PLANNER, DomainSamples.T0, "Schema version 1");
        AuditEntry closed = new AuditEntry(InspectionAudit.CLOSED, DomainSamples.INSPECTOR, DomainSamples.T0.plusSeconds(60), "");

        log.recordAll(new InspectionId("I-1"), List.of(assigned, closed));
        log.record(new InspectionId("I-2"), assigned);

        assertEquals(List.of(assigned, closed), log.historyOf(new InspectionId("I-1")));
        assertEquals(List.of(), new JpaAuditLog<InspectionId>(entries, "finding", InspectionId::value).historyOf(new InspectionId("I-1")));
    }
}
```

`AuditEntry` es un record: su `equals` compara `Person` por id y la acción por identidad del enum, así que la igualdad es exacta.

Run: `mvn -q -pl certiflow-infrastructure -am test-compile 2>&1 | grep -m1 ERROR`
Expected: `cannot find symbol ... JpaCertificateRepository`.

- [ ] **Step 2: Implementar certificados**

`SuspensionEmbeddable` (`reason`, `suspended_by_id`, `suspended_by_name`, `suspended_at`) con `of`/`toDomain`.

`CertificateEntity` (`@Entity @Table(name = "certificate")`, PK `number`) con las columnas de la tabla, `@ElementCollection` de `SuspensionEmbeddable` en `suspension` (`joinColumns = @JoinColumn(name = "certificate_number")`, `@OrderColumn(name = "position")`), `lifecycle` como `@Enumerated(EnumType.STRING) CertificateLifecycle`, y:

```java
    static CertificateEntity from(CertificateSnapshot snapshot) {
        CertificateEntity entity = new CertificateEntity();
        entity.number = snapshot.number().value();
        entity.copyFrom(snapshot);
        return entity;
    }

    void copyFrom(CertificateSnapshot snapshot) {
        PersonColumns issuer = PersonColumns.of(snapshot.issuedBy());
        assetCode = snapshot.assetCode().value();
        inspectionId = snapshot.basedOn().id().value();
        inspectionClosedAt = snapshot.basedOn().closedAt();
        validFrom = snapshot.validity().validFrom();
        validUntil = snapshot.validity().validUntil();
        issuedById = issuer.id();
        issuedByName = issuer.name();
        issuedAt = snapshot.issuedAt();
        lifecycle = snapshot.lifecycle();
        suspensions.clear();
        snapshot.suspensions().forEach(suspension -> suspensions.add(SuspensionEmbeddable.of(suspension)));
    }

    CertificateSnapshot toSnapshot() {
        return new CertificateSnapshot(new CertificateNumber(number), new AssetCode(assetCode),
                new InspectionReference(new InspectionId(inspectionId), inspectionClosedAt), new ValidityPeriod(validFrom, validUntil),
                new PersonColumns(issuedById, issuedByName).toDomain(), issuedAt,
                suspensions.stream().map(SuspensionEmbeddable::toDomain).toList(), lifecycle);
    }
```

`SpringDataCertificateRepository extends JpaRepository<CertificateEntity, String>` con `List<CertificateEntity> findByAssetCode(String assetCode)`.

`JpaCertificateRepository`: `save` (update-or-insert como en hallazgos), `findByNumber`, `findAll`, `findByAsset`, y

```java
    @Override
    public Optional<Certificate> findCurrentFor(AssetCode assetCode, LocalDate referenceDate) {
        return findByAsset(assetCode).stream().filter(certificate -> certificate.isCurrentOn(referenceDate)).findFirst();
    }
```

`SequenceCertificateNumbering`:

```java
package ar.edu.itba.certiflow.infrastructure.certification.persistence;

import ar.edu.itba.certiflow.certification.model.CertificateNumber;
import ar.edu.itba.certiflow.certification.repository.CertificateNumbering;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

@Component
public class SequenceCertificateNumbering implements CertificateNumbering {

    private static final String NUMBER_FORMAT = "CERT-%05d";

    private final EntityManager entityManager;

    public SequenceCertificateNumbering(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public CertificateNumber next() {
        Number next = (Number) entityManager.createNativeQuery("select nextval('certificate_number_seq')").getSingleResult();
        return new CertificateNumber(NUMBER_FORMAT.formatted(next.longValue()));
    }
}
```

`nextval('...')` funciona en PostgreSQL y en H2 en modo PostgreSQL.

- [ ] **Step 3: Implementar la auditoría**

```java
package ar.edu.itba.certiflow.infrastructure.shared.persistence;

import ar.edu.itba.certiflow.catalog.model.AssetAudit;
import ar.edu.itba.certiflow.certification.model.CertificateAudit;
import ar.edu.itba.certiflow.inspection.model.FindingAudit;
import ar.edu.itba.certiflow.inspection.model.InspectionAudit;
import ar.edu.itba.certiflow.schema.model.SchemaAudit;
import ar.edu.itba.certiflow.shared.model.AuditAction;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

final class AuditActions {

    private static final Map<String, AuditAction> BY_NAME = Stream.<Enum<?>[]>of(AssetAudit.values(), SchemaAudit.values(),
                    InspectionAudit.values(), FindingAudit.values(), CertificateAudit.values())
            .flatMap(Stream::of)
            .collect(Collectors.toMap(AuditActions::name, constant -> (AuditAction) constant));

    private AuditActions() {
    }

    static String nameOf(AuditAction action) {
        return name((Enum<?>) action);
    }

    static AuditAction valueOf(String name) {
        AuditAction action = BY_NAME.get(name);
        if (action == null) {
            throw new IllegalStateException("Unknown audit action " + name);
        }
        return action;
    }

    private static String name(Enum<?> constant) {
        return constant.getDeclaringClass().getSimpleName() + "." + constant.name();
    }
}
```

(Borrar el import de `Function` si no se usa.)

`AuditEntryEntity` (`@Entity @Table(name = "audit_entry")`, `@Id @GeneratedValue(strategy = IDENTITY) Long id`, el resto de las columnas) con `static AuditEntryEntity of(String subjectType, String subjectId, AuditEntry entry)` y `AuditEntry toDomain()`.

`SpringDataAuditEntryRepository extends JpaRepository<AuditEntryEntity, Long>` con `List<AuditEntryEntity> findBySubjectTypeAndSubjectIdOrderByIdAsc(String subjectType, String subjectId)`.

```java
package ar.edu.itba.certiflow.infrastructure.shared.persistence;

import ar.edu.itba.certiflow.shared.model.AuditEntry;
import ar.edu.itba.certiflow.shared.model.AuditLog;
import java.util.List;
import java.util.function.Function;

public class JpaAuditLog<S> implements AuditLog<S> {

    private final SpringDataAuditEntryRepository entries;
    private final String subjectType;
    private final Function<S, String> idOf;

    public JpaAuditLog(SpringDataAuditEntryRepository entries, String subjectType, Function<S, String> idOf) {
        this.entries = entries;
        this.subjectType = subjectType;
        this.idOf = idOf;
    }

    @Override
    public void record(S subject, AuditEntry entry) {
        entries.save(AuditEntryEntity.of(subjectType, idOf.apply(subject), entry));
    }

    @Override
    public List<AuditEntry> historyOf(S subject) {
        return entries.findBySubjectTypeAndSubjectIdOrderByIdAsc(subjectType, idOf.apply(subject)).stream()
                .map(AuditEntryEntity::toDomain)
                .toList();
    }
}
```

`SpringDataAuditEntryRepository` tiene que ser `public` porque lo inyecta el módulo web.

- [ ] **Step 4: Ver que pasa y commitear**

Run: `mvn -q clean verify`
Expected: verde.

```bash
git add -A
git commit -m "Persist certificates, certificate numbering and the audit log"
```

---

### Task 11: Módulo web: arranque, composición, transacciones, persona y errores

**Files:**
- Modify: `pom.xml` (agregar `<module>certiflow-web</module>` y `certiflow-web` en `dependencyManagement`)
- Create: `certiflow-web/pom.xml`
- Create en `certiflow-web/src/main/java/ar/edu/itba/certiflow/`: `CertiflowApplication.java`, `web/config/UseCaseConfiguration.java`, `web/config/TransactionalUseCases.java`, `web/config/WebConfiguration.java`, `web/support/ActingPerson.java`, `web/support/ActingPersonResolver.java`, `web/support/ApiExceptionHandler.java`, `web/support/RootController.java`
- Create: `certiflow-web/src/main/resources/application.properties`
- Create: `certiflow-web/src/test/resources/application.properties`
- Test: `certiflow-web/src/test/java/ar/edu/itba/certiflow/web/support/ApiTest.java` (base), `web/support/RootControllerTest.java`, `web/config/TransactionalUseCasesTest.java`

**Interfaces:**
- Consumes: todas las interfaces `XxxUseCase` y sus `XxxUseCaseImpl` (T3, T7), `InspectionResultTranslatorImpl` (T5), los adaptadores JPA (T8–T10), `JpaAuditLog`.
- Produces:
  - Un `@Bean` por caso de uso, tipado por su interfaz y envuelto por `TransactionalUseCases.wrap`.
  - `@Target(PARAMETER) @Retention(RUNTIME) @interface ActingPerson`; `ActingPersonResolver` lo resuelve a `Person` desde `X-Person-Id` y `X-Person-Name`, y lanza `MissingRequestHeaderException` si falta alguno.
  - `ApiExceptionHandler` según la tabla de §6.4 del spec, más: `CriterionNotInSchemaException`, `CorrectiveActionNotInFindingException`, `NoInspectionSchemaForAssetTypeException` → 404; `SectionNotInSchemaException`, `DuplicateCharacteristicException` → 400.
  - `abstract class ApiTest` con `@SpringBootTest @AutoConfigureMockMvc`, `MockMvc mvc` y helpers `post(String uri, String json)`, `put(...)`, `get(...)` que agregan los headers de persona.
  - `RootController`: `GET /api/v1` con links `assets`, `schemas`, `inspections`, `findings`, `certificates`.

- [ ] **Step 1: pom del módulo**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <parent>
    <groupId>ar.edu.itba</groupId>
    <artifactId>certiflow</artifactId>
    <version>1.0.0-SNAPSHOT</version>
  </parent>

  <artifactId>certiflow-web</artifactId>
  <name>Certiflow - web (API REST y arranque)</name>

  <dependencies>
    <dependency>
      <groupId>ar.edu.itba</groupId>
      <artifactId>certiflow-application</artifactId>
    </dependency>
    <dependency>
      <groupId>ar.edu.itba</groupId>
      <artifactId>certiflow-infrastructure</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-webmvc</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-hateoas</artifactId>
    </dependency>
    <dependency>
      <groupId>com.h2database</groupId>
      <artifactId>h2</artifactId>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-webmvc-test</artifactId>
      <scope>test</scope>
    </dependency>
  </dependencies>

  <build>
    <plugins>
      <plugin>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-maven-plugin</artifactId>
        <version>${spring-boot.version}</version>
      </plugin>
    </plugins>
  </build>
</project>
```

En `certiflow-application/pom.xml`, `certiflow-infrastructure` pasa a `<scope>test</scope>` ya lo es; sin cambios. `certiflow-application` no gana dependencias de Spring.

- [ ] **Step 2: Propiedades**

`certiflow-web/src/main/resources/application.properties`:

```properties
spring.application.name=certiflow
spring.datasource.url=${CERTIFLOW_DB_URL:jdbc:postgresql://localhost:5432/certiflow}
spring.datasource.username=${CERTIFLOW_DB_USER:certiflow}
spring.datasource.password=${CERTIFLOW_DB_PASSWORD:certiflow}
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.open-in-view=false
spring.mvc.problemdetails.enabled=true
spring.hateoas.use-hal-as-default-json-media-type=true
```

`certiflow-web/src/test/resources/application.properties`:

```properties
spring.datasource.url=jdbc:h2:mem:certiflow-web;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH
spring.datasource.username=sa
spring.datasource.password=
```

- [ ] **Step 3: Test del proxy transaccional, que falla**

```java
package ar.edu.itba.certiflow.web.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

class TransactionalUseCasesTest {

    interface Greet {
        String execute(String name);
    }

    static final class RecordingManager extends AbstractPlatformTransactionManager {
        final List<String> events = new ArrayList<>();

        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
            events.add("begin");
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
            events.add("commit");
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
            events.add("rollback");
        }
    }

    @Test
    void eachCallRunsInsideATransactionThatCommits() {
        RecordingManager manager = new RecordingManager();
        Greet greet = TransactionalUseCases.wrap(Greet.class, name -> "Hi " + name, new TransactionTemplate(manager));

        assertEquals("Hi Ana", greet.execute("Ana"));
        assertEquals(List.of("begin", "commit"), manager.events);
    }

    @Test
    void aDomainExceptionRollsBackAndReachesTheCallerUnwrapped() {
        RecordingManager manager = new RecordingManager();
        IllegalStateException failure = new IllegalStateException("rule broken");
        Greet greet = TransactionalUseCases.wrap(Greet.class, name -> {
            throw failure;
        }, new TransactionTemplate(manager));

        assertSame(failure, assertThrows(IllegalStateException.class, () -> greet.execute("Ana")));
        assertTrue(manager.events.contains("rollback"));
    }
}
```

Run: `mvn -q -pl certiflow-web -am test -Dtest=TransactionalUseCasesTest -Dsurefire.failIfNoSpecifiedTests=false 2>&1 | grep -m1 ERROR`
Expected: `cannot find symbol ... TransactionalUseCases`.

- [ ] **Step 4: Implementar el proxy**

```java
package ar.edu.itba.certiflow.web.config;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import org.springframework.transaction.support.TransactionTemplate;

public final class TransactionalUseCases {

    private TransactionalUseCases() {
    }

    public static <U> U wrap(Class<U> useCase, U target, TransactionTemplate transactions) {
        Object proxy = Proxy.newProxyInstance(useCase.getClassLoader(), new Class<?>[] {useCase},
                (instance, method, arguments) -> {
                    if (method.getDeclaringClass() == Object.class) {
                        return method.invoke(target, arguments);
                    }
                    return transactions.execute(status -> {
                        try {
                            return method.invoke(target, arguments);
                        } catch (InvocationTargetException failure) {
                            throw unchecked(failure.getCause());
                        } catch (IllegalAccessException failure) {
                            throw new IllegalStateException(failure);
                        }
                    });
                });
        return useCase.cast(proxy);
    }

    private static RuntimeException unchecked(Throwable cause) {
        if (cause instanceof RuntimeException runtime) {
            return runtime;
        }
        if (cause instanceof Error error) {
            throw error;
        }
        return new IllegalStateException(cause);
    }
}
```

Run: el comando del Step 3.
Expected: PASS.

- [ ] **Step 5: Composición**

```java
package ar.edu.itba.certiflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CertiflowApplication {

    public static void main(String[] args) {
        SpringApplication.run(CertiflowApplication.class, args);
    }
}
```

`UseCaseConfiguration` (`@Configuration`) declara, en este orden:

```java
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    AuditLog<AssetCode> assetLog(SpringDataAuditEntryRepository entries) {
        return new JpaAuditLog<>(entries, "asset", AssetCode::value);
    }

    @Bean
    AuditLog<AssetType> schemaLog(SpringDataAuditEntryRepository entries) {
        return new JpaAuditLog<>(entries, "schema", AssetType::name);
    }

    @Bean
    AuditLog<InspectionId> inspectionLog(SpringDataAuditEntryRepository entries) {
        return new JpaAuditLog<>(entries, "inspection", InspectionId::value);
    }

    @Bean
    AuditLog<FindingId> findingLog(SpringDataAuditEntryRepository entries) {
        return new JpaAuditLog<>(entries, "finding", FindingId::value);
    }

    @Bean
    AuditLog<CertificateNumber> certificateLog(SpringDataAuditEntryRepository entries) {
        return new JpaAuditLog<>(entries, "certificate", CertificateNumber::value);
    }

    @Bean
    AssetService assetService(AuditLog<AssetCode> assetLog) {
        return new AssetService(assetLog);
    }

    @Bean
    InspectionSchemaService inspectionSchemaService(AuditLog<AssetType> schemaLog) {
        return new InspectionSchemaService(schemaLog);
    }

    @Bean
    InspectionService inspectionService(AuditLog<InspectionId> inspectionLog) {
        return new InspectionService(new InspectionFactory(), inspectionLog);
    }

    @Bean
    FindingService findingService(AuditLog<FindingId> findingLog) {
        return new FindingService(new FindingFactory(), findingLog);
    }

    @Bean
    CertificateService certificateService(AuditLog<CertificateNumber> certificateLog) {
        return new CertificateService(new CertificateFactory(), certificateLog);
    }

    @Bean
    CertificationEligibility certificationEligibility() {
        return new NoBlockingFindingsEligibility();
    }

    @Bean
    InspectionResultTranslator inspectionResultTranslator(InspectionRepository inspections, FindingRepository findings) {
        return new InspectionResultTranslatorImpl(inspections, findings);
    }
```

y un `@Bean` por caso de uso (los 18 de T3 y los 9 de T7), cada uno así:

```java
    @Bean
    RecordAnswerUseCase recordAnswer(TransactionTemplate tx, InspectionRepository inspections, InspectionService inspectionService,
                                     Clock clock) {
        return TransactionalUseCases.wrap(RecordAnswerUseCase.class,
                new RecordAnswerUseCaseImpl(inspections, inspectionService, clock), tx);
    }
```

Los argumentos de cada constructor son los mismos que usa `CertiflowFixture` para ese `XxxUseCaseImpl`; copiarlos de ahí. `TransactionTemplate` lo provee el autoconfigurado de Spring Boot.

`WebConfiguration implements WebMvcConfigurer` registra `ActingPersonResolver` en `addArgumentResolvers`.

```java
package ar.edu.itba.certiflow.web.support;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface ActingPerson {
}
```

```java
package ar.edu.itba.certiflow.web.support;

import ar.edu.itba.certiflow.shared.model.Person;
import ar.edu.itba.certiflow.shared.model.PersonId;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

public class ActingPersonResolver implements HandlerMethodArgumentResolver {

    static final String ID_HEADER = "X-Person-Id";
    static final String NAME_HEADER = "X-Person-Name";

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(ActingPerson.class) && parameter.getParameterType() == Person.class;
    }

    @Override
    public Person resolveArgument(MethodParameter parameter, ModelAndViewContainer container, NativeWebRequest request,
                                  WebDataBinderFactory binderFactory) throws MissingRequestHeaderException {
        String id = request.getHeader(ID_HEADER);
        String name = request.getHeader(NAME_HEADER);
        if (id == null || id.isBlank()) {
            throw new MissingRequestHeaderException(ID_HEADER, parameter);
        }
        if (name == null || name.isBlank()) {
            throw new MissingRequestHeaderException(NAME_HEADER, parameter);
        }
        return new Person(new PersonId(id), name);
    }
}
```

- [ ] **Step 6: Errores**

```java
package ar.edu.itba.certiflow.web.support;

import ar.edu.itba.certiflow.catalog.model.exceptions.AssetNotFoundException;
import ar.edu.itba.certiflow.catalog.model.exceptions.DuplicateCharacteristicException;
import ar.edu.itba.certiflow.certification.model.exceptions.CertificateNotFoundException;
import ar.edu.itba.certiflow.certification.model.exceptions.NotEligibleForCertificationException;
import ar.edu.itba.certiflow.inspection.model.exceptions.CorrectiveActionNotInFindingException;
import ar.edu.itba.certiflow.inspection.model.exceptions.FindingNotFoundException;
import ar.edu.itba.certiflow.inspection.model.exceptions.InspectionNotFoundException;
import ar.edu.itba.certiflow.schema.model.AnswerTypeMismatchException;
import ar.edu.itba.certiflow.schema.model.InvalidRuleConfigurationException;
import ar.edu.itba.certiflow.schema.model.exceptions.CriterionNotInSchemaException;
import ar.edu.itba.certiflow.schema.model.exceptions.InvalidSchemaException;
import ar.edu.itba.certiflow.schema.model.exceptions.NoInspectionSchemaForAssetTypeException;
import ar.edu.itba.certiflow.schema.model.exceptions.SectionNotInSchemaException;
import ar.edu.itba.certiflow.shared.model.DomainException;
import ar.edu.itba.certiflow.shared.model.RequiredValueMissingException;
import java.net.URI;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Map<Class<? extends DomainException>, HttpStatus> STATUS_BY_EXCEPTION = Map.ofEntries(
            Map.entry(AssetNotFoundException.class, HttpStatus.NOT_FOUND),
            Map.entry(InspectionNotFoundException.class, HttpStatus.NOT_FOUND),
            Map.entry(FindingNotFoundException.class, HttpStatus.NOT_FOUND),
            Map.entry(CertificateNotFoundException.class, HttpStatus.NOT_FOUND),
            Map.entry(NoInspectionSchemaForAssetTypeException.class, HttpStatus.NOT_FOUND),
            Map.entry(CriterionNotInSchemaException.class, HttpStatus.NOT_FOUND),
            Map.entry(CorrectiveActionNotInFindingException.class, HttpStatus.NOT_FOUND),
            Map.entry(RequiredValueMissingException.class, HttpStatus.BAD_REQUEST),
            Map.entry(InvalidSchemaException.class, HttpStatus.BAD_REQUEST),
            Map.entry(InvalidRuleConfigurationException.class, HttpStatus.BAD_REQUEST),
            Map.entry(AnswerTypeMismatchException.class, HttpStatus.BAD_REQUEST),
            Map.entry(SectionNotInSchemaException.class, HttpStatus.BAD_REQUEST),
            Map.entry(DuplicateCharacteristicException.class, HttpStatus.BAD_REQUEST),
            Map.entry(NotEligibleForCertificationException.class, HttpStatus.UNPROCESSABLE_CONTENT));

    @ExceptionHandler(DomainException.class)
    ProblemDetail handle(DomainException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(statusOf(exception), exception.getMessage());
        problem.setType(URI.create("urn:certiflow:" + exception.getClass().getSimpleName()));
        problem.setTitle(exception.getClass().getSimpleName());
        if (exception instanceof NotEligibleForCertificationException notEligible) {
            problem.setProperty("ineligibilities", notEligible.decision().ineligibilities());
        }
        return problem;
    }

    private static HttpStatus statusOf(DomainException exception) {
        for (Class<?> type = exception.getClass(); type != null; type = type.getSuperclass()) {
            HttpStatus status = STATUS_BY_EXCEPTION.get(type);
            if (status != null) {
                return status;
            }
        }
        return HttpStatus.CONFLICT;
    }
}
```

Si `HttpStatus.UNPROCESSABLE_CONTENT` no existe en la versión de Spring, usar `HttpStatus.UNPROCESSABLE_ENTITY`. Verificar el paquete real de cada excepción importada con `grep -rl "class XxxException" certiflow-domain/src/main` y corregir los imports.

- [ ] **Step 7: Punto de entrada y base de tests de API**

```java
package ar.edu.itba.certiflow.web.support;

import org.springframework.hateoas.Link;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class RootController {

    @GetMapping
    public RepresentationModel<?> root() {
        return new RepresentationModel<>().add(
                Link.of("/api/v1").withSelfRel(),
                Link.of("/api/v1/assets").withRel("assets"),
                Link.of("/api/v1/schemas/{assetType}").withRel("schemas"),
                Link.of("/api/v1/inspections").withRel("inspections"),
                Link.of("/api/v1/findings").withRel("findings"),
                Link.of("/api/v1/certificates").withRel("certificates"));
    }
}
```

```java
package ar.edu.itba.certiflow.web.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public abstract class ApiTest {

    @Autowired
    protected MockMvc mvc;

    protected ResultActions postAs(String personId, String uri, String json) throws Exception {
        return mvc.perform(post(uri).contentType(MediaType.APPLICATION_JSON).content(json)
                .header("X-Person-Id", personId).header("X-Person-Name", "Person " + personId));
    }

    protected ResultActions putAs(String personId, String uri, String json) throws Exception {
        return mvc.perform(put(uri).contentType(MediaType.APPLICATION_JSON).content(json)
                .header("X-Person-Id", personId).header("X-Person-Name", "Person " + personId));
    }

    protected ResultActions read(String uri) throws Exception {
        return mvc.perform(get(uri).accept(MediaType.parseMediaType("application/hal+json")));
    }
}
```

`@DirtiesContext` por método recrea la base H2 en memoria entre tests; es lento pero aísla. Si el suite pasa de 60 s, cambiar a limpiar las tablas con un `@AfterEach` que haga `TRUNCATE` vía `JdbcTemplate` en orden inverso de dependencias.

```java
package ar.edu.itba.certiflow.web.support;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

class RootControllerTest extends ApiTest {

    @Test
    void theEntryPointLinksEveryRootCollection() throws Exception {
        read("/api/v1")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.assets.href").value("/api/v1/assets"))
                .andExpect(jsonPath("$._links.inspections.href").value("/api/v1/inspections"))
                .andExpect(jsonPath("$._links.certificates.href").value("/api/v1/certificates"));
    }

    @Test
    void anUnsupportedMethodIs405() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/v1"))
                .andExpect(status().isMethodNotAllowed());
    }
}
```

Si `org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc` no resuelve, buscar el paquete real en el jar de `spring-boot-webmvc-test`.

- [ ] **Step 8: Ver que arranca y commitear**

Run: `mvn -q clean verify`
Expected: verde; el contexto completo levanta con H2 y Flyway.

```bash
git add -A
git commit -m "Add the web module with use case wiring, transactions, acting person and error mapping"
```

---

### Task 12: API de activos y esquemas

**Files:**
- Create en `certiflow-web/src/main/java/ar/edu/itba/certiflow/web/`:
  - `support/PageLinks.java`
  - `catalog/{AssetRequest, AssetResponse, AssetController}.java`
  - `schema/{SchemaRequest, VersionRequest, SectionPayload, CriterionPayload, RulePayload, EvidenceRequirementPayload, SchemaResponse, VersionResponse, SchemaController, SchemaPayloads}.java`
  - `support/AuditEntryResponse.java`
- Test: `certiflow-web/src/test/java/ar/edu/itba/certiflow/web/catalog/AssetApiTest.java`, `web/schema/SchemaApiTest.java`
- Test support: `web/support/Payloads.java` (JSON de ejemplo reutilizado por las tareas 12–14)

**Interfaces:**
- Consumes: `RegisterAssetUseCase`, `FindAssetUseCase`, `ListAssetsUseCase`, `CreateInspectionSchemaUseCase`, `PublishSchemaVersionUseCase`, `FindSchemaUseCase`, `AuditHistoryUseCase`, `@ActingPerson Person`.
- Produces:
  - `record AuditEntryResponse(String action, String performedById, String performedByName, Instant occurredAt, String detail)` con `static of(AuditEntry)` (`action` = `entry.action().label()`).
  - `final class PageLinks { static <T> PagedModel<T> page(List<T> all, int page, int size, String baseUri, String query); }` que corta la lista y agrega `self`, `first`, `last` y, si existen, `prev`, `next`.
  - `SchemaPayloads.toSections(List<SectionPayload>): List<Section>` y `SchemaPayloads.fromSections(List<Section>): List<SectionPayload>` (la tarea 13 usa `CriterionPayload` para mostrar el criterio de una respuesta).
  - `Payloads.asset()`, `Payloads.schema()`, `Payloads.version()` (JSON del extintor con los tres criterios de `CertiflowFixture`).

Formato JSON:

```json
POST /api/v1/assets
{"code":"EXT-0042","name":"Lobby extinguisher","type":"Fire extinguisher","location":"Lobby",
 "responsible":{"id":"EMP-003","name":"Raul Responsible"},
 "characteristics":[{"name":"capacity","value":"10 kg"}]}

POST /api/v1/schemas
{"name":"Portable extinguisher inspection","assetType":"Fire extinguisher","sections":[
 {"title":"Pressure and seal","criteria":[
   {"code":"EXT-01","text":"Gauge pressure within range","severity":"MAJOR",
    "rule":{"type":"numeric-range","unit":"bar","minimum":6.00,"maximum":8.00},
    "requiredEvidence":[{"kind":"PHOTO","minimum":1}]},
   {"code":"EXT-02","text":"Safety seal is intact","severity":"CRITICAL","rule":{"type":"yes-no","expected":"YES"},"requiredEvidence":[]}]},
 {"title":"Signage","criteria":[
   {"code":"EXT-03","text":"Signage condition","severity":"MINOR",
    "rule":{"type":"option-in-list","options":["VISIBLE","REFLECTIVE"]},"requiredEvidence":[]}]}]}

POST /api/v1/schemas/{assetType}/versions
{"sections":[...]}
```

`RulePayload(String type, String unit, BigDecimal minimum, BigDecimal maximum, String expected, Set<String> options)`. La conversión a `ApprovalRule` repite el `switch` de `SectionsJson` pero en el adaptador de entrada; un `type` desconocido lanza `InvalidRuleConfigurationException` (→ 400). Una severidad o un tipo de evidencia desconocidos lanzan `InvalidSchemaException` (→ 400).

- [ ] **Step 1: Tests de API que fallan**

```java
package ar.edu.itba.certiflow.web.catalog;

import static org.hamcrest.Matchers.endsWith;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.itba.certiflow.web.support.ApiTest;
import ar.edu.itba.certiflow.web.support.Payloads;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class AssetApiTest extends ApiTest {

    @Test
    void registeringAnAssetCreatesItsResourceAndItsHistory() throws Exception {
        postAs("EMP-001", "/api/v1/assets", Payloads.asset())
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/v1/assets/EXT-0042")))
                .andExpect(jsonPath("$.code").value("EXT-0042"))
                .andExpect(jsonPath("$._links.self.href").value(endsWith("/api/v1/assets/EXT-0042")))
                .andExpect(jsonPath("$._links.findings-summary.href").exists())
                .andExpect(jsonPath("$._links.audit-entries.href").exists());

        read("/api/v1/assets/EXT-0042/audit-entries")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.auditEntries[0].action").value("Registered"))
                .andExpect(jsonPath("$._embedded.auditEntries[0].performedById").value("EMP-001"));
    }

    @Test
    void registeringTheSameCodeTwiceIs409() throws Exception {
        postAs("EMP-001", "/api/v1/assets", Payloads.asset());

        postAs("EMP-001", "/api/v1/assets", Payloads.asset())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("AssetAlreadyRegisteredException"));
    }

    @Test
    void anUnknownAssetIs404AsProblemJson() throws Exception {
        read("/api/v1/assets/NOPE")
                .andExpect(status().isNotFound())
                .andExpect(header().string("Content-Type", "application/problem+json"));
    }

    @Test
    void writingWithoutTheActingPersonIs400() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/assets")
                        .contentType(MediaType.APPLICATION_JSON).content(Payloads.asset()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void malformedJsonIs400() throws Exception {
        postAs("EMP-001", "/api/v1/assets", "{\"code\":").andExpect(status().isBadRequest());
    }

    @Test
    void assetsAreListedByTypeInPages() throws Exception {
        postAs("EMP-001", "/api/v1/assets", Payloads.asset());
        postAs("EMP-001", "/api/v1/assets", Payloads.asset().replace("EXT-0042", "EXT-0043"));

        read("/api/v1/assets?type=Fire extinguisher&page=0&size=1")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.assets.length()").value(1))
                .andExpect(jsonPath("$.page.totalElements").value(2))
                .andExpect(jsonPath("$._links.next.href").exists())
                .andExpect(jsonPath("$._links.prev").doesNotExist());
    }
}
```

```java
package ar.edu.itba.certiflow.web.schema;

import static org.hamcrest.Matchers.endsWith;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.itba.certiflow.web.support.ApiTest;
import ar.edu.itba.certiflow.web.support.Payloads;
import org.junit.jupiter.api.Test;

class SchemaApiTest extends ApiTest {

    @Test
    void creatingASchemaAndPublishingAVersionCreatesTheirResources() throws Exception {
        postAs("EMP-001", "/api/v1/schemas", Payloads.schema())
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/v1/schemas/Fire%20extinguisher")));

        postAs("EMP-001", "/api/v1/schemas/Fire extinguisher/versions", Payloads.version())
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/v1/schemas/Fire%20extinguisher/versions/2")))
                .andExpect(jsonPath("$.number").value(2));

        read("/api/v1/schemas/Fire extinguisher/versions/1")
                .andExpect(status().isOk())
                .andExpect(header().exists("ETag"))
                .andExpect(jsonPath("$.sections[0].criteria[0].rule.minimum").value(6.00));
    }

    @Test
    void anUnknownRuleTypeIs400() throws Exception {
        postAs("EMP-001", "/api/v1/schemas", Payloads.schema().replace("yes-no", "free-text"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void aVersionThatDidNotChangeAnswers304ToItsETag() throws Exception {
        postAs("EMP-001", "/api/v1/schemas", Payloads.schema());
        String etag = read("/api/v1/schemas/Fire extinguisher/versions/1").andReturn().getResponse().getHeader("ETag");

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/v1/schemas/Fire extinguisher/versions/1").header("If-None-Match", etag))
                .andExpect(status().isNotModified());
    }
}
```

`Payloads` contiene los JSON del bloque de arriba como `static String asset()`, `schema()`, `version()` (`version()` es el mismo `sections` de `schema()` sin `name` ni `assetType`).

Run: `mvn -q -pl certiflow-web -am test 2>&1 | grep -m3 "FAIL\|ERROR"`
Expected: fallan con 404 (no hay controladores).

- [ ] **Step 2: Implementar `PageLinks` y `AuditEntryResponse`**

```java
package ar.edu.itba.certiflow.web.support;

import java.util.List;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedModel;

public final class PageLinks {

    private PageLinks() {
    }

    public static <T> PagedModel<T> page(List<T> all, int page, int size, String baseUri, String query) {
        int totalPages = Math.max(1, (all.size() + size - 1) / size);
        List<T> content = all.stream().skip((long) page * size).limit(size).toList();
        PagedModel<T> model = PagedModel.of(content, new PagedModel.PageMetadata(size, page, all.size(), totalPages));
        model.add(Link.of(uri(baseUri, query, page, size)).withSelfRel(),
                Link.of(uri(baseUri, query, 0, size)).withRel("first"),
                Link.of(uri(baseUri, query, totalPages - 1, size)).withRel("last"));
        if (page > 0) {
            model.add(Link.of(uri(baseUri, query, page - 1, size)).withRel("prev"));
        }
        if (page < totalPages - 1) {
            model.add(Link.of(uri(baseUri, query, page + 1, size)).withRel("next"));
        }
        return model;
    }

    private static String uri(String baseUri, String query, int page, int size) {
        return baseUri + "?" + (query.isEmpty() ? "" : query + "&") + "page=" + page + "&size=" + size;
    }
}
```

Para que la colección se llame `assets` en `_embedded`, cada `XxxResponse` lleva `@Relation(collectionRelation = "assets", itemRelation = "asset")` (y lo análogo en los demás recursos: `auditEntries`, `findings`, `certificates`, `responses`, `revisions`, `versions`).

```java
package ar.edu.itba.certiflow.web.support;

import ar.edu.itba.certiflow.shared.model.AuditEntry;
import java.time.Instant;
import org.springframework.hateoas.server.core.Relation;

@Relation(collectionRelation = "auditEntries", itemRelation = "auditEntry")
public record AuditEntryResponse(String action, String performedById, String performedByName, Instant occurredAt, String detail) {

    public static AuditEntryResponse of(AuditEntry entry) {
        return new AuditEntryResponse(entry.action().label(), entry.performedBy().id().value(), entry.performedBy().name(),
                entry.occurredAt(), entry.detail());
    }
}
```

Si la etiqueta de `AssetAudit.REGISTERED` no es `"Registered"`, ajustar el test a la etiqueta real.

- [ ] **Step 3: Implementar activos**

```java
package ar.edu.itba.certiflow.web.catalog;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.catalog.model.AssetType;
import ar.edu.itba.certiflow.catalog.model.Characteristic;
import ar.edu.itba.certiflow.catalog.model.Characteristics;
import ar.edu.itba.certiflow.catalog.model.Location;
import ar.edu.itba.certiflow.catalog.usecase.RegisterAssetCommand;
import ar.edu.itba.certiflow.shared.model.Person;
import ar.edu.itba.certiflow.shared.model.PersonId;
import java.util.List;

public record AssetRequest(String code, String name, String type, String location, PersonPayload responsible,
                           List<CharacteristicPayload> characteristics) {

    public record PersonPayload(String id, String name) {
    }

    public record CharacteristicPayload(String name, String value) {
    }

    RegisterAssetCommand toCommand(Person registeredBy) {
        return new RegisterAssetCommand(new AssetCode(code), name, new AssetType(type), new Location(location),
                new Person(new PersonId(responsible.id()), responsible.name()),
                new Characteristics(characteristics.stream().map(c -> new Characteristic(c.name(), c.value())).toList()),
                registeredBy);
    }
}
```

Un campo faltante en el JSON llega como `null` y el dominio lanzaría `NullPointerException` (D11: `null` no se valida en el dominio, lo valida el adaptador). Cada `XxxRequest` agrega un constructor compacto que rechaza nulos:

```java
    public AssetRequest {
        RequestFields.require(code, "code");
        RequestFields.require(name, "name");
        RequestFields.require(type, "type");
        RequestFields.require(location, "location");
        RequestFields.require(responsible, "responsible");
        characteristics = characteristics == null ? List.of() : characteristics;
    }
```

con `web.support.RequestFields.require(Object value, String field)` que lanza `RequiredValueMissingException("Missing field " + field)` (→ 400).

```java
package ar.edu.itba.certiflow.web.catalog;

import ar.edu.itba.certiflow.catalog.model.Asset;
import java.util.List;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

@Relation(collectionRelation = "assets", itemRelation = "asset")
public class AssetResponse extends RepresentationModel<AssetResponse> {

    public final String code;
    public final String name;
    public final String type;
    public final String location;
    public final String responsibleId;
    public final String responsibleName;
    public final List<AssetRequest.CharacteristicPayload> characteristics;

    AssetResponse(Asset asset) {
        this.code = asset.code().value();
        this.name = asset.name();
        this.type = asset.type().name();
        this.location = asset.location().description();
        this.responsibleId = asset.responsible().id().value();
        this.responsibleName = asset.responsible().name();
        this.characteristics = asset.characteristics().entries().stream()
                .map(c -> new AssetRequest.CharacteristicPayload(c.name(), c.value())).toList();
    }
}
```

```java
package ar.edu.itba.certiflow.web.catalog;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import ar.edu.itba.certiflow.catalog.model.Asset;
import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.catalog.model.AssetType;
import ar.edu.itba.certiflow.catalog.usecase.FindAssetUseCase;
import ar.edu.itba.certiflow.catalog.usecase.ListAssetsUseCase;
import ar.edu.itba.certiflow.catalog.usecase.RegisterAssetUseCase;
import ar.edu.itba.certiflow.shared.model.Person;
import ar.edu.itba.certiflow.shared.usecase.AuditHistoryUseCase;
import ar.edu.itba.certiflow.shared.usecase.AuditSubject;
import ar.edu.itba.certiflow.web.support.ActingPerson;
import ar.edu.itba.certiflow.web.support.AuditEntryResponse;
import ar.edu.itba.certiflow.web.support.PageLinks;
import java.util.Optional;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/assets")
public class AssetController {

    private final RegisterAssetUseCase registerAsset;
    private final FindAssetUseCase findAsset;
    private final ListAssetsUseCase listAssets;
    private final AuditHistoryUseCase auditHistory;

    public AssetController(RegisterAssetUseCase registerAsset, FindAssetUseCase findAsset, ListAssetsUseCase listAssets,
                           AuditHistoryUseCase auditHistory) {
        this.registerAsset = registerAsset;
        this.findAsset = findAsset;
        this.listAssets = listAssets;
        this.auditHistory = auditHistory;
    }

    @PostMapping
    public ResponseEntity<AssetResponse> register(@RequestBody AssetRequest request, @ActingPerson Person registeredBy) {
        AssetResponse response = resource(registerAsset.execute(request.toCommand(registeredBy)));
        return ResponseEntity.created(response.getRequiredLink("self").toUri()).body(response);
    }

    @GetMapping("/{code}")
    public AssetResponse find(@PathVariable String code) {
        return resource(findAsset.execute(new AssetCode(code)));
    }

    @GetMapping
    public PagedModel<AssetResponse> list(@RequestParam Optional<String> type, @RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "20") int size) {
        return PageLinks.page(listAssets.execute(type.map(AssetType::new)).stream().map(this::resource).toList(), page, size,
                "/api/v1/assets", type.map(t -> "type=" + t).orElse(""));
    }

    @GetMapping("/{code}/audit-entries")
    public CollectionModel<AuditEntryResponse> history(@PathVariable String code) {
        return CollectionModel.of(auditHistory.execute(new AuditSubject.OfAsset(new AssetCode(code))).stream()
                        .map(AuditEntryResponse::of).toList(),
                linkTo(methodOn(AssetController.class).history(code)).withSelfRel());
    }

    private AssetResponse resource(Asset asset) {
        String code = asset.code().value();
        AssetResponse response = new AssetResponse(asset);
        response.add(linkTo(methodOn(AssetController.class).find(code)).withSelfRel(),
                Link.of("/api/v1/assets/" + code + "/findings-summary").withRel("findings-summary"),
                linkTo(methodOn(AssetController.class).history(code)).withRel("audit-entries"),
                Link.of("/api/v1/certificates?asset=" + code).withRel("certificates"),
                Link.of("/api/v1/findings?asset=" + code).withRel("findings"));
        return response;
    }
}
```

El link `findings-summary` lo atiende `FindingController` en la tarea 14.

- [ ] **Step 4: Implementar esquemas**

`SectionPayload(String title, List<CriterionPayload> criteria)`, `CriterionPayload(String code, String text, String severity, RulePayload rule, List<EvidenceRequirementPayload> requiredEvidence)`, `EvidenceRequirementPayload(String kind, int minimum)`, `SchemaRequest(String name, String assetType, List<SectionPayload> sections)`, `VersionRequest(List<SectionPayload> sections)`, todos con chequeo de nulos como `AssetRequest`.

`SchemaPayloads`:

```java
package ar.edu.itba.certiflow.web.schema;

import ar.edu.itba.certiflow.schema.model.ApprovalRule;
import ar.edu.itba.certiflow.schema.model.Criterion;
import ar.edu.itba.certiflow.schema.model.EvidenceKind;
import ar.edu.itba.certiflow.schema.model.EvidenceRequirement;
import ar.edu.itba.certiflow.schema.model.InvalidRuleConfigurationException;
import ar.edu.itba.certiflow.schema.model.NumericRangeRule;
import ar.edu.itba.certiflow.schema.model.OptionInListRule;
import ar.edu.itba.certiflow.schema.model.Range;
import ar.edu.itba.certiflow.schema.model.Section;
import ar.edu.itba.certiflow.schema.model.Severity;
import ar.edu.itba.certiflow.schema.model.StandardEvidenceKind;
import ar.edu.itba.certiflow.schema.model.StandardSeverity;
import ar.edu.itba.certiflow.schema.model.Unit;
import ar.edu.itba.certiflow.schema.model.YesNoAnswer;
import ar.edu.itba.certiflow.schema.model.YesNoRule;
import ar.edu.itba.certiflow.schema.model.exceptions.InvalidSchemaException;
import java.util.Arrays;
import java.util.List;

public final class SchemaPayloads {

    private SchemaPayloads() {
    }

    public static List<Section> toSections(List<SectionPayload> sections) {
        return sections.stream()
                .map(section -> new Section(section.title(), section.criteria().stream().<Criterion<?>>map(SchemaPayloads::criterion).toList()))
                .toList();
    }

    public static List<SectionPayload> fromSections(List<Section> sections) {
        return sections.stream()
                .map(section -> new SectionPayload(section.title(), section.criteria().stream().map(SchemaPayloads::payload).toList()))
                .toList();
    }

    public static CriterionPayload payload(Criterion<?> criterion) {
        return new CriterionPayload(criterion.code(), criterion.text(), ((StandardSeverity) criterion.severity()).name(),
                rulePayload(criterion.rule()),
                criterion.requiredEvidence().stream()
                        .map(r -> new EvidenceRequirementPayload(((StandardEvidenceKind) r.kind()).name(), r.minimum())).toList());
    }

    private static Criterion<?> criterion(CriterionPayload payload) {
        Severity severity = severity(payload.severity());
        List<EvidenceRequirement> evidence = payload.requiredEvidence().stream()
                .map(r -> new EvidenceRequirement(evidenceKind(r.kind()), r.minimum())).toList();
        RulePayload rule = payload.rule();
        return switch (rule.type()) {
            case "numeric-range" -> new Criterion<>(payload.code(), payload.text(),
                    new NumericRangeRule(new Unit(rule.unit()), new Range(rule.minimum(), rule.maximum())), severity, evidence);
            case "yes-no" -> new Criterion<>(payload.code(), payload.text(), new YesNoRule(YesNoAnswer.valueOf(rule.expected())),
                    severity, evidence);
            case "option-in-list" -> new Criterion<>(payload.code(), payload.text(), new OptionInListRule(rule.options()), severity,
                    evidence);
            default -> throw new InvalidRuleConfigurationException("Unknown rule type " + rule.type());
        };
    }

    private static RulePayload rulePayload(ApprovalRule<?> rule) {
        return switch (rule) {
            case NumericRangeRule numeric -> new RulePayload("numeric-range", numeric.unit().symbol(), numeric.range().minimum(),
                    numeric.range().maximum(), null, null);
            case YesNoRule yesNo -> new RulePayload("yes-no", null, null, null, yesNo.expected().name(), null);
            case OptionInListRule option -> new RulePayload("option-in-list", null, null, null, null, option.acceptedOptions());
            default -> throw new IllegalStateException("No payload for rule " + rule.getClass().getName());
        };
    }

    private static Severity severity(String name) {
        return Arrays.stream(StandardSeverity.values()).filter(s -> s.name().equals(name)).findFirst()
                .orElseThrow(() -> new InvalidSchemaException("Unknown severity " + name));
    }

    private static EvidenceKind evidenceKind(String name) {
        return Arrays.stream(StandardEvidenceKind.values()).filter(k -> k.name().equals(name)).findFirst()
                .orElseThrow(() -> new InvalidSchemaException("Unknown evidence kind " + name));
    }
}
```

Si `InvalidSchemaException` o `InvalidRuleConfigurationException` no tienen constructor `(String)` público, agregárselo.

`VersionResponse` (`@Relation(collectionRelation = "versions")`, extiende `RepresentationModel`) con `number`, `publishedAt`, `sections` (`List<SectionPayload>`). `SchemaResponse` con `name`, `assetType`, `currentVersion` y link a `versions`.

`SchemaController` (`/api/v1/schemas`):
- `POST` → `createInspectionSchema.execute(new CreateInspectionSchemaCommand(request.name(), new AssetType(request.assetType()), SchemaPayloads.toSections(request.sections()), person))`; 201 con `Location` = link `self` del esquema (`linkTo(methodOn(SchemaController.class).find(assetType))`, que codifica el espacio como `%20`).
- `GET /{assetType}` → `SchemaResponse` con links `self`, `versions`, `current-version`, `audit-entries`.
- `POST /{assetType}/versions` → `PublishSchemaVersionCommand`; 201 con `Location` de la versión nueva y su `VersionResponse`.
- `GET /{assetType}/versions` → `CollectionModel<VersionResponse>`.
- `GET /{assetType}/versions/{number}` → busca la versión por número en `schema.versions()` (404 con `NoInspectionSchemaForAssetTypeException` si el esquema no existe; si el número no existe, `ResponseEntity.notFound()`); responde con ETag y caché:

```java
    @GetMapping("/{assetType}/versions/{number}")
    public ResponseEntity<VersionResponse> version(@PathVariable String assetType, @PathVariable int number, WebRequest request) {
        String etag = "\"" + assetType + "-v" + number + "\"";
        if (request.checkNotModified(etag)) {
            return null;
        }
        return findSchema.execute(new AssetType(assetType)).versions().stream()
                .filter(version -> version.number() == number)
                .findFirst()
                .map(version -> ResponseEntity.ok()
                        .eTag(etag)
                        .cacheControl(CacheControl.maxAge(Duration.ofDays(1)))
                        .body(versionResource(assetType, version)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
```

Una versión publicada no cambia (S1), así que el ETag por número es estable.
- `GET /{assetType}/audit-entries` → `AuditSubject.OfSchema`.

- [ ] **Step 5: Ver que pasa y commitear**

Run: `mvn -q clean verify`
Expected: verde.

```bash
git add -A
git commit -m "Expose assets and schemas as REST resources"
```

---

### Task 13: API de inspecciones

**Files:**
- Create en `certiflow-web/src/main/java/ar/edu/itba/certiflow/web/inspection/`:
  `AssignmentRequest.java`, `AnswerRequest.java`, `EvidenceRequest.java`, `ObservationRequest.java`, `RectificationRequest.java`, `InspectionResponse.java`, `CriterionResponseResource.java`, `RevisionResource.java`, `InspectionActResource.java`, `EligibilityResource.java`, `InspectionController.java`, `RawAnswers.java`
- Test: `certiflow-web/src/test/java/ar/edu/itba/certiflow/web/inspection/InspectionApiTest.java`

**Interfaces:**
- Consumes: `AssignInspectionUseCase`, `FindInspectionUseCase`, `RecordAnswerUseCase`, `AttachEvidenceUseCase`, `AddObservationUseCase`, `CloseInspectionUseCase`, `RectifyInspectionUseCase`, `GenerateInspectionActUseCase`, `EvaluateCertificationUseCase`, `AuditHistoryUseCase`; `SchemaPayloads.payload` (T12); `AuditEntryResponse`, `@ActingPerson`.
- Produces:
  - `RawAnswers.from(JsonNode value): RawAnswer`: número → `Number` (con `decimalValue()`, que conserva la escala), booleano → `YesNo`, texto → `Option`; nulo u otro tipo → `AnswerTypeMismatchException`. `AnswerRequest(JsonNode value)`.
  - `RawAnswers.toJson(RawAnswer)` para mostrar la respuesta.
  - Rutas y links de §6.2 del spec para inspecciones.

Para que `{"value": 7.00}` llegue como `7.00`, activar en Jackson `USE_BIG_DECIMAL_FOR_FLOATS` (propiedad `spring.jackson.deserialization.use-big-decimal-for-floats=true` en `application.properties` de main), y escribir decimales como números sin notación científica.

- [ ] **Step 1: Tests que fallan**

```java
package ar.edu.itba.certiflow.web.inspection;

import static org.hamcrest.Matchers.endsWith;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.itba.certiflow.web.support.ApiTest;
import ar.edu.itba.certiflow.web.support.Payloads;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class InspectionApiTest extends ApiTest {

    private static final String ASSIGNMENT = """
            {"assetCode":"EXT-0042","inspector":{"id":"EMP-002","name":"Ines"},"plannedDate":"2026-12-01","sections":null}""";

    @Autowired
    private JdbcTemplate jdbc;

    private String inspection;

    @BeforeEach
    void assignInspection() throws Exception {
        postAs("EMP-001", "/api/v1/assets", Payloads.asset());
        postAs("EMP-001", "/api/v1/schemas", Payloads.schema());
        String location = postAs("EMP-001", "/api/v1/inspections", ASSIGNMENT)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");
        inspection = location.substring(location.indexOf("/api/v1/inspections/"));
    }

    private void answerAll(String pressure) throws Exception {
        putAs("EMP-002", inspection + "/responses/EXT-01/answer", "{\"value\":" + pressure + "}").andExpect(status().isNoContent());
        postAs("EMP-002", inspection + "/responses/EXT-01/evidence",
                "{\"kind\":\"PHOTO\",\"reference\":\"photos/gauge.jpg\",\"description\":\"Gauge\"}").andExpect(status().isCreated());
        putAs("EMP-002", inspection + "/responses/EXT-02/answer", "{\"value\":true}").andExpect(status().isNoContent());
        putAs("EMP-002", inspection + "/responses/EXT-03/answer", "{\"value\":\"VISIBLE\"}").andExpect(status().isNoContent());
    }

    @Test
    void anOpenInspectionOffersItsResponsesAndClosureButNotRectifications() throws Exception {
        read(inspection)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.closed").value(false))
                .andExpect(jsonPath("$._links.responses.href").exists())
                .andExpect(jsonPath("$._links.closure.href").exists())
                .andExpect(jsonPath("$._links.rectifications").doesNotExist())
                .andExpect(jsonPath("$._links.act").doesNotExist());
    }

    @Test
    void answeringIsIdempotentAndKeepsTheDecimalScale() throws Exception {
        putAs("EMP-002", inspection + "/responses/EXT-01/answer", "{\"value\":7.00}").andExpect(status().isNoContent());
        putAs("EMP-002", inspection + "/responses/EXT-01/answer", "{\"value\":7.00}").andExpect(status().isNoContent());

        read(inspection + "/responses/EXT-01")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value(7.00))
                .andExpect(jsonPath("$.outcome").value("PENDING"));
    }

    @Test
    void closingCreatesTheFirstRevisionAndLinksTheFindingsItRaised() throws Exception {
        answerAll("5.00");

        postAs("EMP-002", inspection + "/closure", "{}")
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith(inspection + "/revisions/1")))
                .andExpect(jsonPath("$.number").value(1))
                .andExpect(jsonPath("$._links.findings").exists());

        read(inspection)
                .andExpect(jsonPath("$.closed").value(true))
                .andExpect(jsonPath("$._links.closure").doesNotExist())
                .andExpect(jsonPath("$._links.rectifications.href").exists())
                .andExpect(jsonPath("$._links.act.href").exists());
        read(inspection + "/revisions/1").andExpect(status().isOk()).andExpect(header().exists("ETag"));
    }

    @Test
    void closingWithPendingCriteriaIs409AndLeavesNoAuditTrace() throws Exception {
        Integer before = jdbc.queryForObject("select count(*) from audit_entry", Integer.class);

        postAs("EMP-002", inspection + "/closure", "{}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("InspectionIncompleteException"));

        org.junit.jupiter.api.Assertions.assertEquals(before, jdbc.queryForObject("select count(*) from audit_entry", Integer.class));
    }

    @Test
    void anAnswerOfTheWrongTypeIs400() throws Exception {
        putAs("EMP-002", inspection + "/responses/EXT-01/answer", "{\"value\":\"abc\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("AnswerTypeMismatchException"));
        putAs("EMP-002", inspection + "/responses/EXT-02/answer", "{\"value\":5}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void anUnknownCriterionCodeIs404() throws Exception {
        putAs("EMP-002", inspection + "/responses/EXT-99/answer", "{\"value\":true}").andExpect(status().isNotFound());
    }

    @Test
    void rectifyingCreatesTheNextRevisionAndTheActIsAvailable() throws Exception {
        answerAll("7.00");
        postAs("EMP-002", inspection + "/closure", "{}");

        postAs("EMP-001", inspection + "/rectifications", "{\"criterionCode\":\"EXT-03\",\"value\":\"REFLECTIVE\",\"reason\":\"Typo\"}")
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith(inspection + "/revisions/2")));

        read(inspection + "/act")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lines.length()").value(3))
                .andExpect(jsonPath("$.lines[0].answer").value(7.00));
        read(inspection + "/revisions").andExpect(jsonPath("$._embedded.revisions.length()").value(2));
    }

    @Test
    void theActOfAnOpenInspectionIs409() throws Exception {
        read(inspection + "/act").andExpect(status().isConflict());
    }

    @Test
    void eligibilityExplainsWhyAnOpenInspectionCannotBeCertified() throws Exception {
        read(inspection + "/certification-eligibility")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eligible").value(false))
                .andExpect(jsonPath("$.ineligibilities[0].reason").value("INSPECTION_NOT_CLOSED"));
    }

    @Test
    void historyListsEveryStep() throws Exception {
        answerAll("7.00");

        read(inspection + "/audit-entries").andExpect(jsonPath("$._embedded.auditEntries.length()").value(5));
    }
}
```

`com.jayway.jsonpath.JsonPath` se borra si no se usa.

Run: `mvn -q -pl certiflow-web -am test -Dtest=InspectionApiTest 2>&1 | grep -m3 "FAIL\|ERROR"`
Expected: fallan (404 por falta de controlador).

- [ ] **Step 2: Implementar requests y `RawAnswers`**

```java
package ar.edu.itba.certiflow.web.inspection;

import ar.edu.itba.certiflow.schema.model.AnswerTypeMismatchException;
import ar.edu.itba.certiflow.schema.model.RawAnswer;
import java.math.BigDecimal;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;

final class RawAnswers {

    private RawAnswers() {
    }

    static RawAnswer from(JsonNode value) {
        if (value != null && value.isNumber()) {
            return new RawAnswer.Number(value.decimalValue());
        }
        if (value != null && value.isBoolean()) {
            return new RawAnswer.YesNo(value.booleanValue());
        }
        if (value != null && value.isString()) {
            return new RawAnswer.Option(value.stringValue());
        }
        throw new AnswerTypeMismatchException("number, boolean or text", new RawAnswer.Option(String.valueOf(value)));
    }

    static JsonNode toJson(RawAnswer raw) {
        return switch (raw) {
            case RawAnswer.Number(BigDecimal number) -> JsonNodeFactory.instance.numberNode(number);
            case RawAnswer.YesNo(boolean yes) -> JsonNodeFactory.instance.booleanNode(yes);
            case RawAnswer.Option(String option) -> JsonNodeFactory.instance.stringNode(option);
        };
    }
}
```

En Jackson 2 los nombres son `com.fasterxml.jackson.databind.JsonNode`, `isTextual()`, `textValue()` y `textNode(...)`.

Requests (records con chequeo de nulos):
- `AssignmentRequest(String assetCode, PersonPayload inspector, LocalDate plannedDate, Set<String> sections)` → `AssignInspectionCommand(new AssetCode(assetCode), inspector.toDomain(), plannedDate, sections == null ? new FullScope() : new SectionsScope(sections), person)`.
- `AnswerRequest(JsonNode value)`.
- `EvidenceRequest(String kind, String reference, String description)` → `new Evidence(StandardEvidenceKind.valueOf(kind), reference, description)` (un `kind` desconocido → `InvalidSchemaException`, 400; usar el mismo patrón que `SchemaPayloads.evidenceKind`, haciéndolo `public`).
- `ObservationRequest(String text)`.
- `RectificationRequest(String criterionCode, JsonNode value, String reason)`.

`PersonPayload` se mueve de `AssetRequest` a `web.support.PersonPayload(String id, String name)` con `Person toDomain()`, y lo usan las dos.

- [ ] **Step 3: Implementar recursos**

- `InspectionResponse` (`@Relation(collectionRelation = "inspections")`): `id`, `assetCode`, `inspectorId`, `inspectorName`, `plannedDate`, `schemaVersion` (número), `sections` (`null` si es completa), `closed`, `coversWholeSchema`. Links:
  - siempre: `self`, `asset` (`/api/v1/assets/{code}`), `schema-version` (`/api/v1/schemas/{type}/versions/{n}` — el tipo se obtiene del activo con `FindAssetUseCase`), `responses`, `revisions`, `certification-eligibility`, `audit-entries`;
  - abierta: `closure`;
  - cerrada: `rectifications`, `act`, `original-revision`.
- `CriterionResponseResource` (`@Relation(collectionRelation = "responses")`): `criterion` (`CriterionPayload`), `answer` (`JsonNode`, `null` si no hay), `answeredById`, `answeredAt`, `evidence` (lista de `{kind, reference, description, attachedById, attachedAt}`), `observations` (lista de `{text, observedById, observedAt}`), `outcome`. Links: `self`, `inspection`; si la inspección está abierta: `answer` (PUT), `evidence` (POST), `observations` (POST).
- `RevisionResource` (`@Relation(collectionRelation = "revisions")`): `number`, `reason`, `sealedById`, `sealedAt`, `outcomes` (mapa código → `Outcome`). Links: `self`, `inspection`.
- `InspectionActResource`: los campos de `InspectionAct` con `lines` = `{criterionCode, criterionText, answer (JsonNode), answeredById, answeredAt, evidenceCount, observations, outcome}`, `revisions` (números) e `history` (`AuditEntryResponse`).
- `EligibilityResource`: `eligible`, `ineligibilities` (`{reason, detail}`).

- [ ] **Step 4: Implementar el controlador**

`InspectionController` (`/api/v1/inspections`):

| Método | Ruta | Cuerpo de la implementación |
|---|---|---|
| `POST` | `` | `InspectionId id = assignInspection.execute(request.toCommand(person));` → 201, `Location` = self, cuerpo `InspectionResponse` |
| `GET` | `/{id}` | `findInspection.execute(new InspectionId(id))` → `InspectionResponse` |
| `GET` | `/{id}/responses` | `CollectionModel` de `CriterionResponseResource` sobre `view.evaluate().responses()` |
| `GET` | `/{id}/responses/{code}` | busca por `response.criterion().code()`; si no está, lanza `CriterionNotInSchemaException(code)` (→ 404) |
| `PUT` | `/{id}/responses/{code}/answer` | `recordAnswer.execute(new RecordAnswerCommand(id, code, RawAnswers.from(request.value()), person))` → 204 |
| `POST` | `/{id}/responses/{code}/evidence` | `attachEvidence.execute(...)` → 201, `Location` = `/{id}/responses/{code}`, cuerpo `CriterionResponseResource` actualizado |
| `POST` | `/{id}/responses/{code}/observations` | `addObservation.execute(...)` → 201, igual |
| `POST` | `/{id}/closure` | `List<FindingId> raised = closeInspection.execute(...)`; carga la vista; responde 201 con `Location` = `/{id}/revisions/1` y `RevisionResource` de la revisión 1 con un link `findings` por cada id (`/api/v1/findings/{findingId}`, `withRel("findings")`) |
| `POST` | `/{id}/rectifications` | igual con `RectifyInspectionCommand`; `Location` = `/{id}/revisions/{n}` con `n = view.revisions().size()` |
| `GET` | `/{id}/revisions` | `CollectionModel` de `RevisionResource` |
| `GET` | `/{id}/revisions/{n}` | ETag `"inspection-{id}-r{n}"` + `checkNotModified` como en esquemas; 404 si no existe |
| `GET` | `/{id}/act` | `generateInspectionAct.execute(id)` (lanza `InspectionNotClosedException` → 409) |
| `GET` | `/{id}/certification-eligibility` | `evaluateCertification.execute(id)` |
| `GET` | `/{id}/audit-entries` | `AuditSubject.OfInspection` |

Antes de escribir los recursos, leer `InspectionAct` y `ActLine` para tomar los campos tal cual. `Outcome` se serializa por `name()`.

- [ ] **Step 5: Ver que pasa y commitear**

Run: `mvn -q clean verify`
Expected: verde.

```bash
git add -A
git commit -m "Expose inspections, their responses, closure, rectifications and act as REST resources"
```

---

### Task 14: API de hallazgos y certificados, y flujo de punta a punta

**Files:**
- Create en `certiflow-web/src/main/java/ar/edu/itba/certiflow/web/inspection/`: `FindingResource.java`, `CorrectiveActionResource.java`, `CorrectiveActionRequest.java`, `VerificationRequest.java`, `FindingsSummaryResource.java`, `FindingController.java`
- Create en `certiflow-web/src/main/java/ar/edu/itba/certiflow/web/certification/`: `CertificateRequest.java`, `SuspensionRequest.java`, `RenewalRequest.java`, `CertificateResource.java`, `CertificateController.java`
- Test: `certiflow-web/src/test/java/ar/edu/itba/certiflow/web/inspection/FindingApiTest.java`, `web/certification/CertificateApiTest.java`, `web/EndToEndApiTest.java`

**Interfaces:**
- Consumes: `ListFindingsUseCase`, `FindFindingUseCase`, `PlanCorrectiveActionUseCase`, `VerifyCorrectiveActionUseCase`, `GenerateFindingsSummaryUseCase`, `IssueCertificateUseCase`, `ListCertificatesUseCase`, `FindCertificateUseCase`, `GenerateCertificateDocumentUseCase`, `SuspendCertificateUseCase`, `RenewCertificateUseCase`, `AuditHistoryUseCase`, `Clock` (para el estado del certificado hoy).
- Produces: rutas y links de §6.2 del spec para hallazgos y certificados.

Links por estado:
- Hallazgo abierto: `self`, `inspection`, `asset`, `corrective-actions` (POST), `audit-entries`. Cerrado: sin `corrective-actions`.
- Acción abierta: `self`, `finding`, `verifications` (POST). Cerrada o cancelada: sin `verifications`.
- Certificado `ACTIVE`: `self`, `asset`, `inspection`, `suspensions`, `renewals`, `audit-entries`. `SUSPENDED`: sin `suspensions`, con `renewals`. `EXPIRED`: con `renewals`. `RENEWED`: ni `suspensions` ni `renewals`. El estado sale de `certificate.statusOn(LocalDate.now(clock))`.

- [ ] **Step 1: Tests que fallan**

```java
package ar.edu.itba.certiflow.web;

import static org.hamcrest.Matchers.endsWith;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.itba.certiflow.web.support.ApiTest;
import ar.edu.itba.certiflow.web.support.Payloads;
import com.jayway.jsonpath.JsonPath;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class EndToEndApiTest extends ApiTest {

    private String href(String json, String path) {
        String href = JsonPath.read(json, path);
        return href.substring(href.indexOf("/api/v1"));
    }

    private String inspectionAnswered(String pressure) throws Exception {
        String assigned = postAs("EMP-001", "/api/v1/inspections", """
                {"assetCode":"EXT-0042","inspector":{"id":"EMP-002","name":"Ines"},"plannedDate":"2026-12-01","sections":null}""")
                .andReturn().getResponse().getContentAsString();
        String inspection = href(assigned, "$._links.self.href");
        putAs("EMP-002", inspection + "/responses/EXT-01/answer", "{\"value\":" + pressure + "}");
        postAs("EMP-002", inspection + "/responses/EXT-01/evidence", "{\"kind\":\"PHOTO\",\"reference\":\"g.jpg\",\"description\":\"G\"}");
        putAs("EMP-002", inspection + "/responses/EXT-02/answer", "{\"value\":true}");
        putAs("EMP-002", inspection + "/responses/EXT-03/answer", "{\"value\":\"VISIBLE\"}");
        return inspection;
    }

    @Test
    void anAssetGoesFromInspectionToCertificateFollowingTheLinks() throws Exception {
        postAs("EMP-001", "/api/v1/assets", Payloads.asset());
        postAs("EMP-001", "/api/v1/schemas", Payloads.schema());
        String inspection = inspectionAnswered("5.00");

        String closure = postAs("EMP-002", inspection + "/closure", "{}").andReturn().getResponse().getContentAsString();
        String finding = href(closure, "$._links.findings.href");

        read(inspection + "/certification-eligibility")
                .andExpect(jsonPath("$.ineligibilities[0].reason").value("BLOCKING_FINDINGS"));
        postAs("EMP-006", "/api/v1/certificates", "{\"inspectionId\":\"" + inspection.substring(inspection.lastIndexOf('/') + 1)
                + "\",\"validUntil\":\"" + LocalDate.now().plusYears(1) + "\"}")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.ineligibilities[0].reason").value("BLOCKING_FINDINGS"));

        String findingJson = read(finding).andExpect(jsonPath("$.severity").value("MAJOR")).andReturn().getResponse().getContentAsString();
        String action = postAs("EMP-001", href(findingJson, "$._links.corrective-actions.href"),
                "{\"description\":\"Recharge\",\"responsible\":{\"id\":\"EMP-004\",\"name\":\"Marta\"},\"dueDate\":\""
                        + LocalDate.now().plusDays(10) + "\"}")
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");
        postAs("EMP-005", action.substring(action.indexOf("/api/v1")) + "/verifications",
                "{\"result\":\"ACCEPTED\",\"notes\":\"Recharged\"}")
                .andExpect(status().isCreated());
        read(finding).andExpect(jsonPath("$.open").value(false)).andExpect(jsonPath("$._links.corrective-actions").doesNotExist());

        String second = inspectionAnswered("7.00");
        postAs("EMP-002", second + "/closure", "{}");
        String certificate = postAs("EMP-006", "/api/v1/certificates", "{\"inspectionId\":\""
                + second.substring(second.lastIndexOf('/') + 1) + "\",\"validUntil\":\"" + LocalDate.now().plusYears(1) + "\"}")
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/v1/certificates/CERT-00001")))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$._links.suspensions.href").exists())
                .andReturn().getResponse().getHeader("Location");

        read("/api/v1/assets/EXT-0042/findings-summary").andExpect(status().isOk());
        read(certificate.substring(certificate.indexOf("/api/v1")) + "/audit-entries")
                .andExpect(jsonPath("$._embedded.auditEntries[0].action").value("Issued"));
    }
}
```

`status().isUnprocessableContent()` puede llamarse `isUnprocessableEntity()` según la versión; usar el que exista. Ajustar `"Issued"` a la etiqueta real de `CertificateAudit.ISSUED`.

`CertificateApiTest` cubre (cada uno un `@Test`, con un activo, esquema e inspección cerrada conforme preparados en `@BeforeEach` igual que arriba):
- suspender: `POST {cert}/suspensions` `{"reason":"Seal tampered"}` → 201; `GET {cert}` → `status` `SUSPENDED`, sin link `suspensions`, con `renewals`; suspender otra vez → 409 `CertificateNotActiveException`.
- renovar: segunda inspección conforme cerrada; `POST {cert}/renewals` `{"inspectionId":"…","validUntil":"…"}` → 201, `Location` del número nuevo; `GET` del viejo → `status` `RENEWED`, sin `suspensions` ni `renewals`.
- emitir dos veces para el mismo activo → 409 `AssetAlreadyCertifiedException`.
- `GET /api/v1/certificates?asset=EXT-0042` → paginado con un elemento.
- `GET /api/v1/certificates/CERT-99999` → 404.

`FindingApiTest` cubre:
- `GET /api/v1/findings?asset=EXT-0042` → paginado.
- planificar con fecha pasada → 409 `DueDateInThePastException`.
- verificar por el mismo responsable de la acción → 409 `VerifierMustDifferFromResponsibleException`.
- verificar `REJECTED` sin notas → 409 `VerificationNotesRequiredException`.
- acción inexistente → 404.

Run: `mvn -q -pl certiflow-web -am test 2>&1 | grep -m3 "FAIL\|ERROR"`
Expected: fallan con 404.

- [ ] **Step 2: Implementar hallazgos**

`FindingResource` (`@Relation(collectionRelation = "findings")`): `id`, `inspectionId`, `assetCode`, `criterion` (`CriterionPayload`), `severity` (`name()` del `StandardSeverity`), `responsibleId`, `responsibleName`, `open`, `blocksCertification`, `evidence`, `actions` (lista de `CorrectiveActionResource`).
`CorrectiveActionResource`: `id` (número), `description`, `responsibleId`, `dueDate`, `plannedById`, `plannedAt`, `open`, `closed`, `cancelled`, `overdue` (con `isOverdueOn(LocalDate.now(clock))`), `verifications` (`{verifierId, result, notes, verifiedAt}`).
`CorrectiveActionRequest(String description, PersonPayload responsible, LocalDate dueDate)`; `VerificationRequest(String result, String notes)` (`VerificationResult.valueOf`; un valor desconocido → `RequiredValueMissingException("Unknown verification result …")` → 400).
`FindingsSummaryResource`: los campos de `FindingsSummary` y `FindingLine` tal cual (leer esas clases antes).

`FindingController`:
- `GET /api/v1/findings?asset=&page=&size=` → `PageLinks.page`.
- `GET /api/v1/findings/{id}`.
- `POST /api/v1/findings/{id}/corrective-actions` → 201, `Location` `/api/v1/findings/{id}/corrective-actions/{n}`, cuerpo `CorrectiveActionResource`.
- `GET /api/v1/findings/{id}/corrective-actions/{n}` → busca en `finding.actions()` por `id()`; si no está, `CorrectiveActionNotInFindingException` (→ 404).
- `POST /api/v1/findings/{id}/corrective-actions/{n}/verifications` → 201, `Location` = la acción, cuerpo `CorrectiveActionResource` actualizado.
- `GET /api/v1/findings/{id}/audit-entries`.
- `GET /api/v1/assets/{code}/findings-summary` → `generateFindingsSummary.execute(new AssetCode(code))`.

- [ ] **Step 3: Implementar certificados**

`CertificateResource` (`@Relation(collectionRelation = "certificates")`): `number`, `assetCode`, `inspectionId`, `validFrom`, `validUntil`, `issuedById`, `issuedAt`, `status`, `suspensions` (`{reason, suspendedById, suspendedAt}`), construido con `CertificateResource.of(Certificate certificate, LocalDate today)`. El documento imprimible (`CertificateDocument`) es otro recurso: `GET /{number}/document`.
`CertificateRequest(String inspectionId, LocalDate validUntil)`, `SuspensionRequest(String reason)`, `RenewalRequest(String inspectionId, LocalDate validUntil)`.

`CertificateController` (`/api/v1/certificates`):
- `POST` → `IssueCertificateCommand`; 201, `Location` `/api/v1/certificates/{number}`.
- `GET ?asset=&page=&size=` → paginado.
- `GET /{number}` → `findCertificate.execute(...)`.
- `GET /{number}/document` → `generateCertificateDocument.execute(...)`, con los campos de `CertificateDocument` tal cual (leer la clase antes).
- `POST /{number}/suspensions` → 201, `Location` = el certificado, cuerpo `CertificateResource` actualizado.
- `POST /{number}/renewals` → 201, `Location` del certificado nuevo, cuerpo su `CertificateResource`.
- `GET /{number}/audit-entries`.


- [ ] **Step 4: Ver que pasa y commitear**

Run: `mvn -q clean verify`
Expected: verde.

```bash
git add -A
git commit -m "Expose findings, corrective actions and certificates as REST resources with state-driven links"
```

---

### Task 15: Tests parametrizados en el dominio y los casos de uso

**Files:**
- Modify: `certiflow-domain/src/test/java/ar/edu/itba/certiflow/schema/model/NumericRangeRuleTest.java`, `YesNoRuleTest.java`, `OptionInListRuleTest.java`, `CriterionTest.java`
- Modify: `certiflow-application/src/test/java/ar/edu/itba/certiflow/application/CorrectiveActionTest.java`, `CertificationTest.java`
- Modify: `certiflow-domain/pom.xml` si `junit-jupiter-params` no llega por `junit-jupiter` (llega: el agregador lo incluye)

**Interfaces:** ninguna nueva.

Conversiones (el comportamiento probado no cambia; cada caso pasa a ser una fila):

- [ ] **Step 1: `NumericRangeRuleTest`**

Reemplazar el test de límites por:

```java
    @ParameterizedTest(name = "{0} bar satisfies [6.00, 8.00]: {1}")
    @CsvSource({"5.99,false", "6.00,true", "7.00,true", "8.00,true", "8.01,false", "6,true", "8.000,true"})
    void rangeLimitsAreInclusiveAndAnythingBeyondThemFails(String value, boolean satisfied) {
        NumericRangeRule rule = new NumericRangeRule(new Unit("bar"), new Range(new BigDecimal("6.00"), new BigDecimal("8.00")));

        assertEquals(satisfied, rule.isSatisfiedBy(new NumericAnswer(new BigDecimal(value))));
    }
```

Leer el test actual antes y conservar todos los valores que ya probaba.

- [ ] **Step 2: Severidad → resultado**

En `CriterionTest`:

```java
    @ParameterizedTest
    @CsvSource({"MINOR,OBSERVED,false", "MAJOR,REJECTED,true", "CRITICAL,REJECTED,true"})
    void anUnmetCriterionTakesTheOutcomeAndBlockingOfItsSeverity(StandardSeverity severity, Outcome outcome, boolean blocks) {
        Criterion<YesNoAnswer> criterion = new Criterion<>("C-1", "Seal", new YesNoRule(YesNoAnswer.YES), severity, List.of());

        assertEquals(outcome, criterion.outcomeOf(YesNoAnswer.NO));
        assertEquals(Outcome.APPROVED, criterion.outcomeOf(YesNoAnswer.YES));
        assertEquals(blocks, severity.blocksCertification());
    }
```

- [ ] **Step 3: Vencimientos**

En `CorrectiveActionTest`, reemplazar `actionIsNotOverdueOnItsDueDateButIsTheDayAfter` por:

```java
    @ParameterizedTest(name = "{0} days after the due date: overdue={1}")
    @CsvSource({"-1,false", "0,false", "1,true", "30,true"})
    void actionIsOverdueOnlyAfterItsDueDate(int daysAfterDueDate, boolean overdue) {
        FindingId finding = fixture.majorFindingOf(fixture.assignedInspection());
        LocalDate dueDate = CertiflowFixture.TODAY.plusDays(5);
        CorrectiveActionId action = fixture.plan(finding, "Recharge", fixture.maintenanceTechnician, dueDate, fixture.planner);

        assertEquals(overdue, fixture.action(finding, action).isOverdueOn(dueDate.plusDays(daysAfterDueDate)));
    }
```

En `CertificationTest`, reemplazar `certificateIsActiveThroughItsLastDayAndExpiredTheDayAfter` por un `@ParameterizedTest` con `@CsvSource({"-1,ACTIVE", "0,ACTIVE", "1,EXPIRED"})` sobre `validUntil.plusDays(n)` y `CertificateStatus`.

- [ ] **Step 4: Verificar y commitear**

Run: `mvn -q clean verify && grep -rl "@ParameterizedTest" --include=*.java certiflow-*/src/test | wc -l`
Expected: verde; al menos 7 archivos.

```bash
git add -A
git commit -m "Turn repeated boundary checks into parameterized tests"
```

---

### Task 16: CI, base local y protección de rama

**Files:**
- Create: `.github/workflows/ci.yml`
- Create: `docker-compose.yml`

**Interfaces:** ninguna.

- [ ] **Step 1: Workflow**

```yaml
name: CI

on:
  push:
    branches: [main, entrega-2]
  pull_request:
    branches: [main]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '25'
          cache: maven
      - run: mvn -B clean verify
```

- [ ] **Step 2: Base local**

```yaml
services:
  postgres:
    image: postgres:17
    environment:
      POSTGRES_DB: certiflow
      POSTGRES_USER: certiflow
      POSTGRES_PASSWORD: certiflow
    ports:
      - "5432:5432"
```

- [ ] **Step 3: Probar la app contra PostgreSQL**

Run:
```bash
docker compose up -d
mvn -q -DskipTests install
(mvn -q -pl certiflow-web spring-boot:run > $SCRATCH/app.log 2>&1 &)
until curl -s localhost:8080/api/v1 > /dev/null; do sleep 2; done
curl -s localhost:8080/api/v1
curl -s -X POST localhost:8080/api/v1/assets -H 'Content-Type: application/json' -H 'X-Person-Id: EMP-001' -H 'X-Person-Name: Paula' \
  -d '{"code":"EXT-0042","name":"Lobby extinguisher","type":"Fire extinguisher","location":"Lobby","responsible":{"id":"EMP-003","name":"Raul"},"characteristics":[]}' -i | head -5
pkill -f CertiflowApplication; docker compose down
```

Expected: el `GET` devuelve los `_links`; el `POST` devuelve `HTTP/1.1 201` con `Location`. Si Flyway falla en PostgreSQL por alguna sintaxis, corregir `V1__init.sql` para que funcione en las dos bases y volver a correr `mvn -q clean verify`. No usar `sleep` en primer plano si la herramienta lo bloquea: usar el `until` de arriba.

- [ ] **Step 4: Commit**

```bash
git add .github/workflows/ci.yml docker-compose.yml
git commit -m "Run the build and tests in CI and add a local PostgreSQL"
```

La protección de `main` (*Require status checks to pass*, check `build`) se activa después del primer push, cuando el check existe. No pushear sin pedido del usuario; dejar el comando documentado en el README (tarea 17):

```bash
gh api -X PUT repos/ivovila/TP-Integrador-DPS/branches/main/protection \
  -f required_status_checks.strict=true -f 'required_status_checks.contexts[]=build' \
  -F enforce_admins=false -F required_pull_request_reviews=null -F restrictions=null
```

---

### Task 17: Documentación

**Files:**
- Modify: `DESIGN.md`, `README.md`, `CHANGES.md`, `docs/certiflow-domain.puml`, `docs/specs/2026-10-12-fase-1-ddd-persistencia-api.md` (§4.5)

**Interfaces:** ninguna.

- [ ] **Step 1: DESIGN.md**

- Título: "Certiflow · Decisiones de diseño (Entrega 2)"; el párrafo inicial menciona Spring Boot, JPA y la API, y `mvn clean verify`.
- §2 se reescribe con: contextos (tabla del spec §2.1), context map (mermaid y tabla del spec §2.2), módulos y capas (tabla del spec §3 con los paquetes reales), grafo de dependencias entre contextos verificado con `grep -rh "^import ar.edu.itba.certiflow" certiflow-domain/src/main/java/ar/edu/itba/certiflow/<ctx> | sort -u`.
- Nueva sección **Glosario** con cada término del spec §2.1, su definición y la clase.
- Decisiones nuevas D15–D21, con el formato de las existentes (**Principio**, **Dónde**, **Por qué**, **Alternativas descartadas**, **Costo**):
  - D15 Bounded contexts y context map. Descartado: un contexto propio para hallazgos.
  - D16 ACL `InspectionResult`. Descartado: que Certificación siga leyendo `Finding`.
  - D17 Comandos con códigos y `RawAnswer`; cómo preserva D1. Descartado: `instanceof` de la respuesta en el caso de uso; exponer `Criterion` al cliente.
  - D18 Snapshots (Memento). Descartado: anotar el dominio con `@Entity`; reflexión sobre campos privados.
  - D19 Entidades JPA separadas, versión del esquema y evaluación de una revisión como JSON, `instanceof` con patrón en los adaptadores. Descartado: `SINGLE_TABLE` para reglas.
  - D20 Proxy transaccional genérico. Descartado: `@Transactional` en `application`; un decorador escrito a mano por caso de uso.
  - D21 API REST de nivel 3 (reglas de §6.1 del spec, operaciones como recursos, hipermedia por estado, ETag, paginación, errores RFC 9457). Descartado: endpoints con verbos.
- §4 (patrones no aplicados): agregar CQRS (las consultas usan los mismos repositorios; se aplicaría si las lecturas necesitaran otro modelo) y Event Sourcing sigue igual.
- §5 (riesgo → test): agregar una fila por cada test nuevo de las tareas 2–15, con su nombre real.
- §6 (limitaciones): borrar "Auditar y guardar no son atómicos" y "`AuditLog` indexado por objeto"; agregar: paginación en memoria; severidades y tipos de evidencia solo estándar en persistencia y API; `nextId` con UUID; un esquema por tipo de activo sigue sin índice único concurrente más allá de la PK.
- **Por cada funcionalidad nueva** (requisito de la consigna) no aplica todavía: F1–F3 se documentan en sus specs.

- [ ] **Step 2: README.md**

Título "Entrega 2". Secciones: Cómo correr (`mvn clean verify`; `docker compose up -d` + `mvn -q -DskipTests install` + `mvn -pl certiflow-web spring-boot:run`; variables `CERTIFLOW_DB_*`); Estructura (árbol de los cuatro módulos con sus paquetes por contexto); API (base `/api/v1`, headers de persona, ejemplo con `curl` del alta de un activo); CI y el comando `gh api` de protección de rama; Documentación (DESIGN, specs, plans, clases, consigna).

- [ ] **Step 3: CHANGES.md**

Agregar una sección "Entrega 2 · Fase 1" con una entrada numerada por tarea (1–16): qué cambió, por qué, tests agregados y secciones de DESIGN.md tocadas, como las entradas existentes.

- [ ] **Step 4: Spec y diagrama**

Spec §4.5 y §6.2: reemplazar la tabla de consultas por las reales de la tarea 7 y agregar la ruta `GET /api/v1/certificates/{number}/document`, con la razón: devuelven el agregado, que desde afuera solo se puede leer (D13, D14).

`docs/certiflow-domain.puml`: renombrar los paquetes a `catalog.model`, `schema.model`, `inspection.model`, `certification.model`, `shared.model`; agregar `InspectionResult`, `OpenNonConformity`, `InspectionResultTranslator`, `RawAnswer` y los snapshots.

- [ ] **Step 5: Verificar y commitear**

Run: `mvn -q clean verify && grep -c "^### D" DESIGN.md`
Expected: verde; 21.

```bash
git add -A
git commit -m "Document the DDD architecture, persistence and REST API decisions"
```
