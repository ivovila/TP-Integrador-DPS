# Decisiones de diseño · Certiflow

## 1. Objetivo y criterio de alcance

La entrega demuestra el negocio mediante un proyecto Java compilable, implementaciones concretas y pruebas. Se implementan todas las capacidades enumeradas en las dos páginas de `certiflow-entrega-1.pdf` al nivel de dominio y casos de uso. No se agrega REST, autenticación, base de datos, frontend ni despliegue.

Separamos lo exigido por la consigna de las decisiones necesarias para volverla ejecutable. Las políticas de esta sección deben validarse con la cátedra; cambiarlas puede requerir modificar reglas y pruebas.

## 2. Supuestos explícitos de negocio

| Tema no completamente definido | Decisión implementada | Consecuencia |
|---|---|---|
| Versión vigente | Última versión publicada al **iniciar**, no al asignar, la inspección | Una publicación entre asignación e inicio sí se aplica; después del inicio no |
| Vigencia de publicación | Publicar hace efectiva la versión inmediatamente; no hay programación futura | `publishedAt` se obtiene del reloj del caso de uso |
| Alcance | Texto descriptivo; todos los criterios de la versión son obligatorios | No hay selección parcial ni resultado «no aplica» |
| Numéricos | Rangos inclusivos aceptable y preferido; dentro del preferido aprobado, fuera del preferido pero dentro del aceptable observado, fuera del aceptable rechazado | Unidades exactas, sin conversión implícita; `BigDecimal` evita errores binarios |
| Booleanos | Valor esperado configurable; incumplir produce observado o rechazado según regla | La severidad se configura por criterio |
| Documentación | Se verifica declaración y presencia de referencia de tipo documento | No se interpreta contenido de archivos ni se valida su existencia remota |
| Evidencia obligatoria | Se exige al menos una evidencia por cada tipo requerido | Falta de respuesta o evidencia produce `INCOMPLETE`; no se confunde con incumplimiento demostrado |
| Cierre | Reevalúa todas las respuestas; incompletitud impide cerrar, incumplimiento no | El acta puede cerrarse con hallazgos para iniciar correcciones |
| Hallazgos | Uno por criterio observado o rechazado, creado al cerrar | Severidad tomada del criterio; responsable inicial tomado del activo |
| Corrección | Una acción por hallazgo y revisión; debe aportar evidencia, verificarse y cerrarse | Si la verificación falla, vuelve a planificada para nueva presentación |
| Verificador | No puede ser la misma identidad textual que el responsable de la corrección | Regla de separación de funciones; no reemplaza autenticación |
| Certificación | Todos los hallazgos, de cualquier severidad, requieren acción verificada y cerrada | No se reescribe el resultado original de la inspección: se conserva junto a su resolución |
| Vencimiento de acciones | Vencida cuando hoy es posterior a su fecha límite y no está cerrada | Todavía puede corregirse fuera de plazo; el atraso no impide registrar la realidad |
| Vigencia del certificado | Número positivo de días definido en la versión, intervalo `[emisión, vencimiento)` | Expira exactamente en el instante final, sin proceso programado |
| Rectificación | Reabre como una nueva revisión de la misma inspección con motivo, autor e instante | Preserva la revisión anterior, mantiene versión del esquema, invalida evaluación actual y exige recierre |
| Efecto de rectificar | Un certificado de una revisión anterior deja de estar activo automáticamente | Recerrar no lo reactiva; se debe emitir uno para la nueva revisión |
| Acciones de revisiones anteriores | Se conservan pero no se siguen modificando ni resuelven hallazgos nuevos | No se reutiliza una verificación anterior para limpiar automáticamente una revisión nueva |
| Renovación | Nueva inspección del mismo activo, iniciada desde la emisión anterior, elegible | Nuevo certificado; anterior queda `SUPERSEDED` y registra el identificador de reemplazo |
| Duplicados | Una emisión por revisión y a lo sumo un certificado activo por activo | La renovación reemplaza al activo anterior; una rectificación puede justificar una nueva emisión |
| Informes | Proyecciones estructuradas e inmutables de los datos de negocio | PDF/HTML y plantillas visuales quedan para adaptadores posteriores |

## 3. Arquitectura y dependencias

```text
Demo / futura interfaz
        |
        v
Casos de uso (application) ------> contratos (application.port)
        |                                   ^
        v                                   |
Dominio y reglas                 adaptadores en memoria
```

El dominio no importa aplicación ni infraestructura. Los casos de uso dependen de objetos del dominio y contratos de repositorios. Los adaptadores implementan esos contratos. `CertiflowDemo` y la configuración de tests son los lugares que construyen implementaciones concretas.

Las carpetas no son servicios desplegables ni módulos Maven independientes: se utiliza un único artefacto pequeño. Los repositorios se ubican en `application.port` porque expresan necesidades de los casos de uso; las entidades no guardan ni se buscan a sí mismas.

**Alternativa descartada:** usar Spring, JPA y controladores desde el inicio. Agregarían decisiones técnicas sin demostrar mejor las reglas de esta entrega. Consecuencia: el cableado es manual y todavía no hay persistencia duradera ni fronteras transaccionales externas.

## 4. Modelo con comportamiento e invariantes

`Inspection`, `CorrectiveAction` y `Certificate` son clases finales con constructores privados y operaciones que expresan transiciones. No hay setters públicos. Cada operación valida sus precondiciones y devuelve un nuevo objeto.

Ejemplo: `Inspection.record` solo opera en curso, reconoce el criterio y valida el tipo de respuesta y su unidad. `close` recalcula los resultados e impide cerrar datos incompletos. La fábrica de certificados comprueba elegibilidad, incluso si se invoca directamente sin pasar por el caso de uso.

Se devuelven copias inmutables de listas, mapas y conjuntos. Un consumidor no puede alterar una inspección cerrada mediante `getRespuestas().clear()`. Las colecciones anidadas también se construyen con copias defensivas. `Asset`, respuestas, evidencias, criterios y evaluaciones son valores inmutables. La identidad de entidades está dada por UUID; no se usa igualdad estructural de dos snapshots de `Asset` para decidir si es el mismo activo.

**Principios:** encapsulamiento, alta cohesión, Tell Don't Ask y responsabilidad única. La aplicación coordina; los objetos protegen sus reglas locales. Las restricciones que requieren consultar otros agregados —por ejemplo, certificado activo duplicado— se verifican en el caso de uso.

**Alternativas descartadas:** entidades con setters y un servicio central con todas las validaciones; agregar una interfaz a cada entidad. La primera permite estados inválidos, la segunda no aporta sustitución útil.

**Costo aceptado:** copiar colecciones y snapshots consume memoria. Es apropiado para la entrega y simplifica auditoría y pruebas; no es una solución de almacenamiento para volúmenes ilimitados.

## 5. Versionado y rectificación son ejes distintos

`SchemeVersion` es un snapshot publicado de secciones, criterios, evidencias requeridas, reglas y duración del certificado. El repositorio no permite sobrescribir un número de versión. La inspección selecciona su versión al iniciar y mantiene esa referencia inmutable.

`InspectionRevision` preserva el contenido de una inspección cerrada cuando se abre una rectificación. Cambiar la revisión de una inspección **no** cambia la versión del esquema. Cada nueva revisión conserva respuestas como punto de partida, descarta resultados actuales y debe reevaluarse. Los hallazgos generados en el nuevo cierre tienen identidades nuevas.

Las implementaciones de `EvaluationRule` deben ser inmutables y deterministas: forma parte de su contrato. Las tres implementaciones incluidas son records con valores inmutables. Un plugin futuro que retenga estado mutable violaría el contrato y la garantía histórica; debe acompañarse de pruebas de contrato.

**Alternativa descartada:** guardar solo el ID del esquema y consultar «la última versión» en cada evaluación. Cambiaría retrospectivamente las reglas y haría imposible defender los resultados históricos.

## 6. Estrategias de evaluación: OCP con un punto de extensión concreto

`EvaluationRule` es Strategy: abstrae la evaluación de una respuesta, con `NumericRangeRule`, `BooleanRule` y `DocumentaryRule`. `Criterion` compone una estrategia con severidad y evidencias requeridas. `Evaluation` devuelve tanto resultado como explicación.

La comprobación común de evidencia está en `Criterion`; el algoritmo específico está en la regla. Los casos de uso no tienen un switch por tipo de criterio. Agregar una estrategia para los tipos de respuesta existentes no requiere cambiar el evaluador de inspecciones.

`Answer` es una interfaz sellada para representar los tipos de entrada soportados con precisión. Introducir una categoría nueva de dato sí requiere extender esa jerarquía y sus pruebas. No afirmamos que cualquier cambio imaginable pueda implementarse sin modificar código.

Las estrategias rechazan tipos y unidades incompatibles de manera explícita. El contrato contempla ese rechazo, por lo que no se promete que cualquier estrategia evalúe cualquier respuesta. La incompletitud se trata por separado de observado/rechazado.

**Alternativas descartadas:** condicionales por tipo en un servicio central y un motor genérico de expresiones. El primero concentra cambios; el segundo agrega parsing, seguridad y complejidad no pedidos.

## 7. Ciclos de vida y patrón State descartado inicialmente

```text
Inspección: ASSIGNED -> IN_PROGRESS -> CLOSED
                                      CLOSED -> IN_PROGRESS (rectificación, revisión + 1)

Acción: PLANNED -> SUBMITTED -> VERIFIED -> CLOSED
                   SUBMITTED -> PLANNED (verificación rechazada)

Certificado: ACTIVE -> SUSPENDED
             ACTIVE/SUSPENDED -> EXPIRED por tiempo
             ACTIVE/SUSPENDED/EXPIRED -> SUPERSEDED por renovación
```

La rectificación también hace efectiva la suspensión de certificados históricos. `Certificate.statusAt` calcula estado con el instante consultado y la inspección fuente actual. No es una consulta temporal retrospectiva: recibe snapshots actuales. No se almacena un booleano «vencido» que podría quedar desactualizado.

Usamos enums y guardas cercanas a cada operación; **no aplicamos State** con una clase por estado porque las transiciones todavía son pequeñas. Si el comportamiento por estado crece mucho, se puede reconsiderar. No se intenta evitar todo `if`: las guardas expresan reglas legítimas.

## 8. Auditoría como información de negocio

Cada transición de inspección, acción y certificado registra `AuditEntry`: instante, actor, operación y detalle. Registrar una respuesta conserva su valor anterior y nuevo. Las verificaciones aceptadas y rechazadas conservan motivos. Las publicaciones guardan versión, autor e instante, y todas las versiones continúan disponibles.

Las revisiones cerradas se preservan estructuralmente, no solo como mensajes. El acta mantiene el resultado original aunque una acción correctiva permita posteriormente certificar. Los activos no tienen operación de modificación en esta entrega; su alta crea un valor inmutable y la inspección conserva ese snapshot.

El vencimiento y la suspensión derivada de una rectificación no agregan eventos al consultar: se explican por `expiresAt` o por la revisión fuente y su rectificación auditada. La suspensión manual sí registra motivo propio. El informe de certificado explica si su fuente fue rectificada.

**No se implementa Event Sourcing:** la fuente de verdad es el snapshot actual y sus revisiones/historial, no la reproducción de eventos. Tampoco se simula auditoría con logs de consola. Los detalles textuales son útiles para lectura humana, pero no constituyen un esquema de eventos versionado para integraciones externas.

## 9. DIP, ISP e inyección sin framework

`InspectionService` recibe `AssetRepository`, `SchemeRepository`, `InspectionRepository` y `Clock` por constructor. Desconoce si el almacenamiento futuro será SQL, archivos o red. Los demás servicios reciben solo los repositorios necesarios para su responsabilidad.

Cada repositorio tiene contrato propio: no hay un `Repository<T>` público que fuerce operaciones CRUD irrelevantes. `SchemeRepository` expresa publicación append-only. Sus métodos retornan objetos de nuestro negocio, nunca DTOs de un proveedor ni entidades JPA.

Esto aplica la misma lección de dps-tp1: una interfaz debe expresar la necesidad de quien la usa y evitar filtrar detalles externos. Inyectar dependencias es la técnica; invertir la dirección de dependencias es la decisión arquitectónica.

**No hay interfaz para cada caso de uso:** todavía no existen variantes que la justifiquen. El dominio puede depender de clases concretas del propio dominio. Crear `IInspection` o wrappers que solo deleguen no mejora DIP.

## 10. Certificación y consistencia entre agregados

`CertificationEligibility` concentra la regla de elegibilidad y es usada por la fábrica de `Certificate`. Exige inspección cerrada y completa y acciones cerradas para todos sus hallazgos. `CertificationService` agrega la consulta de certificados existentes y las condiciones de renovación. La creación directa tampoco puede eludir la elegibilidad local; la unicidad global pertenece al caso de uso y los repositorios.

Al renovar se construyen y validan ambos snapshots antes de guardar, para que una regla fallida no deje cambios parciales en memoria. Sin embargo, **no hay garantía de transacción frente a fallos de almacenamiento ni concurrencia**: dos escrituras a repositorios externos deberían ejecutarse en una transacción/Unit of Work. Tampoco las comprobaciones de unicidad son atómicas frente a dos procesos simultáneos.

Los adaptadores actuales son para ejecución monohilo en memoria. No se presentan como persistencia de producción. Esta limitación es deliberada, dado que la entrega no exige persistencia real; incorporar una base de datos requerirá transacciones, unicidad y control de versión optimista.

## 11. Tiempo, identidad y errores

Se inyecta `Clock` para poder probar límites exactos de vencimiento sin esperar ni usar sleeps. Los días límite de acciones usan la zona del reloj. Los certificados usan instantes UTC y días de 24 horas; esta decisión evita una ambigüedad de husos horarios y está explicitada.

Se emplean UUID para identidad. No se introduce un generador de IDs como interfaz porque las pruebas pueden usar los identificadores devueltos sin predecirlos. Los actores son identificadores textuales no vacíos; un sistema futuro debe mapear usuarios autenticados a esos identificadores estables.

`DomainException` informa precondiciones de negocio inválidas. Los valores obligatorios nulos se rechazan al construir mediante `Objects.requireNonNull`. Los casos de uso no capturan y silencian errores; un adaptador de entrada futuro podrá traducirlos a mensajes de usuario. El tiempo de auditoría no puede retroceder dentro de una entidad.

## 12. Informes sin acoplar el dominio a un formato

`ReportService` construye un acta con la inspección cerrada, un resumen de hallazgos con acciones de la revisión actual y un certificado con estado efectivo, activo y versión. Son snapshots estructurados e inmutables. El historial de revisiones anteriores sigue disponible en el acta y las acciones históricas en el repositorio.

**Alternativa descartada:** generar PDF dentro de `Inspection` o `Certificate`. Cambiar un logo o una plantilla no debería modificar una regla de negocio. En otra entrega un renderizador consumirá estas proyecciones. No se añade un `ReportRenderer` vacío antes de necesitarlo.

## 13. Otras decisiones de simplicidad

- **Composición sobre herencia:** el tipo de activo es un dato y selecciona esquemas; no hay comportamiento diferente que justifique `Laboratorio extends Activo`.
- **Sin microservicios, CQRS, buses ni Observer:** no existe necesidad de integración distribuida en esta entrega. Reportes separados de comandos no implican una arquitectura CQRS completa.
- **Sin Builder obligatorio:** records y fábricas validan los valores actuales. Si la construcción se vuelve confusa, un Builder puede mejorar ergonomía, pero no sustituye invariantes.
- **Sin repositorio genérico base:** cuatro adaptadores pequeños repiten unas pocas operaciones a cambio de contratos explícitos y fáciles de leer.
- **Sin mocks masivos:** se integran implementaciones reales en memoria para comprobar reglas y colaboración; solo se controla el reloj.
- **Sin porcentaje arbitrario como criterio de calidad:** JaCoCo es diagnóstico opcional. La selección de pruebas parte de comportamientos y riesgos, no de getters.

## 14. Pruebas y defensa de la entrega

| Riesgo de negocio | Prueba representativa |
|---|---|
| Alteración retrospectiva | `versionIsSelectedAtStartAndRemainsFrozenAfterNewPublication` |
| Aprobación incorrecta en límites | `numericBoundariesAreInclusive` |
| Evidencia insuficiente | `missingAnswerOrRequiredEvidenceIsIncomplete` |
| Cambios tras cierre | `closedInspectionRejectsChangesAndDuplicateClosureWithoutDuplicatingFindings` |
| Pérdida de historia | `rectificationPreservesOriginalAnswersResultsFindingsAndVersion` |
| Resultado viejo tras editar respuesta | `updatingAnAnswerInvalidatesPreviousEvaluationAndKeepsBothValuesInAudit` |
| Autoverificación | `correctionRequiresEvidenceIndependentVerificationAndClosure` |
| Reutilizar resolución antigua | `supersededFindingsCannotBeResolvedOrUsedToClearNewRevision` |
| Certificado vencido considerado vigente | `expiryIsEffectiveAtExactBoundaryWithoutBackgroundJob` |
| Certificación apoyada en inspección rectificada | `rectificationImmediatelySuspendsCertificateAndReclosureDoesNotReactivateIt` |
| Duplicados | `cannotIssueTwiceForSameRevisionOrHaveTwoActiveCertificatesForAsset` |
| Renovación fallida altera certificado anterior | `renewalFailureDoesNotModifyOldCertificate` |
| Integración e informes incoherentes | `completeFlowProducesConsistentBusinessReportsAndAudit` |

Para defender el trabajo, seguir el escenario de `CertiflowDemo`: indicar qué regla vive en cada objeto, mostrar qué rechaza la operación y dónde queda el historial. Cambiar el umbral publicando otra versión y demostrar que no cambia la inspección iniciada. La justificación de SOLID se apoya en ese comportamiento observable y en la dirección de dependencias.
