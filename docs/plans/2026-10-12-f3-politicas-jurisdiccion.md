# F3-POLITICAS-JURISDICCION · Plan de implementación

**Spec:** [Contrato](../specs/2026-10-12-f3-politicas-jurisdiccion.md)

**Arquitectura:** cuatro módulos DDD existentes; dominio/aplicación sin frameworks.

**Cambio de alcance autorizado:** sistema nuevo, sin rutas de compatibilidad ni categoría LEGACY.

## Tareas

- [x] Asset/commands/persistencia/API: jurisdicción explícita obligatoria, sin LEGACY ni compatibilidad.
- [x] CertificationPolicy/Resolver/DecisionService: guardas comunes, configuración y decisiones históricas.
- [x] Certificate/snapshot/mapper: modalidad, condiciones y política aplicada; emisión y renovación centralizadas.
- [x] Configurar políticas demo; locks por activo y errores/API. Probar matriz de políticas, plazos y restauración.
- [x] Actualizar fixtures a sistema nuevo, documentación y ejecutar clean verify.

## Validación

Tests unitarios parametrizados, repositorio real y API completa. Ejecutar `mvn -q clean verify`; migraciones PostgreSQL cuando esté disponible. Sin commits automáticos.


**Verificación:** `mvn -q -Dcertiflow.postgres=true clean verify` pasó; 260 tests, incluidos 7 de API sobre PostgreSQL.
