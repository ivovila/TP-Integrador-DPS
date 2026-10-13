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
mvn -pl certiflow-web spring-boot:run
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

certiflow-frontend          interfaz web (fuera del build de Maven): puertos propios y adaptador en memoria
└── src/domain, ports, adapters/inmemory, ui/<contexto>
```

## API

Base `/api/v1`; `GET /api/v1` devuelve los links a todas las colecciones. Toda operación que escribe exige
los headers `X-Person-Id` y `X-Person-Name`. Los recursos se devuelven en HAL (`application/hal+json`) y los
errores en `application/problem+json`.

```bash
curl -i -X POST localhost:8080/api/v1/assets \
  -H 'Content-Type: application/json' -H 'X-Person-Id: EMP-001' -H 'X-Person-Name: Paula' \
  -d '{"code":"EXT-0042","name":"Lobby extinguisher","type":"Fire extinguisher","location":"Lobby",
       "responsible":{"id":"EMP-003","name":"Raul"},"characteristics":[]}'
```

Las rutas, los códigos de estado y las reglas REST que sigue la API están en
[docs/specs/2026-10-12-fase-1-ddd-persistencia-api.md](docs/specs/2026-10-12-fase-1-ddd-persistencia-api.md) (§6)
y en [DESIGN.md](DESIGN.md) (D21).

## Frontend

`certiflow-frontend/` tiene la interfaz web (React + TypeScript + Vite). Por ahora **no llama a la API**: define
sus propios puertos y corre contra una implementación en memoria que aplica las reglas de negocio, así que se
puede usar sin levantar el backend. Requiere Node 24 o superior.

```bash
cd certiflow-frontend && npm install && npm run dev
```

Abre en `http://localhost:5173` con datos de ejemplo. Estructura, uso y pasos para conectar la API en
[certiflow-frontend/README.md](certiflow-frontend/README.md); decisión de diseño en [DESIGN.md](DESIGN.md) (D22).

## Integración continua

`.github/workflows/ci.yml` corre `mvn -B clean verify` en cada push y en cada pull request a `main`. Un segundo job,
`frontend`, corre `npm ci`, el chequeo de tipos, los tests y el build de `certiflow-frontend/`. Para que
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
