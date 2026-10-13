package ar.edu.itba.certiflow.infrastructure.shared.persistence;

import java.util.Arrays;
import java.util.List;
import tools.jackson.databind.json.JsonMapper;

public final class CodesJson {
    private static final JsonMapper MAPPER = JsonMapper.builder().build();
    private CodesJson() { }
    public static String write(List<String> value) { return MAPPER.writeValueAsString(value); }
    public static List<String> read(String value) { return Arrays.asList(MAPPER.readValue(value, String[].class)); }
}
