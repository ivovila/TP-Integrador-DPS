package ar.edu.itba.certiflow.domain.asset;

public record Location(String site, String address, String area) {

    public Location {
        site = requireText(site, "El sitio");
        address = requireText(address, "La direccion");
        area = requireText(area, "El area");
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " no puede estar vacio");
        }
        return value.trim();
    }
}
