> **Reporte histórico, no válido para comparación.** Generado antes de corregir el tratamiento de corridas fallidas y sin huella de las entradas. La línea de base v1 tiene una falla; hay que volver a ejecutar ambas versiones con el harness actual. Las cifras siguientes se conservan solo como evidencia original.

# Harness · prompt v2 · private/nvidia/nemotron-3.5-super-120b-a12b

15 casos, 5 corridas por caso, 2026-11-09T00:04:23.887897490Z. Corridas sin respuesta del proveedor: 0.

## Totales por corrida

| Métrica | Media | Rango |
|---|---|---|
| Hallazgos correctos | 11.00 | 11–11 |
| Severidades acertadas | 9.80 | 9–10 |
| Hallazgos inventados | 0.00 | 0–0 |
| Omitidos | 0.00 | 0–0 |

Propuestas frenadas por guardrail (todas las corridas): ninguna.

## Por caso (media de las corridas)

| Caso | Tipo | Esperado | Correctos | Severidad | Inventados | Omitidos | Fallas |
|---|---|---|---|---|---|---|---|
| case-01 | hidden | HOSE MINOR | 1.00 | 0.80 | 0.00 | 0.00 | 0 |
| case-02 | hidden | SEAL MAJOR | 1.00 | 1.00 | 0.00 | 0.00 | 0 |
| case-03 | hidden | CORROSION MAJOR | 1.00 | 1.00 | 0.00 | 0.00 | 0 |
| case-04 | hidden | TAG MAJOR | 1.00 | 1.00 | 0.00 | 0.00 | 0 |
| case-05 | hidden | PRESSURE CRITICAL | 1.00 | 0.00 | 0.00 | 0.00 | 0 |
| case-06 | hidden | SIGNAGE MINOR | 1.00 | 1.00 | 0.00 | 0.00 | 0 |
| case-07 | hidden | HEIGHT MINOR | 1.00 | 1.00 | 0.00 | 0.00 | 0 |
| case-08 | hidden | LEAKS MAJOR | 1.00 | 1.00 | 0.00 | 0.00 | 0 |
| case-09 | hidden | GROUNDING MAJOR | 1.00 | 1.00 | 0.00 | 0.00 | 0 |
| case-10 | hidden | RELIEF-VALVE CRITICAL, PANEL MINOR | 2.00 | 2.00 | 0.00 | 0.00 | 0 |
| case-11 | clean | — | 0.00 | 0.00 | 0.00 | 0.00 | 0 |
| case-12 | clean | — | 0.00 | 0.00 | 0.00 | 0.00 | 0 |
| case-13 | clean | — | 0.00 | 0.00 | 0.00 | 0.00 | 0 |
| case-14 | trap | — | 0.00 | 0.00 | 0.00 | 0.00 | 0 |
| case-15 | trap | — | 0.00 | 0.00 | 0.00 | 0.00 | 0 |
