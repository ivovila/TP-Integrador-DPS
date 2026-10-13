# F1-CERTIFICACIONES-PARCIALES · Plan de implementación

**Spec:** [Contrato](../specs/2026-10-12-f1-certificaciones-parciales.md)

**Arquitectura:** cuatro módulos DDD existentes; dominio/aplicación sin frameworks.

**Cambio de alcance autorizado:** sistema nuevo, sin rutas de compatibilidad ni categoría LEGACY.

## Tareas

- [x] Asset/Subsystem y SchemaVersion/Section/Criterion: asociación estable y cobertura; pruebas unitarias.
- [x] Finding y ACL: clasificación por versión de origen, resultado por subsistema.
- [x] CertificateScope/CertificateBasis y repositorios: parciales independientes y unicidad por alcance.
- [x] GlobalCertificationPolicy y casos de emisión/renovación global: respaldos inmutables y estado derivado.
- [x] Migraciones, DTOs/HAL e informes; pruebas repositorio/API/E2E y documentación; clean verify.

## Validación

Tests unitarios parametrizados, repositorio real y API completa. Ejecutar `mvn -q clean verify`; migraciones PostgreSQL cuando esté disponible. Sin commits automáticos.


**Verificación:** `mvn -q -Dcertiflow.postgres=true clean verify` pasó; 260 tests, incluidos 7 de API sobre PostgreSQL.
