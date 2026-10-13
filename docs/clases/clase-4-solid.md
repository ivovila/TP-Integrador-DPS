# Clase 4 · SOLID

ITBA 2026 · Fer Cargnelutti

> "Solid principles are about dependencies between classes and its relations."

Cinco principios: Single Responsibility, Open/Closed, Liskov Substitution, Interface Segregation y Dependency
Inversion.

## Single Responsibility

- La responsabilidad que una clase tiene frente a sus usuarios.
- Una sola razón para cambiar: más de una razón significa más de un actor pidiendo cambios.
- Se trata de actores (usuario + rol) que van a requerir cambios en esa clase.
- El actor es la única fuente de cambio de esa responsabilidad.

## Open/Closed

- Cambiar el comportamiento de una clase (abierta) sin cambiar su código fuente (cerrada).
- El truco es abstraer e invertir dependencias.
- Solo protege de los cambios que se sabe que pueden ocurrir. Dos caminos:
  - **BDUF** (Big Design Up-Front): proactivo.
  - **Diseño ágil**: iteraciones, diseño evolutivo, TDD. Reactivo.
- No es posible ni práctico que todo el sistema cumpla OCP.

## Liskov Substitution

- Sustitución: los métodos de las clases derivadas no deben sorprender a los clientes cambiando el
  comportamiento del padre.
- El subtipado es distinto en lenguajes estáticos y dinámicos: herencia vs. *duck typing*.
- Toda violación de LSP es una posible violación de OCP.
- Se trata de tipos. Para un subtipo, ES-UN no alcanza; PUEDE-USARSE-COMO es una mejor relación.
- Las representaciones en software no son lo que representan. Ejemplo con números: `Integer` ES-UN número real.

Historia: Christopher Strachey (1967), Dahl y Nygaard, Alan Kay (1970), Luca Cardelli y Peter Wegner (1985),
Barbara Liskov (1988).

## Interface Segregation

- Descomponer abstracciones grandes e incoherentes en otras más chicas y cohesivas.
- Las interfaces se relacionan más con sus usuarios que con quienes las implementan.
- La herencia es la relación física más fuerte pero la lógica más débil.
- La composición, en cambio, crea una relación lógica más fuerte.

## Dependency Inversion

- Dependencias en tiempo de ejecución (flujo de control) vs. dependencias de código fuente (tiempo de compilación).
- Permite crear límites.
- Los módulos de bajo nivel deben depender de los de alto nivel.
