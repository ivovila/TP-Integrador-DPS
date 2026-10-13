package ar.edu.itba.certiflow.shared.model;

import java.util.List;
import java.util.function.Function;

/** Una página de un listado junto con el total de elementos que hay en todas las páginas. */
public record Page<T>(List<T> items, PageRequest request, long totalItems) {

    public Page {
        items = List.copyOf(items);
    }

    public int totalPages() {
        return (int) Math.max(1, (totalItems + request.size() - 1) / request.size());
    }

    public <R> Page<R> map(Function<T, R> mapper) {
        return new Page<>(items.stream().map(mapper).toList(), request, totalItems);
    }
}
