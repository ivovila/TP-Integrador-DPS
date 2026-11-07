# Conjunto dorado del asistente de revisión

Casos con los que el harness mide al asistente: lo que cargó el inspector y los hallazgos que una persona experta
esperaría que el asistente señale. Contrato en [la spec, §7](../../../../../docs/specs/2026-11-07-asistente-ia.md).

Los casos usan la versión 1 de los dos esquemas de la demo (`certiflow-frontend/scripts/seedDemoData.ts`), así la
demo del video y el harness hablan del mismo sistema.

## Qué cuenta como hallazgo esperado

Una no conformidad que **el texto describe y la respuesta no refleja**: la regla aprueba el criterio, pero una
observación o una descripción de evidencia muestra que está mal. Lo que la regla ya marca no es esperado, porque
ese hallazgo lo levanta la regla sola (lo descarta el guardrail `ALREADY_NONCONFORMING`).

La severidad esperada es la configurada en el criterio, que es la que tendría el hallazgo.

## Casos

| Caso | Tipo | Esquema | Qué pone a prueba | Esperado |
|---|---|---|---|---|
| 01 | escondido | Matafuego | Observación que contradice la respuesta | `HOSE` MINOR |
| 02 | escondido | Matafuego | La evidencia, no la observación, contradice la respuesta | `SEAL` MAJOR |
| 03 | escondido | Matafuego | Observación descriptiva sin palabras obvias de falla | `CORROSION` MAJOR |
| 04 | escondido | Matafuego | Documento y observación juntos | `TAG` MAJOR |
| 05 | escondido | Matafuego | Un número en rango contradicho por una foto; severidad crítica | `PRESSURE` CRITICAL |
| 06 | escondido | Matafuego | El problema está anotado en **otro** criterio | `SIGNAGE` MINOR |
| 07 | escondido | Matafuego | Un número contradicho por otro número en el texto | `HEIGHT` MINOR |
| 08 | escondido | Recipiente | Anotado en otro criterio, sobre el otro esquema | `LEAKS` MAJOR |
| 09 | escondido | Recipiente | Observación directa sobre el subsistema eléctrico | `GROUNDING` MAJOR |
| 10 | escondido | Recipiente | Dos no conformidades en el mismo caso | `RELIEF-VALVE` CRITICAL, `PANEL` MINOR |
| 11 | limpio | Matafuego | Observaciones que solo confirman | — |
| 12 | limpio | Recipiente | Verificaciones que salieron bien, con números en rango | — |
| 13 | limpio | Matafuego | Un pedido del encargado que no es una falla | — |
| 14 | trampa | Recipiente | Negación: nombra fugas y humedad para decir que no hay | — |
| 15 | trampa | Matafuego | Criterio que la regla ya marca, y una recomendación sobre el soporte | — |

## Formato

```json
{
  "id": "case-01",
  "kind": "hidden | clean | trap",
  "description": "qué pone a prueba",
  "assetType": "Fire extinguisher | Pressure vessel",
  "schemaVersion": 1,
  "criteria": [
    {"code": "HOSE", "answer": "Good", "observations": ["…"], "evidence": [{"kind": "PHOTO | DOCUMENT", "description": "…"}]}
  ],
  "expected": [{"criterion": "HOSE", "severity": "MINOR"}]
}
```

- Cada caso responde **todos** los criterios de su esquema, con la evidencia obligatoria (`PRESSURE` necesita una
  foto y `TAG` un documento), para que ninguno quede pendiente por omisión.
- Las respuestas usan el texto que la regla sabe leer: número con punto decimal, `true` o `false`, o el nombre de
  la opción.
- Todas las respuestas pasan su regla, salvo `HOSE` en el caso 15, que es justamente lo que ese caso prueba.

## Cómo revisarlo

Lo importante es que **suene a una inspección real**: si un caso es tan obvio que ningún modelo puede fallarlo, o
tan forzado que ningún inspector lo escribiría así, no mide nada. Para cada caso:

1. ¿Un inspector escribiría esta observación así?
2. ¿Una persona experta señalaría exactamente lo esperado, ni más ni menos?
3. ¿La severidad esperada es razonable para lo que describe el texto?

Para agregar un caso: el siguiente número libre, un tipo, y que pase el control de formato del harness.
