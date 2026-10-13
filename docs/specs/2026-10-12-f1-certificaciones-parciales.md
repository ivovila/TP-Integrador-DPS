# Spec · F1 · Certificaciones parciales por subsistema

Fecha: 2026-10-12 · Rama: `entrega-2-spec1` · Consigna: [CertiFlow · Entrega 2](../consigna/certiflow-entrega-2.md)

## 1. Objetivo

Certificar subsistemas de un mismo activo de manera independiente, conservando la inspección que los respalda,
su vigencia y la decisión aplicada. Derivar la certificación global mediante una política explícita.
Esta spec describe comportamiento futuro; no afirma que las clases propuestas ya existan.

### Criterios de aceptación

1. Un activo declara subsistemas identificados por código estable dentro del activo.
2. Una versión de esquema asocia secciones o criterios a esos códigos y congela la asociación.
3. Una inspección cerrada puede respaldar un parcial de un subsistema completo aunque no cubra todo el esquema.
4. El rechazo de un subsistema no impide emitir el parcial de otro sin impedimentos propios.
5. Cada parcial tiene número, vigencia, auditoría, suspensión y renovación independientes.
6. La unicidad se controla por activo y alcance; dos subsistemas distintos pueden tener certificados simultáneos.
7. El global se deriva de parciales por `GlobalCertificationPolicy`, registra cuáles usó y nunca dura más que ellos.
8. Un parcial suspendido, vencido o renovado deja de respaldar el global; el estado consultado refleja ese cambio.
9. Persistencia y API conservan alcance y relaciones tras reiniciar la aplicación.

### Fuera de alcance

- Certificar criterios individuales, mezclar inspecciones para completar un único parcial o borrar subsistemas usados.
- Secciones anidadas: el código actual tiene `Section(title, criteria)`; F1 no requiere añadir un árbol.
- Frontend, autenticación y reglas regulatorias reales. Los ejemplos son datos de demostración.

---

## 2. Reglas y supuestos de negocio

### 2.1 Identidad y asociación

- `SubsystemCode` es un VO de Catálogo; no se usa el nombre de la sección como identidad.
- `Asset` contiene `List<Subsystem>` (`code`, `name`), con códigos únicos. Dos activos pueden usar `ELECTRICAL`.
- La versión declara `requiredSubsystems`: códigos obligatorios para su certificación global.
- Una sección puede declarar un subsistema por defecto; un criterio puede declarar otro explícitamente.
  La asociación del criterio prevalece; sin asociación propia hereda la de su sección.
- En un esquema con subsistemas, todo criterio debe resolver a exactamente uno y todo subsistema obligatorio debe
  tener al menos un criterio. No hay doble pertenencia ni criterios sueltos. Validar al publicar.
- Al asignar una inspección, el activo debe contener todos los códigos requeridos por la versión. Los adicionales
  del activo no forman parte del global de esa versión. Se captura el conjunto requerido junto con la versión.
- Renombrar un subsistema no cambia la identidad; cambiar asociaciones exige otra versión del esquema.

### 2.2 Cobertura y hallazgos

Un subsistema está cubierto si TODOS los criterios que la versión le asigna pertenecen al alcance capturado
de la inspección. Cerrar sigue exigiendo respuestas y evidencias completas dentro del alcance.
No alcanza con inspeccionar una sección si otra sección también contiene criterios del mismo subsistema.
El resultado distingue `COVERED`, `NOT_COVERED` y la decisión de elegibilidad; no convierte ausencia en aprobación.

Se conserva la regla actual: los hallazgos abiertos del activo pueden impedir nuevas emisiones, incluidos los de
otras inspecciones. `Finding` guarda el subsistema resuelto desde SU versión de origen al nacer. El ACL no intenta
inferirlo usando el código de criterio en la versión nueva: los códigos pueden reasignarse o desaparecer.
Un hallazgo localizado solo afecta su subsistema. Un hallazgo de una inspección sin subsistemas afecta todos los subsistemas
hasta cerrarse por su flujo de acciones correctivas.
Rectificar conserva las asociaciones de la versión capturada y las reglas actuales de acciones correctivas.

### 2.3 Certificados parciales

- `CertificationScope` representa `WholeAsset` o `SubsystemScope(SubsystemCode)`; no usar un null para distinguirlos.
- Se reutiliza `Certificate` como agregado. El tipo de alcance cambia la cobertura requerida, no el ciclo de vida.
- El parcial se emite sobre una inspección cerrada del mismo activo, con el subsistema completamente cubierto y
  elegible. Tras F3, la política jurisdiccional fija modalidad, condiciones y plazo por ese subsistema.
- Una inspección puede generar varios parciales mediante emisiones independientes; no se añade un lote atómico.
- Se permiten distintos vencimientos. Antes de F3 se mantiene `validUntil` del command; después rige su spec.
- Como S10, un suspendido dentro de vigencia ocupa su alcance; no se evade la suspensión emitiendo otro.
- Renovar exige mismo activo y mismo subsistema, inspección cerrada posterior y nueva decisión elegible.
- Validar antes de solicitar número. Un conflicto de concurrencia puede dejar un hueco de secuencia, nunca
  certificados duplicados ni datos/auditoría de una emisión revertida.

### 2.4 Derivación global

`AllRequiredSubsystemsPolicy` exige un parcial `ACTIVE` por cada subsistema requerido, todos del mismo activo,
basados en la misma versión de esquema (tipo + número). Se admite distinta inspección para cada parcial.
No se mezclan versiones aunque sus códigos coincidan. La base del global son los parciales, no una inspección ficticia.

- `GlobalCertificationPolicy.evaluate(requiredSubsystems, partials, onDate)` devuelve decisión y motivos por código.
- `validFrom` del global es el día de emisión; `validUntil` es el menor vencimiento de los parciales usados.
  Verificar que todos están activos ese día; no aceptar una fecha libre que extienda esa vigencia.
- El global conserva números de parciales, versión de esquema, política global y decisiones de origen.
- Con F3, si algún parcial es condicional, el global también lo es y reúne sus condiciones. Un parcial bloqueado
  o un subsistema ausente impide el global; no existe un global que omita obligatorios.
- Al consultar su estado, primero se aplica su propio ciclo de vida; un global que de otro modo sería activo
  aparece `DEPENDENCY_INVALID` si cualquiera de sus respaldos ya no está activo. Guardar esa proyección como
  estado definitivo requeriría sincronización innecesaria. El informe incluye la dependencia invalidada.
- Renovar un parcial no sustituye silenciosamente una referencia del global. Para recuperar el global se renueva
  con un nuevo conjunto de parciales; los anteriores y sus relaciones siguen consultables.
- Suspender el global no suspende parciales. Suspender uno sí invalida su respaldo del global.
- La emisión global usa un command propio; el command actual basado en inspección queda para parciales o activos sin descomposición.
  Renovar un global usa parciales nuevos y exige al menos una base de inspección posterior a su respaldo anterior.

### 2.5 Sistema nuevo

Por indicación del usuario, no se implementan rutas de retrocompatibilidad ni migración de datos previos.
Un activo nuevo puede no estar descompuesto: declara jurisdicción y una lista vacía de subsistemas y usa
`WholeAsset` con esquema sin asociaciones. Esto es un caso de negocio actual, no un modo LEGACY.
En versiones con subsistemas el global siempre se deriva de parciales.
La unicidad se controla por alcance y la cobertura requerida es la del alcance certificado.

---

## 3. Modelo, contextos y casos de uso

Se conserva la arquitectura de [Fase 1](2026-10-12-fase-1-ddd-persistencia-api.md): dominio y aplicación sin Spring,
JPA ni Jackson. El ACL es el único traductor de respuestas y hallazgos hacia Certificación.

| Contexto | Agregar | Modificar |
|---|---|---|
| Catálogo | `SubsystemCode`, `Subsystem` | `Asset`, `RegisterAssetCommand` y registro |
| Esquemas | asociación opcional de sección/criterio y resolución en versión | `Section`, `Criterion`, `SchemaVersion` |
| Inspección | subsistema de origen en hallazgo | `Finding`, snapshots/factory, creación desde eventos |
| Certificación | `CertificationScope`, `SubsystemResult`, `CertificateBasis` (`InspectionBasis`, `PartialCertificatesBasis`), `GlobalCertificationPolicy`, `AllRequiredSubsystemsPolicy` | `InspectionResult`, ACL, `Certificate`, factory/service/snapshot, repository |
| Aplicación | `IssueGlobalCertificateUseCaseImpl`, `RenewGlobalCertificateUseCaseImpl`, consultas de respaldos | evaluar, emitir y renovar parcial; registrar activo y asignar |

`SubsystemResult` lleva código, cobertura y no conformidades abiertas de ese subsistema.
El command parcial lleva `inspectionId`, `subsystemCode` y persona; no lleva objetos `Subsystem` o `Certificate`.
El command global lleva `assetCode`, versión de esquema y números de parciales. El servicio carga y valida
todos los respaldos; el cliente no declara que son elegibles. Repositorio: consultar vigente por activo + alcance.

---

## 4. Persistencia, transacciones y auditoría

- Migración Flyway nueva, sin editar V1: subsistemas del activo, alcance/bases del certificado, subsistema del
  hallazgo y relación global → parciales. Extender JSON del esquema y snapshots para el modelo nuevo.
- Las migraciones se aplican sobre una base vacía; no se reconstruyen datos de versiones anteriores.
- Emisión/renovación y auditoría se confirman juntas usando el mecanismo transaccional de fase 1.
- Bloquear la fila del activo al emitir/renovar/suspender y derivar global para serializar operaciones del mismo
  activo; mantener el chequeo de solapamiento en el repositorio real. Dos emisiones concurrentes del mismo
  alcance: una gana y otra obtiene conflicto. Renovación actualiza viejo y nuevo en la misma transacción.
- Auditoría contiene alcance, base, decisión y política global. Reconstruir no reevalúa ni agrega entradas.

---

## 5. API REST

Base `/api/v1`, HAL y `application/problem+json`; persona en headers existentes.

| Operación | Contrato |
|---|---|
| `POST /assets` | añadir `subsystems: [{code, name}]`; vacío para activos sin descomposición |
| `POST /schemas/{assetType}/versions` | `requiredSubsystems` y asociaciones en secciones/criterios |
| `GET /inspections/{id}/certification-eligibility?subsystem=ELECTRICAL` | 200 con decisión, cobertura y motivos; no emite |
| `POST /certificates` | añadir `subsystemCode` a emisión basada en inspección; 201 + Location |
| `POST /assets/{code}/global-certificates` | versión y `partialCertificateNumbers`; 201 con global y vínculos |
| `POST /certificates/{number}/global-renewals` | nuevos números de parciales; 201 con global nuevo |
| `GET /certificates?asset=...&subsystem=...` | filtro por alcance; mantener paginación |
| `GET /certificates/{number}` y `/document` | alcance, base, modalidad, política y respaldos; estado derivado |

400: asociaciones mal formadas/duplicadas. 404: activo, inspección, certificado o código de subsistema inexistente.
409: cobertura incompleta, no elegible, alcance ocupado o respaldos incompatibles/inactivos.
No exponer enlace de emisión global si faltan respaldos elegibles; el servidor valida igual al recibir el POST.

---

## 6. Pruebas y demostración

| Riesgo / escenario | Nivel y resultado esperado |
|---|---|
| asociación de sección + override de criterio | dominio parametrizado: un solo subsistema efectivo; faltantes/duplicados inválidos |
| una sección cubre solo parte del subsistema | dominio/aplicación: cerrar permitido, parcial no elegible |
| eléctrica aprobada, presión con CRITICAL | aplicación/API: parcial eléctrica emitido, presión bloqueado, global bloqueado |
| hallazgo previo con código reasignado | ACL: sigue afectando subsistema de origen; hallazgo de inspección sin subsistemas afecta todos |
| dos parciales con vencimientos distintos | dominio/API: global vence con el más corto |
| suspensión/renovación/vencimiento de respaldo | integración: global pierde respaldo inmediatamente al consultar |
| mezclar versiones o activos | dominio/API: 409 y ninguna escritura |
| renovar parcial de otro subsistema | aplicación: falla antes de numerar |
| dos POST simultáneos del mismo alcance | repositorio/API real: un éxito, un 409, un solo vigente |
| restauración, rollback y creación de esquema vacío | repositorio H2 y migración: historia, JSON y relaciones intactas |

Demo: dos subsistemas; emitir solo uno, cerrar acciones del otro, emitirlo con otro vencimiento, derivar global,
suspender un parcial y consultar global. Ejecutar `mvn -q clean verify`; probar migración en PostgreSQL del compose.

---

## 7. Alternativas y decisión recomendada

| Alternativa | Ventaja | Costo / motivo |
|---|---|---|
| **Un `Certificate` con alcance y base tipados (recomendada)** | reutiliza auditoría, suspensión y renovación; parciales independientes | refactoriza unicidad y base de renovación |
| agregado diferente `PartialCertificate` | separación explícita | duplica ciclo de vida, snapshots, repositorios y API sin reglas suficientes para justificarlo |
| un certificado con hijos por subsistema | emisión inicial simple | dificulta vigencias y renovaciones independientes; agregado demasiado amplio |
| global como mera proyección sin número | no necesita invalidar respaldos | no conserva un documento global emitido ni su auditoría de emisión |
| global materializado con eventos de actualización | lectura directa | exige coordinar todas las transiciones y el reloj; riesgo de estado desactualizado |

Recomendar global como certificado con respaldos inmutables y estado efectivo derivado, y Strategy para su
política. No agregar Composite para secciones planas ni eventos asíncronos para coordinar esta aplicación monolítica.

## 8. Documentación y definición de terminado

Actualizar DESIGN.md con sustituciones de S10/S17, glosario, clases efectivamente agregadas/modificadas,
refactorizaciones, alternativas descartadas y deuda pendiente (secciones anidadas y seguimiento de condiciones).
Actualizar README, CHANGES.md y ejemplos de API. Se termina al pasar §1 y §6 en CI, con migración y demo reproducibles.
Integrar con [F2](2026-10-12-f2-versiones-vigencia-futura.md) y [F3](2026-10-12-f3-politicas-jurisdiccion.md);
ver [orden de implementación](README.md).
