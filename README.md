# Certiflow · Trabajo Integrador de Diseño de Sistemas (ITBA) · Entrega 2

Plataforma de inspección, habilitación y certificación de activos. Arquitectura DDD con cuatro bounded
contexts, persistencia en PostgreSQL con JPA y una API REST de nivel 3 (HAL).

## Cómo correrlo

Requiere JDK 25 y Maven 3.9 o superior.

```bash
mvn clean verify
```

Compila y corre todos los tests: dominio, casos de uso, repositorios (sobre H2 en modo PostgreSQL) y API.
Usar siempre `clean`: si un IDE compila en paralelo hacia `target/`, puede dejar clases o recursos duplicados.

Para levantar la aplicación contra PostgreSQL:

```bash
docker compose up -d
mvn -q -DskipTests install
mvn -pl certiflow-web spring-boot:run -Dspring-boot.run.profiles=demo
```

La conexión se configura con `CERTIFLOW_DB_URL` (por defecto `jdbc:postgresql://localhost:5432/certiflow`),
`CERTIFLOW_DB_USER` y `CERTIFLOW_DB_PASSWORD` (por defecto `certiflow`). Flyway crea el esquema al arrancar.

## Estructura

Build Maven de cuatro módulos, uno por capa DDD. Dentro de cada uno, un paquete por bounded context:
`catalog`, `schema`, `inspection`, `certification` y el shared kernel `shared`.

```
certiflow-domain            modelo y contratos; no depende de nada
└── ar/edu/itba/certiflow/<contexto>
    ├── model               agregados, value objects, servicios de dominio, fábricas, snapshots, excepciones
    ├── repository          interfaces de repositorio
    └── usecase             una interfaz por caso de uso, su comando y sus modelos de lectura

certiflow-application       implementación de los casos de uso (XxxUseCaseImpl) y el ACL de certificación
└── ar/edu/itba/certiflow/application/<contexto>

certiflow-infrastructure    adaptadores: JPA, Flyway, numeración, bitácora; repositorios en memoria para tests
└── ar/edu/itba/certiflow/infrastructure/<contexto>/persistence, inmemory

certiflow-web               Spring Boot: controladores REST, recursos HAL, composición, transacciones, errores
└── ar/edu/itba/certiflow/web/<contexto>, config, support

certiflow-frontend          interfaz web (fuera del build de Maven): puertos propios y adaptador HTTP sobre la API
└── src/domain, ports, adapters/http, ui/<contexto>
```

## API

Base `/api/v1`; `GET /api/v1` devuelve los links a todas las colecciones. Los recursos se devuelven en HAL
(`application/hal+json`) y los errores en `application/problem+json`.

**Autenticación.** Todo request se autentica. Se ingresa por cualquier path con `Authorization: Basic`; la
respuesta trae un token en `Authorization: Bearer …` (vence en una hora, `Token-Expires`) que se usa en los
requests siguientes. `DELETE /api/v1/tokens/current` hace logout: el token queda revocado. Cada escritura exige
el rol que le corresponde (403 si no); las lecturas, cualquier persona autenticada. Detalle en
[DESIGN.md](DESIGN.md) (D25).

```bash
curl -i -u EMP-006:certiflow localhost:8080/api/v1/assets        # devuelve Authorization: Bearer …
TOKEN=eyJ…                                                       # el valor después de "Bearer "
curl -i -X POST localhost:8080/api/v1/assets -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"code":"EXT-0042","name":"Lobby extinguisher","type":"Fire extinguisher","location":"Lobby",
       "jurisdictionCode":"DEMO-STANDARD","subsystems":[],"responsible":{"id":"EMP-003","name":"Raul Mendez"},
       "characteristics":[]}'
curl -i -X DELETE localhost:8080/api/v1/tokens/current -H "Authorization: Bearer $TOKEN"   # 204
```

**Usuarios de ejemplo.** Con el perfil `demo` (usuarios y datos) o `demo-users` (solo usuarios), contraseña
`certiflow`: `EMP-006` Ana Torres (Administrator), `EMP-001` Paula Rios (Inspection coordinator), `EMP-002` Ines
Duarte (Inspector), `EMP-003` Raul Mendez (Asset responsible), `EMP-004` Vera Soto (Verifier) y `EMP-005` Carlos
Vidal (Certification officer). La clave que firma los tokens se configura con `CERTIFLOW_JWT_SECRET` (mínimo 32
bytes; el valor por defecto es solo para desarrollo). Las tablas de usuarios se agregaron a `V1__init.sql`: una
base local creada antes hay que recrearla.

Las rutas, los códigos de estado y las reglas REST que sigue la API están en
[docs/specs/2026-10-12-fase-1-ddd-persistencia-api.md](docs/specs/2026-10-12-fase-1-ddd-persistencia-api.md) (§6)
y en [DESIGN.md](DESIGN.md) (D21).

## Frontend

`certiflow-frontend/` tiene la interfaz web (React + TypeScript + Vite). Trabaja contra la API: se corre en dos
procesos, y el servidor de desarrollo del frontend reenvía `/api` al backend. Requiere Node 24 o superior.

Con el backend levantado (`mvn -pl certiflow-web spring-boot:run`):

```bash
cd certiflow-frontend && npm install && npm run dev
```

Abre en `http://localhost:5173` con la pantalla de login (usuarios de ejemplo arriba). El rol de quien ingresa
(administradora, coordinadora, inspector, responsable del activo, verificador u oficial de certificación) decide
qué secciones ve y qué acciones encuentra; la API aplica los mismos roles. "Logout" revoca el token.

Para cargar datos de demostración a través de la API (con el backend en el perfil `demo-users`, que crea los usuarios):

```bash
cd certiflow-frontend && npm run seed
```

Para la interfaz, la API ganó `GET /api/v1/schemas` (lista de esquemas), `GET /api/v1/vocabulary` (severidades,
tipos de evidencia y jurisdicciones con política registrada), `subsystemCode` en los hallazgos, `findingId` y
`subsystemCode` en cada línea del resumen de hallazgos, y el link `global-certificates` en los activos con
subsistemas.

Estructura, tests y cómo habla con la API en [certiflow-frontend/README.md](certiflow-frontend/README.md);
decisiones de diseño en [DESIGN.md](DESIGN.md) (D22, D23 y D24).

## Integración continua

`.github/workflows/ci.yml` corre `mvn -B clean verify` en cada push y en cada pull request a `main`. Un segundo job,
`frontend`, corre `npm ci`, el chequeo de tipos, los tests rápidos y el build de `certiflow-frontend/`. Un tercero,
`e2e`, levanta PostgreSQL y el backend y corre contra ellos la suite de contrato y los flujos por pantallas del
frontend (`npm run test:e2e`); todavía no es un check obligatorio. Para que
un pull request con el build en rojo no se pueda mergear, activar la protección de `main` (una vez que el
check `build` haya corrido al menos una vez):

```bash
gh api -X PUT repos/ivovila/TP-Integrador-DPS/branches/main/protection \
  -F required_status_checks[strict]=true -f 'required_status_checks[contexts][]=build' \
  -F enforce_admins=false -F required_pull_request_reviews=null -F restrictions=null
```

## Documentación

- [DESIGN.md](DESIGN.md): supuestos, arquitectura (contextos, context map, capas, glosario), decisiones con
  sus alternativas descartadas, patrones no aplicados, tabla riesgo → test y limitaciones.
- [docs/specs](docs/specs): especificaciones de cada fase.
- [docs/plans](docs/plans): planes de implementación.
- [docs/consigna](docs/consigna): enunciado de la entrega.
- [docs/clases](docs/clases): apuntes de las clases que guían la arquitectura.
- [docs/certiflow-domain.puml](docs/certiflow-domain.puml): diagrama de clases en PlantUML.


## F1, F2 y F3

El modelo nuevo requiere jurisdicción explícita en cada activo y calcula vigencia de certificados en el
servidor. No hay modo LEGACY ni migración de datos anteriores: usar una base nueva para esta versión.
Las specs y planes viven en [docs/specs](docs/specs/README.md) y [docs/plans](docs/plans).

- **F2:** publicar una versión admite `effectiveFrom` ISO-8601 con offset. `GET /api/v1/schemas/{type}/effective-version?at=...`
  consulta historia; asignar captura la versión vigente al iniciar, independientemente de la visita prevista.
- **F3:** `jurisdictionCode` selecciona una política declarada en código de dominio: DEMO-STRICT (12 meses, todo hallazgo
  bloquea), DEMO-CONDITIONAL (6 meses sin hallazgos, 30 días con MINOR/MAJOR; CRITICAL bloquea) y DEMO-STANDARD
  (12 meses, MAJOR/CRITICAL bloquean). Son ejemplos, no normativa. La composición registra explícitamente cada jurisdicción; no hay fallback.
- **F1:** activo declara `subsystems: [{code, name}]`; esquema declara `requiredSubsystems` y `subsystemCode` en sección
  o criterio. Emitir parcial: `POST /api/v1/certificates` con `inspectionId` y `subsystemCode`. No enviar `validUntil`.
  Consultar decisión: `/api/v1/inspections/{id}/certification-eligibility?subsystem=...`.
- **Global:** `POST /api/v1/assets/{code}/global-certificates` con `schemaVersion` y `partialCertificateNumbers`.
  Renovación: `/api/v1/certificates/{number}/global-renewals` con el mismo cuerpo. Su vigencia termina con el
  parcial más corto; estado DEPENDENCY_INVALID si pierde un respaldo. Los documentos muestran alcance,
  bases, política, modalidad y condiciones.

Para repetir la demostración completa y carreras sobre H2: `mvn -q clean verify`.
Para repetirla sobre una base PostgreSQL 17 vacía en localhost:55433, DB certiflow, usuario postgres y clave
de prueba certiflow-test: `mvn -q -Dcertiflow.postgres=true clean verify`.
CI levanta esa base como servicio efímero. `DeliveryFeaturesApiTest` documenta la demostración paso a paso.


Las políticas exponen únicamente `code()` y `evaluate(context)`. Sus reglas son privadas;
la decisión conserva resultado, motivos, condiciones, vigencia y código de política.
El resumen general de hallazgos muestra severidad y resolución. Los impedimentos de certificación
se consultan en la evaluación de una inspección y alcance, no como propiedad absoluta de un hallazgo.
