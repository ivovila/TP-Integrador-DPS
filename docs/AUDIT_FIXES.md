# Correcciones de la auditoría · 9 de noviembre de 2026

Se corrigieron los hallazgos funcionales y de estructura del audit previo en la rama `entrega-3`. Este documento registra el alcance y la evidencia de validación.

## Cambios

- **Harness:** las repeticiones incompletas se excluyen enteras de los totales. Las comparaciones requieren cobertura completa, mismo modelo, mismas repeticiones y una huella de los criterios, respuestas y expectativas. Una ejecución parcial guarda evidencia y termina con error. Los reportes históricos v1/v2 quedaron advertidos como no válidos para comparar. El workflow mide ambas versiones con el mismo checkout.
- **Propuestas:** fundamento obligatorio y una única sugerencia válida decidible por criterio. Las restantes quedan trazadas como `DUPLICATE_CRITERION`. Los guardrails se clasifican una sola vez.
- **Trazabilidad:** la migración V8 conserva la inspección leída por la IA. La API y la pantalla muestran esa captura; las revisiones antiguas declaran que el dato histórico no está disponible. Cambiar la respuesta actual no reescribe la revisión.
- **Modelado y consultas:** las reglas se escriben con `RuleDescriber` y se leen con el catálogo inyectado; se eliminan los casts a clases concretas de reglas y severidades. Los subrecursos de aceptación/descarte se seleccionan por las variantes del dominio. El listado carga una inspección y un índice de severidades, y trae las decisiones juntas.
- **Duplicación y pruebas:** demo y harness leen un único `demo/schemas.json`. El contrato de 1475 líneas se separó en 14 contextos dentro de `test-support`, fuera de los puertos de producción. El adaptador rechaza decisiones incompletas.
- **Fallas encontradas al ejecutar todo:** se midieron retrocesos de unos 0,8 y 1,3 segundos en el reloj de WSL. `OrderedClock` conserva precisión de microsegundos y orden temporal dentro del proceso. Se agregaron regresiones con el salto real observado. El flujo de pantallas usa el filtro de activo para localizar su inspección. En móvil, las tablas desplazan su contenido dentro de las tarjetas y no ensanchan toda la página.

## Verificación realizada

| Verificación | Resultado |
|---|---|
| Maven completo, arquitectura, H2 y PostgreSQL | 559 pruebas pasaron; 1 smoke externo omitido en esta suite y ejecutado por separado |
| Smoke real NVIDIA, prompt v2 | Pasó; Nemotron detectó `EXT-02`, con entrada histórica persistida |
| Frontend: tipos y tests rápidos | 165 pruebas pasaron |
| E2E contra backend y PostgreSQL reales | 119 pruebas pasaron después de corregir el reloj |
| Build de producción frontend | Pasó |
| Harness con stub | 15 casos × 5 corridas, 75 medidas, ninguna falla y huellas presentes |
| Impeccable, componentes y estilos modificados | Sin observaciones pendientes |
| Navegador | Login, respuesta, observación, solicitud de revisión, cambio posterior de respuesta, confirmación y recarga con decisión persistida; revisión de escritorio y móvil |

Comandos principales: `mvn -q -Dcertiflow.postgres=true verify install`, `npm run typecheck`, `npm test`, `npm run test:e2e`, `npm run build` y `mvn -q -pl certiflow-harness exec:exec`. El smoke se ejecutó con `-Dtest=NvidiaAssistantSmokeApiTest -Dsurefire.failIfNoSpecifiedTests=false` y la clave solo en el entorno del proceso.

## Instancia dejada levantada

- Página: http://localhost:5173
- API de demo: http://localhost:8081
- Usuario: `ines.duarte`; contraseña de demo: `certiflow`.
- PostgreSQL aislado: contenedor `certiflow-audit-postgres`, puerto `55433`, base `certiflow_live`. Las bases de pruebas están separadas de la demo.
- La página usa el proveedor stub para la demostración repetible; la integración NVIDIA fue validada con el smoke separado. La clave no se escribió en archivos ni en el repositorio.

## Límite de la evidencia

**No se repitió el benchmark completo v1/v2 contra NVIDIA.** Los reportes viejos no prueban ausencia de regresiones: hay que medir nuevamente ambas versiones con el harness corregido. Con cinco repeticiones, el workflow realiza 75 llamadas por versión. La prueba real realizada aquí valida una revisión de extremo a extremo, no sustituye ese benchmark.

El orden del reloj es local al proceso. Un despliegue con varias instancias sigue requiriendo relojes sincronizados entre servidores.
