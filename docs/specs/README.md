# Specs · Entrega 2 · F1, F2 y F3

Fecha: 2026-10-12 · Rama revisada: `entrega-2-spec1`

## 1. Base y alcance

Estas specs continúan [Fase 1 · DDD, persistencia, API y CI](2026-10-12-fase-1-ddd-persistencia-api.md)
y se basan en el código de la branch y la [consigna de entrega 2](../consigna/certiflow-entrega-2.md).
Son contratos de implementación futura, con supuestos elegidos, alternativas, pruebas y definición de terminado.
No reemplazan el registro de decisiones YA implementadas en DESIGN.md ni afirman que las pruebas nuevas pasen.

| Funcionalidad | Spec | Decisión principal |
|---|---|---|
| F1 | [Certificaciones parciales](2026-10-12-f1-certificaciones-parciales.md) | mismo agregado Certificate con alcance y base tipados; global por Strategy |
| F2 | [Versiones con vigencia futura](2026-10-12-f2-versiones-vigencia-futura.md) | historial inmutable y resolución por instante, sin activador programado |
| F3 | [Políticas por jurisdicción](2026-10-12-f3-politicas-jurisdiccion.md) | Strategy + resolver; reglas privadas de cada política en dominio |

## 2. Orden sugerido de implementación

1. **F2**: extensión pequeña de SchemaVersion y selección en asignación; fija el calendario y la captura inmutable.
2. **F3**: centraliza decisiones, plazos, modalidad y snapshot de política para que F1 los reutilice.
3. **F1**: añade subsistemas, separa cobertura por alcance y compone decisiones/parciales en globales.

El orden de archivo sigue los identificadores de consigna, no el orden de ejecución. Cada funcionalidad debe
tener su plan en `docs/plans` antes de implementarse, con tareas y archivos concretos como el plan de fase 1.
La implementación usa estos contratos con el ajuste autorizado de sistema nuevo: sin retrocompatibilidad ni LEGACY.

## 3. Contratos compartidos

- «Inspección iniciada» significa asignada, como S1: F2 selecciona versión en ese instante.
- La versión congela criterios, asociaciones de subsistema y conjunto requerido; no consulta el esquema actual.
- La jurisdicción se toma del activo al decidir; la política aplicada queda congelada en el certificado.
- La inspección conserva evaluación por severidad. La política jurisdiccional determina elegibilidad de certificado.
- F3 reemplaza el plazo libre del command; F1 usa la vigencia autorizada por política en cada parcial.
- Un certificado global de F1 se limita por sus respaldos; no se evalúa como si fuera otro parcial ni extiende sus plazos.
- Modalidad APPROVED/CONDITIONAL es distinta del ciclo ACTIVE/SUSPENDED/EXPIRED/RENEWED.
- La rectificación conserva historia y no cierra hallazgos automáticamente; los hallazgos se clasifican por origen.
- Dominio/aplicación siguen sin Spring/JPA/Jackson. El ACL arma datos; las políticas deciden sobre esos datos.
- Nuevas migraciones consecutivas según orden implementado; no editar V1 ni fijar el mismo número en dos specs.
- Locks por activo de F1/F3 se implementan una sola vez con orden consistente; publicación F2 bloquea su esquema.

## 4. Supuestos que conviene validar con el equipo

La consigna no define estas reglas; las specs eligen valores concretos para permitir implementación y pruebas.
Se pueden cambiar ANTES de escribir el plan correspondiente, actualizando criterios y escenarios juntos.

| Tema | Elección |
|---|---|
| vigencias futuras fuera de orden | rechazar; sin reprogramar versiones publicadas |
| subsistema de criterio | asociación explícita prevalece sobre sección; un solo subsistema efectivo |
| parcial respaldado por varias inspecciones | fuera de alcance; una inspección cubre todo el subsistema |
| global con parciales de versiones diferentes | rechazar; misma versión del esquema |
| global tras renovación de parcial | no sustituir automáticamente; renovar el global |
| hallazgo de inspección sin subsistemas | afecta todos hasta cerrarlo |
| cambio de política | nuevas decisiones usan configuración actual; historia no se recalcula |
| certificado condicional | vigencia corta y condiciones visibles; no se transforma automáticamente al corregir |
| jurisdicciones de ejemplo | DEMO-STRICT y DEMO-CONDITIONAL, sin atribución normativa |

## 5. Verificación y documentación

Cada spec contiene una matriz riesgo → prueba de dominio, aplicación, repositorio y API. Se ejecuta
`mvn -q clean verify` para implementación, con migraciones comprobadas en PostgreSQL y regresiones de fase 1.
La entrega final agrega a DESIGN.md las clases efectivamente modificadas, refactorizaciones, alternativas
descartadas y deuda de cada funcionalidad; README/CHANGES reflejan lo implementado, sin confundir propuesta con hecho.

## 6. Frontend

| Spec | Decisión principal |
|---|---|
| [Frontend sobre puertos](2026-10-12-frontend.md) | puertos propios con los nombres de los casos de uso; acciones entregadas por el sistema |
| [Frontend conectado a la API](2026-10-13-frontend-api.md) | adaptador HTTP detrás de los mismos puertos; se elimina el modo en memoria; tests rápidos y contra el backend real |
| [Roles en el frontend](2026-10-13-frontend-roles.md) | una tabla de datos por rol; una acción se muestra si el sistema la permite y el rol puede; solo interfaz, sin control de acceso |
