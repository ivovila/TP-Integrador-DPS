# DESIGN.md

Decisiones de diseño del módulo de dominio — Entrega 1.

Plataforma de inspección, habilitación y certificación de activos.

---

## 0. Alcance y premisas

El entregable es únicamente el **módulo de dominio**: modelos, contratos (interfaces) y casos de uso, con tests. No hay API REST, persistencia real, frontend, seguridad ni despliegue.

Esa restricción es una decisión de diseño en sí misma: el dominio **no depende de ninguna tecnología**. No hay anotaciones de frameworks, ni tipos de librerías de terceros en las firmas públicas, ni `java.sql`, ni serialización. Todo lo que el dominio necesita del exterior está declarado como interfaz propia (ver §2, *Ports*).

La única dependencia de compilación es **Lombok** (`@Getter`, alcance `provided`): genera getters en tiempo de compilación y no deja ningún tipo propio en las firmas ni en el classpath de ejecución.

Consecuencia deliberada: cuando en la Entrega 2 se agregue persistencia y API, **ningún archivo del paquete `domain/model` debería cambiar**. Ese es el criterio con el que evaluamos si el diseño fue correcto.

---

## 1. Estructura de paquetes

```
ar.edu.itba.certiflow
├── domain
│   ├── model
│   │   ├── asset          Asset, AssetId, AssetType, Location, Characteristics
│   │   ├── schema         InspectionSchema, SchemaVersion, Section, Criterion,
│   │   │                  SchemaVersionPublished
│   │   ├── inspection     Inspection, InspectionState (+ states/), Rectification,
│   │   │                  InspectionEvaluation, CriterionResult, eventos
│   │   ├── finding        Finding, FindingDetails, Closure, CorrectiveAction, Verification,
│   │   │                  Severity, eventos
│   │   ├── certificate    Certificate, CertificateStatus, ValidityPeriod,
│   │   │                  CertificationPolicy, SeverityCertificationPolicy, eventos
│   │   ├── report         InspectionReport, FindingsSummary, CertificateReport
│   │   ├── audit          AuditLog
│   │   └── shared         PersonId, Response, Measurement, Evidence, EvidenceType,
│   │                      DomainEvent, AggregateRoot, excepciones de dominio
│   ├── rules              CriterionRule, MeasurementRule, implementaciones, CriterionOutcome
│   ├── ports              Repositorios, Clock, EventPublisher
│   └── usecase            Un caso de uso = una clase con execute(...)
└── test
    └── support            Implementaciones in-memory de los ports, Fixtures, TestContext
```

**Principio aplicado:** paquetes por *feature del dominio*, no por capa técnica (`entities/`, `dtos/`, `services/`). Los conceptos que cambian juntos viven juntos, y el acoplamiento entre paquetes queda visible en los imports.

**Alternativa descartada:** el layout clásico `model / service / repository` plano. Con diez agregados terminás con un paquete `service` de veinte clases sin relación entre sí y ninguna pista de dónde está la lógica de un concepto. Lo descartamos por legibilidad y porque oculta los límites reales del dominio.

**Decisión: sin ciclos entre paquetes.** Las dependencias van en un solo sentido:

```
shared ← asset ─┐
shared ← rules ─┴─ schema ← inspection ← finding ← certificate
shared ← audit
inspection, finding, certificate, asset ← report
ports ← usecase        (ports y usecase dependen de todo model; nadie depende de ellos)
```

Para lograrlo, el vocabulario que comparten el esquema y la inspección (`Response`, `Measurement`, `Evidence`, `EvidenceType`) vive en `shared`. Si `Response` estuviera en `inspection`, las reglas (que evalúan una respuesta) dependerían de `inspection`, el esquema dependería de las reglas y la inspección del esquema: un ciclo. Por el mismo motivo `DomainEvent` está en `shared` y cada evento concreto vive junto a su agregado, y la política de certificación vive en `certificate` y no en `rules` (necesita ver hallazgos).

---

## 2. Arquitectura: Ports & Adapters (Hexagonal), parcial

**Dónde:** `domain/ports` declara todo lo que el dominio necesita y no controla.

```java
public interface FindingRepository {
    Optional<Finding> findById(FindingId id);
    void save(Finding finding);
    List<Finding> findByInspection(InspectionId inspectionId);
    List<Finding> findByAsset(AssetId assetId);
}

public interface Clock          { LocalDateTime now(); }
public interface EventPublisher { void publish(DomainEvent event); }
```

Repositorios: `AssetRepository`, `SchemaRepository`, `InspectionRepository`, `FindingRepository` (`findByInspection`, `findByAsset`) y `CertificateRepository` (`findByAsset`).

**Por qué:** invierte la dependencia (DIP). El dominio define el contrato; la infraestructura futura lo implementa. En esta entrega los únicos adaptadores son los in-memory de `test/support`, lo cual es suficiente para probar los casos de uso completos sin base de datos.

**Decisión concreta: `Clock` como port.** Ningún objeto de dominio llama a `LocalDateTime.now()`: los agregados reciben el instante como parámetro y los casos de uso lo obtienen del `Clock`. Las fechas de vencimiento de acciones correctivas y certificados son reglas de negocio centrales; si el tiempo viene de una llamada estática, esas reglas no se pueden testear de forma determinista y aparecen tests que fallan según el día. Con `Clock` inyectado, un test puede parar el reloj o adelantarlo un año en una línea (`FixedClock.advanceDays`).

**Alternativa descartada:** `java.time.Clock` de la JDK. Es perfectamente válido, pero la interfaz propia es más chica (un método), no arrastra `Instant`/`ZoneId` a firmas donde no aportan, y refuerza que el dominio no depende de nada externo. Costo asumido: reinventamos algo que ya existe.

**Alternativa descartada: `IdGenerator` como port.** Los identificadores son Value Objects tipados (`AssetId`, `InspectionId`, `FindingId`, …) que envuelven un `UUID` y se generan con `XxxId.generate()`. Un id tipado evita pasar un `FindingId` donde se espera un `InspectionId`, cosa que un `String newId()` no puede impedir. Los tests nunca dependen del valor concreto de un id, así que no hizo falta hacerlos deterministas. Consecuencia: si la persistencia necesitara ids secuenciales, habrá que introducir el port.

**Repositorios específicos, no genéricos.** No existe `Repository<T, ID>`. Cada repositorio expone solo las consultas que el negocio necesita. Un repositorio genérico con `findAll()` y `delete()` habilita operaciones que el dominio nunca debería permitir (borrar una inspección cerrada, por ejemplo) y filtra decisiones de persistencia hacia arriba.

---

## 3. Modelo rico, no anémico

**Principio aplicado:** encapsulamiento y *Tell, Don't Ask*. El estado de los agregados es privado y solo se modifica a través de métodos que expresan intención del negocio.

```java
inspection.register(criterionId, response);
inspection.close(clock.now());
finding.verifyAction(actionId, verifier, clock.now());
certificate.suspend(reason, clock.now());
```

No hay setters públicos en las entidades. No existe `inspection.setStatus(CLOSED)`. Las colecciones se exponen como copias inmutables. Las entidades internas de un agregado (`CorrectiveAction` dentro de `Finding`) solo cambian a través de la raíz: `CorrectiveAction.verify` es de paquete.

**Por qué:** es la decisión de la que dependen todas las demás. Si el estado es público, las reglas ("una inspección cerrada no se modifica", "un certificado suspendido no se renueva") se dispersan entre los casos de uso y hay que confiar en que nadie se olvide. Encapsuladas en el agregado, se cumplen por construcción.

**Alternativa descartada — y es la descartada más importante de este documento:** el modelo anémico (entidades con getters/setters + una capa de servicios que hace todo). Es el camino de menor resistencia y el que vuelve irrelevante la calidad del modelado. Consecuencia de haberlo evitado: los casos de uso quedan casi vacíos (buscar, invocar, guardar, publicar eventos), lo cual es correcto pero exige disciplina para no "ayudar un poquito" desde ellos.

---

## 4. Versionado de esquemas: snapshot inmutable

Requisito: *"Una inspección debe conservar las reglas que estaban vigentes cuando fue iniciada, aun cuando posteriormente se publique una versión nueva."*

**Decisión:** `InspectionSchema` es la plantilla editable y la única puerta para modificarla: `addSection(name)` y `addCriterion(sectionName, criterion)`, que controla que la sección exista y que el `CriterionId` no se repita en todo el esquema. `Section` es un `record` inmutable; agregar un criterio reemplaza la sección por una nueva, así que nadie puede modificar el esquema a través de una sección obtenida con `getSections()`. Editar la plantilla no genera versiones. `publish()` crea una `SchemaVersion` **inmutable** con número correlativo. Al **iniciar** una inspección, la inspección **guarda la `SchemaVersion` completa** vigente en ese momento, no su identificador.

```java
public class Inspection {
    private final SchemaId schemaId;
    private InspectionState state;
    ...
}

public record InProgress(SchemaVersion schema) implements InspectionState { ... }
```

Al asignar solo se conoce el `SchemaId`. La `SchemaVersion` la guarda el estado desde que la inspección se inicia (`InProgress`, `Closed`, `Rectified`); pedir el esquema de una inspección asignada lanza `InvalidTransitionException`.

**Por qué al iniciar y no al asignar:** el enunciado habla de las reglas vigentes *cuando fue iniciada*. Una inspección asignada hoy y comenzada dentro de un mes debe usar la versión publicada en ese momento. Al asignar solo se valida que el esquema aplique al tipo del activo.

**Por qué así y no con un id:** si la inspección guardara `schemaVersionId` y lo resolviera contra un repositorio al evaluar, la inmutabilidad dependería de que nadie modifique nunca esa versión, de que la versión no se borre, y de que la evaluación no se haga en un momento distinto al esperado. Guardando el snapshot, **la regla es una propiedad estructural del modelo**: publicar la v2 no puede afectar a una inspección iniciada con la v1 porque no hay ningún camino de código que las conecte.

**Patrón:** *copy-on-publish* + objetos inmutables (Value Objects).

**Alternativas descartadas:**

| Alternativa | Por qué se descartó |
|---|---|
| Referencia por id a `SchemaVersion` | La inmutabilidad pasa a ser una convención, no una garantía. Es exactamente el bug que la consigna busca. |
| Incrementar la versión con cada edición | Cada cambio intermedio de la plantilla generaba una versión que nadie publicó; las inspecciones podían tomar un esquema a medio editar. |
| `Section` mutable con su propio `addCriterion` | La lista de secciones se copiaba, pero cada sección seguía siendo el mismo objeto: se podía agregar criterios salteando al agregado, y `SchemaVersion` (un `record`) comparaba secciones por identidad. |
| Esquema inmutable construido con Builder | Obliga a reconstruir el esquema entero para agregar un criterio. La plantilla editable + `publish()` separa mejor "diseñar" de "poner en vigencia". |
| Versionado por *soft delete* / flag `activo` sobre un único esquema mutable | Se pierde el histórico real: no se puede reconstruir qué criterios existían en una fecha dada. |
| Event sourcing del esquema | Reconstruir la versión vigente en cada evaluación agrega complejidad sin beneficio en este alcance. |

**Costo asumido:** duplicación de datos (cada inspección carga su copia del esquema). En un modelo persistido esto se resuelve con una tabla de versiones inmutables y una FK; la decisión se mantiene porque el costo es de almacenamiento, no de corrección.

---

## 5. Reglas de evaluación: Strategy + Composite

Requisito: *"Determinación automática de criterios aprobados, observados o rechazados"*, con criterios de distinta naturaleza (mediciones numéricas, respuestas booleanas, opciones, evidencia obligatoria).

```java
public interface CriterionRule {
    CriterionOutcome evaluate(Response response);
}

public interface MeasurementRule extends CriterionRule {
    boolean accepts(Measurement measurement);
}
```

Cada `Criterion` tiene una `CriterionRule`. Implementaciones:

| Regla | Aprobado | Observado | Rechazado |
|---|---|---|---|
| `NumericRangeRule` | medición dentro de `[min, max]` | dentro de la tolerancia | fuera de la tolerancia o sin medición |
| `BooleanRule` | respuesta igual a la esperada | — | respuesta distinta o sin respuesta |
| `EnumOptionRule` | opción aprobada | opción observada | otra opción o sin opción |
| `RequiredEvidenceRule` | están todos los tipos de evidencia exigidos | — | falta alguno |
| `CompositeRule` | el peor resultado de sus reglas | | |

`Response` es lo que el inspector registró para un criterio: medición, respuesta, opción, evidencias y observaciones. Es inmutable y se construye progresivamente (`withMeasurement`, `withEvidence`, …).

La evidencia obligatoria es una regla más: un criterio "cartel visible con foto" es `CompositeRule.allOf(new BooleanRule(true), new RequiredEvidenceRule(Set.of(PHOTO)))`.

**Agregación.** Al cerrar (y al rectificar) la inspección calcula una `InspectionEvaluation` y la guarda en su estado: un `CriterionResult` por criterio de la versión en vigor, con el resultado y la evidencia en la que se basó (un criterio sin respuesta se evalúa contra una respuesta vacía y queda rechazado). El resultado de una sección y el global son el peor resultado de sus criterios. La evaluación queda fija: `getEvaluation()` no recalcula, y hallazgos y certificados trabajan sobre ese resultado.

**Principios aplicados:** Strategy (cada tipo de criterio encapsula su algoritmo), Composite (`CompositeRule` se usa igual que una regla simple), **Open/Closed** (agregar un tipo de criterio es agregar una clase; ningún archivo existente se modifica).

**Por qué:** es el eje de extensibilidad del sistema. Una entidad de certificación agrega tipos de criterio permanentemente. La alternativa natural — un `switch (criterio.getTipo())` dentro de un `EvaluationService` — obliga a tocar y re-testear el mismo método en cada incorporación, y ese método crece sin techo.

**Validación al registrar.** La inspección rechaza, al momento de registrar, una medición que el criterio no evalúa (otra magnitud u otra unidad). Sin eso, un error de carga se descubriría recién al evaluar, como un rechazo silencioso. Esa capacidad vive en `MeasurementRule`, que implementan `NumericRangeRule` y `CompositeRule` (acepta si alguna de sus reglas de medición acepta), y no en `CriterionRule` (**ISP**): `BooleanRule`, `EnumOptionRule` y `RequiredEvidenceRule` no heredan un método que no les corresponde.

**Alternativa descartada — `default boolean accepts(Measurement)` en `CriterionRule`:** obligaba a todas las reglas a cargar con la validación de mediciones, y cada validación nueva al registrar (opciones, evidencias) iba a sumar otro método por defecto a la misma interfaz. Costo asumido de separarla: `Criterion` y `CompositeRule` preguntan `instanceof MeasurementRule`.

**Alternativa descartada — Interpreter / DSL de reglas:** permitiría definir reglas como texto configurable sin recompilar, que es hacia dónde tiende un producto real. Se descartó para esta entrega porque requiere parser, validación y manejo de errores de expresión, y **Strategy+Composite ya cubre todos los tipos previstos**. Consecuencia: cuando el negocio pida reglas configurables por el usuario, habrá que escribir el intérprete; el diseño lo permite sin romper nada, porque el DSL solo necesita producir un `CriterionRule`.

**Alternativa descartada — reglas embebidas en `Criterion`:** ata la definición del criterio a su forma de evaluarlo e impide reutilizar una misma regla en criterios distintos.

**Alternativa descartada — jerarquía polimórfica de respuestas (`Answer` → `YesNoAnswer`, …):** la probamos y obligaba a las reglas a filtrar por tipo con `instanceof`. Un único `Response` con los datos opcionales deja que cada regla lea lo que necesita.

**Alternativa descartada — `SectionRule` / política de aprobación por severidad:** la severidad no es del criterio sino del hallazgo (§8). Sin severidad en el esquema, agregar secciones es tomar el peor resultado y no justifica una estrategia propia; la decisión que sí depende de la severidad es la de certificar, y vive en `CertificationPolicy`.

---

## 6. Ciclo de vida: cada tipo de `if` con su salida

```
Inspection:       Assigned → InProgress → Closed → Rectified (→ Rectified)
Certificate:      ISSUED → SUSPENDED → ISSUED
                  ISSUED | SUSPENDED → EXPIRED
                  ISSUED → RENEWED
Finding:          abierto (sin Closure) → cerrado (con Closure)
CorrectiveAction: planificada (sin Verification) → verificada (con Verification)
```

Toda transición ilegal lanza `InvalidTransitionException` (excepción de dominio propia).

Los condicionales que aparecen alrededor de un ciclo de vida no son todos iguales. Distinguimos tres tipos y cada uno tiene su propia solución; buscar una sola técnica para todos lleva a sobrediseñar.

**Tipo 1 — guardas de transición.** "¿Puedo pasar de este estado a aquel?"

- En `Certificate` la tabla de transiciones vive en el enum: cada constante declara `next()`, el conjunto de estados a los que puede pasar, y `isValid()`. El agregado tiene un único método privado `transitionTo(target, reason, now)` que valida contra la tabla, cambia el estado y registra `CertificateStatusChanged`. `suspend`, `reinstate`, `expire` y `renew` delegan en él. La máquina de estados completa se lee en el enum, hay un solo `if` de transición y el evento se registra en un solo lugar.
- En `Inspection` se aplicó el patrón State (`InspectionState` + `inspection/states`). Ahí lo que varía entre estados no es solo a dónde se puede ir sino **qué operaciones admite**: registrar respuestas solo en curso, consultar la evaluación solo cerrada o rectificada, rectificar solo después del cierre. Cada estado es un `record`, sobrescribe lo que permite y el resto lo rechaza por defecto. Los estados guardan sus propios datos: `InProgress(schema)`, `Closed(schema, evaluation)`, `Rectified(schema, evaluation)`.

**Tipo 2 — datos que solo existen en un estado.** Se modelan como un Value Object opcional en lugar de un enum más campos que quedan en `null`:

- `CorrectiveAction` tiene una `Verification(verifier, verifiedAt)`; está verificada si la tiene.
- `Finding` tiene un `Closure(closedAt)`; está abierto si no lo tiene.
- `Certificate.getRenewedBy()` devuelve `Optional<CertificateId>`.
- En `Inspection` el esquema y la evaluación viven dentro de los estados que los tienen, así que no hay campos que valgan `null` mientras la inspección está asignada o en curso.

El estado *es* el dato, no una etiqueta paralela que hay que mantener sincronizada. La comprobación queda encapsulada en una sola consulta (`isVerified()`, `isOpen()`) que el resto del código usa sin conocer la implementación. Así desaparecieron `ActionStatus` y `FindingStatus`.

**Tipo 3 — reglas de negocio.** "Quien ejecuta la acción no la verifica", "el vencimiento no puede estar en el pasado", "el hallazgo se cierra con todas sus acciones verificadas", "el certificado no vence mientras su vigencia no terminó". Estos `if` se quedan como precondiciones explícitas al principio de cada método (*design by contract*): son el dominio, no deuda técnica.

**Alternativas descartadas:**

- **Patrón State en `Certificate`, `Finding` y `CorrectiveAction`.** Lo evaluamos. En `Finding` y `CorrectiveAction` (dos estados, una transición) cambiaba un `if` por una interfaz y dos clases. En `Certificate` solo se justificaba para guardar el motivo de suspensión dentro de un estado `Suspended`, y nadie lo consulta desde el agregado: el motivo queda en el evento de auditoría. Tampoco mejora OCP: agregar un estado obliga igualmente a modificar la interfaz y los estados desde los que se llega a él.
- **Enum + `requireStatus(...)` en cada método.** Repetía la guarda en cada transición y dispersaba el registro del evento.
- **Estado derivado sin enum en `Certificate`.** Con cuatro estados, deducirlo de campos opcionales (suspensión, renovación, fecha) exigía combinar condiciones en cada consulta.

**Consecuencia asumida:** conviven dos técnicas para las guardas de transición. El criterio es explícito: State cuando varía el conjunto de operaciones permitidas por estado; tabla en el enum cuando solo varían las transiciones válidas. Si el certificado empieza a variar qué operaciones admite en cada estado, conviene migrarlo a State.

---

## 7. Inmutabilidad post-cierre: rectificación auditable

Requisito: *"Una inspección no puede alterarse libremente luego de cerrarse. Las correcciones posteriores deben realizarse mediante una rectificación auditable."*

**Decisión:** una inspección cerrada es de solo lectura (`register` lanza `InvalidTransitionException`). Corregirla no la modifica: agrega una `Rectification` con autor, motivo, fecha y las respuestas corregidas por criterio. La inspección pasa a `Rectified` y conserva sus respuestas originales intactas (`getResponses()`). El resultado efectivo se resuelve como *original + rectificaciones aplicadas en orden* (`effectiveResponses()`); cada rectificación recalcula la evaluación y el estado `Rectified` guarda la nueva.

**Analogía de diseño:** el asiento de ajuste contable. No se borra ni se edita el asiento original; se emite uno nuevo que lo corrige, y ambos quedan en el libro.

**Por qué:** una entidad de certificación necesita poder demostrar qué se registró originalmente y qué se corrigió después. Editar en el lugar destruye esa información, por más que se registre un log paralelo.

**Alternativas descartadas:**

- **Editar y loguear el cambio en una tabla de auditoría.** El log y el dato pueden divergir, y el estado original deja de ser reconstruible desde el modelo.
- **Versionar la inspección entera (copia completa por corrección).** Funciona, pero pierde la intención: no queda explícito *qué* se corrigió ni *por qué*.
- **Event sourcing completo de la inspección.** Da todo esto de forma natural, pero obliga a reconstruir el estado por replay en cada lectura y a versionar los eventos. Desproporcionado para la Entrega 1. Ver §12.

---

## 8. Hallazgos y acciones correctivas

Requisitos: *"Creación de no conformidades con severidad, evidencia y responsable"* y *"Planificación, vencimiento, verificación y cierre"* de acciones correctivas.

**Decisión: la severidad es del hallazgo, no del criterio.** Un mismo criterio puede fallar de forma leve o grave; lo que tiene gravedad es el problema encontrado. `Criterion` no tiene severidad y `Severity` vive en `finding`.

**Cómo nace un hallazgo:** el sistema determina *qué* criterios no aprobaron (evaluación); una persona levanta el hallazgo sobre uno de ellos indicando severidad, descripción y responsable. `Finding.raise(id, evaluation, details, inspectionFindings, now)` recibe la `InspectionEvaluation` fija (no el agregado `Inspection`), los datos que declara la persona (`FindingDetails`: criterio, severidad, descripción, responsable) y los hallazgos ya levantados en esa inspección. Rechaza un criterio aprobado, rechaza un segundo hallazgo para el mismo criterio y copia la evidencia del `CriterionResult`. Las dos reglas viven en el agregado: ningún caso de uso tiene que acordarse de validarlas.

**Finding es un agregado propio**, separado de la inspección: la inspección se cierra y queda inmutable, pero el hallazgo sigue cambiando de estado durante semanas. `CorrectiveAction` es una entidad dentro del agregado `Finding`.

**Reglas:**

- Una acción correctiva no puede vencer en el pasado.
- Quien ejecuta la acción no puede verificarla.
- Una acción está vencida si sigue sin verificar después de su fecha límite.
- Un hallazgo se cierra solo si tiene acciones y todas están verificadas.

**Alternativa descartada — generar los hallazgos automáticamente con la severidad del criterio:** obligaba a fijar la gravedad al diseñar el esquema, sin mirar lo que realmente se encontró.

**Alternativa descartada — validar el duplicado en el caso de uso:** así estaba al principio; cualquier otro camino para levantar hallazgos se salteaba la regla (modelo anémico).

**Alternativa descartada — hallazgo como parte de la inspección:** su ciclo de vida posterior al cierre chocaría con la inmutabilidad de §7.

---

## 9. Certificación: política intercambiable

**Decisión:** `Certificate.issue(id, evaluation, inspectionFindings, policy, validity, now)` consulta una `CertificationPolicy` (Strategy) con la evaluación fija de la inspección y sus hallazgos. Si recibe hallazgos de otra inspección lanza `IllegalArgumentException` en vez de filtrarlos en silencio. La implementación `SeverityCertificationPolicy(blockingSeverity)` permite certificar si:

1. todo criterio rechazado tiene un hallazgo levantado, y
2. no hay hallazgos abiertos de severidad igual o mayor a la bloqueante.

Los criterios observados no bloquean. Un hallazgo menor abierto tampoco, pero una acción correctiva vencida sin verificar suspende el certificado. Esa regla vive en el agregado: `Certificate.suspendIfActionsOverdue(assetFindings, now)` decide y suspende con el motivo `OVERDUE_ACTIONS`; el caso de uso `SuspendForOverdueActions` solo carga los datos, guarda y publica.

**Renovación:** solo un certificado vigente (`ISSUED`) se renueva, con una nueva inspección del mismo activo que también debe cumplir la política. El certificado anterior queda `RENEWED` y apunta al nuevo.

**Por qué Strategy:** la exigencia para certificar cambia por tipo de activo, por norma o por cliente; la política se inyecta en el caso de uso y cambiarla no toca el agregado.

---

## 10. Informes: vistas de lectura armadas desde los agregados

Requisito: *"Acta de inspección, resumen de hallazgos y certificado"*.

**Decisión:** cada informe es un `record` inmutable en `model/report` con una fábrica estática que lo arma a partir de los agregados, y un caso de uso que solo carga los datos:

| Informe | Se arma con | Contiene |
|---|---|---|
| `InspectionReport` (acta) | `Inspection` cerrada | esquema y versión, inspector, fecha prevista, alcance, resultado global, cada sección con su resultado y cada criterio con resultado, evidencias y observaciones, rectificaciones |
| `FindingsSummary` | hallazgos de una inspección + fecha | abiertos por severidad, cerrados, y por hallazgo: responsable, acciones y acciones vencidas |
| `CertificateReport` | `Certificate` + `Asset` + fecha | activo (tipo, ubicación, responsable), inspección de origen, vigencia, estado y si vale en la fecha |

El acta solo se puede generar de una inspección cerrada o rectificada: usa la evaluación fija, y pedirla antes lanza `InvalidTransitionException`. Los informes no modifican nada ni emiten eventos.

**Por qué en el dominio y no en la aplicación:** decidir qué entra en un acta (resultado por sección, rectificaciones incluidas) es conocimiento del negocio. Los casos de uso `GenerateInspectionReport`, `GenerateFindingsSummary` y `GenerateCertificateReport` quedan en tres líneas.

**Alternativa descartada — modelos de lectura separados (CQRS):** ver §12. Con el volumen actual recorrer los agregados alcanza.

**Alternativa descartada — formato de salida (PDF, HTML):** fuera del alcance del dominio. Los `record` son la estructura que después cualquier adaptador puede renderizar.

---

## 11. Auditoría vía Domain Events

Requisito: *"Historial de modificaciones, decisiones y transiciones."*

Los agregados registran eventos en las transiciones relevantes: `SchemaVersionPublished`, `InspectionStarted`, `InspectionClosed`, `InspectionRectified`, `FindingRaised`, `CorrectiveActionPlanned`, `CorrectiveActionVerified`, `FindingClosed`, `CertificateIssued` y `CertificateStatusChanged` (con estado de origen, destino y motivo). El caso de uso los retira con `pullEvents()` y los publica a través del port `EventPublisher`; un `AuditLog` **append-only** los guarda tal cual y permite consultar el historial de un agregado (`history(aggregateId)`).

**Principio aplicado:** Observer / Domain Events, y **SRP**: los agregados expresan qué pasó; no saben ni les importa quién lo registra.

**Por qué:** la alternativa es que cada caso de uso escriba a mano su entrada de auditoría. Esa duplicación se olvida exactamente en el caso que después hace falta, y mezcla dos responsabilidades en cada clase.

**Alternativa descartada:** auditoría por AOP / interceptores. Requiere framework (lo cual está fuera del alcance) y produce registros de bajo nivel ("se llamó al método X") en vez de hechos del negocio ("se suspendió el certificado por acción correctiva vencida").

**Alternativa descartada:** convertir cada evento en una `AuditEntry` con campos propios. Duplicaba los datos que el evento ya tiene y obtenía el tipo por reflexión; el evento de dominio ya es el registro de auditoría.

**Alternativa descartada:** un evento por cada transición del certificado (`CertificateSuspended`, `CertificateReinstated`, …). Todas llevan la misma información; `CertificateStatusChanged` evita cuatro clases idénticas y el tipo de transición queda en `from`/`to`.

**Costo asumido:** los eventos se publican dentro del caso de uso, no en el agregado, para no darle al modelo una dependencia con el publisher. Es un compromiso consciente: el agregado *acumula* los eventos (`AggregateRoot`) y el caso de uso los publica. Si un caso de uso olvida publicarlos, se pierden.

---

## 12. Patrones que decidimos NO aplicar

| Patrón | Por qué no | Consecuencia de no aplicarlo |
|---|---|---|
| **Event Sourcing (completo)** | Resolvería auditoría y rectificación de forma natural, pero exige replay para leer, versionado de eventos y snapshots. Desproporcionado para el alcance. | Tenemos el estado actual como fuente de verdad y la auditoría como derivada. Si el negocio exige reconstruir el estado a cualquier instante arbitrario, esta decisión hay que revisarla. |
| **CQRS** | No hay presión de lectura ni modelos de consulta distintos del de escritura. | Los informes (`InspectionReport`, `FindingsSummary`, `CertificateReport`) se arman recorriendo los agregados (§10). Si los informes crecen en volumen o complejidad, van a forzar consultas ineficientes y CQRS pasará a estar justificado. |
| **Patrón State en `Certificate`, `Finding` y `CorrectiveAction`** | Ver §6. La tabla de transiciones en el enum y los Value Objects opcionales quitan los condicionales de estado sin sumar clases, y State no mejora OCP. | Si el certificado empieza a variar qué operaciones admite en cada estado, la tabla se queda corta y hay que migrarlo a State. |
| **Interpreter / DSL de reglas** | Ver §5. Strategy+Composite cubre los tipos previstos. | Las reglas nuevas requieren recompilar. No se pueden configurar desde la aplicación. |
| **Repositorio genérico** | Expone operaciones que el negocio no debe permitir y filtra decisiones de persistencia. | Más interfaces para escribir, cada una con su puñado de métodos. Aceptado. |
| **Specification (para consultas)** | Lo usamos conceptualmente en las reglas de evaluación, pero no como mecanismo de consulta a repositorios. | Los criterios de búsqueda son métodos explícitos del repositorio. Si se multiplican, habrá que revisitarlo. |
| **ORM / persistencia** | Fuera del alcance de la entrega. | Los adaptadores in-memory no ejercitan problemas de mapeo, transacciones ni concurrencia. Aparecerán en la Entrega 2. |
| **Inyección de dependencias por framework** | Fuera del alcance y contamina el dominio con anotaciones. | Las dependencias se pasan por constructor y se arman a mano en los tests (`TestContext`). Es más verboso y perfectamente explícito. |

---

## 13. Estrategia de tests

Tests **de los casos más relevantes del negocio**, no de getters. Los agregados y las reglas tienen tests unitarios (`*Test`, surefire). Los casos de uso se prueban de punta a punta contra los adaptadores in-memory (`*IT`, failsafe), lo cual los vuelve tests de integración del dominio sin infraestructura. `mvn verify` corre ambos y genera la cobertura con JaCoCo.

Casos centrales cubiertos:

1. **Aislamiento de versiones.** Iniciar una inspección con la v1, publicar la v2 con criterios distintos, evaluar: el resultado responde a la v1. *(`InspectionLifecycleIT`; valida §4.)* También: la versión se toma al iniciar, no al asignar.
2. **Evidencia obligatoria faltante** → criterio `REJECTED`. *(`CriterionRulesTest`.)*
3. **Hallazgo crítico abierto** → no se puede emitir certificado; tras verificar la acción y cerrar el hallazgo, sí. *(`CertificationIT`, `CertificateTest`.)*
4. **Modificar una inspección cerrada** → `InvalidTransitionException`; rectificarla → cambia el resultado, deja las respuestas originales intactas y queda en la auditoría. *(`InspectionTest`, `InspectionLifecycleIT`.)*
5. **Acción correctiva vencida sin verificar** → suspende el certificado. *(`CertificationIT`; usa el `FixedClock` para adelantar el tiempo.)*
6. **Transiciones ilegales del certificado** (renovar uno suspendido, suspender uno vencido, vencer uno vigente) → excepción. *(`CertificateTest`.)*
7. **Evaluación mixta**: criterios aprobados + observados → resultado global observado y certificable. *(`CertificationIT`.)*
8. **Informes**: el acta muestra cada sección con sus criterios, observaciones y rectificaciones; el resumen cuenta hallazgos abiertos por severidad y acciones vencidas; el certificado refleja la vigencia según la fecha. *(`ReportsIT`.)*

**Decisión:** no usamos librerías de mocking. Los adaptadores in-memory son unas pocas clases de `Map` y sirven para todos los tests; son más legibles que cadenas de `when(...).thenReturn(...)` y no acoplan los tests a la firma exacta de cada método.

---

## 14. Resumen de principios y dónde se aplican

| Principio / patrón | Dónde |
|---|---|
| **SRP** | Casos de uso que orquestan; agregados que deciden; `AuditLog` que registra |
| **OCP** | `CriterionRule`: tipos de criterio nuevos sin tocar código existente; `CertificationPolicy`: políticas nuevas sin tocar `Certificate`; eventos nuevos sin tocar `AuditLog` |
| **LSP** | Todas las implementaciones de `CriterionRule` son intercambiables; `CompositeRule` es una más |
| **ISP** | Ports chicos y específicos (`Clock` con un método; repositorios por agregado); `MeasurementRule` separada de `CriterionRule` |
| **DIP** | El dominio define las interfaces; la infraestructura las implementa |
| **Tell, Don't Ask** | Métodos de intención en los agregados; sin setters públicos |
| **Inmutabilidad** | `SchemaVersion` publicada, `Response`, `Rectification`, `Verification`, `Closure`, Value Objects, colecciones defensivas |
| **Strategy** | `CriterionRule`, `CertificationPolicy` |
| **Composite** | `CompositeRule` |
| **State** | `Inspection` / `InspectionState` |
| **Tabla de transiciones en enum** | `CertificateStatus.next()` + `Certificate.transitionTo` |
| **Domain Events / Observer** | Eventos de cada agregado → `EventPublisher` → `AuditLog` |
