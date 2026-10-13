package ar.edu.itba.certiflow.web.support;

import ar.edu.itba.certiflow.shared.model.Page;
import ar.edu.itba.certiflow.shared.model.PageRequest;
import java.util.List;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedModel;

public final class PageLinks {

    private PageLinks() {
    }

    public static PageRequest request(int page, int size) {
        if (page < 0 || size < 1) {
            throw new InvalidRequestException("page must be 0 or more and size 1 or more");
        }
        return new PageRequest(page, size);
    }

    /** Pagina en memoria un listado completo. */
    public static <T> PagedModel<T> page(List<T> all, int page, int size, String baseUri, String query) {
        PageRequest request = request(page, size);
        return of(new Page<>(all.stream().skip(request.offset()).limit(size).toList(), request, all.size()), baseUri, query);
    }

    /** Publica una página que ya vino paginada del repositorio. */
    public static <T> PagedModel<T> of(Page<T> page, String baseUri, String query) {
        int number = page.request().number();
        int size = page.request().size();
        int totalPages = page.totalPages();
        PagedModel<T> model = PagedModel.of(page.items(), new PagedModel.PageMetadata(size, number, page.totalItems(), totalPages));
        model.add(Link.of(uri(baseUri, query, number, size)).withSelfRel(),
                Link.of(uri(baseUri, query, 0, size)).withRel("first"),
                Link.of(uri(baseUri, query, totalPages - 1, size)).withRel("last"));
        if (number > 0) {
            model.add(Link.of(uri(baseUri, query, number - 1, size)).withRel("prev"));
        }
        if (number < totalPages - 1) {
            model.add(Link.of(uri(baseUri, query, number + 1, size)).withRel("next"));
        }
        return model;
    }

    private static String uri(String baseUri, String query, int page, int size) {
        return baseUri + "?" + (query.isEmpty() ? "" : query + "&") + "page=" + page + "&size=" + size;
    }
}
