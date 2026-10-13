# Calidad del backend · Inventario mecánico

Generado sobre el commit `9224a30` de `entrega-2-spec1` con una búsqueda de texto sobre el código principal (`src/main/java`) de los cuatro módulos. Es el anexo de la [spec](2026-10-13-calidad-backend.md): sirve para tildar cada ocurrencia y para no volver a buscar. No incluye tests.

Las rutas son relativas a `certiflow-<módulo>/src/main/java/ar/edu/itba/certiflow/`. Los números de línea valen para ese commit: la rama se mueve, así que conviene regenerar el inventario antes de empezar cada tarea y al cerrarla:

```bash
python3 docs/specs/2026-10-13-calidad-backend-inventario.py . docs/specs/2026-10-13-calidad-backend-inventario.md
```

## Resumen

| Módulo | Archivos | Líneas | `sealed` | `instanceof` | `switch` | tipo por reflexión | casts | registros | estáticos | `null` | `Illegal*` | `orElseThrow()` | `if` sin `throw` | `Object` |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| domain | 282 | 5801 | 0 | 0 | 0 | 0 | 0 | 0 | 24 | 0 | 0 | 0 | 1 | 0 |
| application | 40 | 1519 | 0 | 0 | 0 | 0 | 0 | 0 | 2 | 0 | 0 | 1 | 0 | 0 |
| infrastructure | 64 | 2487 | 0 | 0 | 0 | 2 | 0 | 1 | 2 | 24 | 1 | 0 | 0 | 2 |
| web | 90 | 4074 | 0 | 5 | 0 | 5 | 6 | 3 | 39 | 60 | 7 | 2 | 21 | 3 |
| **total** | 476 | 13881 | 0 | 5 | 0 | 7 | 6 | 4 | 67 | 84 | 8 | 3 | 22 | 5 |

`instanceof` no cuenta los que están dentro de `equals` ni los del proxy de transacciones, aceptados por el equipo; figuran aparte en la sección 2. Las columnas «casts», «registros», «`if` sin `throw`» y «`Object`» salen de una heurística: pueden traer algún falso positivo y hay que leer la línea.

## 1. Interfaces `sealed`

0 ocurrencias. Tareas del plan: 2.

Ninguna.

## 2. `instanceof`

5 ocurrencias. Tareas del plan: 2 (alcance, base y alcance de inspección), 6 (`ApiExceptionHandler`), 11 (seguridad).

| Módulo | Ubicación | Línea |
|---|---|---|
| web | `web/security/AllowedRoleInterceptor.java:16` | `if (!(handler instanceof HandlerMethod method)) {` |
| web | `web/security/IssueTokenAfterBasicFilter.java:31` | `if (authentication instanceof UsernamePasswordAuthenticationToken basic && basic.isAuthenticated()) {` |
| web | `web/security/TokenController.java:30` | `if (!(authentication instanceof JwtAuthenticationToken bearer)) {` |
| web | `web/support/ActingPersonResolver.java:30` | `if (request.getUserPrincipal() instanceof JwtAuthenticationToken bearer) {` |
| web | `web/support/ActingPersonResolver.java:33` | `if (request.getUserPrincipal() instanceof Authentication basic) {` |

## 2.1 `instanceof` dentro de `equals` (aceptado)

2 ocurrencias. Tareas del plan: ninguna.

| Módulo | Ubicación | Línea |
|---|---|---|
| domain | `shared/model/Person.java:26` | `return other instanceof Person person && id.equals(person.id);` |
| infrastructure | `infrastructure/inspection/persistence/CorrectiveActionEntity.java:75` | `return other instanceof Key key && number == key.number && Objects.equals(findingId, key.findingId);` |

## 2.2 `instanceof` del proxy de transacciones (aceptado)

2 ocurrencias. Tareas del plan: ninguna.

| Módulo | Ubicación | Línea |
|---|---|---|
| web | `web/config/TransactionalUseCases.java:39` | `if (cause instanceof RuntimeException runtime) {` |
| web | `web/config/TransactionalUseCases.java:42` | `if (cause instanceof Error error) {` |

## 3. `switch`

0 ocurrencias. Tareas del plan: 2.

Ninguna.

## 4. Tipo consultado por reflexión (`isInstance`, `cast`, `getClass`)

7 ocurrencias. Tareas del plan: 3 (`RawAnswer`), 5 (`SectionsJson`, `SchemaPayloads`, `AuditActions`), 6 (`ApiExceptionHandler`), 8 (bloqueo). Las de `TransactionalUseCases` están aceptadas.

| Módulo | Ubicación | Línea |
|---|---|---|
| infrastructure | `infrastructure/certification/persistence/CertificationTransactionLocks.java:37` | `.filter(LocksAsset.class::isInstance)` |
| infrastructure | `infrastructure/certification/persistence/CertificationTransactionLocks.java:38` | `.map(LocksAsset.class::cast)` |
| web | `web/config/TransactionalUseCases.java:21` | `if (method.getDeclaringClass() == Object.class) {` |
| web | `web/config/TransactionalUseCases.java:35` | `return useCase.cast(proxy);` |
| web | `web/schema/SchemaPayloads.java:93` | `return Optional.ofNullable(RULE_WRITERS.get(rule.getClass()))` |
| web | `web/schema/SchemaPayloads.java:94` | `.orElseThrow(() -> new IllegalStateException("No payload for rule " + rule.getClass().getName()))` |
| web | `web/support/ProblemDetails.java:21` | `return of(status, exception.getClass().getSimpleName(), exception.getMessage());` |

## 5. Casts a un tipo concreto (heurística)

6 ocurrencias. Tareas del plan: 2, 5, 10 (`DemoDataSeeder`).

| Módulo | Ubicación | Línea |
|---|---|---|
| web | `web/config/DemoDataSeeder.java:160` | `answers.put((String) codesAndAnswers[index], (YesNoAnswer) codesAndAnswers[index + 1]);` |
| web | `web/schema/SchemaPayloads.java:43` | `NumericRangeRule numeric = (NumericRangeRule) rule;` |
| web | `web/schema/SchemaPayloads.java:47` | `YesNoRule.class, rule -> new RulePayload("yes-no", null, null, null, ((YesNoRule) rule).expected().name(), ...` |
| web | `web/schema/SchemaPayloads.java:49` | `((OptionInListRule) rule).acceptedOptions()));` |
| web | `web/schema/SchemaPayloads.java:67` | `return new CriterionPayload(criterion.code(), criterion.text(), ((StandardSeverity) criterion.severity()).n...` |
| web | `web/support/AuditEntryResponse.java:12` | `return new AuditEntryResponse(((Enum<?>) entry.action()).name(), entry.action().label(), entry.performedBy(...` |

## 6. Registros por clase o por texto (heurística)

4 ocurrencias. Tareas del plan: 3, 5, 6, 8, 10 (`CLOSED_BY_STATUS`).

| Módulo | Ubicación | Línea |
|---|---|---|
| infrastructure | `infrastructure/certification/persistence/CertificationTransactionLocks.java:23` | `private final Map<String, Function<String, Optional<String>>> assetCodeByOwner;` |
| web | `web/inspection/InspectionController.java:46` | `private static final Map<String, Boolean> CLOSED_BY_STATUS = Map.of("open", false, "closed", true);` |
| web | `web/schema/SchemaPayloads.java:31` | `private static final Map<String, Function<RulePayload, ApprovalRule<?>>> RULE_READERS = new LinkedHashMap<>();` |
| web | `web/schema/SchemaPayloads.java:41` | `private static final Map<Class<?>, Function<ApprovalRule<?>, RulePayload>> RULE_WRITERS = Map.of(` |

## 7. Métodos y bloques estáticos

67 ocurrencias. Tareas del plan: 1, 3, 5, 7, 8, 9, 10, 11.

| Módulo | Archivo | Líneas |
|---|---|---|
| domain | `catalog/model/Characteristics.java` | 22 |
| domain | `certification/model/AllRequiredSubsystemsPolicy.java` | 43, 62, 69, 77 |
| domain | `certification/model/Requirement.java` | 14, 20 |
| domain | `certification/model/SeverityCertificationPolicy.java` | 30, 36, 42 |
| domain | `inspection/model/Revisions.java` | 20 |
| domain | `schema/model/SchemaVersion.java` | 29, 35, 41, 49, 64, 71 |
| domain | `schema/model/SchemaVersionLog.java` | 30, 50 |
| domain | `shared/model/StoredInstant.java` | 14 |
| domain | `shared/usecase/AssetLockKey.java` | 11, 15, 19, 23 |
| application | `application/certification/GlobalCertificationService.java` | 76 |
| application | `application/certification/InspectionResultTranslatorImpl.java` | 61 |
| infrastructure | `infrastructure/certification/persistence/CertificationTransactionLocks.java` | 50 |
| infrastructure | `infrastructure/shared/persistence/Instants.java` | 10 |
| web | `CertiflowApplication.java` | 9 |
| web | `web/certification/CertificationPayloads.java` | 21, 28, 42, 57 |
| web | `web/config/DemoDataSeeder.java` | 157, 186 |
| web | `web/config/TransactionalUseCases.java` | 13, 17, 38 |
| web | `web/inspection/CriterionResponseController.java` | 69 |
| web | `web/inspection/InspectionController.java` | 126 |
| web | `web/inspection/RawAnswers.java` | 18 |
| web | `web/schema/SchemaPayloads.java` | 33, 54, 60, 66, 73, 80, 86, 92, 98, 103, 108, 113 |
| web | `web/security/AllowedRoleInterceptor.java` | 33, 37 |
| web | `web/security/SecurityConfiguration.java` | 89 |
| web | `web/support/AuditEntryResponse.java` | 11 |
| web | `web/support/PageLinks.java` | 14, 22, 28, 45 |
| web | `web/support/PersonPayload.java` | 13 |
| web | `web/support/RequestFields.java` | 11, 17 |
| web | `web/support/Uris.java` | 15, 22, 26 |

## 8. Literales `null`

84 ocurrencias. Tareas del plan: 1, 2, 4, 9, 10, 11, 12.

| Módulo | Archivo | Líneas |
|---|---|---|
| infrastructure | `infrastructure/certification/persistence/CertificateEntity.java` | 98 |
| infrastructure | `infrastructure/certification/persistence/DecisionDocumentMapper.java` | 36 |
| infrastructure | `infrastructure/inspection/persistence/InspectionEntity.java` | 88 |
| infrastructure | `infrastructure/inspection/persistence/StoredAnsweringDescriber.java` | 12 |
| infrastructure | `infrastructure/inspection/persistence/StoredCancellationDescriber.java` | 11 |
| infrastructure | `infrastructure/schema/persistence/RuleDocumentDescriber.java` | 15, 20, 25 |
| infrastructure | `infrastructure/shared/persistence/Instants.java` | 11 |
| infrastructure | `infrastructure/shared/persistence/SubsystemColumn.java` | 17 |
| web | `web/certification/BasisPayloadDescriber.java` | 14, 20 |
| web | `web/certification/CertificateController.java` | 189 |
| web | `web/certification/CertificateResource.java` | 39 |
| web | `web/certification/CertificationPayloads.java` | 49, 50 |
| web | `web/certification/EligibilityController.java` | 37 |
| web | `web/certification/ScopePayloadDescriber.java` | 11 |
| web | `web/inspection/AssignmentRequest.java` | 20, 27 |
| web | `web/inspection/CriterionResponseController.java` | 138, 139, 140 |
| web | `web/inspection/CriterionResponseResource.java` | 31, 32, 33 |
| web | `web/inspection/EvidenceRequest.java` | 12 |
| web | `web/inspection/FindingController.java` | 153, 165 |
| web | `web/inspection/InspectionController.java` | 114, 117, 119, 121 |
| web | `web/inspection/InspectionResponse.java` | 28 |
| web | `web/inspection/RawAnswers.java` | 20 |
| web | `web/inspection/RevisionController.java` | 112 |
| web | `web/inspection/VerificationRequest.java` | 12 |
| web | `web/schema/SchemaController.java` | 141, 144, 150 |
| web | `web/schema/SchemaPayloads.java` | 45, 47, 48 |
| web | `web/schema/SchemaRequest.java` | 10 |
| web | `web/schema/VersionRequest.java` | 15 |
| web | `web/security/AllowedRoleInterceptor.java` | 27, 38 |
| web | `web/security/JwtProperties.java` | 13, 16 |
| web | `web/security/NotRevokedValidator.java` | 11, 21 |
| web | `web/support/RequestFields.java` | 12, 18, 21 |
| web | `web/support/SubsystemCodeField.java` | 17 |

## 8.1 `Optional.ofNullable`

22 ocurrencias. Tareas del plan: 4, 9, 10.

| Módulo | Ubicación | Línea |
|---|---|---|
| infrastructure | `infrastructure/certification/persistence/CertificationTransactionLocks.java:36` | `Optional.ofNullable(arguments).stream().flatMap(Arrays::stream)` |
| infrastructure | `infrastructure/certification/persistence/CertificationTransactionLocks.java:45` | `return Optional.ofNullable(assetCodeByOwner.get(key.owner()))` |
| infrastructure | `infrastructure/certification/persistence/StoredScope.java:13` | `return Optional.ofNullable(subsystemCode)` |
| infrastructure | `infrastructure/certification/policy/ConfiguredPolicyResolver.java:28` | `return Optional.ofNullable(policies.get(jurisdictionCode));` |
| infrastructure | `infrastructure/inspection/persistence/StoredAnswering.java:28` | `return Optional.ofNullable(answerType)` |
| infrastructure | `infrastructure/inspection/persistence/StoredCancellation.java:15` | `return Optional.ofNullable(cancelledAt)` |
| infrastructure | `infrastructure/inspection/persistence/StoredInspectionScope.java:14` | `return Optional.ofNullable(scopeSections)` |
| infrastructure | `infrastructure/shared/persistence/SubsystemColumn.java:21` | `return Optional.ofNullable(subsystemCode)` |
| web | `web/catalog/AssetController.java:68` | `return PageLinks.page(Optional.ofNullable(type).map(value -> listAssets.execute(new AssetType(value))).orEl...` |
| web | `web/certification/CertificateController.java:110` | `List<CertificateView> certificates = Optional.ofNullable(asset).map(code -> listCertificates.execute(new As...` |
| web | `web/certification/CertificateController.java:113` | `.filter(view -> Optional.ofNullable(subsystem)` |
| web | `web/certification/CertificateRequest.java:25` | `return Optional.ofNullable(subsystemCode).map(code -> new SubsystemScope(new SubsystemCode(code)));` |
| web | `web/inspection/FindingController.java:74` | `return PageLinks.page(Optional.ofNullable(asset).map(code -> listFindings.execute(new AssetCode(code))).orE...` |
| web | `web/inspection/InspectionController.java:75` | `Optional<Boolean> closed = Optional.ofNullable(status).map(InspectionController::closedFor);` |
| web | `web/inspection/InspectionController.java:76` | `List<InspectionResponse> inspections = Optional.ofNullable(asset).map(code -> listInspections.execute(new A...` |
| web | `web/inspection/InspectionController.java:127` | `return Optional.ofNullable(CLOSED_BY_STATUS.get(status))` |
| web | `web/schema/SchemaPayloads.java:87` | `return Optional.ofNullable(RULE_READERS.get(rule.type()))` |
| web | `web/schema/SchemaPayloads.java:93` | `return Optional.ofNullable(RULE_WRITERS.get(rule.getClass()))` |
| web | `web/schema/SchemaPayloads.java:99` | `return Optional.ofNullable(value)` |
| web | `web/schema/VersionRequest.java:21` | `return Optional.ofNullable(effectiveFrom)` |
| web | `web/support/SubsystemCodeField.java:21` | `return Optional.ofNullable(subsystemCode)` |
| web | `web/support/Uris.java:23` | `return Optional.ofNullable(value).map(present -> name + "=" + UriUtils.encodeQueryParam(present, StandardCh...` |

## 8.2 `Objects.requireNonNull`

0 ocurrencias. Tareas del plan: 6.

Ninguna.

## 8.3 Parámetros de query opcionales

7 ocurrencias. Tareas del plan: 10.

| Módulo | Ubicación | Línea |
|---|---|---|
| web | `web/catalog/AssetController.java:66` | `public PagedModel<AssetResponse> list(@RequestParam(required = false) String type, @RequestParam(defaultVal...` |
| web | `web/certification/CertificateController.java:107` | `public PagedModel<CertificateResource> list(@RequestParam(required = false) String asset,` |
| web | `web/certification/CertificateController.java:108` | `@RequestParam(required = false) String subsystem, @RequestParam(defaultValue = "0") int page,` |
| web | `web/certification/EligibilityController.java:36` | `public EligibilityResource eligibility(@PathVariable String id, @RequestParam(required = false) String subs...` |
| web | `web/inspection/FindingController.java:72` | `public PagedModel<FindingResource> list(@RequestParam(required = false) String asset, @RequestParam(default...` |
| web | `web/inspection/InspectionController.java:72` | `public PagedModel<InspectionResponse> list(@RequestParam(required = false) String asset,` |
| web | `web/inspection/InspectionController.java:73` | `@RequestParam(required = false) String status,` |

## 9. `IllegalArgumentException` e `IllegalStateException`

8 ocurrencias. Tareas del plan: 1, 6, 11.

| Módulo | Ubicación | Línea |
|---|---|---|
| infrastructure | `infrastructure/certification/persistence/CertificationTransactionLocks.java:46` | `.orElseThrow(() -> new IllegalStateException("Unknown lock owner " + key.owner()))` |
| web | `web/config/TransactionalUseCases.java:31` | `throw new IllegalStateException(failure);` |
| web | `web/config/TransactionalUseCases.java:45` | `return new IllegalStateException(cause);` |
| web | `web/schema/SchemaPayloads.java:94` | `.orElseThrow(() -> new IllegalStateException("No payload for rule " + rule.getClass().getName()))` |
| web | `web/security/JwtProperties.java:14` | `throw new IllegalStateException("certiflow.security.jwt.secret must have at least " + MINIMUM_SECRET_BYTES ...` |
| web | `web/security/JwtProperties.java:17` | `throw new IllegalStateException("certiflow.security.jwt.ttl must be a positive duration");` |
| web | `web/support/ActingPersonResolver.java:36` | `.orElseThrow(() -> new IllegalStateException("No user account for " + basic.getName()));` |
| web | `web/support/ActingPersonResolver.java:38` | `throw new IllegalStateException("The request reached a controller without an authenticated person");` |

## 10. `orElseThrow()` sin excepción propia

3 ocurrencias. Tareas del plan: 1, 6.

| Módulo | Ubicación | Línea |
|---|---|---|
| application | `application/certification/GlobalCertificationService.java:77` | `return partials.stream().map(Certificate::latestInspectionClosedAt).max(Instant::compareTo).orElseThrow();` |
| web | `web/catalog/AssetController.java:85` | `Link.of("/api/v1/certificates?" + Uris.query("asset", code).orElseThrow()).withRel("certificates"),` |
| web | `web/catalog/AssetController.java:86` | `Link.of("/api/v1/findings?" + Uris.query("asset", code).orElseThrow()).withRel("findings"));` |

## 11. `if` cuyo cuerpo no lanza (heurística)

22 ocurrencias. Tareas del plan: 2, 9, 10, 11. El de `Finding.verifyAction` está aceptado (D8) y los de `TransactionalUseCases` también.

| Módulo | Ubicación | Línea |
|---|---|---|
| domain | `inspection/model/Finding.java:70` | `if (verification.isAccepted()) {` |
| web | `web/catalog/AssetController.java:87` | `if (!asset.subsystems().isEmpty()) {` |
| web | `web/certification/CertificateController.java:188` | `if (status == CertificateStatus.ACTIVE) {` |
| web | `web/certification/CertificateController.java:191` | `if (status != CertificateStatus.RENEWED) {` |
| web | `web/certification/EligibilityController.java:43` | `if (eligibility.eligible) {` |
| web | `web/config/DemoDataSeeder.java:110` | `if (!listAssets.execute().isEmpty()) {` |
| web | `web/config/DemoDataSeeder.java:169` | `if (withAccess) {` |
| web | `web/config/TransactionalUseCases.java:21` | `if (method.getDeclaringClass() == Object.class) {` |
| web | `web/config/TransactionalUseCases.java:39` | `if (cause instanceof RuntimeException runtime) {` |
| web | `web/inspection/CriterionResponseController.java:137` | `if (!inspection.isClosed()) {` |
| web | `web/inspection/FindingController.java:152` | `if (finding.isOpen()) {` |
| web | `web/inspection/FindingController.java:164` | `if (action.isOpen()) {` |
| web | `web/inspection/InspectionController.java:116` | `if (inspection.isClosed()) {` |
| web | `web/security/AllowedRoleInterceptor.java:16` | `if (!(handler instanceof HandlerMethod method)) {` |
| web | `web/security/AllowedRoleInterceptor.java:20` | `if (method.hasMethodAnnotation(AllowedRole.class)) {` |
| web | `web/security/IssueTokenAfterBasicFilter.java:31` | `if (authentication instanceof UsernamePasswordAuthenticationToken basic && basic.isAuthenticated()) {` |
| web | `web/security/SecurityProblems.java:28` | `if (!BROWSER_REQUEST_VALUE.equals(request.getHeader(BROWSER_REQUEST_HEADER))) {` |
| web | `web/support/ActingPersonResolver.java:30` | `if (request.getUserPrincipal() instanceof JwtAuthenticationToken bearer) {` |
| web | `web/support/ActingPersonResolver.java:33` | `if (request.getUserPrincipal() instanceof Authentication basic) {` |
| web | `web/support/PageLinks.java:36` | `if (number > 0) {` |
| web | `web/support/PageLinks.java:39` | `if (number < totalPages - 1) {` |
| web | `web/support/RequestFields.java:18` | `if (values == null) {` |

## 12. `Object` en firmas o campos (heurística)

5 ocurrencias. Tareas del plan: 3, 8, 10. El de `AllowedRoleInterceptor` es la frontera F4.

| Módulo | Ubicación | Línea |
|---|---|---|
| infrastructure | `infrastructure/certification/persistence/CertificationTransactionLocks.java:20` | `public class CertificationTransactionLocks implements Consumer<Object[]> {` |
| infrastructure | `infrastructure/certification/persistence/CertificationTransactionLocks.java:35` | `public void accept(Object[] arguments) {` |
| web | `web/config/DemoDataSeeder.java:157` | `private static Map<String, YesNoAnswer> answers(Object... codesAndAnswers) {` |
| web | `web/security/AllowedRoleInterceptor.java:15` | `public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {` |
| web | `web/support/RequestFields.java:11` | `public static void require(Object value, String field) {` |

## 13. Imports con comodín

2 ocurrencias. Tareas del plan: 13.

| Módulo | Ubicación | Línea |
|---|---|---|
| web | `web/certification/GlobalCertificateController.java:5` | `import ar.edu.itba.certiflow.certification.usecase.*;` |
| web | `web/certification/GlobalCertificateController.java:11` | `import org.springframework.web.bind.annotation.*;` |

## 14. `var`

7 ocurrencias. Tareas del plan: 13.

| Módulo | Ubicación | Línea |
|---|---|---|
| application | `application/certification/CertificationAssessmentService.java:40` | `var asset = assets.findByCode(result.assetCode())` |
| application | `application/certification/CertificationAssessmentService.java:42` | `var policy = policies.resolve(asset.jurisdictionCode());` |
| application | `application/certification/CertificationAssessmentService.java:43` | `var now = StoredInstant.normalize(clock.instant());` |
| application | `application/certification/CertificationAssessmentService.java:44` | `var context = new CertificationAssessmentContext(result, asset.jurisdictionCode(),` |
| application | `application/certification/CertificationAssessmentService.java:46` | `var reasons = Requirement.unmetBy(COMMON_REQUIREMENTS, result);` |
| web | `web/certification/GlobalCertificateController.java:26` | `var number = issue.execute(new IssueGlobalCertificateCommand(new AssetCode(code), request.schemaVersion(),` |
| web | `web/certification/GlobalCertificateController.java:28` | `var resource = certificates.find(number.value());` |

## 15. Líneas de más de 150 caracteres

38 líneas en 20 archivos. Tareas del plan: 13.

| Módulo | Archivo | Cantidad |
|---|---|---|
| web | `web/config/InspectionConfiguration.java` | 10 |
| web | `web/schema/SchemaController.java` | 4 |
| web | `web/config/CertificationConfiguration.java` | 3 |
| web | `web/schema/SchemaPayloads.java` | 3 |
| web | `web/certification/CertificateController.java` | 2 |
| web | `web/config/SchemaConfiguration.java` | 2 |
| application | `application/inspection/GenerateInspectionActUseCaseImpl.java` | 1 |
| application | `application/inspection/PlanCorrectiveActionUseCaseImpl.java` | 1 |
| domain | `catalog/usecase/RegisterAssetCommand.java` | 1 |
| domain | `certification/usecase/CertificateDocument.java` | 1 |
| domain | `inspection/usecase/RectifyInspectionCommand.java` | 1 |
| domain | `inspection/usecase/VerifyCorrectiveActionCommand.java` | 1 |
| domain | `schema/usecase/CreateInspectionSchemaCommand.java` | 1 |
| web | `web/catalog/AssetController.java` | 1 |
| web | `web/catalog/AssetRequest.java` | 1 |
| web | `web/config/CatalogConfiguration.java` | 1 |
| web | `web/config/DemoDataSeeder.java` | 1 |
| web | `web/config/SharedConfiguration.java` | 1 |
| web | `web/inspection/FindingController.java` | 1 |
| web | `web/inspection/InspectionController.java` | 1 |
