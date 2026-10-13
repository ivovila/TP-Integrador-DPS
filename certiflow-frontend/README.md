# CertiFlow · Frontend

Interfaz web de CertiFlow. Cubre todas las capacidades actuales del sistema y **todavía no llama a la API**:
corre contra una implementación en memoria que aplica las reglas de negocio. Spec:
[docs/specs/2026-10-12-frontend.md](../docs/specs/2026-10-12-frontend.md).

## Cómo correrlo

Requiere Node 24 o superior. No necesita backend ni base de datos.

```bash
npm install
```

```bash
npm run dev
```

Abre en `http://localhost:5173` con datos de ejemplo. Los datos viven en memoria: recargar la página los
restablece.

```bash
npm run typecheck
```

```bash
npm test
```

```bash
npm run build
```

## Estructura

```
src
├── domain/            tipos del lenguaje ubicuo, sin lógica
├── ports/             lo que la interfaz le pide al sistema: una interfaz por contexto, comandos y errores
│   └── certiflowContract.ts   suite de tests que cualquier adaptador tiene que pasar
├── adapters/
│   └── inmemory/      implementación actual: reglas, reloj controlable, bitácora y datos de ejemplo
├── ui/
│   ├── app/           layout, rutas, sesión (persona que actúa), lista de personas de ejemplo
│   ├── shared/        piezas comunes y el registro de tipos de regla (ruleKinds.tsx)
│   └── catalog/ schema/ inspection/ certification/   pantallas por bounded context
├── composition.ts     raíz de composición: único archivo que conoce el adaptador
└── main.tsx
```

Reglas de dependencia: `ui` importa `ports` y `domain`, nunca `adapters`. `adapters` importa `ports` y `domain`,
nunca `ui`.

## Cómo se usa

- **Acting as** (encabezado): la persona que ejecuta cada operación. Queda registrada en el historial y es la
  que evalúan reglas como "quien asigna no inspecciona" o "el verificador no puede ser el responsable".
- **Today (demo)**: cambia la fecha del sistema en memoria. Sirve para ver una acción correctiva vencida o un
  certificado expirado. Solo aparece con el adaptador en memoria.

## Cómo se conecta la API

1. Crear `src/adapters/http/` con una implementación de cada puerto de `src/ports/`. Cada método hace el request
   y traduce la respuesta a los tipos de `src/domain/`:
   - la persona del comando (`answeredBy`, `closedBy`…) va a los headers `X-Person-Id` y `X-Person-Name`;
   - `actions` de cada recurso se arma con los `_links` de HAL (tabla `rel` → acción dentro del adaptador);
   - un `application/problem+json` se convierte en `DomainError` usando su `title` como `name`.
2. Cambiar `src/composition.ts` para que construya ese adaptador (sin `demoControls`, y el control de fecha
   desaparece solo).
3. Correr la suite de contrato contra el adaptador nuevo, igual que `inMemoryCertiflow.test.ts`.
4. Resolver cómo se sirve: un `server.proxy` de Vite hacia el backend en desarrollo, y mismo origen o CORS al
   desplegar.

`src/domain`, `src/ports` y `src/ui` no cambian.

## Cómo se extiende

| Cambio | Dónde |
|---|---|
| Un tipo de regla nuevo | Una variante en `ApprovalRule` (`domain/schema.ts`) y una entrada en `ui/shared/ruleKinds.tsx`. Mientras exista el adaptador en memoria, su evaluación en `adapters/inmemory/rules.ts`. |
| Una transición nueva de un recurso | Un valor en su unión de acciones (`domain/`), el método en el puerto y el botón en la pantalla, condicionado a `actions.includes(...)`. |
| Un dato nuevo de un recurso | El campo en su tipo de `domain/` y la fila en la pantalla. |
| F1, F2 y F3 | Ver §6 de la spec: qué se agrega en la interfaz, en los puertos y en el adaptador para cada una. |
