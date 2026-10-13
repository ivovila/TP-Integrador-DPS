# Roles en el frontend · Plan de implementación

**Goal:** Separar la interfaz por rol: cada rol ve en el menú sus secciones y solo encuentra las acciones que le corresponden.

**Architecture:** Todo en `certiflow-frontend/src/ui`. Una tabla de datos (`ui/app/roles.ts`) define por rol su etiqueta, sus secciones y sus acciones. La sesión guarda persona y rol y expone `permitted(actions)`, que las pantallas aplican sobre las acciones que ya entrega el sistema.

**Tech Stack:** el del frontend actual.

**Spec:** [docs/specs/2026-10-13-frontend-roles.md](../specs/2026-10-13-frontend-roles.md)

## Global Constraints

- No cambia ningún archivo del backend ni de `src/ports`, `src/domain` o `src/adapters`.
- Ninguna pantalla nombra un rol ni pregunta por el estado de un recurso: usa `permitted(resource.actions)`.
- El rol no viaja a la API.
- Nombres largos y explícitos. Sin comentarios explicativos en código nuevo. Interfaz en inglés.
- Commits sin `Co-Authored-By` ni atribución; un commit por tarea, mensaje en inglés y en imperativo.
- Los tests se verifican en un paso aparte antes de cada commit.

## Review Focus

1. **Cada acción la puede exactamente un rol.** Si una acción queda sin rol, nadie puede hacerla desde la interfaz.
2. **El proceso completo sigue siendo posible**: los flujos contra el backend lo recorren cambiando de persona.
3. **Cambiar de persona no deja la pantalla en un estado viejo**: los botones cambian en el momento.

## Mapa de archivos

| Archivo | Cambio |
|---|---|
| `ui/app/roles.ts` | nuevo: roles, secciones y acciones permitidas |
| `ui/app/roster.ts` | cada persona con su rol tipado; se agrega la administradora |
| `ui/app/SessionContext.tsx` | persona y rol; `permitted(actions)`, `mayDo(action)` |
| `ui/app/Layout.tsx`, `ui/app/routes.tsx` | menú por rol, página de inicio por rol, rutas de alta protegidas |
| `ui/shared/PersonPicker.tsx` | rol al escribir otra persona en el encabezado; filtro por rol en formularios |
| `ui/shared/RoleNotices.tsx` | nuevo: aviso de solo lectura y aviso de rol necesario |
| pantallas de `ui/{catalog,schema,inspection,certification}` | `permitted(...)` donde hoy se usa `actions` |
| `scripts/seedDemoData.ts` | quién registra, crea y planifica |
| tests | tabla de permisos, pantalla, menú, flujos contra el backend |
| raíz | `DESIGN.md` (D24), `README.md`, `CHANGES.md`, `docs/specs/README.md` |

---

### Task 1: Tabla de roles y sesión

- [ ] `roles.ts`, `roster.ts` y `SessionContext` con persona y rol.
- [ ] Test parametrizado de la tabla: cada acción en exactamente un rol; secciones por rol.

### Task 2: Menú, inicio y selector de persona

- [ ] Menú y página de inicio por rol.
- [ ] "Acting as" define el rol; "Someone else…" pide rol. Selectores de formulario filtrados por rol.

### Task 3: Acciones por rol en las pantallas

- [ ] `permitted(...)` en las pantallas; botones y rutas de alta por rol; avisos de solo lectura y de rol necesario.
- [ ] Tests de pantalla y de menú.

### Task 4: Flujos, datos y documentación

- [ ] `applicationFlow.e2e.test.tsx` elige la persona del rol antes de cada paso.
- [ ] `seedDemoData.ts` ajustado; base local recreada y cargada.
- [ ] `DESIGN.md` (D24 y limitación), READMEs, `CHANGES.md`, índice de specs.
- [ ] Recorrido en el navegador como cada rol.
