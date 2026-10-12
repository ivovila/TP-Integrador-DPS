# Clase 8 · Spring Boot

ITBA 2026 · Fer Cargnelutti · 29/09/2026

## Calendario de octubre

| Día | Tema |
|---|---|
| Martes 6 | Agentic programming |
| Martes 13 | Career Dev · Soft Skills (Entrega 2 TPI) |
| Martes 20 | REST API |
| Martes 27 | Spring Data · SQL y NoSQL |

## Principios

- Naturaleza "opinionated" y auto-configuración.
- Starters (`spring-boot-starter-{kafka, aop, cache, validation, actuator, jpa, test}`).
- Actuator: monitoreo, métricas y health check listos para usar.
- Configuración simplificada en YAML.
- Basado en anotaciones.
- Sin efecto en la ejecución de los tests unitarios.

## Actividades con el TP#1

1. Elegir una arquitectura: DDD, Hexagonal o Clean.
2. Agregar Spring Boot a la solución.
3. Formalizar propiedades (`apiKey`, `baseUrl`) con `@ConfigurationProperties`.
4. Crear una implementación alternativa del rate provider configurable desde propiedades.
5. Se puede usar https://frankfurter.dev/, que no requiere API key.

## Pasos generales

1. Agregar el BOM de Spring Boot y los starters básicos (boot, web), y el plugin de Maven.
2. Anotar la clase de arranque con `@SpringBootApplication`.
3. Configurar la conexión a la API externa.
4. Agregar `@Service` y `@Component` donde haga falta.
5. Si hay tests de integración, pasarlos a `@SpringBootTest`.

## Configuración Maven

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-dependencies</artifactId>
      <version>4.1.1</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>

<dependencies>
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter</artifactId>
  </dependency>
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
  </dependency>
</dependencies>
```
