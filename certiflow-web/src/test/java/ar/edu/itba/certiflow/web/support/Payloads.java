package ar.edu.itba.certiflow.web.support;



public final class Payloads {

    private static final String SECTIONS = """
            [
            {"title":"Pressure and seal","criteria":[
              {"code":"EXT-01","text":"Gauge pressure within range","severity":"MAJOR",
               "rule":{"type":"numeric-range","unit":"bar","minimum":6.00,"maximum":8.00},
               "requiredEvidence":[{"kind":"PHOTO","minimum":1}]},
              {"code":"EXT-02","text":"Safety seal is intact","severity":"CRITICAL",
               "rule":{"type":"yes-no","expected":"YES"},"requiredEvidence":[]}]},
            {"title":"Signage","criteria":[
              {"code":"EXT-03","text":"Signage condition","severity":"MINOR",
               "rule":{"type":"option-in-list","options":["VISIBLE","REFLECTIVE"]},"requiredEvidence":[]}]}]""";

    private Payloads() {
    }

    public static String asset() {
        return """
                {"code":"EXT-0042","name":"Lobby extinguisher","type":"Fire extinguisher","jurisdictionCode":"DEMO-STANDARD","subsystems":[],"location":"Lobby",
                 "responsible":{"id":"EMP-003","name":"Raul Responsible"},
                 "characteristics":[{"name":"capacity","value":"10 kg"}]}""";
    }

    public static String schema() {
        return "{\"name\":\"Portable extinguisher inspection\",\"assetType\":\"Fire extinguisher\",\"sections\":" + SECTIONS + "}";
    }

    public static String version() {
        return "{\"sections\":" + SECTIONS + "}";
    }
}
