# Certiflow · Trabajo Integrador de Diseño de Sistemas (ITBA) · Entrega 1

Dominio de una plataforma de inspección, habilitación y certificación de activos.
No incluye API, persistencia real ni interfaz: solo el modelo, los casos de uso y sus tests.

## Cómo correrlo

Requiere JDK 25 y Maven 3.9 o superior.

```bash
mvn clean test
```

Usar siempre `clean`: si un IDE compila en paralelo hacia `target/` con un JDK anterior al 21,
pisa las clases de Maven y los tests fallan con "Unresolved compilation problem".

## Estructura

Build Maven de tres módulos. La dirección de dependencias (`application → domain ← details`) la impone el
compilador.

```
certiflow-domain            toda la lógica de negocio y todas las interfaces; no depende de nada
└── src/main/java/ar/edu/itba/certiflow
    ├── models              entidades, value objects, reglas, servicios de dominio y excepciones
    │   ├── shared          Person, PersonId, DomainException
    │   ├── audit           AuditLog, AuditEntry, AuditAction, AuditTrail
    │   ├── asset           Asset, AssetCode, AssetType, Location, Characteristics
    │   ├── evaluation      respuestas, reglas de aprobación, severidad, evidencia
    │   ├── schema          InspectionSchema, SchemaVersionLog, Section, Criterion, InspectionSchemaService
    │   ├── inspection      Inspection, revisiones, evaluación, alcance y su fábrica
    │   ├── finding         Finding con sus CorrectiveAction y su fábrica
    │   ├── certificate     Certificate, vigencia, ciclo de vida y su fábrica
    │   └── certification   servicio de elegibilidad y CertificationDecision
    ├── ports               puertos de salida: repositorios y CertificateNumbering
    └── usecases            puertos de entrada: una interfaz por caso de uso y los modelos de lectura
        └── asset, schema, inspection, finding, certificate, report

certiflow-application       implementación de los casos de uso; depende de certiflow-domain
└── src/main/java/ar/edu/itba/certiflow/application
    └── asset, schema, inspection, finding, certificate, report   (XxxInteractor implements Xxx)

certiflow-details           implementaciones de los puertos; depende de certiflow-domain
└── src/main/java/ar/edu/itba/certiflow/details/inmemory

Tests
├── certiflow-domain/src/test        tests unitarios de reglas, evidencia, criterios, esquemas y auditoría
└── certiflow-application/src/test   tests de integración por caso de uso, sin mocks;
                                     support/CertiflowFixture es la raíz de composición
```

## Documentación

- [DESIGN.md](DESIGN.md): supuestos, arquitectura, decisiones con sus alternativas descartadas,
  patrones no aplicados, tabla riesgo → test y limitaciones.
- [docs/certiflow-domain.puml](docs/certiflow-domain.puml): diagrama de clases en PlantUML.
