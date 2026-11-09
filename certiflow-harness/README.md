# Harness de evaluación del asistente

El asistente usa un modelo de lenguaje, que no responde siempre igual: un test con `assertEquals` no alcanza para
saber si funciona. El harness corre un **conjunto dorado** de inspecciones con lo que una persona experta esperaría
que el asistente señale, cada caso **varias veces**, y mide tasas. Así un cambio de prompt se decide con números.

Es un módulo aparte, que no forma parte de la aplicación. Carga cada caso en una inspección del dominio, la traduce
con el mismo traductor que la aplicación, llama al asistente con la misma configuración (`SpringAiAssistantConfiguration`)
y aplica los mismos guardrails (`AssistantReviewFactory`). Mide lo que el inspector vería.

## Qué mide

Sobre las sugerencias que pasan los guardrails, por caso y en total (spec §7.2):

| Métrica | Qué cuenta | Mejor |
|---|---|---|
| Hallazgos correctos | criterios sugeridos que estaban esperados | más |
| Severidades acertadas | de los correctos, los que estimaron la severidad esperada | más |
| Hallazgos inventados | criterios sugeridos que no estaban esperados | menos |
| Omitidos | criterios esperados sin sugerencia | menos |

Además, cuántas propuestas frenó cada guardrail. Cuenta criterios, no sugerencias: proponer dos veces el mismo
criterio no es acertar dos veces. Cada caso corre 5 veces; se reportan la media y el rango. Los totales suman los
casos de cada corrida. Una corrida en la que el proveedor no respondió queda como falla y no se mide. Para los totales se excluye toda repetición que no haya completado todos los casos; se informa cuántas repeticiones quedaron completas. Una falla nunca se convierte en cero.

**Cuándo empeora una versión:** cuando la media de una métrica queda fuera del rango que se observó con la versión
anterior, en la dirección mala. Una diferencia dentro de ese rango es el ruido propio del modelo.

## Cómo se corre

Desde la raíz, en WSL, después de `mvn -q -DskipTests install`:

```bash
mvn -q -pl certiflow-harness exec:exec
```

Por defecto corre contra el stub, sin red ni key. Contra el modelo real, con la key en el entorno:

```bash
HARNESS_PROVIDER=spring-ai HARNESS_PROMPT_VERSION=v1 mvn -q -pl certiflow-harness exec:exec
```

| Variable | Por defecto | Qué cambia |
|---|---|---|
| `HARNESS_PROVIDER` | `stub` | `stub` o `spring-ai` |
| `HARNESS_PROMPT_VERSION` | `v1` | versión de prompt publicada |
| `HARNESS_RUNS` | `5` | corridas por caso |
| `NVIDIA_API_KEY`, `NVIDIA_MODEL`, `CERTIFLOW_ASSISTANT_EXTRA_BODY` | — , Nemotron 3.5 Super, `{}` | igual que en la aplicación |

El reporte queda en `results/<versión de prompt>/<modelo>.json`, con lo que el asistente propuso en cada corrida, y su
resumen en `.md`. Se versionan: el pull request que cambia un prompt muestra su efecto. Para comparar dos versiones:

```bash
mvn -q -pl certiflow-harness exec:exec -Dharness.args="compare results/v1/<modelo>.json results/v2/<modelo>.json"
```

Escribe `results/v2/comparison-with-v1.md` y termina con código 1 si alguna métrica empeoró. También rechaza reportes incompletos, modelos distintos, cantidades de corridas distintas o huellas diferentes de criterios, respuestas y expectativas. `run` guarda el reporte parcial y termina con error si hubo fallas.

Los reportes históricos de v1/v2 carecen de huellas y no son una comparación válida: volver a ejecutar ambas versiones. Los JSON originales se conservan como evidencia, sin inventar corridas ni huellas retrospectivas.

El seed del frontend y los activos del harness cargan los esquemas de `certiflow-infrastructure/src/main/resources/demo/schemas.json`. Una única definición mantiene alineados reglas, severidades y evidencia requerida.

## En CI y fuera de CI

- **CI** (`ci.yml`) corre los tests del módulo: el formato del conjunto dorado, las métricas sobre respuestas
  conocidas y la línea de base exacta del stub. No usa red.
- **El modelo real** corre en `harness.yml`, a mano o cuando cambia un archivo de prompts: mide la última versión,
  la compara con la anterior y falla si empeoró. Necesita el secreto `NVIDIA_API_KEY` del repositorio.

El conjunto dorado y cómo revisarlo están en [`src/main/resources/golden`](src/main/resources/golden/README.md).
