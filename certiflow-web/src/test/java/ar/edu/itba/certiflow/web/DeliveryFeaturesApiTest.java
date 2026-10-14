package ar.edu.itba.certiflow.web;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import ar.edu.itba.certiflow.web.support.ApiTest;
import ar.edu.itba.certiflow.web.support.Flows;
import com.jayway.jsonpath.JsonPath;
import java.time.*;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

@Import(DeliveryFeaturesApiTest.TimeConfiguration.class)
public class DeliveryFeaturesApiTest extends ApiTest {
    @TestConfiguration
    public static class TimeConfiguration {
        @Bean @Primary ControlledClock featureClock() { return new ControlledClock(); }
    }

    public static class ControlledClock extends Clock {
        private final AtomicReference<Instant> time = new AtomicReference<>(Instant.parse("2026-10-12T12:00:00Z"));
        public void set(String value) { time.set(Instant.parse(value)); }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return Clock.fixed(instant(), zone); }
        @Override public Instant instant() { return time.get(); }
    }

    @Autowired protected ControlledClock clock;
    @Autowired protected JdbcTemplate jdbc;
    @Autowired protected TransactionTemplate transactions;

    @BeforeEach void resetTime() { clock.set("2026-10-12T12:00:00Z"); }

    private String asset(String code, String jurisdiction) {
        return """
                {"code":"%s","name":"Boiler","type":"Boiler","location":"Plant",
                 "responsible":{"id":"OWNER","name":"Owner"},"characteristics":[],
                 "jurisdictionCode":"%s","subsystems":[{"code":"E","name":"Electrical"},{"code":"P","name":"Pressure"}]}
                """.formatted(code, jurisdiction);
    }

    private String sections(String pressureSubsystem) {
        return """
                [{"title":"Electrical","subsystemCode":"E","criteria":[
                 {"code":"C-E","text":"Grounding","severity":"MAJOR","rule":{"type":"yes-no","expected":"YES"},"requiredEvidence":[]}]},
                 {"title":"Pressure","subsystemCode":"%s","criteria":[
                 {"code":"C-P","text":"Pressure safety","severity":"CRITICAL","rule":{"type":"yes-no","expected":"YES"},"requiredEvidence":[]}]}]
                """.formatted(pressureSubsystem);
    }

    private void setup(String jurisdiction) throws Exception {
        postAs("PLANNER", "/api/v1/assets", asset("A-1", jurisdiction)).andExpect(status().isCreated());
        postAs("PLANNER", "/api/v1/schemas", "{\"name\":\"Boiler safety\",\"assetType\":\"Boiler\",\"requiredSubsystems\":[\"E\",\"P\"],\"sections\":" + sections("P") + "}")
                .andExpect(status().isCreated());
    }

    private String assign(String asset, String scope) throws Exception {
        return JsonPath.read(postAs("PLANNER", "/api/v1/inspections", """
                {"assetCode":"%s","inspector":{"id":"INSPECTOR","name":"Inspector"},"plannedDate":"2027-01-01","sections":%s}
                """.formatted(asset, scope)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.id");
    }

    private void answer(String id, String criterion, boolean value) throws Exception {
        putAs("INSPECTOR", "/api/v1/inspections/" + id + "/responses/" + criterion + "/answer", "{\"value\":" + value + "}")
                .andExpect(status().isNoContent());
    }

    private void close(String id) throws Exception {
        postAs("INSPECTOR", "/api/v1/inspections/" + id + "/closure", "{}").andExpect(status().isCreated());
    }

    private String partial(String id, String subsystem) throws Exception {
        return JsonPath.read(postAs("ISSUER", "/api/v1/certificates", "{\"inspectionId\":\"" + id + "\",\"subsystemCode\":\"" + subsystem + "\"}")
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.number");
    }

    private String globalRequest(String electrical, String pressure) {
        return "{\"schemaVersion\":1,\"partialCertificateNumbers\":[\"" + electrical + "\",\"" + pressure + "\"]}";
    }

    @Test
    void partialsHaveIndependentLifecyclesAndGlobalTracksItsImmutableBacking() throws Exception {
        setup("DEMO-STRICT");
        String inspection = assign("A-1", "null");
        answer(inspection, "C-E", true); answer(inspection, "C-P", false); close(inspection);
        String electrical = partial(inspection, "E");
        postAs("ISSUER", "/api/v1/certificates", "{\"inspectionId\":\"" + inspection + "\",\"subsystemCode\":\"P\"}")
                .andExpect(status().isConflict());
        assertEquals(1, jdbc.queryForObject("select count(*) from certificate", Integer.class));
        postAs("ISSUER", "/api/v1/assets/A-1/global-certificates", "{\"schemaVersion\":1,\"partialCertificateNumbers\":[\"" + electrical + "\"]}")
                .andExpect(status().isConflict());
        String finding = JsonPath.read(read("/api/v1/findings?asset=A-1").andReturn().getResponse().getContentAsString(), "$._embedded.findings[0].id");
        String action = postAs("PLANNER", "/api/v1/findings/" + finding + "/corrective-actions", """
                {"description":"Repair pressure safety","responsible":{"id":"OWNER","name":"Owner"},"dueDate":"2026-10-20"}
                """).andExpect(status().isCreated()).andReturn().getResponse().getHeader("Location");
        postAs("VERIFIER", Flows.path(action) + "/verifications", "{\"result\":\"ACCEPTED\",\"notes\":\"Verified repair\"}")
                .andExpect(status().isCreated());
        clock.set("2026-10-13T12:00:00Z");
        String pressure = partial(inspection, "P");
        String global = JsonPath.read(postAs("ISSUER", "/api/v1/assets/A-1/global-certificates", globalRequest(electrical, pressure))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.validUntil").value("2027-10-11"))
                .andReturn().getResponse().getContentAsString(), "$.number");
        read("/api/v1/certificates/" + pressure).andExpect(jsonPath("$.validUntil").value("2027-10-12"));
        postAs("ISSUER", "/api/v1/certificates/" + electrical + "/suspensions", "{\"reason\":\"Grounding damaged\"}")
                .andExpect(status().isCreated());
        read("/api/v1/certificates/" + global).andExpect(jsonPath("$.status").value("DEPENDENCY_INVALID"))
                .andExpect(jsonPath("$.scope.type").value("WHOLE_ASSET"))
                .andExpect(jsonPath("$.basis.type").value("PARTIAL_CERTIFICATES"))
                .andExpect(jsonPath("$.basis.partialCertificateNumbers[0]").value(electrical))
                .andExpect(jsonPath("$.schemaReference.number").value(1));
        read("/api/v1/certificates/" + electrical).andExpect(jsonPath("$.basis.type").value("INSPECTION"))
                .andExpect(jsonPath("$.basis.inspectionId").value(inspection));
        read("/api/v1/certificates/" + global + "/document").andExpect(jsonPath("$.status").value("DEPENDENCY_INVALID"));
        String later = assign("A-1", "null"); answer(later, "C-E", true); answer(later, "C-P", true); close(later);
        String replacement = JsonPath.read(postAs("ISSUER", "/api/v1/certificates/" + electrical + "/renewals",
                "{\"inspectionId\":\"" + later + "\"}").andExpect(status().isCreated())
                .andExpect(jsonPath("$.scope.type").value("SUBSYSTEM"))
                .andExpect(jsonPath("$.scope.subsystemCode").value("E"))
                .andReturn().getResponse().getContentAsString(), "$.number");
        postAs("ISSUER", "/api/v1/certificates/" + pressure + "/renewals",
                "{\"inspectionId\":\"" + later + "\",\"subsystemCode\":\"E\"}").andExpect(status().isConflict());
        read("/api/v1/certificates/" + global).andExpect(jsonPath("$.status").value("DEPENDENCY_INVALID"));
        postAs("ISSUER", "/api/v1/certificates/" + global + "/global-renewals", globalRequest(replacement, pressure))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("ACTIVE"));
        read("/api/v1/certificates/" + global).andExpect(jsonPath("$.status").value("RENEWED"));
    }

    @Test
    void futureVersionStartsAtExactBoundaryAndExistingInspectionKeepsItsVersion() throws Exception {
        setup("DEMO-STRICT");
        clock.set("2026-10-13T12:00:00Z");
        postAs("PLANNER", "/api/v1/schemas/Boiler/versions", "{\"effectiveFrom\":\"2026-11-01T00:00:00-03:00\",\"requiredSubsystems\":[\"E\",\"P\"],\"sections\":" + sections("P") + "}")
                .andExpect(status().isCreated()).andExpect(jsonPath("$.number").value(2));
        read("/api/v1/schemas/Boiler").andExpect(jsonPath("$.currentVersion").value(1));
        read("/api/v1/schemas/Boiler/effective-version?at=2026-11-01T02:59:59.999999Z")
                .andExpect(jsonPath("$.number").value(1));
        clock.set("2026-11-01T02:59:59.999999Z"); String old = assign("A-1", "null");
        clock.set("2026-11-01T03:00:00Z"); String next = assign("A-1", "null");
        read("/api/v1/inspections/" + old).andExpect(jsonPath("$.schemaVersion").value(1));
        read("/api/v1/inspections/" + next).andExpect(jsonPath("$.schemaVersion").value(2));
        answer(old, "C-E", true); answer(old, "C-P", true); close(old);
        read("/api/v1/inspections/" + old).andExpect(jsonPath("$.schemaVersion").value(1));
        read("/api/v1/schemas/Boiler/effective-version?at=2026-01-01T00:00:00Z").andExpect(status().isNotFound());
        postAs("PLANNER", "/api/v1/schemas/Boiler/versions", "{\"effectiveFrom\":\"2026-11-01T03:00:00Z\",\"requiredSubsystems\":[\"E\",\"P\"],\"sections\":" + sections("P") + "}")
                .andExpect(status().isConflict());
        assertEquals(2, jdbc.queryForObject("select count(*) from schema_version", Integer.class));
    }

    @Test
    void sameResultProducesConditionalOrRejectedDecisionsAndKeepsPolicyInDocument() throws Exception {
        setup("DEMO-CONDITIONAL");
        postAs("PLANNER", "/api/v1/assets", asset("A-2", "DEMO-STRICT")).andExpect(status().isCreated());
        String permissive = assign("A-1", "[\"Electrical\"]"); answer(permissive, "C-E", false); close(permissive);
        String strict = assign("A-2", "[\"Electrical\"]"); answer(strict, "C-E", false); close(strict);
        read("/api/v1/inspections/" + permissive + "/certification-eligibility?subsystem=E")
                .andExpect(jsonPath("$.decision.outcome").value("CONDITIONAL"));
        read("/api/v1/inspections/" + strict + "/certification-eligibility?subsystem=E")
                .andExpect(jsonPath("$.decision.outcome").value("REJECTED"));
        String number = partial(permissive, "E");
        read("/api/v1/certificates/" + number + "/document")
                .andExpect(jsonPath("$.decision.policyCode").value("CONDITIONAL"))
                .andExpect(jsonPath("$.decision.conditions[0].dueDate").value("2026-11-10"))
                .andExpect(jsonPath("$.validUntil").value("2026-11-10"));
        postAs("ISSUER", "/api/v1/certificates", "{\"inspectionId\":\"" + permissive + "\",\"subsystemCode\":\"E\",\"validUntil\":\"2099-01-01\"}")
                .andExpect(status().isBadRequest());
        clock.set("2026-11-11T00:00:00Z");
        read("/api/v1/certificates/" + number).andExpect(jsonPath("$.status").value("EXPIRED"));
    }

    @Test
    void anAssetSplitIntoSubsystemsIsRejectedAsAWholeInsteadOfFailing() throws Exception {
        setup("DEMO-STRICT");
        String inspection = assign("A-1", "null"); answer(inspection, "C-E", true); answer(inspection, "C-P", true); close(inspection);
        read("/api/v1/inspections/" + inspection + "/certification-eligibility")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eligible").value(false))
                .andExpect(jsonPath("$.decision.outcome").value("REJECTED"))
                .andExpect(jsonPath("$.decision.scope.type").value("WHOLE_ASSET"))
                .andExpect(jsonPath("$.decision.reasons[0].reason").value("REQUIRES_PARTIAL_CERTIFICATES"));
        postAs("ISSUER", "/api/v1/certificates", "{\"inspectionId\":\"" + inspection + "\"}").andExpect(status().isConflict());
    }

    @Test
    void aGlobalCertificateForAMissingSchemaVersionIsNotFound() throws Exception {
        setup("DEMO-STRICT");
        postAs("ISSUER", "/api/v1/assets/A-1/global-certificates", "{\"schemaVersion\":9,\"partialCertificateNumbers\":[\"CERT-00001\"]}")
                .andExpect(status().isNotFound());
    }

    @Test
    void newAssetsRequireAnExplicitConfiguredJurisdiction() throws Exception {
        postAs("PLANNER", "/api/v1/assets", asset("A-1", "UNCONFIGURED")).andExpect(status().isBadRequest());
        postAs("PLANNER", "/api/v1/assets", asset("A-1", "DEMO-STRICT").replace("\"jurisdictionCode\":\"DEMO-STRICT\",", ""))
                .andExpect(status().isBadRequest());
        assertEquals(0, jdbc.queryForObject("select count(*) from asset", Integer.class));
    }
    @Test
    void priorFindingUsesItsOriginalSubsystemWhenCriterionCodeMoves() throws Exception {
        setup("DEMO-STRICT");
        String original = assign("A-1", "null"); answer(original, "C-E", false); answer(original, "C-P", true); close(original);
        clock.set("2026-10-13T12:00:00Z");
        String swapped = sections("P").replace("C-E", "PLACEHOLDER").replace("C-P", "C-E").replace("PLACEHOLDER", "C-P");
        postAs("PLANNER", "/api/v1/schemas/Boiler/versions", "{\"requiredSubsystems\":[\"E\",\"P\"],\"sections\":" + swapped + "}")
                .andExpect(status().isCreated());
        String later = assign("A-1", "null"); answer(later, "C-E", true); answer(later, "C-P", true); close(later);
        read("/api/v1/inspections/" + later + "/certification-eligibility?subsystem=P")
                .andExpect(jsonPath("$.decision.outcome").value("APPROVED"));
        read("/api/v1/inspections/" + later + "/certification-eligibility?subsystem=E")
                .andExpect(jsonPath("$.decision.outcome").value("REJECTED"));
    }

    @Test
    void simultaneousIssuanceOfSameScopeHasOneWinnerAndRollsBackTheOther() throws Exception {
        setup("DEMO-STRICT");
        String inspection = assign("A-1", "null"); answer(inspection, "C-E", true); answer(inspection, "C-P", true); close(inspection);
        String body = "{\"inspectionId\":\"" + inspection + "\",\"subsystemCode\":\"E\"}";
        var start = new CountDownLatch(1);
        var ready = new CountDownLatch(2);
        try (var executor = Executors.newFixedThreadPool(2)) {
            Callable<Integer> request = () -> {
                ready.countDown(); start.await();
                return postAs("ISSUER", "/api/v1/certificates", body).andReturn().getResponse().getStatus();
            };
            var first = executor.submit(request); var second = executor.submit(request);
            assertTrue(ready.await(20, TimeUnit.SECONDS)); start.countDown();
            assertEquals(Set.of(201, 409), Set.of(first.get(30, TimeUnit.SECONDS), second.get(30, TimeUnit.SECONDS)));
        }
        assertEquals(1, jdbc.queryForObject("select count(*) from certificate", Integer.class));
        assertEquals(1, jdbc.queryForObject("select count(*) from audit_entry where subject_type = 'certificate'", Integer.class));
    }

    @Test
    void simultaneousPublicationDoesNotLoseHistoryOrDuplicateEffectiveDates() throws Exception {
        setup("DEMO-STRICT"); clock.set("2026-10-13T12:00:00Z");
        String body = "{\"effectiveFrom\":\"2026-11-01T03:00:00Z\",\"requiredSubsystems\":[\"E\",\"P\"],\"sections\":" + sections("P") + "}";
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            Callable<Integer> request = () -> {
                start.await();
                return postAs("PLANNER", "/api/v1/schemas/Boiler/versions", body).andReturn().getResponse().getStatus();
            };
            var first = executor.submit(request); var second = executor.submit(request); start.countDown();
            assertEquals(Set.of(201, 409), Set.of(first.get(30, TimeUnit.SECONDS), second.get(30, TimeUnit.SECONDS)));
        }
        assertEquals(2, jdbc.queryForObject("select count(*) from schema_version", Integer.class));
        assertEquals(2, jdbc.queryForObject("select count(*) from audit_entry where subject_type = 'schema'", Integer.class));
    }

    /**
     * Mientras otra transacción tiene bloqueado el activo, el pedido espera; cuando lo suelta, termina.
     * Un caso de uso que leyera antes de bloquear terminaría sin esperar, con datos que el otro pudo cambiar.
     */
    private void assertWaitsForWhoeverHoldsTheAsset(Callable<Integer> request, int expectedStatus) throws Exception {
        var assetHeld = new CountDownLatch(1);
        var releaseAsset = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var holder = executor.submit(() -> transactions.execute(status -> {
                jdbc.queryForList("select code from asset where code = 'A-1' for update");
                assetHeld.countDown();
                try {
                    return releaseAsset.await(20, TimeUnit.SECONDS);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }));
            assertTrue(assetHeld.await(20, TimeUnit.SECONDS));
            var waiting = executor.submit(request);
            assertThrows(TimeoutException.class, () -> waiting.get(400, TimeUnit.MILLISECONDS));
            releaseAsset.countDown();
            assertEquals(expectedStatus, waiting.get(30, TimeUnit.SECONDS));
            assertEquals(Boolean.TRUE, holder.get(30, TimeUnit.SECONDS));
        }
    }

    @Test
    void everyWriteOnAnOpenInspectionWaitsForWhoeverHoldsItsAsset() throws Exception {
        setup("DEMO-STRICT");
        String responses = "/api/v1/inspections/" + assign("A-1", "null") + "/responses/C-E";
        assertWaitsForWhoeverHoldsTheAsset(() -> putAs("INSPECTOR", responses + "/answer", "{\"value\":true}")
                .andReturn().getResponse().getStatus(), 204);
        assertWaitsForWhoeverHoldsTheAsset(() -> postAs("INSPECTOR", responses + "/evidence",
                "{\"kind\":\"PHOTO\",\"reference\":\"grounding.jpg\",\"description\":\"Grounding\"}")
                .andReturn().getResponse().getStatus(), 201);
        assertWaitsForWhoeverHoldsTheAsset(() -> postAs("INSPECTOR", responses + "/observations", "{\"text\":\"Checked twice\"}")
                .andReturn().getResponse().getStatus(), 201);
    }

    @Test
    void planningACorrectiveActionWaitsForWhoeverHoldsTheAssetOfItsFinding() throws Exception {
        setup("DEMO-STRICT");
        String inspection = assign("A-1", "null");
        answer(inspection, "C-E", true); answer(inspection, "C-P", false); close(inspection);
        String finding = jdbc.queryForObject("select id from finding", String.class);
        assertWaitsForWhoeverHoldsTheAsset(() -> postAs("OWNER", "/api/v1/findings/" + finding + "/corrective-actions",
                "{\"description\":\"Replace the valve\",\"responsible\":{\"id\":\"TECH\",\"name\":\"Technician\"},\"dueDate\":\"2027-06-01\"}")
                .andReturn().getResponse().getStatus(), 201);
    }

    @Test
    void aFindingAndItsSummaryLineCarryTheSubsystemOfTheirCriterion() throws Exception {
        setup("DEMO-STRICT");
        String inspection = assign("A-1", "null");
        answer(inspection, "C-E", true); answer(inspection, "C-P", false); close(inspection);
        String finding = JsonPath.read(read("/api/v1/findings?asset=A-1").andReturn().getResponse().getContentAsString(),
                "$._embedded.findings[0].id");
        read("/api/v1/findings/" + finding).andExpect(jsonPath("$.subsystemCode").value("P"));
        read("/api/v1/assets/A-1/findings-summary")
                .andExpect(jsonPath("$.findings[0].findingId").value(finding))
                .andExpect(jsonPath("$.findings[0].subsystemCode").value("P"));
    }
}
