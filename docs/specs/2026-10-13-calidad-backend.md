# Spec · Calidad de código del backend

Fecha: 2026-10-13 · Rama: `entrega-2-spec1` · Commit base: `c7d4b7a` · Anexo: [inventario mecánico](2026-10-13-calidad-backend-inventario.md) · Plan: [docs/plans/2026-10-13-calidad-backend.md](../plans/2026-10-13-calidad-backend.md)

## 1. Objetivo

Revisamos el backend mirando el código y no su funcionamiento: las reglas de diseño que el equipo fijó en la
entrega 1, SOLID y las correcciones de la cátedra (`Corrections.md`). Esta spec propone una solución para cada
hallazgo y deja el orden en que conviene aplicarlas. **No cambia qué hace el sistema**: es una refactorización, y
los tests que ya existen son la red que lo comprueba.

El punto de partida más visible son las dos interfaces `sealed` del modelo de certificación
(`CertificationScope` y `CertificateBasis`) y los `instanceof` y `switch` que provocan en los cuatro módulos.

**Alcance vigente (decisión del 2026-10-13, §9.1).** Esta spec se implementa en `certiflow-domain`,
`certiflow-application` y `certiflow-infrastructure`. Los hallazgos propios de `certiflow-web` (§5.10, §5.11 y
§5.12) quedan para un plan aparte. En `certiflow-web` solo se toca lo que deja de compilar por un cambio de los
otros módulos, y lo que se toca produce el mismo JSON. El detalle de cómo se resuelve cada hallazgo, con el
código actual y el propuesto, está en `BACKEND-QUALITY-SOLUTIONS.md`.

### Criterios de aceptación

1. No queda ninguna interfaz `sealed`, ningún `switch` y ningún `instanceof` fuera de los de §3.2.
2. No queda ningún registro que elija comportamiento por clase o por texto (`Map<Class<?>, …>`,
   `getClass()`, `isInstance`, mapas de etiqueta a función) ni casts a un tipo concreto.
3. `certiflow-domain` y `certiflow-application` no tienen ningún `null`, ningún método estático, ningún
   `Optional` como campo o parámetro y ninguna `IllegalArgumentException` o `IllegalStateException`.
4. `certiflow-infrastructure` cumple lo mismo salvo en las fronteras de §3.3, que quedan listadas con su
   motivo. `certiflow-web` queda para su propio plan: los criterios 1, 2 y 5 no se le exigen todavía.
5. Todo `if` es una guarda que lanza; las decisiones por tipo o por estado las toma el objeto que las conoce.
6. `shared` no importa ningún contexto y `certification` conoce a `inspection` solo por su capa
   anticorrupción y por sus ids. Hay tests de arquitectura (ArchUnit) que lo comprueban.
7. Volver a generar el [inventario](2026-10-13-calidad-backend-inventario.md) da cero en cada columna, salvo
   lo aceptado en §3.
8. `mvn -q clean verify` y los tests del frontend contra el backend (`npm run test:e2e`) pasan después de cada
   tarea del plan.
9. La base de datos no cambia: ninguna migración nueva y los mismos valores en las columnas que ya existen.
10. El contrato HTTP cambia solo en lo que lista §6: el estado de las excepciones reclasificadas y el título
    de algunos problemas. Ningún cuerpo de respuesta cambia de forma.

### Fuera de alcance

- Funcionalidad nueva y corrección de comportamiento. Lo que se vio de paso está en §9.4, sin tarea.
- El código de tests, salvo lo que haya que adaptar para que compile y siga probando lo mismo.
- Los hallazgos propios de `certiflow-web`, incluidos los del login y los `null` de las respuestas de la API.
- El frontend, salvo lo que haga falta por la reclasificación de estados de §5.6.
- Paginación en la consulta para todas las colecciones: hoy es una limitación declarada en `DESIGN.md`.

---

## 2. Punto de partida

La revisión se hizo sobre `4be9e55` y se volvió a verificar dos veces, porque la rama recibió 18 commits
mientras tanto. Esta spec vale para `c7d4b7a`.

**Qué ya se resolvió desde la revisión** (commits `a7e1db8` y `e09e537`): los controllers de inspección y la
configuración se dividieron; las reglas de cobertura por subsistema y las de reemplazo de un certificado
pasaron al dominio (`Inspection.covers`, `OpenNonConformity.affects`, `Certificate.ensureReplaceableBy`);
desaparecieron los `Optional` como parámetro, los métodos `default` de `CertificateRepository` y los
repositorios en memoria.

**Qué cambió de forma sin resolverse**: los `Optional` que eran componentes de un record pasaron a ser
componentes que pueden valer `null`, con un accessor que devuelve `Optional`. Por eso ahora el `null` cruza
hacia el dominio: `certiflow-application` pasó de 0 a 3 literales y cinco lugares de infraestructura terminan
en `.orElse(null)` para llamar a un constructor del dominio.

**Qué se agregó**: el login (`web.security` e `infrastructure.security`, spec
[Login y logout](2026-10-13-login.md)). Dominio y aplicación no cambiaron con el login.

Conteo en `c7d4b7a`, código principal:

| Módulo | Archivos | `sealed` | `instanceof` | `switch` | registros y reflexión | estáticos | `null` | `Illegal*` |
|---|---|---|---|---|---|---|---|---|
| domain | 231 | 2 | 4 | 1 | 1 | 33 | 13 | 6 |
| application | 39 | 0 | 1 | 0 | 0 | 3 | 3 | 0 |
| infrastructure | 45 | 0 | 5 | 0 | 9 | 39 | 39 | 8 |
| web | 77 | 0 | 10 | 2 | 12 | 41 | 71 | 7 |

Cada ocurrencia, con archivo y línea, está en el anexo. «Registros y reflexión» incluye las líneas del proxy
de transacciones, que están aceptadas (§3.2).

---

## 3. Reglas y fronteras

### 3.1 Las reglas

Las de la entrega 1, aplicadas con el mismo rigor en los cuatro módulos (decisión del 2026-10-13):

- Nada de `sealed`, `instanceof`, `switch` por tipo o estado, casts a un tipo concreto ni registros que
  elijan comportamiento por clase o por etiqueta.
- `if` solo como guarda que lanza. Un ternario que calcula un valor a partir de una condición no es un
  hallazgo: `Corrections.md` cita como bien resuelto
  `rule.isSatisfiedBy(answer) ? Outcome.APPROVED : severity.outcomeWhenUnmet()`.
- **D8 sigue vigente**: no se reemplaza un `if` por un condicional disfrazado. La cátedra criticó el «cero
  `if` como dogma». Por eso ninguna solución de esta spec es una tabla de predicados ni un enum creado para
  esquivar una condición: o la decisión pasa al objeto que la conoce, o se queda.
- Sin `null`. `Optional` solo como tipo de retorno de una ausencia legítima; no como campo, parámetro ni
  reemplazo de un `if`.
- Sin `Object` en firmas, sin setters, sin estado guardado que se pueda derivar.
- Sin métodos estáticos ni clases utilitarias.
- Excepciones propias; el tiempo llega por un `Clock` inyectado.
- Nombres largos que se explican solos, una sentencia por línea, sin imports con comodín.

### 3.2 Aceptado por el equipo

| Qué | Dónde | Por qué |
|---|---|---|
| `instanceof` dentro de `equals` | `Person`, `CorrectiveActionEntity.Key` | es la forma estándar de `equals` |
| Proxy dinámico de transacciones, con `Object[]` y `instanceof` al desempaquetar excepciones | `TransactionalUseCases` | se deja como está |

Dos cosas más se dejan como están, pero no porque el equipo las haya aceptado en esta revisión: lo dicen
documentos anteriores, y la revisión original las había marcado. Están en §9.3 para que el equipo decida.

| Qué | Dónde | Qué dice el documento anterior |
|---|---|---|
| `InspectionFactory`, `FindingFactory`, `CertificateFactory`, que no seleccionan nada | dominio | `Corrections.md`: «Generator es una Factory. Conviene usar el vocabulario del patrón». La regla del equipo dice lo contrario: Factory es la que selecciona con `appliesTo` y `findFirst`. |
| El `if` de `Finding.verifyAction` («si la verificación fue aceptada, cancelar las demás acciones») | dominio | D8 lo nombra como decisión puntual de negocio; la regla del equipo es `if` solo como guarda. |

### 3.3 Fronteras que impone un framework

Son los únicos lugares donde una regla no se puede cumplir sin dejar de usar la herramienta. Cada una queda
encerrada en una clase y no sale de ahí.

| # | Frontera | Qué queda | Cómo se encierra |
|---|---|---|---|
| F1 | **Espejos de un formato externo**: cuerpo de un request, documento JSON guardado, columnas JPA. El formato permite que un dato falte y Jackson o JPA lo entregan como `null`. | Un componente que puede valer `null` y, al escribir, un `.orElse(null)`. | El espejo convierte ese dato a un objeto del dominio (§4.3) en un único método propio. Ningún `null` llega a una firma del dominio o de aplicación. |
| F2 | `main` | `public static void main` | exigido por la JVM |
| F3 | Constructor sin argumentos de las entidades JPA | `protected XxxEntity() { }` | exigido por JPA |
| F4 | `AllowedRoleInterceptor.preHandle(…, Object handler)` y su `instanceof HandlerMethod` | la firma es de Spring MVC | decisión D25 del login: el rol se controla antes de leer el cuerpo |

F1 es la única que pide una confirmación (§9.3): el alcance que se le da en las **respuestas** de la API decide
si hay que tocar el frontend.

---

## 4. Los cinco mecanismos

Casi todos los hallazgos se resuelven con cinco formas de hacer las cosas. Se nombran una vez acá y el resto
de la spec las cita.

### 4.1 El objeto responde (polimorfismo)

Si una decisión depende de qué variante es un objeto, la toma la variante. Es el primer recurso siempre.
`CertificateLifecycle` ya lo hace con `statusOn`.

### 4.2 Describir a un receptor

Cuando otra capa necesita **escribir** una variante en su propio formato (una columna, un JSON, un payload, un
link), la variante se describe a un receptor que le pasan:

```java
public interface CertificationScopeDescriber<T> {
    T wholeAsset();
    T subsystem(SubsystemCode subsystemCode);
}

public record SubsystemScope(SubsystemCode subsystemCode) implements CertificationScope {
    @Override
    public <T> T describedBy(CertificationScopeDescriber<T> describer) {
        return describer.subsystem(subsystemCode);
    }
}
```

La entidad JPA, el payload de la API y el documento JSON implementan el receptor, cada uno con su formato.
Nadie pregunta de qué tipo es el alcance. Es double dispatch, y cumple la condición que puso el equipo: **ningún
método del receptor tiene implementación por defecto ni lanza**, porque quien escribe necesita de verdad todas
las variantes.

Costo: una variante nueva obliga a agregar un método al receptor y a todos los que lo implementan. El
compilador lo exige, que es la misma garantía que la cátedra valoró en los genéricos de `ApprovalRule`.

### 4.3 La ausencia es una variante

Un dato que puede no estar no es `null` ni un `Optional` guardado: es un objeto que representa «no hay», con
el comportamiento que corresponde. Ejemplo: un criterio sin subsistema.

```java
public interface SubsystemAssociation {
    SubsystemAssociation orElse(SubsystemAssociation inherited);
    boolean concerns(SubsystemCode subsystemCode);
    <T> T describedBy(SubsystemAssociationDescriber<T> describer);
}
public record NoSubsystem() implements SubsystemAssociation { … }
public record InSubsystem(SubsystemCode subsystemCode) implements SubsystemAssociation { … }
```

`NoSubsystem.concerns(…)` responde `true` (una no conformidad sin subsistema afecta a todos) y
`NoSubsystem.orElse(inherited)` devuelve lo heredado de la sección. Las dos variantes hacen algo con sentido.

### 4.4 Seleccionar con `appliesTo` y `findFirst`

Para **leer** una variante desde una etiqueta guardada o recibida (`"numeric-range"`, `"SUBSYSTEM"`,
`"INSPECTION"`) se usa la selección que la materia llama Factory: una lista de candidatos, cada uno responde
`appliesTo(etiqueta)`, y se toma el primero. Los candidatos son objetos con nombre, no lambdas en un mapa
estático.

### 4.5 Objetos con colaboradores en lugar de estáticos

Una clase utilitaria pasa a ser un objeto que se crea en la composición y se inyecta. Un método estático que
solo crea (`of`, `from`, `none`) pasa a ser un constructor. Un `private static` auxiliar pasa a ser
comportamiento de la colección o del valor sobre el que trabaja.

### Dónde no se aplican

- **El mapa de `SeverityCertificationPolicy` se queda** (`Map<Severity, FindingTreatment>`). La cátedra marcó
  como «switch disfrazado» un mapa `VerificationResult → FindingAudit` porque esa relación es fija y entonces
  le corresponde al enum. Acá la relación cambia con la política: `MINOR` bloquea en `STRICT` y se tolera en
  `STANDARD`. No es una propiedad de la severidad sino la configuración de cada política, y no se puede mover
  al enum. Si el equipo igual prefiere no tener el mapa, la alternativa equivalente es una lista de reglas
  `(severidad, tratamiento)` seleccionada con §4.4.
- **Los snapshots se quedan** (decisión D18). Riesgo: los records `…Snapshot` y `…Factory.restore` son
  públicos, así que cualquiera podría reconstruir un agregado salteando las validaciones de alta. Hoy solo lo
  hacen los repositorios JPA. Se documenta y no se cambia.

---

## 5. Diseño por paquete de trabajo

Cada paquete es una tarea del plan con el mismo número.

### 5.1 Decisión de certificación y plazos

**Hallazgos.** `CertificationDecision` tiene cuatro creadores estáticos, una vigencia que puede valer `null`,
dos invariantes escritas a mano con `IllegalArgumentException` («solo las decisiones elegibles tienen
vigencia») y un `outcome` que se compara desde afuera. `Certificate`, `DecidedCertification` y
`DecidedGlobalCertification` hacen `decision.validity().orElseThrow()`. `Certificate` guarda `scope` y
`validity` aunque los dos están en su decisión. `ValidityTerm.notGranted()` devuelve un plazo cuyo único método
siempre lanza. `CertificateLifecycle`, `CertificateStatus` y `CertificationOutcome` se comparan con `==`.

**Solución.**

- `CertificationDecision` pasa a ser una interfaz con dos variantes (§4.3): `GrantedDecision` (condiciones y
  vigencia) y `RejectedDecision` (motivos). La invariante deja de escribirse: una decisión rechazada no tiene
  dónde guardar una vigencia. `granted()` devuelve `Optional<GrantedDecision>`, que es una ausencia legítima.
- Emitir y renovar hacen
  `decision.granted().orElseThrow(() -> new NotEligibleForCertificationException(decision))` una sola vez.
  `Certificate` recibe un `GrantedDecision`: construir un certificado sin vigencia no compila, y las guardas
  de su constructor se van. `validity()` y `scope()` se leen de la decisión.
- `PolicyAssessment` construye la variante en cada estado; desaparecen `rejected`, `approved`, `conditional` y
  `granted`.
- `ValidityTerm` queda con dos records, `MonthsTerm` y `DaysTerm`, y una excepción propia para un plazo no
  positivo. El plazo condicional viaja con el tratamiento que condiciona
  (`ConditionalTreatment(ValidityTerm term)`), así `STRICT` y `STANDARD` no guardan un plazo que no pueden
  cumplir y `notGranted()` desaparece sin un objeto que lance.
- Los enums responden: `CertificateLifecycle.allowsReplacement()` y `occupiesScope()`,
  `CertificateStatus.isActive()`.

El JSON de `certificate.decision` no cambia: un certificado siempre guardó una decisión concedida.

### 5.2 Alcance, base y alcance de inspección sin `sealed`

**Hallazgos.** Las dos interfaces `sealed`; 12 `instanceof`, 3 `switch` y 3 casts sobre ellas en los cuatro
módulos; `WholeAsset(String kind)`, que guarda una constante; `subsystemCode()` como `Optional` que solo sirve
para distinguir la variante; `Certificate.basedOn()`, que lanza para un global y solo usan los tests;
`CertificateResource.inspectionId`, que repite `basis`; `RenewCertificateCommand`, que se pasa `null`;
`InspectionScope`, que es polimórfica en el dominio pero se guarda y se publica con `instanceof` y un `null`
que significa «alcance completo».

**Solución.**

| Tipo | El objeto responde (§4.1) | Se describe a un receptor (§4.2) |
|---|---|---|
| `CertificationScope` → `WholeAssetScope`, `SubsystemScope` | `narrow(InspectionResult)`, `requiresPartialCertificates(subsystems)` | `wholeAsset()`, `subsystem(code)` |
| `CertificateBasis` → `InspectionBasis`, `PartialCertificatesBasis` | `latestInspectionClosedAt()`, `isSupportedOn(date, assetCertificates)`, `backsGlobalCertificate()` | `inspection(reference)`, `partialCertificates(numbers, globalPolicyCode, latestInspectionClosedAt)` |
| `InspectionScope` → `FullScope`, `SectionsScope` | `criteriaIn(schemaVersion)` (ya existe) | `full()`, `sections(titles)` |

Receptores que se escriben: columnas de `CertificateEntity` e `InspectionEntity`, `subsystemCode` del JSON de
la decisión, payloads de alcance y base, links de `inspection`, `partial-certificates`, `renewals` y
`global-renewals`, y dentro del dominio la lista de subsistemas certificados que usa
`AllRequiredSubsystemsPolicy`.

Lectura desde `scope_type` y `basis_type`: selección de §4.4 dentro de la persistencia. Los valores guardados
(`WHOLE`, `SUBSYSTEM`, `INSPECTION`, `PARTIALS`) no cambian.

`basedOn()` y `inspectionId` se borran. El alcance pedido al renovar pasa a ser una variante:
`SameScopeAsCurrent` devuelve el alcance del certificado y `RequestedScope(scope)` lo compara y lanza si
difiere.

Advertencia que queda escrita: `InspectionScope` pasa a ser cerrada en la práctica, porque un tercer alcance
obliga a tocar el receptor. Ya lo era: hoy un tercer alcance se guardaría como `null` y volvería como
`FullScope`.

### 5.3 Respuestas: `RawAnswer` y `Answer`

**Hallazgos.** `RawAnswer(Object value)` con `as(Class<T>)`, que es un `instanceof` hecho con reflexión; cada
regla le pregunta el tipo. `AnswerColumns` repite la separación con un registro por clase, y `RawAnswers`
(web) con una tabla de predicados que produce `Object`. Los recursos publican `Object answer` con
`given.value().raw().value()`.

**Solución.** El problema tiene dos direcciones y cada una usa el mecanismo que le corresponde.

- **Entrada** (lo que llega → respuesta tipada). Un receptor no sirve: una `NumericRangeRule` tendría que
  implementar «me llegó un sí/no» lanzando, que es lo que el equipo ya descartó en D1. Entonces `RawAnswer`
  lleva **texto** y cada regla interpreta el suyo: `NumericRangeRule` lo convierte a decimal, `YesNoRule` elige
  la constante de `YesNoAnswer` que se escribe así (§4.4), `OptionInListRule` lo toma tal cual. Si no puede,
  `AnswerTypeMismatchException`. Reemplaza lo que D17 decía sobre `RawAnswer`; los comandos de D17 siguen igual.
- **Salida** (respuesta tipada → columna, JSON o texto). `Answer.describedBy(AnswerDescriber<T>)` con
  `number(BigDecimal)`, `yesNo(boolean)` y `option(String)`. Acá el receptor sí cumple la condición: quien
  escribe una respuesta necesita los tres casos.

`answer_type` y `answer_value` se siguen escribiendo con los mismos valores; al leer alcanza con el texto,
porque la regla del criterio ya sabe qué espera. La API sigue devolviendo número, booleano o texto.

Consecuencia que se acepta: al no mirar el tipo JSON, un `"true"` enviado como texto vale como respuesta
sí/no, y un número enviado a un criterio de opciones se toma como el texto de la opción.

### 5.4 Ausencias del dominio

**Hallazgos.** Once de los trece componentes del dominio que pueden valer `null`, con sus accessors
`Optional` (los otros dos están en §5.1 y §5.2); `null` pasado a constructores del dominio desde aplicación,
infraestructura y web; `PublishSchemaVersionCommand`, que se pasa `null`; `CorrectiveAction.cancellation`.

**Solución** (§4.3 en todos):

| Dato | Variantes | Dónde se usa |
|---|---|---|
| Subsistema de un criterio, sección, hallazgo o no conformidad | `NoSubsystem`, `InSubsystem` | `Criterion`, `Section`, `Finding`, `FindingSnapshot`, `FindingLine`, `OpenNonConformity`, `SchemaVersion` |
| Respuesta de un criterio | `NotAnswered`, `GivenAnswer` | `CriterionResponse.outcome()` deja de usar `map(…).orElse(PENDING)` |
| Cancelación de una acción | `NotCancelled`, `Cancellation` | `CorrectiveAction`, `CorrectiveActionSnapshot` |
| Cierre de la inspección que se certifica | `StillOpen`, `ClosedAt` | `InspectionResult` |
| Vigencia pedida al publicar | `EffectiveOnPublication`, `EffectiveFrom` | `PublishSchemaVersionCommand` |

Cada variante tiene comportamiento propio; ninguna existe solo para evitar un `null`. Ejemplos:
`NotAnswered` responde `PENDING` como resultado, `NoSubsystem` afecta a todos los subsistemas, y
`EffectiveOnPublication` resuelve la vigencia con el instante de publicación.

Pedirle la referencia de inspección a un resultado `StillOpen` es una ausencia legítima
(`Optional<InspectionReference>`). Quien la necesita lanza una excepción de certificación, y
`InspectionResult` deja de importar `InspectionNotClosedException` del contexto de inspección: es la fuga de
la capa anticorrupción que queda hoy.

### 5.5 Reglas, severidades, tipos de evidencia y acciones de bitácora

**Hallazgos.** `SchemaPayloads` (registro por clase, registro por texto, cuatro casts, un bloque `static` y un
payload con cinco campos que valen `null` según la regla); `SectionsJson` (lo mismo para guardar);
`(StandardSeverity) criterion.severity()`; `StandardSeverity.valueOf`, `StandardEvidenceKind.valueOf` y
`StandardIneligibilityReason.valueOf` en seis lugares, que atan la interfaz al único enum; `(Enum<?>) action`
en web y en persistencia, porque `AuditAction` solo declara `label()`.

**Solución.**

- `ApprovalRule.describedBy(ApprovalRuleDescriber<T>)` con `numericRange(unit, range)`, `yesNo(expected)` y
  `optionInList(acceptedOptions)`. La API devuelve un payload por tipo de regla, sin campos de relleno.
- Lectura de una regla desde su `type`: §4.4, un lector por tipo en web y otro en persistencia.
- Catálogos en el dominio: `SeverityCatalog`, `EvidenceKindCatalog` e `IneligibilityReasonCatalog` reciben la
  lista de valores conocidos y resuelven por `code()` con §4.4. La composición es el único lugar que nombra los
  enums `Standard…`. `VocabularyController` lee del catálogo.
- `AuditAction` declara `name()` (que todo enum ya cumple) y `code()`, con el mismo texto que hoy se guarda en
  `audit_entry.action`. Un `AuditActionCatalog` resuelve al leer.

### 5.6 Excepciones

**Hallazgos.** 21 `IllegalArgumentException` o `IllegalStateException`; 11 `orElseThrow()` sin excepción;
`CertificationConflictException` con texto libre; excepciones del dominio que la capa web lanza para decir
400 o 404; `Objects.requireNonNull` y chequeos `== null` en value objects; `ApiExceptionHandler`, con un mapa
de clase de excepción a estado HTTP, un recorrido de superclases que nunca encuentra nada (las 43 excepciones
extienden `DomainException` directamente) y un `instanceof`. Las 25 que no están en el mapa responden 409 por
descarte.

**Solución.**

- Tres categorías abstractas bajo `DomainException`, con significado de negocio: **no existe**, **dato
  inválido** y **regla incumplida**. Cada excepción extiende la suya.
- `ApiExceptionHandler` tiene un método por categoría y uno para `NotEligibleForCertificationException`;
  Spring elige por tipo. Se van el mapa, el recorrido y el `instanceof`. Una excepción nueva declara su
  categoría en el dominio y no toca la capa web.
- Los estados HTTP no cambian: las 18 excepciones del mapa van a la categoría que les da hoy su estado, y
  las otras 25 van a **regla incumplida**, que responde 409 como hoy. Reclasificar alguna (por ejemplo
  `JurisdictionPolicyNotFoundException`, que hoy responde 409) es un cambio de contrato y queda fuera.
- Cada `Illegal*` del dominio pasa a una excepción propia; las de infraestructura, a una de infraestructura
  para datos guardados que no se reconocen.
- Cada texto libre de `CertificationConflictException` pasa a una excepción con nombre.
- La capa web usa sus propias `InvalidRequestException` y `ResourceNotFoundException`.
- Los chequeos de `null` de los value objects se borran: sin `null` en el dominio no tienen qué atajar.

### 5.7 Reglas de negocio en el dominio y estáticos del dominio

**Hallazgos.** Es el primer punto de `Corrections.md`. En `certiflow-application` siguen los tres requisitos
comunes de elegibilidad y la elección entre rechazar y consultar a la política
(`CertificationAssessmentService`), la regla «un alcance tiene un solo certificado vigente»
(`CertificateIssuanceService`), el cálculo de la base de un global (`GlobalCertificationService`), la
comparación del alcance al renovar y `CertificateStatuses`, que tres casos de uso crean con `new`. Además, los
33 estáticos del dominio.

**Solución.**

- `CertificationEligibility`, servicio de dominio: requisitos comunes más política de la jurisdicción. El
  servicio de aplicación carga el activo, lee el reloj y delega.
- `AssetCertificates`, colección de primera clase con los certificados de un activo:
  `ensureScopeAvailable(scope, date)`, `ensureScopeAvailableToReplace(certificate, scope, date)` y
  `statusOf(certificate, date)`. Absorbe `CertificateStatuses`.
- `Requirement` pasa a ser un record y `Requirements` su colección, con `unmetBy(candidate)`.
- Las tres políticas de ejemplo se construyen en `CertificationConfiguration`, que ya es el lugar que las
  asocia a cada jurisdicción.
- `SchemaVersion` delega sus seis validaciones estáticas en `SchemaSections`, una colección que se valida al
  nacer y responde `criteria()`, `requiredSubsystems()` y `criterion(code)`.
- `Characteristics.none()`, `Revisions.none()` y `SchemaVersionLog.startingWith(…)` pasan a constructores.
- `StoredInstant` sale del dominio, y su trabajo queda en dos lugares que no son el dominio: el `Clock` que
  se inyecta entrega instantes con la precisión que se guarda (`Clock.tick`), y los instantes que manda el
  cliente (la vigencia pedida de una versión) se truncan en el record del request. El comportamiento es el
  mismo; el dominio deja de saber cuánta precisión tiene la base.
- `RegisterAssetUseCaseImpl` deja de importar `CertificationPolicyResolver`: `catalog` declara un puerto para
  saber si una jurisdicción es conocida y el registro de políticas lo implementa. Hoy `catalog` depende de
  `certification`.

### 5.8 Bloqueo por activo y bitácora

**Hallazgos.** `shared.usecase` importa `catalog`, `inspection` y `certification` (`AuditSubject`, `AuditLogs`,
`AssetLockKey`). `AssetLockKey` es una etiqueta de texto creada por cuatro estáticos, y
`CertificationTransactionLocks` la resuelve con un mapa de texto a función, SQL sobre tablas de otros
contextos y una `IllegalStateException`. Se aplica a 26 de los 32 casos de uso, consultas incluidas, aunque
solo 8 comandos bloquean.

**Solución.**

- Cada caso de uso que hoy bloquea lo hace de forma explícita: pide el activo al repositorio de su contexto
  (`assetCodeOf(inspectionId)`, y lo mismo para hallazgo y certificado) y llama al puerto `AssetLock`, que ya
  existe. Se van `LocksAsset`, `AssetLockKey` y `CertificationTransactionLocks`. El proxy no se toca: se usa
  su forma sin acción previa.
- **El orden no cambia y es parte del contrato: resolver el código del activo, bloquear, y recién entonces
  cargar agregados.** `assetCodeOf` es una consulta liviana que no carga el agregado. Cargar antes de
  bloquear rompe la serialización por activo que protege `DeliveryFeaturesApiTest`.
- Cada contexto tiene su caso de uso de historial (`InspectionHistoryUseCase`, y los de hallazgo, certificado,
  activo y esquema) sobre su propio `AuditLog`. Se van `AuditSubject` y `AuditLogs`.

Con esto `shared` no importa ningún contexto, y se cierra la excepción que `DESIGN.md` declara hoy en su mapa
de dependencias.

### 5.9 Infraestructura

**Hallazgos.** 39 estáticos: siete clases utilitarias (`SectionsJson`, `DecisionJson`, `EvaluationJson`,
`CodesJson`, `ResponseRebuilder`, `AuditActions`, `Instants`) y trece `from`/`of` en entidades y embebibles;
`JsonMapper.builder().build()` siete veces; `null` usado como etiqueta de tipo; dos fusiones posicionales con
`if/else`; `JpaInspectionRepository` depende de una clase concreta de otro contexto; código sin uso.

**Solución.**

- Las utilitarias pasan a objetos que el repositorio recibe y entrega a la entidad cuando esta los necesita.
  Hay un único `JsonMapper`, de la composición.
- `from` y `of` pasan a constructores.
- Lo que hoy se distingue con `null` o con `equals("WHOLE")` usa §4.2 al escribir y §4.4 al leer.
- Las fusiones posicionales pasan a dos recorridos sin condición: actualizar los que ya existen y agregar el
  resto.
- `Instants` se borra junto con `StoredInstant` (§5.7).
- `JpaInspectionRepository` recibe un colaborador con la consulta que necesita, no el repositorio de esquemas.
- Se borran `JpaInspectionSchemaRepository.versionEntity` y `SchemaVersionEntity.getId`.

Los `null` que quedan son los de F1 y F3.

### 5.10 Web

**Hallazgos.** Diez `if` que deciden links preguntando un estado; 25 `null` como argumento de `methodOn(…)`;
siete clases utilitarias; `GlobalCertificateController` depende de `CertificateController`, e
`InspectionController` y `EligibilityController` se importan entre sí; handlers que llaman a otros handlers;
filtros y paginado en memoria dentro del controller; `CLOSED_BY_STATUS`; el controller calcula «hoy»
(`overdue`, versión vigente); ids de subrecursos deducidos de `size()`; `VersionResponse.resolvedAt` mutable;
`issueGlobal` inyectado y sin uso; `DemoDataSeeder` dentro de `web.config`, con `Object...` y casts.

**Solución.**

- Un ensamblador por recurso, inyectado. Arma el recurso y sus links; los controllers dependen de
  ensambladores y casos de uso, nunca de otro controller.
- Links que dependen del estado: qué se puede hacer con una inspección, un hallazgo, una acción o un
  certificado lo sabe el dominio, no el controller. La vista entrega sus **transiciones disponibles**
  (`inspection.availableTransitions()`, por ejemplo cerrar, o rectificar y generar el acta) y cada transición
  se describe a un receptor de links (§4.2). El ensamblador no pregunta por el estado. Los links que dependen
  de la base del certificado salen del receptor de la base.
- `Page` responde si tiene página anterior y siguiente.
- Links armados por ruta, sin `methodOn(…, null)`.
- Las utilitarias pasan a objetos inyectados; los payloads salen de los receptores de §5.2, §5.3 y §5.5.
- Los parámetros de filtro llegan como lista (vacía si faltan) y se convierten en filtros del caso de uso; el
  controller no filtra ni corta listas.
- Lo que depende del reloj vuelve resuelto del caso de uso, como ya hace `CertificateView`.
- El alta de un subrecurso devuelve su identificador.
- Los records de request son espejos F1: validan sus campos obligatorios en su constructor, con una guarda
  que lanza la excepción propia de web, y convierten los opcionales en variantes. No se agrega Bean
  Validation, que sería una dependencia nueva. `RequestFields` desaparece.
- `DemoDataSeeder` va a su propio paquete, sin `Object...`.

### 5.11 Seguridad

**Hallazgos** en el código del login: cuatro `instanceof` además de F4
(`ActingPersonResolver` ×2, `IssueTokenAfterBasicFilter`, `TokenController`); `if` anidados que no son guardas
en `AllowedRoleInterceptor`; cuatro `IllegalStateException`; chequeos de `null`; estáticos; y seis clases de
`certiflow-web` que usan repositorios de Spring Data de `certiflow-infrastructure` fuera de la configuración,
entre ellas un controller que guarda con `@Transactional`.

**Solución propuesta**, a coordinar con quien escribió el login:

- Las dos formas de autenticarse entregan el mismo principal, con la persona adentro. `@ActingPerson` pasa a
  resolverlo Spring Security y `ActingPersonResolver` desaparece.
- Emitir el token al autenticarse con Basic usa el punto de extensión del filtro de Basic
  (`onSuccessfulAuthentication`), sin preguntar el tipo de autenticación.
- Que solo un token se pueda revocar lo declara la cadena de seguridad para esa ruta.
- Cuentas y tokens revocados pasan a ser un contexto chico con sus puertos y un caso de uso `RevokeToken`;
  la capa web deja de importar infraestructura.
- `AllowedRoleInterceptor` queda con guardas que lanzan y con F4.

### 5.12 Variantes en las respuestas y frontend

Ver §6. Es la única tarea que toca `certiflow-frontend`.

### 5.13 Nombres, formato y código sin uso

- `Stamp(Person by, Instant at)` y los parámetros `by`: la convención del equipo es nombrar quién y cuándo
  por la operación y no usar preposiciones como nombre.
- Lambdas `c`, `r`, `s`, `k`, `item`; `tx`; `own`, `backing`, `valid`, `until`, `INVALID`; `recorded(…)` para
  un método que registra; `GlobalRequest`; «publish» en el caso de uso, `addVersion` en el servicio y
  `VERSION_ADDED` en la bitácora.
- Value objects que faltan: `CriterionCode`, `SectionTitle`, `PolicyCode`.
- Imports con comodín, `var`, líneas de más de 150 caracteres, varias sentencias por línea, imports sin orden
  o sin uso.
- Código que solo llaman los tests: `CertificationDecision.isBlockedBy`, `InspectionSchema.appliesTo`,
  `Characteristics.valueOf`, `Evaluation.outcomeOf`, `Revisions.size`.

### Lo que no se toca y por qué

- `Severity`, `EvidenceKind`, `IneligibilityReason` y `FindingTreatment` como interfaz más enum: es el patrón
  acordado para conjuntos configurables. Lo que se corrige es que otras capas vuelvan a atarlos al enum (§5.5).
- Puertos y casos de uso con una sola implementación: son la inversión de dependencias que pidió la cátedra.
- `TOLERATED` con cuerpo vacío: tolerar es no agregar nada a la evaluación.
- Las tres `…Factory` y el `if` de `Finding.verifyAction`, hasta que el equipo decida (§3.2 y §9.3).

---

## 6. Contrato HTTP, base de datos y frontend

**Base de datos.** Sin migraciones. Las columnas `scope_type`, `basis_type`, `answer_type`, `answer_value` y
`action`, y los JSON de `sections`, `criterion`, `evaluation` y `decision`, conservan forma y valores.

**Contrato HTTP.** No cambia ninguna ruta, ningún `rel` ni ningún código de estado.

| Cambio | Campos | Efecto en el frontend |
|---|---|---|
| Un payload por variante, sin campos de relleno | `scope.subsystemCode`, `basis.inspectionId`, `basis.globalPolicy`, `basis.partialCertificateNumbers`, campos de `rule` | ninguno: el adaptador solo los lee bajo el `type` que corresponde |
| Campo que se borra | `inspectionId` del certificado | ninguno: no se lee |
| Se quedan como espejo F1 | `subsystemCode` de hallazgo, línea, criterio y sección; `resolvedAt` | ninguno |
| **A confirmar (§9.3)**: dejan de venir en `null` y pasan a no venir | `sections` de la inspección; `answer`, `answeredBy` y `answeredAt` de una respuesta pendiente; `validFrom` y `validUntil` de una decisión rechazada | `inspectionMapping.ts` y `certificationMapping.ts` comparan con `=== null`: hay que adaptar tres mapeos, `wire.ts` y volver a capturar las respuestas de ejemplo |

Si no se confirma la última fila, esos tres casos quedan como espejos F1 y `certiflow-frontend` no se toca.

---

## 7. Tests

| Qué | Cómo |
|---|---|
| Que nada cambió de comportamiento | la suite actual: dominio, casos de uso con Mockito, escenarios sobre la aplicación real, API, repositorios JPA y los 115 tests del frontend contra el backend |
| Cada variante y cada receptor nuevos | un test de dominio por variante; un test de persistencia de ida y vuelta por cada cosa que hoy se distingue con `instanceof` |
| `RawAnswer` como texto | `RawAnswerTest` parametrizado: qué texto acepta y rechaza cada regla, incluidos los dos casos de §5.3 |
| Categorías de excepción | un test de API por categoría y uno que recorre las excepciones del dominio y comprueba que ninguna extiende `DomainException` directamente |
| Reglas de dependencia (criterio 6) | **Propuesta**: tests con ArchUnit (dependencia nueva, solo de test) para `shared` sin contextos, web sin infraestructura fuera de la configuración y dominio sin frameworks |
| Criterio 7 | regenerar el inventario al cerrar cada tarea con `docs/specs/2026-10-13-calidad-backend-inventario.py` |

---

## 8. Documentación

- `DESIGN.md`: una decisión nueva por cada mecanismo de §4 con sus alternativas descartadas; D17 pasa a
  «reemplazada»; D18 suma el riesgo declarado; D21 se actualiza con los payloads por variante; las fronteras
  de §3.3 van a limitaciones.
- `DESIGN.md` tiene hoy cuatro líneas que ya no describen el código y se corrigen en la misma tarea:
  `RawAnswer` descripta como `sealed` (D17), «la API serializa tipos del dominio» (dos veces) y «elegibilidad
  del activo entero responde 409».
- `docs/certiflow-domain.puml`, `README.md`, `CHANGES.md` y `docs/specs/README.md`.

---

## 9. Decisiones

### 9.1 Confirmadas (2026-10-13)

1. `instanceof` dentro de `equals` no es un hallazgo.
2. El proxy de `TransactionalUseCases` se deja como está.
3. Las reglas se aplican con el mayor rigor posible en los cuatro módulos.
4. Double dispatch es aceptable si ningún método del receptor tiene un `default` que lanza; si lo tuviera, se
   usa la otra opción.
5. Primero spec y plan; el código después.
6. **Los hallazgos de `certiflow-web` se dejan para un plan aparte**, incluido el login (§5.10, §5.11, §5.12).
7. El `if` de `Finding.verifyAction` se queda, por D8.
8. Las tres `…Factory` conservan su nombre, por la corrección de la cátedra.
9. `RawAnswer` lleva texto, el mapa de `SeverityCertificationPolicy` se queda, cada caso de uso de escritura
   bloquea su activo de forma explícita y los snapshots siguen públicos.
10. El título de algunos problemas HTTP puede cambiar si el estado se conserva.
11. **Las 25 excepciones que hoy responden 409 por descarte se reclasifican** según lo que significan, y el
    frontend se adapta si depende del estado (§5.6).
12. Los nombres propuestos de §10 se aceptan. El componente de `SchemaSections` se llama `all`, para que se
    lea `version.sections().all()`.
13. ArchUnit entra como dependencia de test.
14. Quedaron a criterio de quien implementa, y se resolvieron así: `RuleDocument` conserva sus seis `null`
    para que el JSON guardado sea idéntico; los constructores con cinco o seis colaboradores no se tocan; el
    detalle de bitácora de un certificado emitido describe el alcance con un texto legible.
15. **Enmienda posterior a la implementación (Task 15 del plan).** Una auditoría externa y una corrección del
    profesor («si en el futuro agrego otro tipo de regla tengo que cambiar muchísimos archivos del core»)
    cambiaron dos cosas de esta spec:
    - El punto 14 deja de valer para las reglas: una regla se describe por clase de valor (`RuleDescriber`) y
      cada tipo de regla la arma de vuelta (`ApprovalRuleKind`), así que una regla guardada lleva solo sus
      parámetros. Las filas anteriores, con los parámetros de los otros tipos en `null`, se siguen leyendo.
      No hay migración y el JSON de la API no cambia.
    - A los cinco mecanismos de §4 se suma uno: cuando solo una variante puede cumplir un pedido, el pedido es
      una guarda que lanza la excepción de negocio (`ensureGranted()`), en lugar de un método que la otra
      variante responde con un vacío. El receptor con un método por variante (M2) queda para las jerarquías
      cerradas. Está explicado en `DESIGN.md` D26.

### 9.2 Derivadas de las anteriores

1. Alcance, base, alcance de inspección, respuestas (al escribir) y reglas (al escribir) usan un receptor: en
   todos, quien escribe necesita cada variante.
2. `RawAnswer` (al leer) no usa un receptor, porque cada regla tendría que lanzar en dos de tres métodos:
   lleva texto.
3. El mapa de `SeverityCertificationPolicy` se queda (§4, «Dónde no se aplican»).
4. Los snapshots se quedan como en D18.

### 9.3 Pendiente para el plan del módulo web

1. Los links que dependen del estado (§5.10): operaciones disponibles entregadas por el dominio, o los `if`
   bajo D8.
2. `prev` y `next` en la paginación.
3. Campos obligatorios de un request: un objeto propio o Bean Validation.
4. El `return` temprano de `DemoDataSeeder`.
5. Quitar los `null` de tres respuestas de la API y adaptar el frontend (§5.12).
6. El login (§5.11), a coordinar con quien lo escribió.

### 9.4 Visto de paso, sin tarea

No es calidad de código y no se ejecutó nada para comprobarlo.

- El job `e2e` de CI falla en `c7d4b7a`: el backend no llega a levantar.
- `Certificate.statusOn` es recursiva y no se protege de un ciclo entre certificados.
- `GlobalRequest` descarta el resultado de `requireElements`: una lista ausente queda en `null`.
- 9 de los 17 comandos no bloquean el activo.
- Conviven dos formas de cargar datos de ejemplo: `DemoDataSeeder` y `npm run seed`.

---

## 10. Nombres propuestos

Los nombres los decide el equipo; estos son para poder escribir el plan.

| Concepto | Nombre propuesto |
|---|---|
| Receptores | `CertificationScopeDescriber`, `CertificateBasisDescriber`, `InspectionScopeDescriber`, `AnswerDescriber`, `ApprovalRuleDescriber`, `SubsystemAssociationDescriber` |
| Método de las variantes | `describedBy(…)` |
| Alcances | `WholeAssetScope`, `SubsystemScope` |
| Decisión | `GrantedDecision`, `RejectedDecision` |
| Plazos | `MonthsTerm`, `DaysTerm`, `ConditionalTreatment` |
| Ausencias | `NoSubsystem` / `InSubsystem`; `NotAnswered` / `GivenAnswer`; `NotCancelled` / `Cancellation`; `StillOpen` / `ClosedAt`; `EffectiveOnPublication` / `EffectiveFrom`; `SameScopeAsCurrent` / `RequestedScope` |
| Catálogos | `SeverityCatalog`, `EvidenceKindCatalog`, `IneligibilityReasonCatalog`, `AuditActionCatalog` |
| Colecciones y servicios | `AssetCertificates`, `Requirements`, `SchemaSections`, `CertificationEligibility` |
| Categorías de excepción | `NotFoundException`, `InvalidValueException`, `RuleViolationException` |
| Web | `…ResourceAssembler`, `…TransitionDescriber` |
| Transiciones | `InspectionTransition`, `FindingTransition`, `CorrectiveActionTransition`, `CertificateTransition` |
