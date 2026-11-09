# Traspaso: fase 4 de Asistencia, integración real SIN VALIDAR

> **Resuelto el 2026-11-08.** La integración quedó validada de punta a punta con Nemotron 3.5 Super
> (`docs/validation/2026-11-08-nvidia-first-review.json`, `NvidiaAssistantSmokeApiTest`). Gemma y Mistral
> Large 4 no respondían en el endpoint gratuito. Este documento queda como registro de lo que se intentó.

**Rama: `entrega-3`. Estado: API implementada y probada con stub; NO probada con éxito de extremo a extremo
con NVIDIA. No hay ninguna generación real exitosa ni resultados del harness. No presentar esta fase como
una integración de IA operativa.**

La prioridad del próximo agente es conseguir una generación real y recorrer la API con ella. La key fue
aportada para las pruebas, pero no se guardó: el usuario pidió persistirla solo si funcionaba, y no se cumplió
esa condición. No está en el repositorio, el entorno persistente ni un archivo local de configuración.
La autenticación tampoco está confirmada: el listado de modelos es público.

## Qué incluye este cambio

- `AssistantReviewController` y DTOs en `certiflow-web/.../web/assistance/`: pedir/listar revisiones, consultar
  revisión y sugerencia, aceptar o descartar con motivo, GET de la decisión creada y auditoría.
- `AssistanceConfiguration`: compone los seis casos de uso, servicio, traductor y bitácora. Pedir una revisión
  conserva sus transacciones explícitas: el proveedor se llama afuera. Las decisiones y consultas usan el proxy.
- HAL: `assistant-reviews` desde la inspección, incluso cerrada para consultar historia; `acceptance` y
  `dismissal` solo en sugerencias pendientes; `criterion` enlaza la respuesta que el inspector debe corregir.
- `INSPECTOR` para pedir/decidir y lectores de inspecciones para consultar. Aceptar solo traza la decisión:
  no responde, no cierra y no levanta hallazgos en la inspección.
- Auditoría `AssistanceAudit` registrada en `SharedConfiguration`, limpieza de tablas nuevas entre tests,
  contrato OpenAPI actualizado. No se modifica la migración V7 ya existente.
- Preparación experimental del proveedor: `spring-ai-openai` 2.0.2 solo en infraestructura,
  `SpringAiAssistantConfiguration` condicional y perfil `application-nvidia.properties`.

## Qué está probado y qué NO

Última corrida, con la configuración Gemma actual: `mvn -q clean verify` terminó correctamente.
Surefire informó **505 tests, 0 fallas, 0 errores y 19 omitidos: 486 ejecutados y aprobados**.
Incluye arquitectura, API con stub y cinco tests de protocolo HTTP contra un servidor local.

Antes de cambiar la selección de proveedor a Gemma, la fase API se ejecutó también con PostgreSQL 17:
`mvn -q -Dcertiflow.postgres=true clean verify`, 500 tests aprobados. Eso NO constituye una corrida de la
configuración final de Gemma contra PostgreSQL ni una prueba con un modelo real. El nuevo
`PostgresAssistantReviewApiTest` queda disponible para CI y corridas con esa propiedad.

**No se verificaron** una respuesta válida del modelo real, su conversión por Spring AI, guardrails sobre
esa respuesta, el flujo HTTP completo con NVIDIA, persistencia de una revisión real ni calidad del modelo.
Los tests con stub o servidor local no prueban disponibilidad, permisos de la cuenta ni compatibilidad efectiva
del endpoint externo. El estado de CI del nuevo push debe consultarse; no se afirma que esté verde.

## Configuración actual y archivos que mirar primero

| Archivo | Qué revisar |
|---|---|
| `certiflow-infrastructure/src/main/java/ar/edu/itba/certiflow/infrastructure/assistance/SpringAiAssistantConfiguration.java` | URL, modelo, timeout, retries=0 y `chat_template_kwargs.enable_thinking` |
| `certiflow-infrastructure/src/main/java/ar/edu/itba/certiflow/infrastructure/assistance/SpringAiInspectionAssistant.java` | Prompt, `responseEntity(ModelReview.class)`, validación y conversión a `AssistantUnavailableException` |
| `certiflow-infrastructure/src/main/resources/prompts/inspection-review/v1.st` | Prompt publicado e inmutable; cualquier ajuste exige v2 y su checksum |
| `certiflow-web/src/main/resources/application-nvidia.properties` | Perfil opt-in con key por entorno |
| `certiflow-web/src/main/java/ar/edu/itba/certiflow/web/config/AssistanceConfiguration.java` | Stub por defecto y límites transaccionales |
| `certiflow-web/src/test/java/ar/edu/itba/certiflow/web/assistance/AssistantReviewApiTest.java` | Escenarios, fixtures y contrato HAL que el proveedor real debe respetar |
| `certiflow-web/src/test/java/ar/edu/itba/certiflow/web/assistance/AssistantUnavailableApiTest.java` | Falla externa: 503 sin revisión ni auditoría y llamada sin transacción |
| `certiflow-infrastructure/src/test/java/ar/edu/itba/certiflow/infrastructure/assistance/NvidiaAssistantConfigurationTest.java` | Pruebas HTTP locales; NO son smoke tests de NVIDIA |
| `docs/validation/2026-11-08-nvidia-gemma-connectivity.json` | Evidencia externa sanitizada, sin secretos |

El perfil usa `https://integrate.api.nvidia.com/v1` y `google/gemma-4-31b-it`.
Lee `NVIDIA_API_KEY`, `NVIDIA_MODEL`, `CERTIFLOW_ASSISTANT_TIMEOUT` (PT60S),
`CERTIFLOW_ASSISTANT_PROMPT_VERSION` (v1) y `CERTIFLOW_ASSISTANT_ENABLE_THINKING` (false).
No hay fallback automático al stub ni reintentos automáticos. Otro modelo puede no aceptar
`chat_template_kwargs`: revisar su contrato antes de cambiar `NVIDIA_MODEL`.

Los Llama 3.3 70B y 3.1 8B inicialmente sugeridos tienen el endpoint gratuito retirado.
La selección de Gemma se basó en disponibilidad del catálogo; su calidad todavía NO está medida.
La comparación pequeña sigue pendiente de elegir y comprobar otro modelo disponible.

## Qué pasó al probar NVIDIA

1. `GET /v1/models` devolvió 200 y listó Gemma, pero también devolvió 200 sin key: NO valida autenticación.
2. Una solicitud directa con una observación sintética agotó 45 segundos sin respuesta.
3. El adaptador Java real, con `enable_thinking=false` y un timeout de 120 segundos, falló con
   `AssistantUnavailableException` causado por errores de I/O/timeout. No llegó a producir una revisión.
4. Una solicitud mínima de 32 tokens, sin razonamiento y con `NVCF-POLL-SECONDS: 5`, devolvió 504 sin cuerpo.
5. Una repetición con el caso de la manguera, 512 tokens y `NVCF-POLL-SECONDS: 30` devolvió 504 en 35.62 segundos.
   El script estaba condicionado a guardar la key únicamente después de un JSON válido; **no la guardó**.

No se diagnosticó de forma concluyente si el problema es cola/capacidad, cuenta/permisos u otra condición del
servicio. No atribuirlo a una key inválida ni darla por validada: no hubo inferencia exitosa. Un 504 con ventana
de polling corta tampoco prueba por sí mismo una caída total del servicio.

## Cómo retomar

1. Obtener la credencial por un medio autorizado e inyectarla solo en el proceso. No está en este documento.
   Si se usa una terminal WSL, se puede ingresar sin mostrarla ni dejarla en el historial:

   ```bash
   read -rsp 'NVIDIA API key: ' NVIDIA_API_KEY
   export NVIDIA_API_KEY
   printf '\n'
   ```

2. Probar primero un POST directo con datos sintéticos. El endpoint es
   `https://integrate.api.nvidia.com/v1/chat/completions`; usar un timeout acotado y este cuerpo:

   ```json
   {
     "model": "google/gemma-4-31b-it",
     "temperature": 0,
     "max_tokens": 512,
     "chat_template_kwargs": {"enable_thinking": false},
     "messages": [{"role": "user", "content": "Criterio HOSE: manguera en buen estado. Respuesta Good, resultado APPROVED. Observación: La manguera tiene una grieta cerca del acople. Devolvé solo JSON con findings: una lista de objetos con criterion, severity, rationale y quotes literales de la observación."}]
   }
   ```

   Enviar la key solo en `Authorization: Bearer ...`, nunca en la URL ni en logs. Registrar estado, latencia,
   modelo, finish_reason y contenido generado sin credenciales. Si hay 202, revisar el protocolo oficial de
   polling antes de interpretarlo como un ChatCompletion. Si persiste 504, comprobar servicio/capacidad y
   permisos con evidencia; no repetir a ciegas todo el conjunto dorado ni subir ilimitadamente los timeouts.

3. Con una inferencia válida, arrancar la aplicación con una base de desarrollo propia. Desde el repo:

   ```bash
   mvn -q -DskipTests install
   mvn -pl certiflow-web spring-boot:run -Dspring-boot.run.profiles=demo,nvidia
   ```

   El perfil `demo` agrega usuarios y datos. Elegir una inspección abierta (GET `/api/v1/inspections?status=open`),
   cargar una observación/evidencia que contradiga una respuesta aprobada y pedir la revisión como inspector:

   ```bash
   curl -i -u ines.duarte:certiflow -X POST \
     'http://localhost:8080/api/v1/inspections/ID_DE_INSPECCION_ABIERTA/assistant-reviews'
   ```

4. Comprobar 201 y Location consultable, JSON convertible por el adaptador, citas admitidas por guardrails,
   consulta de sugerencias y severidades, aceptar/descartar una sola vez, auditoría y que la inspección no cambie.
   Verificar también persistencia después de reiniciar. Reproducir una falla del proveedor: debe dar 503 sin
   dejar una revisión o auditoría parcial. Mantener la llamada fuera de la transacción.
5. Registrar la evidencia real, parámetros, prompt/modelo y límites de la prueba; recién entonces marcar la
   integración externa como validada. Conservar los tests deterministas en CI sin key.
6. Continuar fase 5: el directorio `certiflow-harness` solo contiene los 15 casos dorados y su README;
   **no existe todavía un módulo Maven ejecutable ni métricas, comparador o workflow del harness**.
   Implementarlos antes de pedir 5 repeticiones por caso y comparar v1/v2 y dos modelos. Fase 6 (panel de
   frontend), diagrama/documentación final y video también siguen pendientes.

## Referencias

- Plan: `docs/plans/2026-11-07-entrega-3-asistente-ia.md`.
- Spec: `docs/specs/2026-11-07-asistente-ia.md`.
- [Modelo y disponibilidad](https://build.nvidia.com/google/gemma-4-31b-it).
- [API y enable_thinking](https://docs.api.nvidia.com/nim/reference/google-gemma-4-31b-it-infer).
- [Protocolo de invocación/polling de NVIDIA](https://docs.nvidia.com/nvcf/dev/overview/generic-http-function-invocation).
