# CertiFlow · Frontend

Interfaz web de CertiFlow. Cubre todas las capacidades de la consigna y F1, F2 y F3, y trabaja contra la API
REST del backend. Specs: [frontend sobre puertos](../docs/specs/2026-10-12-frontend.md) y
[frontend conectado a la API](../docs/specs/2026-10-13-frontend-api.md).

## Cómo corre

Requiere Node 24 o superior y el backend levantado (ver el [README de la raíz](../README.md)).

```bash
npm install
```

```bash
npm run dev
```

Abre en `http://localhost:5173`. El servidor de desarrollo reenvía `/api` al backend, así el navegador habla con
un solo origen. Por defecto el backend está en `http://localhost:8080`; para otro, definir `CERTIFLOW_API_URL`
antes de `npm run dev`.

Para partir de una base con datos de demostración (activos en tres jurisdicciones, un esquema con una versión
programada, hallazgos, certificados parciales y uno global):

```bash
npm run seed
```

Usa la misma API que la interfaz. Si los datos ya están, avisa y no carga nada.

## Tests

```bash
npm run typecheck
```

```bash
npm test
```

Tests rápidos, sin backend: el cliente HTTP, la traducción de las respuestas de la API (contra respuestas reales
guardadas en `src/adapters/http/__fixtures__/`) y las pantallas con puertos sustitutos.

```bash
npm run test:e2e
```

Tests contra el backend levantado: la suite de contrato de los puertos y los flujos manejados desde las
pantallas. Cada test usa códigos y tipos de activo propios, así que se pueden correr varias veces sobre la misma
base.

`scripts/captureApiResponses.sh` vuelve a generar las respuestas guardadas. Hay que correrlo contra una base
vacía cuando cambia la forma de una respuesta de la API.

## Estructura

```
src
├── domain/            tipos del lenguaje ubicuo, sin lógica
├── ports/             lo que la interfaz le pide al sistema: una interfaz por contexto, comandos y errores
│   └── certiflowContract.ts   suite de contrato que corre contra el backend
├── adapters/
│   └── http/          implementación sobre la API REST
│       ├── HalClient.ts        fetch, headers, credenciales, errores, paginación
│       ├── actions.ts          tabla rel → acción
│       ├── *Mapping.ts         de la respuesta de la API a los tipos de domain/
│       ├── Http*Port.ts        un puerto por contexto
│       └── __fixtures__/       respuestas reales de la API
├── ui/
│   ├── app/           layout, rutas, sesión (persona y rol), tabla de roles, lista de personas de ejemplo
│   ├── shared/        piezas comunes y el registro de tipos de regla (ruleKinds.tsx)
│   └── catalog/ schema/ inspection/ certification/   pantallas por bounded context
├── composition.ts     raíz de composición: único archivo que conoce el adaptador
└── main.tsx
scripts/               datos de demostración y captura de respuestas
```

Reglas de dependencia: `ui` importa `ports` y `domain`, nunca `adapters`. `adapters` importa `ports` y `domain`,
nunca `ui`.

## Roles

Se ingresa con id de persona y contraseña; el rol sale del token. Cada rol ve en el menú sus secciones y encuentra solo las
acciones que le corresponden; el resto se puede abrir por los links entre registros, en solo lectura.

| Rol (persona de ejemplo) | Menú | Puede |
|---|---|---|
| Administrator (Ana Torres) | Assets, Schemas | Registrar activos, crear esquemas, publicar versiones |
| Inspection coordinator (Paula Rios) | Inspections, Assets | Asignar inspecciones |
| Inspector (Ines Duarte) | Inspections | Responder, adjuntar evidencia, observar, cerrar, rectificar |
| Asset responsible (Raul Mendez) | Findings, Assets | Planificar acciones correctivas |
| Verifier (Vera Soto) | Findings | Aceptar o rechazar acciones correctivas |
| Certification officer (Carlos Vidal) | Certificates, Inspections, Assets | Emitir, suspender y renovar certificados, incluidos los globales |

Una acción aparece si el sistema la permite en ese estado (lo dice la API) y el rol puede hacerla (lo dice
`src/ui/app/roles.ts`). Es una separación de la interfaz: la API no tiene login y no restringe por rol.

## Cómo habla con la API

- **URLs.** El adaptador arma la URL de cada recurso con su identificador y sigue los links de la respuesta para
  las acciones disponibles, la paginación y los recursos relacionados.
- **Links.** La API mezcla links absolutos y relativos. Todos se reducen a ruta y query antes de usarse, para que
  pasen por el mismo origen.
- **Acciones.** Lo que se puede hacer con un recurso sale de sus links (`closure` → cerrar, `suspensions` →
  suspender…). La tabla está en `actions.ts`. Las pantallas muestran un botón por acción y no miran el estado.
- **Persona que actúa.** Es quien ingresó. `BrowserSession` manda Basic una vez (en `GET /api/v1`), guarda el
  token que vuelve en `sessionStorage` y lo envía como `Authorization: Bearer` en cada request, junto con
  `X-Requested-With` para que el navegador no abra su propio diálogo. Un 401 lo borra y vuelve al login; "Logout"
  hace `DELETE /api/v1/tokens/current`. Los scripts y las suites e2e usan `PersonLogins`, que ingresa como la
  persona de cada comando.
- **Errores.** Un `problem+json` con una excepción de dominio se convierte en `DomainError`; cualquier otro rechazo,
  en `RejectedRequestError`. Las pantallas muestran siempre el motivo que dio el servidor.
- **Fechas.** Los instantes se muestran en la hora local del navegador. Las fechas sin hora viajan como
  `AAAA-MM-DD`.

## Cómo se extiende

| Cambio | Dónde |
|---|---|
| Un tipo de regla nuevo | Una variante en `ApprovalRule` (`domain/schema.ts`), una entrada en `ui/shared/ruleKinds.tsx` y su traducción en `adapters/http/schemaMapping.ts`. |
| Una transición nueva de un recurso | Un valor en su unión de acciones (`domain/`), su `rel` en `adapters/http/actions.ts`, el método en el puerto y el botón en la pantalla, condicionado a `actions.includes(...)`. |
| Un dato nuevo de un recurso | El campo en su tipo de `domain/`, en `adapters/http/wire.ts` y en su función de mapeo, y la fila en la pantalla. |
| Una jurisdicción nueva | Nada en el frontend: el alta de activos la toma de `GET /api/v1/vocabulary`. |
| Un rol nuevo, o una acción que pasa a otro rol | `ui/app/roles.ts` (y una persona con ese rol en `ui/app/roster.ts`). Ninguna pantalla nombra un rol. |
