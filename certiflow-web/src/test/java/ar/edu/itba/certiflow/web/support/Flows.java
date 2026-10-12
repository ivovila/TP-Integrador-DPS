package ar.edu.itba.certiflow.web.support;

import com.jayway.jsonpath.JsonPath;
import java.time.LocalDate;

public final class Flows {

    private final ApiTest api;

    public Flows(ApiTest api) {
        this.api = api;
    }

    public static String path(String href) {
        return href.substring(href.indexOf("/api/v1"));
    }

    public void assetAndSchema() throws Exception {
        api.postAs("EMP-001", "/api/v1/assets", Payloads.asset());
        api.postAs("EMP-001", "/api/v1/schemas", Payloads.schema());
    }

    public String closedInspection(String pressure) throws Exception {
        String assigned = api.postAs("EMP-001", "/api/v1/inspections", """
                {"assetCode":"EXT-0042","inspector":{"id":"EMP-002","name":"Ines"},"plannedDate":"2026-12-01","sections":null}""")
                .andReturn().getResponse().getContentAsString();
        String inspection = path(JsonPath.read(assigned, "$._links.self.href"));
        api.putAs("EMP-002", inspection + "/responses/EXT-01/answer", "{\"value\":" + pressure + "}");
        api.postAs("EMP-002", inspection + "/responses/EXT-01/evidence", "{\"kind\":\"PHOTO\",\"reference\":\"g.jpg\",\"description\":\"G\"}");
        api.putAs("EMP-002", inspection + "/responses/EXT-02/answer", "{\"value\":true}");
        api.putAs("EMP-002", inspection + "/responses/EXT-03/answer", "{\"value\":\"VISIBLE\"}");
        api.postAs("EMP-002", inspection + "/closure", "{}");
        return inspection;
    }

    public String findingOf(String inspection) throws Exception {
        String asset = JsonPath.read(api.read(inspection).andReturn().getResponse().getContentAsString(), "$.assetCode");
        String findings = api.read("/api/v1/findings?asset=" + asset).andReturn().getResponse().getContentAsString();
        return path(JsonPath.read(findings, "$._embedded.findings[0]._links.self.href"));
    }

    public String certificateFor(String inspection) throws Exception {
        String id = inspection.substring(inspection.lastIndexOf('/') + 1);
        String location = api.postAs("EMP-006", "/api/v1/certificates",
                        "{\"inspectionId\":\"" + id + "\",\"validUntil\":\"" + LocalDate.now().plusYears(1) + "\"}")
                .andReturn().getResponse().getHeader("Location");
        return path(location);
    }

    public static String idOf(String path) {
        return path.substring(path.lastIndexOf('/') + 1);
    }
}
