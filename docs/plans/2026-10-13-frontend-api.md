# Frontend conectado a la API · Plan de implementación

**Goal:** Que la interfaz de `certiflow-frontend/` funcione contra el backend real: adaptador HTTP detrás de los puertos existentes, sin modo en memoria, con las lecturas que le faltan a la API.

**Architecture:** `src/adapters/http` implementa los seis puertos sobre un cliente HAL propio. Funciones puras traducen las respuestas de la API a los tipos de `src/domain`. `composition.ts` sigue siendo el único archivo que conoce el adaptador. El backend gana cuatro agregados de solo lectura.

**Tech Stack:** el de la fase anterior, más `tsx` (script de datos). Backend: Java 25, Spring Boot 4.1.1, PostgreSQL.

**Spec:** [docs/specs/2026-10-13-frontend-api.md](../specs/2026-10-13-frontend-api.md)

## Global Constraints

- Backend: solo se agregan endpoints, campos y links. Ninguna respuesta existente cambia de forma. Cada agregado tiene su test de API y `mvn -q clean verify` queda en verde.
- `certiflow-domain` y `certiflow-application` siguen sin importar Spring, JPA ni Jackson.
- Frontend: ningún archivo de `src/ui` importa de `src/adapters`; las firmas de los puertos no cambian salvo lo indicado en la spec §3.7.
- Todo `href` y `Location` se reduce a ruta y query antes de usarse.
- El adaptador envía exactamente los campos del contrato: un campo de más es un 400.
- Nombres largos y explícitos. Sin comentarios explicativos en código nuevo. Interfaz en inglés.
- Commits sin `Co-Authored-By` ni atribución; un commit por tarea, mensaje en inglés y en imperativo.
- Los tests se verifican en un paso aparte antes de cada commit.

## Review Focus

1. **Un `href` absoluto nunca llega a `fetch`.** Con el proxy de Vite, seguirlo sin reducir saltea el proxy.
2. **Los tests contra el backend no dependen del orden ni de una base limpia.** Cada uno usa códigos y tipo de activo únicos.
3. **Un rechazo del servidor siempre se ve con su motivo**, sea de negocio o no.
4. **Las jurisdicciones salen del registro de políticas**, no de una segunda lista.

## Mapa de archivos

| Dónde | Qué |
|---|---|
| `certiflow-domain` | `schema.usecase.ListSchemasUseCase`; `InspectionSchemaRepository.findAll()`; `certification.model.RegisteredJurisdictions`, `JurisdictionPolicy`; `certification.usecase.ListJurisdictionsUseCase`; `FindingLine` con `findingId` y `subsystemCode` |
| `certiflow-application` | `ListSchemasUseCaseImpl`, `ListJurisdictionsUseCaseImpl`; `GenerateFindingsSummaryUseCaseImpl` |
| `certiflow-infrastructure` | `findAll()` en los dos repositorios de esquemas; `ConfiguredPolicyResolver implements RegisteredJurisdictions` |
| `certiflow-web` | `SchemaController` (lista), `VocabularyController`, `RootController` (`schema-list`, `vocabulary`), `FindingResource`, `FindingsSummaryResource`, `AssetController` (link), `UseCaseConfiguration`; tests de API |
| `certiflow-frontend/src/adapters/http` | cliente, tipos de la API, tabla de acciones, mapeos, seis puertos, `__fixtures__/` |
| `certiflow-frontend/src/ports` | `RejectedRequestError.ts`; se va `DemoControls.ts`; `certiflowContract.ts` adaptada |
| `certiflow-frontend` | `vite.config.ts` (proxy), `vitest.e2e.config.ts`, `scripts/seedDemoData.ts`, `package.json` |
| raíz | `.github/workflows/ci.yml` (job `e2e`), `README.md`, `DESIGN.md`, `CHANGES.md` |

---

### Task 1: Agregados a la API

- [ ] `GET /api/v1/schemas` con `ListSchemasUseCase` y `findAll()`; link `schema-list` en la raíz.
- [ ] `GET /api/v1/vocabulary`: severidades, tipos de evidencia y jurisdicciones del registro de políticas; link `vocabulary` en la raíz.
- [ ] `subsystemCode` en `FindingResource`; `findingId` y `subsystemCode` en las líneas del resumen.
- [ ] Link `global-certificates` en activos con subsistemas.
- [ ] Un test de API por agregado. Verificación: `mvn -q clean verify`.

### Task 2: Respuestas capturadas

- [ ] Volver a correr el flujo de captura contra el backend con los agregados y guardar las respuestas en `src/adapters/http/__fixtures__/`.

### Task 3: Cliente y mapeos

- [ ] `HalClient`: URL base, headers, persona al escribir, reducción de `href`, paginación, errores.
- [ ] `wire.ts`, `actions.ts` y los cinco archivos de mapeo, con sus tests contra las respuestas capturadas.
- [ ] `RejectedRequestError` en `src/ports` y `ErrorBanner` que muestra cualquier rechazo.

### Task 4: Puertos HTTP y composición

- [ ] Los seis puertos y `createHttpCertiflow`.
- [ ] `composition.ts` usa el adaptador HTTP; proxy de `/api` en `vite.config.ts`.
- [ ] Fechas: hora local del navegador al mostrar y al enviar `effectiveFrom`.
- [ ] `Jurisdiction` sin `description`; se quita el control de fecha del encabezado.

### Task 5: Tests contra el backend

- [ ] `certiflowContract.ts`: códigos y tipo de activo únicos por test; fuera los siete casos que mueven la fecha.
- [ ] `vitest.e2e.config.ts` y `npm run test:e2e`; la suite de contrato y los flujos por pantallas corren contra `CERTIFLOW_API_URL`.
- [ ] Verificación: `npm run test:e2e` con el backend levantado, varias veces seguidas sobre la misma base.

### Task 6: Quitar el modo en memoria

- [ ] Borrar `src/adapters/inmemory/`, `src/ports/DemoControls.ts` y `src/application.test.tsx`.
- [ ] Verificación: `npm run typecheck && npm test && npm run build`.

### Task 7: Datos de demostración

- [ ] `scripts/seedDemoData.ts` y `npm run seed` (spec §6).
- [ ] Recrear la base local vacía y cargarla.

### Task 8: CI y documentación

- [ ] Job `e2e` en `ci.yml`.
- [ ] `DESIGN.md` (D22, D23, limitaciones), `README.md`, `certiflow-frontend/README.md`, `CHANGES.md`.
- [ ] Recorrido manual en el navegador contra el backend con los datos cargados.
