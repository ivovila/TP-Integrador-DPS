# TP-Integrador-DPS — CertiFlow

Módulo de dominio de una plataforma de inspección, habilitación y certificación de activos (Entrega 1).

## Requisitos

- JDK 21 o superior
- Maven 3.9+

## Comandos

| Comando | Qué hace |
|---|---|
| `mvn compile` | Compila |
| `mvn test` | Corre los tests unitarios (`*Test`) |
| `mvn verify` | Tests unitarios + de integración (`*IT`) + reporte de cobertura |

El reporte de cobertura queda en `target/site/jacoco/index.html`.

## Estructura

```
src/main/java/ar/edu/itba/certiflow   código del dominio
src/test/java/ar/edu/itba/certiflow   tests (unitarios: *Test, integración: *IT)
DESIGN.md                             decisiones de diseño
```
