package ar.edu.itba.certiflow.web.support;

import ar.edu.itba.certiflow.shared.model.Person;
import ar.edu.itba.certiflow.shared.model.PersonId;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

public class ActingPersonResolver implements HandlerMethodArgumentResolver {

    static final String ID_HEADER = "X-Person-Id";
    static final String NAME_HEADER = "X-Person-Name";

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(ActingPerson.class) && parameter.getParameterType() == Person.class;
    }

    @Override
    public Person resolveArgument(MethodParameter parameter, ModelAndViewContainer container, NativeWebRequest request,
                                  WebDataBinderFactory binderFactory) throws MissingRequestHeaderException {
        String id = request.getHeader(ID_HEADER);
        String name = request.getHeader(NAME_HEADER);
        if (id == null || id.isBlank()) {
            throw new MissingRequestHeaderException(ID_HEADER, parameter);
        }
        if (name == null || name.isBlank()) {
            throw new MissingRequestHeaderException(NAME_HEADER, parameter);
        }
        return new Person(new PersonId(id), name);
    }
}
