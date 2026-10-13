# Spec · F3 · Políticas de certificación por jurisdicción

Fecha: 2026-10-12 · Rama: `entrega-2-spec1` · Consigna: [CertiFlow · Entrega 2](../consigna/certiflow-entrega-2.md)

## 1. Objetivo

Seleccionar una política de certificación según la jurisdicción del activo. La política determina qué
severidades bloquean, si admite certificación condicional y qué plazo corresponde; cada decisión conserva
la identidad y versión de la política aplicada. Esta spec define comportamiento futuro sobre fase 1.

### Criterios de aceptación

1. Un activo tiene una jurisdicción explícita, distinta de su ubicación como texto.
2. El mismo resultado cerrado produce decisiones diferentes bajo dos jurisdicciones de demostración.
3. Añadir una jurisdicción requiere registrar una política/configuración, sin editar el servicio central ni un switch.
4. Evaluación, emisión y renovación aplican el mismo mecanismo de selección y validación.
5. Las decisiones distinguen `APPROVED`, `CONDITIONAL` y `REJECTED`, con motivos estructurados.
6. El plazo y las condiciones se obtienen de la política; el cliente no puede extenderlos arbitrariamente.
7. La emisión persiste decisión, jurisdicción, referencia de política y datos suficientes para explicar el resultado.
8. Cambiar una política afecta decisiones nuevas; no reescribe certificados ni decisiones emitidas.
9. Sin política registrada se rechaza explícitamente; no se aplica una jurisdicción de fallback.
10. F1 reutiliza estas políticas por subsistema y deriva un global que conserva las decisiones de sus parciales.

### Fuera de alcance

- Motor de reglas genérico, DSL, administración de políticas por REST, reglamentaciones legales reales.
- Cambio de jurisdicción de activos ya registrados (no hay caso de uso actual de edición).
- Activación futura de políticas: F2 programa versiones de ESQUEMAS, no versiones de política.
- Resolver condiciones automáticamente, revocar por cada hallazgo nuevo, notificaciones y autenticación.

---

## 2. Reglas y supuestos de negocio

### 2.1 Jurisdicción y momento de aplicación

- `JurisdictionCode` es un VO de Catálogo, no un enum cerrado ni una cadena inferida de `Location`.
- El registro de activo recibe ese código y valida que exista política configurada; no exige importar Spring.
- Al evaluar, emitir o renovar se carga el activo por su código, se selecciona la política vigente en la
  configuración actual y se captura `evaluatedAt` del Clock una sola vez. Usar su zona para fechas calendario.
- Se aplica la jurisdicción del activo al decidir, no la que el cliente adjunte al command de certificación.
- La decisión persiste jurisdicción y política como snapshot. Aunque en el futuro se permita mover un activo,
  la historia de certificados conserva la jurisdicción original.
- El registro de políticas se construye como mapa inmutable por proceso. Dos entradas activas para el mismo
  código son error de configuración al iniciar; una solicitud sin entrada genera error explícito.

### 2.2 Separar evaluación de criterio y certificación

`Severity.outcomeWhenUnmet()` sigue determinando `OBSERVED`/`REJECTED` en la inspección.
La jurisdicción NO altera respuestas, actas, severidades ni hallazgos para cambiar el resultado de inspección.
Para certificar se deja de usar `Severity.blocksCertification()` como decisión universal: el método se elimina y todos los flujos consultan la política seleccionada.
Actualizar `OpenNonConformity.blocksCertification`, `InspectionResult.blockingNonConformities` y
`NoBlockingFindingsEligibility` para que no exista un segundo camino con reglas fijas contradictorias.

### 2.3 Decisión

`CertificationPolicy.evaluate(CertificationAssessmentContext)` es Strategy. El contexto contiene resultado del ACL,
alcance a certificar, jurisdicción, instante, fecha de emisión y datos necesarios del esquema. No contiene JPA.

`CertificationDecision` evoluciona para llevar:

- `outcome`: `APPROVED`, `CONDITIONAL` o `REJECTED`.
- `reasons`: códigos y detalles con hallazgos/subsistemas afectados, no solo un mensaje concatenado.
- `conditions`: referencias de hallazgos abiertos, descripción y fecha límite cuando es condicional.
- `validity`: período autorizado si es emitible; ausente si se rechaza.
- `policyReference`: código inmutables; `jurisdictionCode`, `evaluatedAt` y `scope`.

`isEligible()` significa APPROVED o CONDITIONAL. Rechazar no lanza excepción en una consulta de evaluación:
se devuelve la decisión. Intentar emitir con decisión rechazada sí produce conflicto.
No usar una lista vacía de motivos para deducir que un certificado condicional es incondicional.

Guardas comunes preceden a reglas jurisdiccionales: inspección cerrada, activo correcto, cobertura completa
del alcance, subsistema existente y renovación válida. Ninguna política puede aprobar una inspección abierta
o incompleta. Implementarlas en un servicio de decisión compartido, no duplicarlas en cada Strategy.
La política recibe el resultado de esas guardas o solo se ejecuta si se cumplieron; el response registra la
política seleccionada incluso cuando una guarda común rechaza.

### 2.4 Ejemplos de políticas (fixtures, no normativa)

| Política | Bloquea | Condicional | Plazo de aprobado | Plazo condicional |
|---|---|---|---|---|
| `STRICT` v1 / jurisdicción `DEMO-STRICT` | MINOR, MAJOR, CRITICAL | no | 12 meses calendario | no aplica |
| `CONDITIONAL` v1 / jurisdicción `DEMO-CONDITIONAL` | CRITICAL | MINOR o MAJOR abierto | 6 meses calendario | 30 días |

Sin hallazgos abiertos ambas aprueban. Un MINOR o MAJOR abierto rechaza en STRICT y es condicional en
CONDITIONAL; CRITICAL rechaza en ambas. No conformidades ya cerradas no bloquean, como en el flujo actual.
Severidades desconocidas para una política no aprueban por omisión: decisión rechazada con `UNSUPPORTED_SEVERITY`.
El registry existente de severidades estándar de infraestructura/API conserva su limitación documentada.

Para F1 se evalúan solo hallazgos que afectan el subsistema, más los de inspecciones sin subsistemas.
Hasta F1, se evalúan todos los abiertos del activo y se exige cobertura global, como en la base actual.

### 2.5 Plazos y condiciones

Se conserva S9: extremos inclusivos y estado EXPIRED al día siguiente de `validUntil`.
Un plazo de N días significa `validUntil = validFrom.plusDays(N - 1)`; N debe ser positivo.
Un plazo de N meses calendario significa `validUntil = validFrom.plusMonths(N).minusDays(1)` con ajuste
calendario de Java y N positivo. Definir y probar explícitamente fines de mes y años bisiestos.
Cada política elige duración según alcance/modalidad; inicialmente los ejemplos usan el mismo plazo para
todos los subsistemas. La interfaz permite variar ese plazo sin tocar emisión.

Todas las condiciones vencen a más tardar en `validUntil`. En las fixtures condicionales el límite de cada
condición coincide con el fin de los 30 días. Corregir antes no convierte el documento automáticamente:
permanece condicional hasta renovación; vencido, deja de habilitar por la regla temporal habitual.
Condicional es la modalidad del certificado, independiente de ACTIVE/SUSPENDED/EXPIRED/RENEWED.
Auditoría e informe muestran ambas. Para F1 el global hereda condiciones y se limita al menor vencimiento;
no vuelve a conceder un plazo mayor que los respaldos ni aplica una política distinta a parciales existentes.

### 2.6 Consulta, emisión y renovación

Una consulta es una previsualización sin auditoría persistida ni reserva de número. Emisión y renovación
recalculan contra hallazgos y política actuales dentro de su transacción; no aceptan un `eligible=true` ni
una decisión calculada por el cliente. Si algo cambió, la respuesta puede diferir de la previsualización.
La decisión usada para el certificado es la que se guarda; no volver a evaluar para generar su documento.

El command y el request de emisión/renovación no reciben `validUntil`: el vencimiento se calcula exclusivamente
en el servidor. Los campos JSON desconocidos se rechazan con 400. No hay categoría ni política LEGACY.
Renovación consulta la política actual, conserva misma identidad/alcance y exige base posterior.

---

## 3. Modelo, puertos y aplicación

| Contexto/capa | Agregar | Modificar |
|---|---|---|
| Catálogo | `JurisdictionCode` | `Asset`, `RegisterAssetCommand`, consultas de activo |
| Certificación dominio | `CertificationPolicy`, `CertificationAssessmentContext`, `policyCode`, `CertificationPolicyResolver` (puerto), `CertificationOutcome`, `CertificationCondition`, `las políticas Strict, Conditional y Standard` | `CertificationDecision`, `Certificate`, snapshot/factory/service y documento |
| Aplicación | resolución y armado de contexto en un colaborador común de decisión | evaluar, emitir y renovar; registro de activo |
| Infraestructura/config | registry de políticas y carga de datos/configuración | mappers de activo/certificado y composición en `UseCaseConfiguration` |
| Web | resource de decisión con política, modalidad, vigencia y condiciones | requests de activo, emisión y renovación; certificado e informe |

`CertificationPolicy` expone solo code() y evaluate(context). Sus implementaciones encapsulan
las reglas en código de dominio; la composición únicamente registra jurisdicción → política.
las políticas Strict, Conditional y Standard usa un mapa privado, admisión condicional y plazos. Otra implementación
puede utilizar un algoritmo distinto sin cambiar el servicio central.

Se persiste la decisión completa y la código de la definición aplicada. No se serializan
reglas ni implementaciones de Strategy; las decisiones previas conservan su resultado.
Los informes generales muestran severidades y resolución; la evaluación contextual muestra impedimentos
y condiciones, sin reclasificar hallazgos desde el informe.

---

## 4. Persistencia, consistencia y auditoría

- Nueva migración Flyway: jurisdicción del activo y metadatos de decisión/modalidad/condiciones del certificado.
- No se migran activos ni certificados previos; se parte de una base vacía.
- No recalcular decisiones ni auditoría durante `restore`. Los informes usan snapshot aunque cambie el registry.
- Persistir emisión/renovación, transición del certificado anterior y auditoría en una sola transacción.
- Serializar decisiones con mutaciones de hallazgos del mismo activo: usar bloqueo de fila del activo en
  emisión/renovación, cierre/rectificación que crea hallazgos y verificación que los cierra. Así la emisión
  confirma una decisión consistente con el conjunto de hallazgos leído. Orden común: activo antes de otros
  agregados; el adaptador/proxy transaccional gestiona locks sin contaminar dominio/aplicación con JPA.
- Nuevos hallazgos posteriores a emisión no revocan automáticamente un documento: se conserva el flujo de
  suspensión explícita. F1 sí deriva el estado global de sus parciales.
- Auditoría de emisión incluye política/versionado, jurisdicción, resultado, plazo y condiciones. Operaciones
  rechazadas no dejan auditoría exitosa; previsualizaciones no se guardan como decisiones emitidas.

---

## 5. API REST

| Operación | Contrato |
|---|---|
| `POST /api/v1/assets` | `jurisdictionCode` requerido en registros nuevos; desconocido: 400 con error explícito |
| `GET /api/v1/assets/{code}` | expone jurisdicción registrada |
| `GET /api/v1/inspections/{id}/certification-eligibility` | 200 con APPROVED/CONDITIONAL/REJECTED, motivos, vigencia, condiciones y política; filtro subsistema tras F1 |
| `POST /api/v1/certificates` | inspección, alcance tras F1; plazo calculado por servidor; 201 + Location |
| `POST /api/v1/certificates/{number}/renewals` | nueva inspección; recalcula bajo política actual; 201 |
| `GET /api/v1/certificates/{number}` y `/document` | jurisdicción y decisión histórica completa, modalidad separada de estado |

HAL, persona en headers y errores problem+json como fase 1. 400: código mal formado o campos desconocidos como un plazo solicitado. 404: activo/inspección/certificado inexistente. 409: no elegible, renovación inválida,
alcance ocupado o política ausente para un activo existente (`JurisdictionPolicyNotFoundException`).
Consultar una inspección abierta devuelve 200 con decisión rechazada; emitir responde 409.
La API no ofrece parámetro de jurisdicción en emisión que permita eludir la registrada en el activo.
Los enlaces de emisión aparecen en APPROVED y CONDITIONAL; las condiciones se muestran antes de emitir.

---

## 6. Pruebas y demostración

| Escenario | Nivel / expectativa |
|---|---|
| mismo resultado bajo STRICT/CONDITIONAL | dominio parametrizado: matriz §2.4 completa, incluye sin hallazgos y severidad desconocida |
| abierto, cobertura incompleta o activo incorrecto | dominio/aplicación: guardas comunes no pueden ser saltadas por Strategy permisiva |
| tercera jurisdicción | aplicación: registrar una nueva política y certificar con el mismo servicio sin modificarlo |
| plazo y condiciones | dominio: inclusividad, fines de mes, bisiesto, 30 días y rechazo de plazo no positivo |
| request intenta cambiar jurisdicción/plazo | API: no altera la selección, plazo contradictorio 400 |
| evaluación y emisión con estado sin cambios | API: misma decisión; con cambio de hallazgo/política se recalcula |
| renovar con política distinta | aplicación/repositorio: nuevo snapshot, viejo inalterado y base posterior |
| restart y cambio de registry | repositorio/API: informe antiguo conserva decisión, condiciones y política |
| fallo de emisión/renovación | repositorio real: rollback de certificado y auditoría |
| cierre/verificación concurrente con emisión | integración real: decisión y hallazgos consistentes por serialización |
| esquema nuevo | repositorio: jurisdicción obligatoria, política explícita y restauración de decisiones nuevas |
| parciales F1 bajo política condicional | E2E: rechazo de uno no bloquea otro; global hereda condiciones y menor vigencia |

Demo: dos activos de igual tipo con resultados equivalentes y distinta jurisdicción; MAJOR abierto;
STRICT rechaza, CONDITIONAL emite por 30 días. Agregar tercera política al registry; repetir sin editar
servicios. Renovar después de cerrar acción; verificar historia. Ejecutar `mvn -q clean verify` y migración
en PostgreSQL. Los tests de repositorio y API usan persistencia real; mocks solo en unitarios.

---

## 7. Alternativas y decisión recomendada

| Alternativa | Ventaja | Costo / decisión |
|---|---|---|
| **Strategy + resolver + reglas privadas en dominio (recomendada)** | extensible, prueba aislada y reutiliza una política para reglas similares | validar registro/versiones y persistir explicación |
| una clase por jurisdicción | código directo para reglas singulares | duplicación si solo cambian severidades y plazos; reservar para lógica distinta |
| tabla de parámetros editable en DB | cambios sin desplegar | requiere administración, auditoría y publicación/versionado de políticas; no pedido |
| switch por jurisdicción en servicio | poco código inicial | viola criterio de aceptación al agregar cada jurisdicción |
| motor de reglas / DSL | reglas arbitrarias sin recompilar | operación, validación y debugging desproporcionados para estas variaciones |

Usar Strategy para comportamiento y datos para variaciones de parámetros. Mantener guardas comunes fuera
de las Strategies. No añadir Factory por jurisdicción, Chain of Responsibility ni jerarquías de certificados
si no aparece una necesidad que sus contratos actuales no cubran.

## 8. Documentación y definición de terminado

DESIGN.md reemplaza S5 (bloqueo universal) y amplía S9 (plazo decidido por política) y D10; documenta identidad
de jurisdicción, clases/refactorizaciones reales, matriz demo, alternativas y deuda (administración de políticas,
cambio de jurisdicción, seguimiento automático de condiciones). README/CHANGES y ejemplo de decisión condicional.
Terminado al cumplir §1, §6 y CI con migración y demo reproducibles. Ver [coordinación F1–F3](README.md).

La configuración incluye también `DEMO-STANDARD` (MAJOR/CRITICAL bloquean, sin modalidad condicional, 12 meses)
como tercera política de demostración. No se selecciona por defecto ni actúa como compatibilidad.

Las precondiciones comunes se comprueban en CertificationAssessmentService antes de invocar la política;
no existe una clase separada CertificationPrerequisites. El Outcome de los criterios conserva su
comportamiento anterior y no forma parte de esta refactorización.
