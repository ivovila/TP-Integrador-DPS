package ar.edu.itba.certiflow.infrastructure.shared.persistence;

import ar.edu.itba.certiflow.catalog.model.SubsystemCode;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class CodesDocumentMapper {

    private final StoredJson storedJson;

    public CodesDocumentMapper(StoredJson storedJson) {
        this.storedJson = storedJson;
    }

    public String write(List<String> codes) {
        return storedJson.write(codes);
    }

    public List<String> read(String storedCodes) {
        return storedJson.readListOf(storedCodes, String.class);
    }

    /** Los códigos se guardan ordenados para que el mismo conjunto se escriba siempre igual. */
    public String writeSubsystemCodes(Set<SubsystemCode> subsystemCodes) {
        return write(subsystemCodes.stream().map(SubsystemCode::value).sorted().toList());
    }

    public Set<SubsystemCode> readSubsystemCodes(String storedCodes) {
        return read(storedCodes).stream().map(SubsystemCode::new).collect(Collectors.toSet());
    }
}
