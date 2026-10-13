package ar.edu.itba.certiflow.web.support;

import java.util.List;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedModel;

public final class PageLinks {

    private PageLinks() {
    }

    public static <T> PagedModel<T> page(List<T> all, int page, int size, String baseUri, String query) {
        if (page < 0 || size < 1) {
            throw new InvalidRequestException("page must be 0 or more and size 1 or more");
        }
        int totalPages = Math.max(1, (all.size() + size - 1) / size);
        List<T> content = all.stream().skip((long) page * size).limit(size).toList();
        PagedModel<T> model = PagedModel.of(content, new PagedModel.PageMetadata(size, page, all.size(), totalPages));
        model.add(Link.of(uri(baseUri, query, page, size)).withSelfRel(),
                Link.of(uri(baseUri, query, 0, size)).withRel("first"),
                Link.of(uri(baseUri, query, totalPages - 1, size)).withRel("last"));
        if (page > 0) {
            model.add(Link.of(uri(baseUri, query, page - 1, size)).withRel("prev"));
        }
        if (page < totalPages - 1) {
            model.add(Link.of(uri(baseUri, query, page + 1, size)).withRel("next"));
        }
        return model;
    }

    private static String uri(String baseUri, String query, int page, int size) {
        return baseUri + "?" + (query.isEmpty() ? "" : query + "&") + "page=" + page + "&size=" + size;
    }
}
