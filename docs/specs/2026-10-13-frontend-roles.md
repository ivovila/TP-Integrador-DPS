# Spec · Roles en el frontend

Fecha: 2026-10-13 · Rama: `entrega-2-spec1` · Continúa [Frontend conectado a la API](2026-10-13-frontend-api.md).

## 1. Objetivo

Hoy la interfaz le muestra a cualquiera todas las acciones, como si una sola persona hiciera el proceso
entero. Esta fase separa la interfaz por rol: cada tipo de usuario ve en el menú las secciones donde trabaja y
solo encuentra los formularios y botones de lo que le corresponde hacer.

Es una separación **de la interfaz**. El backend no tiene login ni roles: sigue aceptando a cualquier persona en
los headers, como hasta ahora. No es un control de acceso.

### Criterios de aceptación

1. Hay seis roles (§2) y cada persona de la lista de ejemplo tiene uno.
2. Elegir una persona en "Acting as" define el rol. Al escribir otra persona, se elige también su rol.
3. El menú muestra solo las secciones del rol (§3) y la aplicación abre en la primera.
4. Una acción aparece solo si el sistema la permite en ese estado **y** el rol puede hacerla.
5. Las pantallas de otras secciones se pueden abrir por un link y se ven completas, sin acciones.
6. Entrar por URL a un formulario que el rol no puede usar muestra qué rol hace falta, en lugar del formulario.
7. No cambia ningún archivo del backend ni de `src/ports`, `src/domain` o `src/adapters`.

### Fuera de alcance

- Autenticación, contraseñas o que el backend rechace por rol.
- Mostrar a cada persona solo sus propios registros (las inspecciones de este inspector, por ejemplo).
- Una persona con varios roles.

---

## 2. Roles y lo que hace cada uno

| Rol | Puede |
|---|---|
| **Administrator** | Registrar activos · crear esquemas · publicar versiones |
| **Inspection coordinator** | Asignar inspecciones |
| **Inspector** | Registrar respuestas, adjuntar evidencia, agregar observaciones · cerrar · rectificar |
| **Asset responsible** | Planificar acciones correctivas |
| **Verifier** | Aceptar o rechazar acciones correctivas |
| **Certification officer** | Emitir, suspender y renovar certificados, incluidos los globales |

Todos pueden leer cualquier registro, su historial y los tres informes (acta, resumen de hallazgos y documento
del certificado).

La tabla es coherente con las reglas que el dominio ya aplica: quien asigna no inspecciona, el responsable del
activo no asigna ni inspecciona su activo, y quien verifica no es el responsable de la acción.

---

## 3. Qué ve cada rol en el menú

| Rol | Secciones, en este orden | Por qué |
|---|---|---|
| Administrator | Assets · Schemas | Donde da de alta y define plantillas |
| Inspection coordinator | Inspections · Assets | Asigna desde la lista de inspecciones o desde un activo |
| Inspector | Inspections | Ejecuta, cierra y rectifica |
| Asset responsible | Findings · Assets | Planifica sobre los hallazgos; consulta sus activos |
| Verifier | Findings | Verifica las acciones de cada hallazgo |
| Certification officer | Certificates · Inspections · Assets | La emisión parte de la elegibilidad de una inspección; el certificado global, del activo |

La aplicación abre en la primera sección del rol. Cambiar de persona en una pantalla que no está en su menú no
redirige: la pantalla sigue visible, sin acciones.

Las secciones que no están en el menú siguen siendo alcanzables por los links entre registros (del hallazgo a su
inspección, del certificado a su activo). Así nadie queda en un callejón sin salida al seguir un caso.

---

## 4. Diseño

Todo vive en `src/ui`. Los puertos, el dominio y el adaptador HTTP no cambian: `resource.actions` sigue
significando "lo que el sistema permite ahora" (D22, D23).

- **`ui/app/roles.ts`**: una única tabla de datos, como `ruleKinds` y `actions.ts`. Por cada rol, su etiqueta,
  sus secciones del menú y el conjunto de acciones que puede hacer. Las acciones son las uniones que ya existen
  (`InspectionAction`, `CertificateAction`…) más las tres de alta que no dependen de un recurso: `registerAsset`,
  `createInspectionSchema` y `assignInspection`.
- **Sesión**: guarda persona y rol. Expone `permitted(actions)`, que devuelve de una lista de acciones las que el
  rol puede hacer. Las pantallas pasan `permitted(resource.actions)` donde hoy usan `resource.actions`; siguen
  sin preguntar por el estado ni por el nombre del rol.
- **Altas**: los tres botones que hoy se muestran siempre ("Register asset", "Create schema", "Assign inspection")
  se muestran solo si el rol puede. Sus rutas, y la de publicar una versión, muestran un aviso con el rol
  necesario cuando se entra por URL con otro rol.
- **Aviso de solo lectura**: cuando el sistema ofrece acciones sobre un registro y el rol no puede hacer ninguna,
  la pantalla lo dice en una línea ("Your role, Verifier, can read this inspection but not change it").
- **Lista de personas**: se agrega una administradora (`EMP-006`). Cada entrada lleva su rol tipado en lugar de un
  texto libre.
- **Lo que no viaja**: el rol no se envía a la API. Los headers siguen llevando solo id y nombre.

Agregar un rol o mover una acción de un rol a otro es editar `roles.ts`. Ninguna pantalla nombra un rol.

---

## 5. Tests

| Tipo | Qué |
|---|---|
| Tabla de permisos | Parametrizado rol × acción: cada acción la puede exactamente un rol, y las secciones de cada rol son las de §3. |
| Pantalla | `InspectionDetailPage`: con las mismas acciones ofrecidas por el sistema, un inspector ve los botones y un verificador ve el aviso de solo lectura. El test actual, que entra como la primera persona de la lista, pasa a entrar como inspector. |
| Menú y altas | El menú de cada rol; el aviso al entrar a un formulario de alta con otro rol. |
| Contra el backend | Los cinco flujos de `applicationFlow.e2e.test.tsx` eligen la persona del rol antes de cada paso (administradora para registrar, coordinadora para asignar, inspectora para ejecutar…). Es además la comprobación de que el proceso completo se puede hacer con los seis roles. |

La suite de contrato de los puertos no cambia: prueba el sistema, que no sabe de roles.

---

## 6. Documentación

- `DESIGN.md`: **D24. Roles en la interfaz** (dónde, por qué una tabla de datos y la intersección con las
  acciones del sistema, alternativas descartadas: rechazo en el backend sin login, rol elegido aparte de la
  persona, filtrar registros por persona) y la limitación "los roles no son un control de acceso".
- `README.md`, `certiflow-frontend/README.md`, `CHANGES.md` y `docs/specs/README.md`.
- Plan en `docs/plans/2026-10-13-frontend-roles.md`.

---

## 7. Decisiones confirmadas (2026-10-13)

1. **La tabla de roles de §2**, con la administradora aparte: rectificar lo hace el inspector y planificar
   acciones correctivas, el responsable del activo.
2. **El menú de §3**, con Inspections y Assets para el oficial de certificación.
3. **Selectores de persona en los formularios**: "Inspector" ofrece solo inspectores y "Responsible" solo
   responsables de activo, manteniendo "Someone else…".
4. **Datos de demostración**: `npm run seed` registra activos y esquemas como la administradora y planifica las
   acciones correctivas como el responsable del activo. Se recrea la base local.
5. **Aviso de solo lectura** cuando el sistema ofrece acciones y el rol no puede hacer ninguna.
