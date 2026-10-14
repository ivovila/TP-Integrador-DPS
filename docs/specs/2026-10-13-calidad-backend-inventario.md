# Calidad del backend · Inventario mecánico

Generado sobre el commit `5c1a64a` de `entrega-2-spec1` con una búsqueda de texto sobre el código principal (`src/main/java`) de los cuatro módulos. Es el anexo de la [spec](2026-10-13-calidad-backend.md): sirve para tildar cada ocurrencia y para no volver a buscar. No incluye tests.

Las rutas son relativas a `certiflow-<módulo>/src/main/java/ar/edu/itba/certiflow/`. Los números de línea valen para ese commit: la rama se mueve, así que conviene regenerar el inventario antes de empezar cada tarea y al cerrarla:

```bash
python3 docs/specs/2026-10-13-calidad-backend-inventario.py . docs/specs/2026-10-13-calidad-backend-inventario.md
```

## Resumen

| Módulo | Archivos | Líneas | `sealed` | `instanceof` | `switch` | tipo por reflexión | casts | registros | estáticos | `null` | `Illegal*` | `orElseThrow()` | `if` sin `throw` | `Object` |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| domain | 297 | 6067 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 1 | 0 |
| application | 42 | 1590 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 |
| infrastructure | 58 | 2484 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 12 | 0 | 0 | 0 | 0 |
| web | 93 | 4217 | 0 | 5 | 0 | 5 | 6 | 3 | 39 | 60 | 7 | 2 | 21 | 3 |
| **total** | 490 | 14358 | 0 | 5 | 0 | 5 | 6 | 3 | 39 | 72 | 7 | 2 | 22 | 3 |

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

5 ocurrencias. Tareas del plan: 3 (`RawAnswer`), 5 (`SectionsJson`, `SchemaPayloads`, `AuditActions`), 6 (`ApiExceptionHandler`), 8 (bloqueo). Las de `TransactionalUseCases` están aceptadas.

| Módulo | Ubicación | Línea |
|---|---|---|
| web | `web/config/TransactionalUseCases.java:21` | `if (method.getDeclaringClass() == Object.class) {` |
| web | `web/config/TransactionalUseCases.java:35` | `return useCase.cast(proxy);` |
| web | `web/schema/SchemaPayloads.java:95` | `return Optional.ofNullable(RULE_WRITERS.get(rule.getClass()))` |
| web | `web/schema/SchemaPayloads.java:96` | `.orElseThrow(() -> new IllegalStateException("No payload for rule " + rule.getClass().getName()))` |
| web | `web/support/ProblemDetails.java:21` | `return of(status, exception.getClass().getSimpleName(), exception.getMessage());` |

## 5. Casts a un tipo concreto (heurística)

6 ocurrencias. Tareas del plan: 2, 5, 10 (`DemoDataSeeder`).

| Módulo | Ubicación | Línea |
|---|---|---|
| web | `web/config/DemoDataSeeder.java:162` | `answers.put((String) codesAndAnswers[index], (YesNoAnswer) codesAndAnswers[index + 1]);` |
| web | `web/schema/SchemaPayloads.java:45` | `NumericRangeRule numeric = (NumericRangeRule) rule;` |
| web | `web/schema/SchemaPayloads.java:49` | `YesNoRule.class, rule -> new RulePayload("yes-no", null, null, null, ((YesNoRule) rule).expected().name(), ...` |
| web | `web/schema/SchemaPayloads.java:51` | `((OptionInListRule) rule).acceptedOptions()));` |
| web | `web/schema/SchemaPayloads.java:69` | `return new CriterionPayload(criterion.code().value(), criterion.text(), ((StandardSeverity) criterion.sever...` |
| web | `web/support/AuditEntryResponse.java:12` | `return new AuditEntryResponse(((Enum<?>) entry.action()).name(), entry.action().label(), entry.performedBy(...` |

## 6. Registros por clase o por texto (heurística)

3 ocurrencias. Tareas del plan: 3, 5, 6, 8, 10 (`CLOSED_BY_STATUS`).

| Módulo | Ubicación | Línea |
|---|---|---|
| web | `web/inspection/InspectionController.java:45` | `private static final Map<String, Boolean> CLOSED_BY_STATUS = Map.of("open", false, "closed", true);` |
| web | `web/schema/SchemaPayloads.java:33` | `private static final Map<String, Function<RulePayload, ApprovalRule<?>>> RULE_READERS = new LinkedHashMap<>();` |
| web | `web/schema/SchemaPayloads.java:43` | `private static final Map<Class<?>, Function<ApprovalRule<?>, RulePayload>> RULE_WRITERS = Map.of(` |

## 7. Métodos y bloques estáticos

39 ocurrencias. Tareas del plan: 1, 3, 5, 7, 8, 9, 10, 11.

| Módulo | Ubicación | Línea |
|---|---|---|
| web | `CertiflowApplication.java:9` | `public static void main(String[] args) {` |
| web | `web/certification/CertificationPayloads.java:21` | `public static ScopePayload of(CertificationScope scope) {` |
| web | `web/certification/CertificationPayloads.java:28` | `public static BasisPayload of(CertificateBasis basis) {` |
| web | `web/certification/CertificationPayloads.java:42` | `public static DecisionPayload of(CertificationDecision decision) {` |
| web | `web/certification/CertificationPayloads.java:48` | `public static SchemaReferencePayload of(SchemaReference reference) {` |
| web | `web/config/DemoDataSeeder.java:159` | `private static Map<String, YesNoAnswer> answers(Object... codesAndAnswers) {` |
| web | `web/config/DemoDataSeeder.java:188` | `private static Criterion<YesNoAnswer> yesNo(String code, String text, StandardSeverity severity) {` |
| web | `web/config/TransactionalUseCases.java:13` | `public static <U> U wrap(Class<U> useCase, U target, TransactionTemplate transactions) {` |
| web | `web/config/TransactionalUseCases.java:17` | `public static <U> U wrap(Class<U> useCase, U target, TransactionTemplate transactions,` |
| web | `web/config/TransactionalUseCases.java:38` | `private static RuntimeException unchecked(Throwable cause) {` |
| web | `web/inspection/CriterionResponseController.java:70` | `private static CriterionResponse<?> criterionResponse(InspectionView inspection, String code) {` |
| web | `web/inspection/InspectionController.java:125` | `private static boolean closedFor(String status) {` |
| web | `web/inspection/RawAnswers.java:18` | `static RawAnswer from(JsonNode node) {` |
| web | `web/schema/SchemaPayloads.java:35` | `static {` |
| web | `web/schema/SchemaPayloads.java:56` | `public static List<Section> toSections(List<SectionPayload> sections) {` |
| web | `web/schema/SchemaPayloads.java:62` | `public static List<SectionPayload> fromSections(List<Section> sections) {` |
| web | `web/schema/SchemaPayloads.java:68` | `public static CriterionPayload payload(Criterion<?> criterion) {` |
| web | `web/schema/SchemaPayloads.java:75` | `private static Criterion<?> criterion(CriterionPayload payload) {` |
| web | `web/schema/SchemaPayloads.java:82` | `private static <A extends Answer> Criterion<A> criterion(CriterionPayload payload, ApprovalRule<A> rule, Se...` |
| web | `web/schema/SchemaPayloads.java:88` | `private static ApprovalRule<?> rule(RulePayload rule) {` |
| web | `web/schema/SchemaPayloads.java:94` | `private static RulePayload rulePayload(ApprovalRule<?> rule) {` |
| web | `web/schema/SchemaPayloads.java:100` | `private static <T> T required(T value, String field) {` |
| web | `web/schema/SchemaPayloads.java:105` | `private static YesNoAnswer expected(String name) {` |
| web | `web/schema/SchemaPayloads.java:110` | `private static Severity severity(String name) {` |
| web | `web/schema/SchemaPayloads.java:115` | `public static EvidenceKind evidenceKind(String name) {` |
| web | `web/security/AllowedRoleInterceptor.java:33` | `private static boolean isRead(HttpServletRequest request) {` |
| web | `web/security/AllowedRoleInterceptor.java:37` | `private static boolean hasAnyRole(Authentication authentication, StaffRole... roles) {` |
| web | `web/security/SecurityConfiguration.java:89` | `private static AbstractAuthenticationToken authenticationOf(Jwt jwt) {` |
| web | `web/support/AuditEntryResponse.java:11` | `public static AuditEntryResponse of(AuditEntry entry) {` |
| web | `web/support/PageLinks.java:14` | `public static PageRequest request(int page, int size) {` |
| web | `web/support/PageLinks.java:22` | `public static <T> PagedModel<T> page(List<T> all, int page, int size, String baseUri, String query) {` |
| web | `web/support/PageLinks.java:28` | `public static <T> PagedModel<T> of(Page<T> page, String baseUri, String query) {` |
| web | `web/support/PageLinks.java:45` | `private static String uri(String baseUri, String query, int page, int size) {` |
| web | `web/support/PersonPayload.java:13` | `public static PersonPayload of(Person person) {` |
| web | `web/support/RequestFields.java:11` | `public static void require(Object value, String field) {` |
| web | `web/support/RequestFields.java:17` | `public static <T> List<T> requireElements(List<T> values, String field) {` |
| web | `web/support/Uris.java:15` | `public static String path(String... segments) {` |
| web | `web/support/Uris.java:22` | `public static Optional<String> query(String name, String value) {` |
| web | `web/support/Uris.java:26` | `public static String etag(String... parts) {` |

## 8. Literales `null`

72 ocurrencias. Tareas del plan: 1, 2, 4, 9, 10, 11, 12.

| Módulo | Archivo | Líneas |
|---|---|---|
| infrastructure | `infrastructure/certification/persistence/CertificateEntity.java` | 99 |
| infrastructure | `infrastructure/certification/persistence/DecisionDocumentMapper.java` | 34 |
| infrastructure | `infrastructure/inspection/persistence/InspectionEntity.java` | 88 |
| infrastructure | `infrastructure/inspection/persistence/StoredAnsweringDescriber.java` | 12 |
| infrastructure | `infrastructure/inspection/persistence/StoredCancellationDescriber.java` | 11 |
| infrastructure | `infrastructure/shared/persistence/SubsystemColumn.java` | 17 |
| web | `web/certification/BasisPayloadDescriber.java` | 15, 21 |
| web | `web/certification/CertificateController.java` | 189 |
| web | `web/certification/CertificateResource.java` | 39 |
| web | `web/certification/EligibilityController.java` | 37 |
| web | `web/certification/ScopePayloadDescriber.java` | 11 |
| web | `web/certification/ValidityFields.java` | 14 |
| web | `web/inspection/AssignmentRequest.java` | 22, 29 |
| web | `web/inspection/CriterionResponseController.java` | 139, 140, 141 |
| web | `web/inspection/CriterionResponseResource.java` | 31, 32, 33 |
| web | `web/inspection/EvidenceRequest.java` | 12 |
| web | `web/inspection/FindingController.java` | 152, 164 |
| web | `web/inspection/InspectionController.java` | 113, 116, 118, 120 |
| web | `web/inspection/InspectionResponse.java` | 28 |
| web | `web/inspection/RawAnswers.java` | 20 |
| web | `web/inspection/RevisionController.java` | 114 |
| web | `web/inspection/VerificationRequest.java` | 12 |
| web | `web/schema/SchemaController.java` | 140, 143, 149 |
| web | `web/schema/SchemaPayloads.java` | 47, 49, 50 |
| web | `web/schema/SchemaRequest.java` | 10 |
| web | `web/schema/VersionRequest.java` | 16 |
| web | `web/security/AllowedRoleInterceptor.java` | 27, 38 |
| web | `web/security/JwtProperties.java` | 13, 16 |
| web | `web/security/NotRevokedValidator.java` | 11, 21 |
| web | `web/support/RequestFields.java` | 12, 18, 21 |
| web | `web/support/SubsystemCodeField.java` | 17 |

## 8.1 `Optional.ofNullable`

21 ocurrencias. Tareas del plan: 4, 9, 10.

| Módulo | Ubicación | Línea |
|---|---|---|
| infrastructure | `infrastructure/certification/persistence/StoredScope.java:13` | `return Optional.ofNullable(subsystemCode)` |
| infrastructure | `infrastructure/certification/policy/ConfiguredPolicyResolver.java:30` | `return Optional.ofNullable(policies.get(jurisdictionCode));` |
| infrastructure | `infrastructure/inspection/persistence/StoredAnswering.java:28` | `return Optional.ofNullable(answerType)` |
| infrastructure | `infrastructure/inspection/persistence/StoredCancellation.java:15` | `return Optional.ofNullable(cancelledAt)` |
| infrastructure | `infrastructure/inspection/persistence/StoredInspectionScope.java:15` | `return Optional.ofNullable(scopeSections)` |
| infrastructure | `infrastructure/schema/persistence/StoredRuleParameters.java:40` | `return Optional.ofNullable(storedRule.get(parameterName))` |
| infrastructure | `infrastructure/shared/persistence/SubsystemColumn.java:21` | `return Optional.ofNullable(subsystemCode)` |
| web | `web/catalog/AssetController.java:67` | `return PageLinks.page(Optional.ofNullable(type).map(value -> listAssets.execute(new AssetType(value))).orEl...` |
| web | `web/certification/CertificateController.java:110` | `List<CertificateView> certificates = Optional.ofNullable(asset).map(code -> listCertificates.execute(new As...` |
| web | `web/certification/CertificateController.java:113` | `.filter(view -> Optional.ofNullable(subsystem)` |
| web | `web/certification/CertificateRequest.java:25` | `return Optional.ofNullable(subsystemCode).map(code -> new SubsystemScope(new SubsystemCode(code)));` |
| web | `web/inspection/FindingController.java:73` | `return PageLinks.page(Optional.ofNullable(asset).map(code -> listFindings.execute(new AssetCode(code))).orE...` |
| web | `web/inspection/InspectionController.java:74` | `Optional<Boolean> closed = Optional.ofNullable(status).map(InspectionController::closedFor);` |
| web | `web/inspection/InspectionController.java:75` | `List<InspectionResponse> inspections = Optional.ofNullable(asset).map(code -> listInspections.execute(new A...` |
| web | `web/inspection/InspectionController.java:126` | `return Optional.ofNullable(CLOSED_BY_STATUS.get(status))` |
| web | `web/schema/SchemaPayloads.java:89` | `return Optional.ofNullable(RULE_READERS.get(rule.type()))` |
| web | `web/schema/SchemaPayloads.java:95` | `return Optional.ofNullable(RULE_WRITERS.get(rule.getClass()))` |
| web | `web/schema/SchemaPayloads.java:101` | `return Optional.ofNullable(value)` |
| web | `web/schema/VersionRequest.java:25` | `return Optional.ofNullable(effectiveFrom)` |
| web | `web/support/SubsystemCodeField.java:21` | `return Optional.ofNullable(subsystemCode)` |
| web | `web/support/Uris.java:23` | `return Optional.ofNullable(value).map(present -> name + "=" + UriUtils.encodeQueryParam(present, StandardCh...` |

## 8.2 `Objects.requireNonNull`

0 ocurrencias. Tareas del plan: 6.

Ninguna.

## 8.3 Parámetros de query opcionales

7 ocurrencias. Tareas del plan: 10.

| Módulo | Ubicación | Línea |
|---|---|---|
| web | `web/catalog/AssetController.java:65` | `public PagedModel<AssetResponse> list(@RequestParam(required = false) String type, @RequestParam(defaultVal...` |
| web | `web/certification/CertificateController.java:107` | `public PagedModel<CertificateResource> list(@RequestParam(required = false) String asset,` |
| web | `web/certification/CertificateController.java:108` | `@RequestParam(required = false) String subsystem, @RequestParam(defaultValue = "0") int page,` |
| web | `web/certification/EligibilityController.java:36` | `public EligibilityResource eligibility(@PathVariable String id, @RequestParam(required = false) String subs...` |
| web | `web/inspection/FindingController.java:71` | `public PagedModel<FindingResource> list(@RequestParam(required = false) String asset, @RequestParam(default...` |
| web | `web/inspection/InspectionController.java:71` | `public PagedModel<InspectionResponse> list(@RequestParam(required = false) String asset,` |
| web | `web/inspection/InspectionController.java:72` | `@RequestParam(required = false) String status,` |

## 9. `IllegalArgumentException` e `IllegalStateException`

7 ocurrencias. Tareas del plan: 1, 6, 11.

| Módulo | Ubicación | Línea |
|---|---|---|
| web | `web/config/TransactionalUseCases.java:31` | `throw new IllegalStateException(failure);` |
| web | `web/config/TransactionalUseCases.java:45` | `return new IllegalStateException(cause);` |
| web | `web/schema/SchemaPayloads.java:96` | `.orElseThrow(() -> new IllegalStateException("No payload for rule " + rule.getClass().getName()))` |
| web | `web/security/JwtProperties.java:14` | `throw new IllegalStateException("certiflow.security.jwt.secret must have at least " + MINIMUM_SECRET_BYTES ...` |
| web | `web/security/JwtProperties.java:17` | `throw new IllegalStateException("certiflow.security.jwt.ttl must be a positive duration");` |
| web | `web/support/ActingPersonResolver.java:36` | `.orElseThrow(() -> new IllegalStateException("No user account for " + basic.getName()));` |
| web | `web/support/ActingPersonResolver.java:38` | `throw new IllegalStateException("The request reached a controller without an authenticated person");` |

## 10. `orElseThrow()` sin excepción propia

2 ocurrencias. Tareas del plan: 1, 6.

| Módulo | Ubicación | Línea |
|---|---|---|
| web | `web/catalog/AssetController.java:84` | `Link.of("/api/v1/certificates?" + Uris.query("asset", code).orElseThrow()).withRel("certificates"),` |
| web | `web/catalog/AssetController.java:85` | `Link.of("/api/v1/findings?" + Uris.query("asset", code).orElseThrow()).withRel("findings"));` |

## 11. `if` cuyo cuerpo no lanza (heurística)

22 ocurrencias. Tareas del plan: 2, 9, 10, 11. El de `Finding.verifyAction` está aceptado (D8) y los de `TransactionalUseCases` también.

| Módulo | Ubicación | Línea |
|---|---|---|
| domain | `inspection/model/Finding.java:70` | `if (verification.isAccepted()) {` |
| web | `web/catalog/AssetController.java:86` | `if (!asset.subsystems().isEmpty()) {` |
| web | `web/certification/CertificateController.java:188` | `if (status == CertificateStatus.ACTIVE) {` |
| web | `web/certification/CertificateController.java:191` | `if (status != CertificateStatus.RENEWED) {` |
| web | `web/certification/EligibilityController.java:43` | `if (eligibility.eligible) {` |
| web | `web/config/DemoDataSeeder.java:112` | `if (!listAssets.execute().isEmpty()) {` |
| web | `web/config/DemoDataSeeder.java:171` | `if (withAccess) {` |
| web | `web/config/TransactionalUseCases.java:21` | `if (method.getDeclaringClass() == Object.class) {` |
| web | `web/config/TransactionalUseCases.java:39` | `if (cause instanceof RuntimeException runtime) {` |
| web | `web/inspection/CriterionResponseController.java:138` | `if (!inspection.isClosed()) {` |
| web | `web/inspection/FindingController.java:151` | `if (finding.isOpen()) {` |
| web | `web/inspection/FindingController.java:163` | `if (action.isOpen()) {` |
| web | `web/inspection/InspectionController.java:115` | `if (inspection.isClosed()) {` |
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

3 ocurrencias. Tareas del plan: 3, 8, 10. El de `AllowedRoleInterceptor` es la frontera F4.

| Módulo | Ubicación | Línea |
|---|---|---|
| web | `web/config/DemoDataSeeder.java:159` | `private static Map<String, YesNoAnswer> answers(Object... codesAndAnswers) {` |
| web | `web/security/AllowedRoleInterceptor.java:15` | `public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {` |
| web | `web/support/RequestFields.java:11` | `public static void require(Object value, String field) {` |

## 13. Imports con comodín

2 ocurrencias. Tareas del plan: 13.

| Módulo | Ubicación | Línea |
|---|---|---|
| web | `web/certification/GlobalCertificateController.java:5` | `import ar.edu.itba.certiflow.certification.usecase.*;` |
| web | `web/certification/GlobalCertificateController.java:11` | `import org.springframework.web.bind.annotation.*;` |

## 14. `var`

2 ocurrencias. Tareas del plan: 13.

| Módulo | Ubicación | Línea |
|---|---|---|
| web | `web/certification/GlobalCertificateController.java:26` | `var number = issue.execute(new IssueGlobalCertificateCommand(new AssetCode(code), request.schemaVersion(),` |
| web | `web/certification/GlobalCertificateController.java:28` | `var resource = certificates.find(number.value());` |

## 15. Líneas de más de 150 caracteres

20 líneas en 11 archivos. Tareas del plan: 13.

| Módulo | Archivo | Cantidad |
|---|---|---|
| web | `web/schema/SchemaController.java` | 4 |
| web | `web/config/InspectionConfiguration.java` | 3 |
| web | `web/schema/SchemaPayloads.java` | 3 |
| web | `web/config/CertificationConfiguration.java` | 2 |
| web | `web/config/SchemaConfiguration.java` | 2 |
| web | `web/catalog/AssetController.java` | 1 |
| web | `web/catalog/AssetRequest.java` | 1 |
| web | `web/certification/CertificateController.java` | 1 |
| web | `web/config/DemoDataSeeder.java` | 1 |
| web | `web/inspection/FindingController.java` | 1 |
| web | `web/inspection/InspectionController.java` | 1 |
