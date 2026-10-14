package ar.edu.itba.certiflow.infrastructure.shared.persistence;

import java.util.List;
import org.springframework.stereotype.Component;
import tools.jackson.databind.cfg.JsonNodeFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * El JSON de las columnas de texto. Tiene su propio mapper y no lo publica: el de la API lleva otra
 * configuración y cambiarla no debe cambiar lo que ya está guardado. Los decimales conservan su escala
 * también cuando se leen sin un tipo fijo (`6.00` sigue siendo `6.00`).
 */
@Component
public class StoredJson {

    private final JsonMapper jsonMapper = JsonMapper.builder()
            .enable(JsonNodeFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .disable(JsonNodeFeature.STRIP_TRAILING_BIGDECIMAL_ZEROES)
            .build();

    public <T> String write(T document) {
        return jsonMapper.writeValueAsString(document);
    }

    public <T> T read(String storedDocument, Class<T> documentType) {
        return jsonMapper.readValue(storedDocument, documentType);
    }

    public <T> List<T> readListOf(String storedDocument, Class<T> elementType) {
        return jsonMapper.readValue(storedDocument,
                jsonMapper.getTypeFactory().constructCollectionType(List.class, elementType));
    }
}
