package ar.edu.itba.certiflow.infrastructure.shared.persistence;

import java.util.List;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * El JSON de las columnas de texto. Tiene su propio mapper y no lo publica: el de la API lleva otra
 * configuración y cambiarla no debe cambiar lo que ya está guardado.
 */
@Component
public class StoredJson {

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

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
