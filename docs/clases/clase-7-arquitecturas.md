# Clase 7 · DDD, Clean y Hexagonal

ITBA 2026 · Fer Cargnelutti

> Los detalles dependen de las reglas de negocio.

Ejemplo: `CesarCipher`, `Reader` y `Writer` son negocio; `ConsoleReader`, `ConsoleWriter` y `Main` son detalles.

## Object Oriented Software Engineering

Ivar Jacobson, 1993, *A Use Case Driven Approach*: actor driven, goal oriented, system boundary focused,
value delivered, foco en el "qué" y no en el "cómo".

Historia: DDD (Eric Evans, 2003), Hexagonal (Alistair Cockburn, 2005), Clean Architecture (Robert Martin, 2017).

## Clean Architecture

- **Principios:** Dependency Rule; independencia de frameworks, UI, base de datos y fuentes externas; testeabilidad.
- **Conceptos:** Entities; Interactors (casos de uso); Interface adapters (Controllers, Presenters, Gateways).

Diagrama de la clase:

| Paquete | Elementos |
|---|---|
| `domain` | `UseCase`, `RequestModel`, `InputBoundary`, `OutputBoundary`, `ResponseModel`, `Entity`, `BusinessRule`, `DataGateway` |
| `application` | `UseCaseImpl` (implementa `UseCase`, usa `Entity`, `BusinessRule`, `DataGateway`) |
| `web` | `Controller`, `Presenter` |
| `infrastructure` | `DataRepositoryImpl` (implementa `DataGateway`) |

## Domain Driven Design

- **Principios:** Bounded Context; Ubiquitous Language; Context Mapping (p. ej. ACL); foco en el Core Domain; Model-Driven Design.
- **Conceptos:** Entities; Value Objects; Aggregates (de value objects); Domain Services; Repositories (Gateways).

Diagrama de la clase:

| Paquete | Elementos |
|---|---|
| `domain` | interfaz `UseCase { execute(command: Command): Response }`, interfaz `Repository`, `DomainService`, interfaz `EventPublisher` |
| `domain.entity` | `AggregateRoot`, `Entity`, `ValueObject`, `DomainEvent`, `Command` «input», `Response` «output» |
| `application` | `UseCaseImpl { execute(command: Command): DTO }` implementa `UseCase`; usa `DomainService`, `EventPublisher`, `AggregateRoot` y `Repository` |
| `web` | `Controller { handleRequest(): HttpResponse }`; llama a `UseCase` y arma el `Command` |
| `infrastructure` | `RepositoryImpl { save(), findById() }` implementa `Repository`; `EventPublisherImpl { publish(event) }` implementa `EventPublisher` |

Relaciones internas: `AggregateRoot` → `Entity`, `DomainEvent`; `Entity` → `ValueObject`; `DomainService` → `Entity`, `ValueObject`; `Repository` → `AggregateRoot`.

## Hexagonal Architecture

- **Principios:** aislamiento de la lógica central; inversión de control; independencia tecnológica; testeabilidad.
- **Conceptos:** el hexágono (dominio); Ports (driving y driven); Adapters (driving y driven).

Capas del primer diagrama: Interface Layer (REST client, event producer, SOAP client) → Interface Adapters →
Business Layer (Services, Business Model, Repositories, Data Sources) → Infrastructure Layer (message broker,
email, REST server, SOAP server, ORM adapter). Las dependencias apuntan hacia adentro.

Diagrama de la clase:

| Paquete | Elementos |
|---|---|
| `domain.ports` | `InboundPort { execute(request): Response }`, `OutboundPort { loadById(id): Data, save(data: Data) }` |
| `domain.model` | `Data`, `Request`, `Response` |
| `application` | `UseCaseImpl { execute(request): Response }` implementa `InboundPort` |
| `adapters.driving` | `RestController { handleRequest(): HttpResponse }` usa `InboundPort` |
| `adapters.driven` | `RepositoryAdapter { loadById(), save() }` implementa `OutboundPort` |

## MVC

Trygve Reenskaug, 1979: *"The essential purpose of MVC is to bridge the gap between the human user's mental
model and the digital model that exists in the computer. The ideal MVC solution supports the user illusion of
seeing and manipulating the domain information directly."*

- El Model es observado por la View; el Controller manipula el Model.
- **MVC no es una arquitectura**: es un patrón de la capa de presentación. MVC web y Model View Presenter
  (Controller → Input Boundary → Interactor → Output Boundary → Presenter → View Model → View) son variantes.
