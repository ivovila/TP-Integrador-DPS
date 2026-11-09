> **Reporte histórico, no válido para comparación.** Generado antes de corregir el tratamiento de corridas fallidas y sin huella de las entradas. La línea de base v1 tiene una falla; hay que volver a ejecutar ambas versiones con el harness actual. Las cifras siguientes se conservan solo como evidencia original.

# Prompt v2 contra v1

Modelo: private/nvidia/nemotron-3.5-super-120b-a12b. Casos: 15, 5 corridas por caso.

| Métrica | v1 (media, rango) | v2 (media, rango) | ¿Empeoró? |
|---|---|---|---|
| Hallazgos correctos | 10.80 (10–11) | 11.00 (11–11) | no |
| Severidades acertadas | 7.60 (7–8) | 9.80 (9–10) | no |
| Hallazgos inventados | 0.00 (0–0) | 0.00 (0–0) | no |
| Omitidos | 0.00 (0–0) | 0.00 (0–0) | no |

Ninguna métrica empeoró.
