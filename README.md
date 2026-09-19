# Certiflow · Trabajo Integrador de Diseño de Sistemas (ITBA) · Entrega 1

Módulo de dominio de una plataforma de inspección, habilitación y certificación de activos.
No incluye API, persistencia real ni interfaz: solo el modelo, los casos de uso y sus tests.

## Cómo correrlo

Requiere JDK 25 y Maven 3.9 o superior.

```bash
mvn clean test
```

Usar siempre `clean`: si un IDE compila en paralelo hacia `target/` con un JDK anterior al 21,
pisa las clases de Maven y los tests fallan con "Unresolved compilation problem".

## Estructura

```
src/main/java/ar/edu/itba/certiflow
├── models            entidades, value objects, reglas y el puerto AuditService
│   ├── shared        Person, DomainException
│   ├── audit         AuditService, AuditTrail, AuditEntry, AuditAction
│   ├── asset         Asset, AssetCode, AssetType, Location
│   ├── evaluation    respuestas, reglas de aprobación, severidad, evidencia
│   ├── schema        InspectionSchema, SchemaVersion, Section, Criterion
│   ├── inspection    Inspection, revisiones, evaluación, decorador y generador
│   ├── finding       Finding con sus CorrectiveAction, decorador y generador
│   └── certificate   Certificate, vigencia, ciclo de vida, decorador y generador
├── ports             repositorios y CertificateNumbering: lo que el negocio le pide al exterior
├── usecases          un caso de uso por clase, agrupados por concepto
│   └── asset, schema, inspection, finding, certificate, report
├── application       lo que acompaña a los casos de uso sin ser uno
│   ├── certification CertificationEligibility y NoBlockingFindingsEligibility
│   ├── report        modelos de lectura: InspectionAct, FindingsSummary, CertificateDocument
│   └── exceptions    reglas que cruzan agregados
└── details
    └── inmemory      implementaciones en memoria de los puertos

src/test/java/ar/edu/itba/certiflow
├── models            tests unitarios de reglas, evidencia y criterios
├── usecases          tests de integración por caso de uso, sin mocks
└── support           CertiflowFixture (raíz de composición) y MutableClock
```

## Documentación

- [DESIGN.md](DESIGN.md): supuestos, arquitectura, decisiones con sus alternativas descartadas,
  patrones no aplicados, tabla riesgo → test y limitaciones.
- [docs/certiflow-domain.puml](docs/certiflow-domain.puml): diagrama de clases en PlantUML.
