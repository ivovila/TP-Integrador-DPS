# Certiflow · Decisiones de diseño (Entrega 1)

Módulo de dominio de una plataforma de inspección, habilitación y certificación de activos.
Java 25, Maven, JUnit 5. Sin frameworks: el código es el documento de diseño principal y este
archivo explica por qué tiene la forma que tiene.

```bash
mvn clean test
```

---

## 1. Supuestos de negocio donde el enunciado es ambiguo

| # | Tema | Decisión | Consecuencia |
|---|---|---|---|
| S1 | "Inspección iniciada" | La inspección nace asignada y abierta en un solo paso, y en ese momento captura la `SchemaVersion` vigente, que es inmutable. | Publicar una versión nueva crea otro objeto; la inspección conserva su referencia. Un criterio agregado en v2 lanza `CriterionNotInSchemaException` en una inspección abierta con v1. |
| S2 | Esquema por tipo de activo | Existe un único `InspectionSchema` por `AssetType`, con N versiones. | `AssignInspection` busca el esquema por el tipo del activo. Crear un segundo esquema para el mismo tipo falla con `SchemaAlreadyDefinedForAssetTypeException`. |
| S3 | Aprobado, observado, rechazado | La regla solo dice si la respuesta cumple. Si no cumple, la severidad del criterio decide: `MINOR` → `OBSERVED`; `MAJOR` y `CRITICAL` → `REJECTED`. Sin respuesta o sin la evidencia exigida: `PENDING`. | La decisión es un dato de `Severity`, no un condicional repartido por el código. |
| S4 | Hallazgos | Se levantan al cerrar la inspección, uno por criterio observado o rechazado, con la severidad del criterio, la evidencia adjunta y el responsable del activo. | `Finding` es un agregado propio cuyo ciclo de vida sobrevive a la inspección. |
| S5 | Qué bloquea la certificación | Solo los hallazgos abiertos cuya severidad lo declara (`Severity.blocksCertification()`): `MAJOR` y `CRITICAL`. Los `MINOR` se siguen con acciones correctivas pero no impiden certificar. | "Observado" y "rechazado" tienen consecuencias distintas y defendibles. |
| S6 | Rectificación y hallazgos | Rectificar corrige el acta y su evaluación derivada; no regenera ni elimina hallazgos ya levantados. | Sin efectos en cascada entre agregados. Queda como limitación conocida (ver sección 6). |
| S7 | Revisión original | Cerrar crea la `Revision` 1; cada rectificación agrega otra; ninguna se modifica. | `originalRevision()` devuelve siempre lo que se firmó al cerrar. |
| S8 | Acción correctiva | Vive dentro de su hallazgo. Está cerrada cuando tiene una verificación aceptada; está vencida cuando sigue abierta y la fecha es posterior a `dueDate`. El verificador debe ser distinto del responsable de la acción. | Una verificación aceptada cierra acción y hallazgo a la vez, sin coordinación externa. El día `dueDate` todavía no está vencida. |
| S9 | Vencimiento del certificado | `ValidityPeriod(validFrom, validUntil)` con ambos extremos incluidos. La vigencia arranca el día de emisión. `EXPIRED` se deriva de la fecha consultada. | Vigente todo el día `validUntil`, vencido el siguiente. No existe un booleano que pueda quedar desactualizado. |
| S10 | Unicidad | Un activo tiene a lo sumo un certificado vigente. Un certificado suspendido dentro de su validez sigue ocupando ese lugar. | No se puede esquivar una suspensión emitiendo otro certificado; la salida es renovar. |
| S11 | Renovación | Emite un certificado nuevo, basado en otra inspección cerrada y sujeto a la misma elegibilidad; el anterior pasa a `RENEWED`, estado terminal. No se modela "levantar una suspensión". | La historia no se borra. Menos transiciones que probar. |
| S12 | Límites numéricos | `Range[minimum, maximum]` cerrado en ambos extremos; un mínimo mayor que el máximo es configuración inválida. La unidad es configuración de la regla, no un dato libre de la respuesta numérica. | La inspección trabaja con el valor en la unidad que define su esquema; no hay conversión de unidades. |
| S13 | Evidencia obligatoria | Un criterio puede exigir una cantidad mínima de evidencias de un tipo concreto: `EvidenceRequirement(kind, minimum)`, a lo sumo un requisito por tipo. Cada evidencia exigida tiene que ser un archivo distinto: adjuntar dos veces la misma referencia al mismo criterio se rechaza (`DuplicateEvidenceException`). | Un criterio respondido pero sin la foto exigida queda `PENDING` y bloquea el cierre. "Dos fotos" no se cumple con la misma foto dos veces, y un criterio con dos requisitos del mismo tipo es un esquema inválido (`InvalidSchemaException`) en lugar de dos requisitos que se cumplen con una sola foto. El mismo archivo sí puede respaldar criterios distintos. |
| S14 | Personas | Inspector, responsable, verificador y emisor son `Person(name)`. El rol lo da el campo que la referencia. | La regla "verificador distinto del responsable" compara personas sin conversiones entre tipos de rol. |
| S15 | Identidad | Solo tienen identificador los conceptos que el negocio identifica: `AssetCode` (placa del activo) y `CertificateNumber`. El resto se referencia por objeto. | Los casos de uso reciben objetos, no ids. Ver D3. |
| S16 | Informes | `InspectionAct`, `FindingsSummary` y `CertificateDocument` son records sin formato. El acta solo se emite para inspecciones cerradas. | Son modelos de lectura: ahí se usan getters a propósito. |

---

## 2. Arquitectura y dirección de dependencias

```
models  <──  ports  <──  application  <──  usecases
   ▲           ▲
   └───────────┴──────  details.inmemory

tests: el fixture es la raíz de composición y es el único que conoce todos los paquetes
```

Cada flecha se lee "es importado por". Además `application` y `usecases` importan `models` directamente.

- **`models`**: entidades, value objects, reglas, los decoradores de auditoría y el puerto `AuditLog<S>`. No importa ningún otro paquete ni consulta relojes.
- **`ports`**: lo que el negocio le pide al exterior: los cinco repositorios y `CertificateNumbering`, definidos según lo que los casos de uso necesitan. Solo importa `models`.
- **`usecases`**: un caso de uso por clase, agrupados por concepto (`asset`, `schema`, `inspection`, `finding`, `certificate`, `report`). Son los únicos que leen `java.time.Clock`.
- **`application`**: lo que acompaña a los casos de uso sin ser uno: la política de elegibilidad (`application.certification`), los modelos de lectura de los informes (`application.report`) y las excepciones de las reglas que cruzan agregados (`application.exceptions`).
- **`details.inmemory`**: implementaciones de los puertos.

Los nombres `models`, `usecases`, `ports` y `application` son los que la cátedra propone para un módulo de
negocio, el equivalente al módulo `domain` de DDD: todo el módulo es negocio, sin base de datos, controllers
ni framework. Dentro de `models` y de `usecases` se agrupa por concepto y no por tipo de clase, porque
`Standard...`, `Audited...` y su generador comparten visibilidad de paquete (ver D7) y separarlos obligaría a
hacerlos públicos.

**Por qué `AuditLog` no está en `ports`.** Es un puerto, pero quienes lo usan son los decoradores y los
generadores, que viven en `models`. Mudarlo haría que `models` importe `ports` mientras `ports` ya importa
`models`: un ciclo. Se queda en `models.audit`, al lado del código que lo consume, que es donde la inversión
de dependencias ubica la interfaz.

**Negocio y detalles.** Se sigue la separación de la clase 3 (ejemplo del cifrado César): `models`,
`ports`, `usecases` y `application` son el negocio y `details` son los detalles. Un detalle es un mecanismo técnico que el negocio
necesita pero cuyo funcionamiento no le importa: cómo se guardan los agregados, cómo se numeran los
certificados, dónde queda la bitácora. Las reglas de aprobación (`NumericRangeRule`, `YesNoRule`,
`OptionInListRule`), las respuestas y las escalas estándar de severidad y evidencia son negocio aunque se
enchufen detrás de una interfaz, igual que `CesarCipher` es negocio siendo una clase concreta. Por eso viven
en `models.evaluation`. Los detalles implementan interfaces que declara el negocio y nunca al revés. El fixture
de tests cumple el rol del componente `Main`: crea los detalles y se los entrega a los casos de uso.

Dependencias entre los paquetes de `models`, sin ciclos:

```
shared      <──  audit, asset, evaluation
asset, evaluation  <──  schema
schema, audit      <──  inspection
inspection         <──  finding
inspection         <──  certificate
```

`models` no importa ningún otro paquete, `ports` solo importa `models` y nadie importa `details`. Ambos
grafos se verificaron recorriendo los `import` de todo `src/main`.

**Dónde vive cada validación.** Lo que involucra un solo agregado vive en el agregado: cerrar con criterios
pendientes, verificador distinto del responsable, renovar dos veces. Lo que cruza agregados vive fuera
del modelo: hallazgos bloqueantes (`CertificationEligibility`, en `application`), unicidad del certificado
vigente (`IssueCertificate`), un esquema por tipo de activo (`CreateInspectionSchema`).

**Tiempo.** Los casos de uso leen el `Clock` y le pasan `Instant` o `LocalDate` al dominio. El dominio es
determinista y los tests usan un reloj controlable.

---

## 3. Decisiones principales

### D1. Reglas de aprobación: Strategy con el tipo de respuesta en la firma

- **Principios:** OCP, LSP, ISP.
- **Dónde:** `ApprovalRule<A extends Answer>`, `NumericRangeRule`, `YesNoRule`, `OptionInListRule`, `Criterion<A>`, `CriterionResponse<A>`, `Inspection.recordAnswer(Criterion<A>, A, ...)`.
- **Por qué:** agregar una regla es agregar una clase; nada más cambia. El contrato LSP es explícito en el tipo: `ApprovalRule<NumericAnswer>` declara qué acepta. Ofrecerle un `YesNoAnswer` a un criterio numérico **no compila**, así que ese error no necesita excepción ni test de ejecución. La unidad pertenece a la regla del criterio y la respuesta solo conserva el valor. `CriterionResponse<A>` lleva consigo su `Criterion<A>`, de modo que evaluar no requiere ningún cast.
- **Alternativas descartadas:**
  - *Double dispatch con métodos `default` que lanzan excepción:* funcionaba, pero la interfaz prometía tres capacidades y cada regla cumplía una. Es la violación de ISP de la clase 2 maquillada.
  - *Visitor con métodos abstractos:* obliga a cada regla a implementar "no soy yo" para los otros tipos.
  - *`instanceof` en la regla:* viola OCP y el objetivo de la materia.
- **Costo:** comodines (`Criterion<?>`) en `Section` y `SchemaVersion`; `Answer` queda como interfaz sin métodos, cuyo único fin es acotar los genéricos. Cuando exista una API, el adaptador que traduzca la entrada a un `Answer` deberá construir el tipo correcto para el criterio.

### D2. Conjuntos extensibles con interfaz más enum; vocabulario cerrado con enum simple

- **Principio:** OCP aplicado donde el cambio es previsible.
- **Dónde:** `Severity` + `StandardSeverity`; `EvidenceKind` + `StandardEvidenceKind`; `AuditAction` + `InspectionAudit`, `FindingAudit`, `CertificateAudit`.
- **Por qué:** las escalas de severidad y los tipos de evidencia los define cada entidad certificadora, y las acciones de auditoría crecen con cada agregado. Con la interfaz, sumar valores es sumar un enum, sin tocar el existente. Frente a un `record` con constantes, el enum evita crear un tipo nuevo por un error de tipeo. Los tests `customSeverityScaleIsHonouredWithoutChangingTheDomain` y `schemaDefinedEvidenceKindsWorkWithoutTouchingTheStandardOnes` lo demuestran.
- **Qué quedó como enum simple y por qué:** `Outcome`, `YesNoAnswer`, `VerificationResult`, `CertificateStatus`. Son vocabulario cerrado por el enunciado; si cambian, cambió el lenguaje del negocio y el dominio debe cambiar. Un punto de extensión ahí sería especulativo.
- **Costo:** quien implemente `Severity` o `EvidenceKind` fuera de un enum debe definir `equals`, porque `Criterion` se usa como clave.

### D3. Identidad de negocio y referencias por objeto

- **Principio:** modelar objetos que colaboran, no filas.
- **Dónde:** `Person`, `AssetCode`, `CertificateNumber`; `Finding` referencia su `Inspection` y su `Criterion`; `Certificate` referencia su `Asset` y su `Inspection`; los casos de uso reciben objetos.
- **Por qué:** una entidad tiene identidad solo si el negocio la identifica, y con su identificador real. Los value objects nunca tienen id. Los repositorios quedaron con entre dos y tres métodos cada uno.
- **Alternativa descartada:** un `XxxId` sintético por agregado y casos de uso que reciben ids. Es el estilo habitual con persistencia, pero trataba a los objetos como registros de una base que todavía no existe.
- **Costo:** con persistencia real algunas referencias entre agregados probablemente vuelvan a ser identificadores para no cargar grafos completos. Las entidades sin identificador de negocio se comparan por identidad de objeto.

### D4. `CorrectiveAction` dentro del agregado `Finding`

- **Principio:** frontera de consistencia; Tell, Don't Ask.
- **Dónde:** `Finding.planAction`, `Finding.verifyAction`; `CorrectiveAction.verify` es de paquete.
- **Por qué:** verificar con éxito una acción cierra el hallazgo. Si fueran dos agregados, esa reacción la tendría que orquestar un caso de uso. Adentro del mismo agregado es una consecuencia: `Finding.isOpen()` se deriva de sus acciones.
- **Costo:** un hallazgo con muchísimas acciones se carga entero; irrelevante a esta escala.

### D5. Estado derivado en lugar de almacenado

- **Dónde:** `Inspection.isClosed()` = tiene revisiones; `Finding.isOpen()` = ninguna acción verificada con éxito; `CorrectiveAction.isClosed()` e `isOverdueOn(date)`; `Certificate.statusOn(date)`; `Inspection.evaluate()`.
- **Por qué:** lo que no se guarda no puede quedar inconsistente. La evaluación se recalcula siempre desde las respuestas y las reglas, por eso corregir una respuesta cambia el resultado sin pasos extra.
- **Alternativa descartada:** `boolean closed`, `boolean expired`, enums de estado para hallazgos y acciones, resultados guardados junto a respuestas editables.
- **Excepción deliberada:** `CertificateLifecycle` (`ISSUED`, `SUSPENDED`, `RENEWED`) sí se guarda, porque suspender y renovar son decisiones de alguien y no se pueden deducir del calendario.

### D6. Ciclo de vida de la inspección: revisiones y guardas

- **Dónde:** `StandardInspection.revisions`, `ensureOpen()`, `ensureClosed()`, `Revision`.
- **Por qué:** una inspección cerrada es una inspección con al menos una revisión. `Revision` concentra lo que de otro modo serían un estado "cerrado" con datos y un registro de rectificación aparte. El antes y el después de una rectificación se obtienen comparando dos revisiones consecutivas.
- **Alternativa descartada:** patrón State con `Open` y `Closed`. Los dos estados no comparten ninguna operación válida, así que la interfaz común obligaba a implementar métodos que solo lanzan excepción. Ver sección 4.

### D7. Auditoría con Decorator, separando hechos de dominio de bitácora

- **Principios:** SRP, OCP, DIP; patrón Decorator.
- **Dónde:** `AuditedInspection`, `AuditedFinding`, `AuditedCertificate`; `AuditedInspectionGenerator`, `AuditedFindingGenerator`, `AuditedCertificateGenerator`; puerto `AuditLog<S>`; `AuditEntry`; `InMemoryAuditLog<S>`.
- **Por qué:**
  - Los **hechos de dominio** que las reglas y los informes necesitan viven en el agregado: `Revision` (motivo, autor, fecha), `Verification`, `Suspension`, y desde D12 también el autor y la fecha de cada respuesta, evidencia adjunta y acción planificada. Una rectificación no puede ocurrir sin su revisión.
  - La **bitácora** cronológica de quién hizo qué es transversal y vive en los decoradores. `StandardInspection` no tiene una sola línea de auditoría.
  - El decorador registra después de que la operación fue aceptada: si lanza una excepción, no queda entrada.
  - Las clases `Standard...` y `Audited...` son de paquete. El único punto público donde nace un agregado es su `...Generator`, que exige el `AuditLog` de su tipo por constructor. No hay forma de obtener una inspección sin auditar, ni armando mal la aplicación. El generador también registra la entrada de alta, de modo que ningún caso de uso escribe auditoría.
  - El `AuditLog` se entrega por constructor porque es un colaborador que no cambia entre llamadas; por parámetro van los datos de cada operación.
  - **Un historial por tipo de agregado.** El puerto es `AuditLog<S> { record(S, AuditEntry); historyOf(S) }` y cada agregado auditado tiene el suyo: `AuditLog<Inspection>`, `AuditLog<Finding>` y `AuditLog<Certificate>`. `AuditEntry` no sabe de qué objeto habla, porque el sujeto es la clave del historial. Así `historyOf(asset)` o `historyOf("texto")` no compilan, con la misma garantía que D1 da para las reglas, y cada cliente depende solo del historial que usa (ISP): `GenerateInspectionAct` recibe `AuditLog<Inspection>`. `InMemoryAuditLog<S>` es una sola clase genérica en `details`, y el fixture crea una instancia por tipo. Guarda por identidad (`IdentityHashMap`): el historial es de esa instancia, no de otra que resulte igual.
  - La renovación nace del certificado y no del generador. Por eso `AuditedCertificate.renew` decora el certificado nuevo y registra su emisión él mismo, sin guardar una referencia a su generador. El costo son dos líneas repetidas respecto de `AuditedCertificateGenerator.generate`; la alternativa era una dependencia del decorador hacia quien lo crea.
- **Alternativas descartadas:**
  - *Auditoría dentro del agregado* (`audit.record(...)` en cada método): más barata, y defendible porque la auditoría es requisito de negocio, pero mezcla dos niveles de abstracción en cada operación.
  - *Decorar casos de uso:* exige una interfaz genérica común a los 17 casos de uso y audita con la granularidad equivocada, porque `CloseInspection` produce dos hechos distintos.
  - *Método estático de creación en la interfaz:* hacía que la abstracción conociera a sus implementaciones y obligaba a los casos de uso a transportar el servicio de auditoría.
  - *Herencia* (`AuditedInspection extends Inspection`): menos código, pero contradice composición sobre herencia y audita dos veces si un método público llama a otro.
  - *Bitácora global con `AuditEntry(Object subject, ...)` y `entriesFor(Object)`:* era la versión anterior. Compilaba `entriesFor(asset)` y devolvía una lista vacía sin aviso, y el tipo no decía qué se podía auditar. Además, `AuditTrail` solo existía para armar la entrada con ese `Object`, y desapareció junto con él. Lo que se pierde es la vista cronológica única de todo el sistema, que ningún caso de uso ni informe consultaba: el enunciado pide el historial de cada objeto.
  - *Interfaz marcadora `Auditable`* (vacía, implementada por `Inspection`, `Finding` y `Certificate`) en lugar de `Object`: resolvía el tipo, pero agregaba una interfaz sin métodos cuyo único propósito era tapar el `Object`. Con el sujeto como parámetro genérico del puerto no hace falta ninguna marca.
  - *Interfaces delante de los generadores* (`InspectionGenerator`, `FindingGenerator`, `CertificateGenerator`): se probaron y se retiraron. Tenían una sola implementación y ninguna variante a la vista, que es el anti-patrón que este diseño evita. Tampoco las pedía DIP: los generadores son negocio, viven en `domain`, y que un caso de uso dependa de una clase concreta del dominio ya respeta la dirección de dependencias. Lo único que aportaban era que los casos de uso dejaran de nombrar una clase llamada `Audited...`, y eso no justificaba tres tipos más. Si aparece una segunda forma de crear agregados, la interfaz se extrae en ese momento.
- **Costo:** diez clases más que la versión interna: tres interfaces, tres decoradores, tres generadores e `InMemoryAuditLog`. Reenvío manual de las consultas en cada decorador. Tres historiales para armar en la raíz de composición en lugar de uno.
- **Nombres:** `...Generator` y `generate` fueron elegidos por el equipo.

### D8. Política de condicionales

- **Regla:** `if` solo como cláusula de guarda que lanza una excepción. Nunca `if`, `switch` ni `instanceof` para decidir por tipo o por estado.
- **Verificación:** en `src/main` hay 42 `if` y los 42 preceden a un `throw`; cero `instanceof`, cero `switch`, cero `return null`, cero setters, cero métodos estáticos.
- **Qué reemplaza a los condicionales:** genéricos (D1), enums con comportamiento (`Severity`, `Outcome.raisesFinding`, `VerificationResult.closesAction`, `CertificateLifecycle.statusOn`), estado derivado (D5), `Optional.orElseThrow` e `ifPresent` en búsquedas, streams para selección, y una tabla `Map<VerificationResult, FindingAudit>`.
- **Tres ternarios declarados**, los tres sobre un predicado de negocio y ninguno sobre un tipo o un estado: `Criterion.outcomeOf` (cumple → aprobado; si no, lo que diga la severidad), `CriterionResponse.outcomeOfAnswered` (con la evidencia exigida → resultado; si no, pendiente) y `CertificateLifecycle.ISSUED.statusOn` (dentro de la vigencia → activo; si no, vencido).
- **`Optional`** se usa solo donde la ausencia es un resultado legítimo: `CriterionResponse.answer()` y las búsquedas de repositorio. No se usa como control de flujo.

### D9. Puertos segregados

- **Principios:** ISP, DIP.
- **Dónde:** `AssetRepository {save, findByCode}`, `InspectionSchemaRepository {save, findByAssetType}`, `InspectionRepository {save, findByAsset}`, `FindingRepository {save, saveAll, findByAsset}`, `CertificateRepository {save, findCurrentFor}`, `CertificateNumbering {next}`, `AuditLog<S> {record, historyOf}`. Los seis primeros viven en `ports`; `AuditLog` vive en `models.audit` por el motivo explicado en la sección 2.
- **Por qué:** cada interfaz tiene lo que sus casos de uso usan. La única consulta con lógica, `findCurrentFor`, delega la decisión en `Certificate.isCurrentOn(date)`.

### D10. Elegibilidad para certificar como política intercambiable

- **Principio:** OCP; patrón Strategy.
- **Dónde:** interfaz `CertificationEligibility` e implementación `NoBlockingFindingsEligibility`, usadas por `IssueCertificate` y `RenewCertificate`.
- **Por qué:** a diferencia de los generadores, acá sí hay una segunda política identificada. El equipo discutió y descartó "cualquier hallazgo abierto impide certificar" (ver S5); otra entidad certificadora podría elegirla. Cambiar de política es escribir otra implementación y entregarla al armar la aplicación, sin tocar los casos de uso.
- **Costo:** una interfaz con una sola implementación hoy. Se acepta porque el eje de variación es concreto y ya tiene un segundo candidato, que es el criterio que este documento exige para cada abstracción.

### D11. Errores explícitos

- Reglas de negocio: excepciones con nombre del dominio que extienden `DomainException` (`InspectionAlreadyClosedException`, `CriterionNotInSchemaException`, `VerifierMustDifferFromResponsibleException`, `BlockingFindingsPreventCertificationException`, entre otras).
- Los constructores validan reglas de dominio, no la forma del dato. `null` nunca es un valor válido en este código y por eso no se chequea: que un dato llegue nulo es un problema de entrada, y cuando exista una API lo validará el adaptador.
- Los textos vacíos sí se validan, porque un código de activo, un motivo o un alcance en blanco no significan nada para el negocio: `IllegalArgumentException` en los value objects, `InvalidSchemaException` en el esquema y `RectificationReasonRequiredException` al rectificar.
- Las excepciones de dominio viven en un subpaquete `exceptions` dentro del paquete de su concepto (`models.inspection.exceptions`, `models.finding.exceptions`, `models.certificate.exceptions`, `models.schema.exceptions`). La de `models.evaluation` queda junto a la clase que la lanza, y las de reglas que cruzan agregados viven en `application.exceptions`.
- Las operaciones validan antes de mutar. El test `rectificationWithoutReasonIsRejectedAndChangesNothing` lo comprueba.

### D12. Quién y cuándo como hechos de dominio

- **Principio:** coherencia del modelo; ningún parámetro existe solo para otro objeto.
- **Dónde:** `GivenAnswer(value, answeredBy, answeredAt)`, `AttachedEvidence(evidence, attachedBy, attachedAt)`, `CorrectiveAction.plannedBy()` y `plannedAt()`; `CriterionResponse` guarda `given` y `attachments`.
- **Por qué:** registrar una respuesta, adjuntar evidencia y planificar una acción recibían quién y cuándo, pero el núcleo los descartaba y solo el decorador los usaba para la bitácora. Observaciones, revisiones, verificaciones y suspensiones ya guardaban su autor y su fecha, así que era una inconsistencia. Ahora el dato vive en el agregado: corregir una respuesta produce un `GivenAnswer` nuevo, y el acta muestra quién respondió cada criterio sin consultar la auditoría.
- **Una sola representación pública.** `CriterionResponse` expone únicamente `given()` y `attachments()`. La respuesta y la evidencia "peladas" que necesita la evaluación son métodos privados del record. `Finding` conserva la evidencia con su procedencia (`List<AttachedEvidence>`) y `ActLine` muestra autor y fecha de la respuesta.
- **Alternativa descartada:** exponer además `answer()` y `evidence()` públicos. Evitaba tocar `Finding` y el acta, pero ofrecía dos formas de pedir lo mismo y dejaba el dato nuevo sin ningún consumidor en producción.
- **Costo:** dos records más; autor y fecha quedan tanto en el dominio como en la bitácora, igual que ya ocurría con las revisiones.

### D13. Vista de solo lectura de la inspección

- **Principio:** ISP.
- **Dónde:** `InspectionView` declara las consultas; `Inspection` la extiende y agrega los cinco comandos. `Finding.inspection()` y `Certificate.basedOn()` devuelven `InspectionView`.
- **Por qué:** referenciar objetos en lugar de ids (D3) entrega el objeto entero. Antes compilaba `finding.inspection().rectify(...)`: tener un hallazgo o un certificado daba acceso de escritura a otro agregado. Ahora ven el mismo objeto a través de un tipo más angosto, que dice lo único que necesitan saber: si la inspección está cerrada y a qué activo corresponde.
- **Alternativa descartada:** volver a ids entre agregados. Resolvía la fuga, pero deshacía D3.
- **Costo:** una interfaz más. La protección es de compilación: un cast la saltearía, y los casts no se usan en este código. Los casos de uso de certificación y el acta siguen recibiendo `Inspection` aunque solo leen.

---

## 4. Patrones que se decidió no aplicar

| Patrón | Por qué no | Consecuencia |
|---|---|---|
| **State** para la inspección | Abierta y cerrada no comparten operaciones válidas; la interfaz común forzaba métodos que solo lanzan excepción. Con guardas permitidas y el estado derivado de las revisiones, eran dos clases de estructura sin comportamiento. | Si aparecen más estados con comportamiento compartido (por ejemplo "en revisión"), habría que reconsiderarlo. |
| **Factory de la clase 2** (selección por `applyFor`) | Resuelve elegir en ejecución entre implementaciones. Acá no hay nada que elegir: hay una sola clase de inspección y la selección regla-respuesta la resolvieron los genéricos. | Los `...Generator` componen, no seleccionan, y por eso no se llaman Factory. Son clases concretas; si aparece una segunda forma de crear agregados se extrae la interfaz en ese momento, como ocurrió en la clase con `CallCostCalculatorFactory`. |
| **Event Sourcing / eventos de dominio** | La bitácora por decorador alcanza y la única reacción entre agregados desapareció al mover `CorrectiveAction` dentro de `Finding`. | No se puede reconstruir el estado desde el historial. |
| **Builder** | Los records con constructores compactos validados alcanzan; el fixture de tests cubre la comodidad. | Construcciones con cinco o seis argumentos en algunos lugares. |
| **Repositorio genérico `Repository<T, ID>`** | Viola ISP: ningún caso de uso necesita CRUD completo, y la mayoría de los agregados no tiene id. | Una interfaz por agregado. |
| **Herencia para tipos de activo o de evidencia** | El tipo es un dato. | `AssetType` es un value object; `EvidenceKind` un enum extensible. |
| **Interfaces con una sola implementación sin motivo** | `Inspection`, `Finding` y `Certificate` son interfaces porque el Decorator exige dos implementaciones. `CertificationEligibility` tiene hoy una sola, pero con un eje de variación concreto (D10). Los casos de uso, `Asset`, `InspectionSchema` y los generadores son clases concretas; las interfaces de generadores se probaron y se retiraron (D7). | Ninguna `XxxImpl`. |
| **Estados "planificada" y "en ejecución"** | El enunciado no los pide y cada estado suma transiciones que probar. | La inspección nace abierta; la acción correctiva nace planificada. |

---

## 5. Riesgo de negocio → test que lo cubre

| Riesgo | Test |
|---|---|
| Una versión nueva del esquema altera una inspección en curso | `SchemaVersioningTest.newSchemaVersionDoesNotAffectAnInspectionAlreadyStarted` |
| Las inspecciones nuevas no toman la última versión | `SchemaVersioningTest.inspectionsAssignedAfterPublishingUseTheNewVersion` |
| Se cierra con criterios sin responder | `InspectionExecutionTest.inspectionCannotBeClosedWhileCriteriaRemainUnanswered` |
| Se cierra sin la evidencia obligatoria | `InspectionExecutionTest.answeredCriterionStaysPendingUntilItsRequiredEvidenceIsAttached` |
| La misma evidencia adjunta dos veces cuenta como dos | `InspectionExecutionTest.sameEvidenceCannotBeAttachedTwiceToTheSameCriterion` |
| Dos requisitos del mismo tipo se cumplen con una sola evidencia | `CriterionTest.criterionCannotRequireTheSameEvidenceKindTwice` |
| Corregir una respuesta no cambia la evaluación o no deja rastro | `InspectionExecutionTest.correctingAnAnswerBeforeClosingChangesTheEvaluationAndIsAudited` |
| Una inspección cerrada acepta cambios directos | `InspectionExecutionTest.closedInspectionRejectsNewAnswersEvidenceAndObservations` |
| Una operación rechazada deja igual una entrada de auditoría | `InspectionExecutionTest.closedInspectionRejectsNewAnswersEvidenceAndObservations` |
| Hallazgos con severidad, evidencia o responsable incorrectos | `FindingsTest.closingRaisesOneFindingPerNonConformityWithItsSeverityEvidenceAndAssetResponsible` |
| Acción vencida un día antes o un día después | `CorrectiveActionTest.actionIsNotOverdueOnItsDueDateButIsTheDayAfter` |
| Una verificación rechazada cierra algo | `CorrectiveActionTest.rejectedVerificationKeepsActionAndFindingOpenAndIsAudited` |
| El responsable de la acción se verifica a sí mismo | `CorrectiveActionTest.whoeverIsResponsibleForTheActionCannotVerifyIt` |
| Se certifica con un hallazgo bloqueante abierto | `CertificationTest.assetWithAnOpenBlockingFindingIsNotEligible` |
| Un hallazgo menor impide certificar | `CertificationTest.openMinorFindingsDoNotPreventCertification` |
| No se puede certificar después de corregir | `CertificationTest.assetBecomesEligibleOnceItsBlockingFindingIsClosed` |
| Se certifica sobre una inspección sin cerrar | `CertificationTest.inspectionStillInProgressCannotBackACertificate` |
| Dos certificados vigentes para el mismo activo | `CertificationTest.assetHoldsASingleCurrentCertificate` |
| Se esquiva una suspensión emitiendo otro certificado | `CertificationTest.suspendedCertificateStillOccupiesTheAssetSoANewOneCannotBeIssuedAroundIt` |
| Vence un día antes o un día después | `CertificationTest.certificateIsActiveThroughItsLastDayAndExpiredTheDayAfter` |
| Suspender dos veces, o sin motivo registrado | `CertificationTest.suspensionIsRecordedWithItsReasonAndCannotBeRepeated` |
| Renovar pierde el certificado anterior o se saltea la elegibilidad | `CertificationTest.renewalIssuesANewCertificateAndRetiresThePreviousOne`, `renewalIsSubjectToTheSameEligibilityAsIssuance` |
| La rectificación pierde la revisión original o no queda auditada | `RectificationTest.rectificationPreservesTheOriginalRevisionAndIsAudited` |
| Una rectificación inválida modifica el acta | `RectificationTest.rectificationWithoutReasonIsRejectedAndChangesNothing` |
| Una rectificación altera hallazgos ya levantados | `RectificationTest.rectificationLeavesFindingsAlreadyRaisedUntouched` |
| Los informes no reflejan el estado real | `EndToEndFlowTest.assetGoesFromInspectionToCertificateAndEveryReportTellsTheSameStory` |
| Límites del rango mal interpretados | `NumericRangeRuleTest.rangeLimitsAreInclusiveAndAnythingBeyondThemFails` |
| Regla numérica mal configurada | `NumericRangeRuleTest.rangeWhoseMinimumExceedsItsMaximumIsAnInvalidConfiguration`, `OptionInListRuleTest.ruleWithoutAcceptedOptionsIsAnInvalidConfiguration` |
| Tipo de respuesta incorrecto para un criterio | No compila: `ApprovalRule<A>` y `recordAnswer(Criterion<A>, A, ...)`. Ver D1. |
| Pedir el historial de algo que no es el agregado auditado | No compila: `AuditLog<Inspection>.historyOf(...)` solo acepta una inspección. Ver D7. |
| Se pierde quién registró una respuesta, adjuntó una evidencia o planificó una acción | `InspectionExecutionTest.answersAndEvidenceRememberWhoGaveThemAndWhen`, `CorrectiveActionTest.actionRemembersWhoPlannedItAndWhen` |
| Extender severidades o tipos de evidencia obliga a tocar el dominio | `CriterionTest.customSeverityScaleIsHonouredWithoutChangingTheDomain`, `EvidenceRequirementTest.schemaDefinedEvidenceKindsWorkWithoutTouchingTheStandardOnes` |

Los tests de integración usan los repositorios en memoria reales, sin mocks, y un `MutableClock`.
`CertiflowFixture` es la raíz de composición compartida. Total: 72 tests.

---

## 6. Limitaciones conocidas y concesiones

- **Concurrencia.** Los agregados no son seguros para uso concurrente y los repositorios en memoria tampoco. Dos cierres simultáneos de la misma inspección no están contemplados. `SequentialCertificateNumbering` es la única pieza con un contador atómico.
- **Transacciones.** `CloseInspection` guarda la inspección y los hallazgos en dos llamadas; `RenewCertificate` guarda dos certificados. Con persistencia real hace falta una unidad de trabajo.
- **Persistencia.** Las referencias por objeto entre agregados (D3) y la igualdad por identidad suponen un único proceso en memoria. `Criterion` se usa como clave de mapa gracias a la igualdad por valor de los records.
- **Garantía de auditoría.** Está dada por la API (el generador exige un `AuditLog`), no por la operación misma. Un `AuditLog` que descarte entradas la anula.
- **Sin vista cronológica global.** Cada tipo de agregado tiene su propio historial (D7). Una consulta del estilo "todo lo que pasó el martes" exigiría recorrer los tres historiales o sumar un puerto de lectura global cuando algún caso de uso lo pida.
- **Rectificación.** No revisa hallazgos ya levantados (S6). Si una rectificación demuestra que un hallazgo bloqueante era un error de carga, igual hay que cerrarlo por su flujo de acción correctiva.
- **Suspensión.** No se puede levantar; la salida es renovar (S11).
- **Navegación en modelos de lectura.** Los casos de uso de informes y los generadores leen dos niveles (`finding.criterion().code()`, `inspection.asset()` y luego `asset.responsible()`). Es una concesión a la Ley de Demeter aceptada en lectura, no en lógica de negocio.
- **Entorno.** Si un IDE compila en paralelo hacia `target/` con un JDK anterior al 21, pisa las clases de Maven y los tests fallan con "Unresolved compilation problem". `mvn clean test` lo resuelve.
