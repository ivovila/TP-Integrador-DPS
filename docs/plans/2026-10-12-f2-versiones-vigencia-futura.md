# F2-VERSIONES-VIGENCIA-FUTURA · Plan de implementación

**Spec:** [Contrato](../specs/2026-10-12-f2-versiones-vigencia-futura.md)

**Arquitectura:** cuatro módulos DDD existentes; dominio/aplicación sin frameworks.

**Cambio de alcance autorizado:** sistema nuevo, sin rutas de compatibilidad ni categoría LEGACY.

## Tareas

- [x] SchemaVersion/SchemaVersionLog: calendario inmutable, precisión común y validaciones; pruebas de límites.
- [x] PublishSchemaVersionCommand/UseCaseImpl y AssignInspectionUseCaseImpl: selección por un único instante.
- [x] SchemaVersionEntity, migración V2 y locks de publicación: persistencia y concurrencia reales.
- [x] SchemaController/request/response: consulta temporal, caché y errores HTTP; pruebas API.
- [x] Documentar cambios y ejecutar clean verify.

## Validación

Tests unitarios parametrizados, repositorio real y API completa. Ejecutar `mvn -q clean verify`; migraciones PostgreSQL cuando esté disponible. Sin commits automáticos.


**Verificación:** `mvn -q -Dcertiflow.postgres=true clean verify` pasó; 260 tests, incluidos 7 de API sobre PostgreSQL.
