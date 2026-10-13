package ar.edu.itba.certiflow.web.support;

import ar.edu.itba.certiflow.infrastructure.security.SpringDataUserAccountRepository;
import ar.edu.itba.certiflow.shared.model.Person;
import ar.edu.itba.certiflow.shared.model.PersonId;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

public class ActingPersonResolver implements HandlerMethodArgumentResolver {

    private final SpringDataUserAccountRepository userAccounts;

    public ActingPersonResolver(SpringDataUserAccountRepository userAccounts) {
        this.userAccounts = userAccounts;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(ActingPerson.class) && parameter.getParameterType() == Person.class;
    }

    @Override
    public Person resolveArgument(MethodParameter parameter, ModelAndViewContainer container, NativeWebRequest request,
                                  WebDataBinderFactory binderFactory) {
        if (request.getUserPrincipal() instanceof JwtAuthenticationToken bearer) {
            return new Person(new PersonId(bearer.getToken().getSubject()), bearer.getToken().getClaimAsString("name"));
        }
        if (request.getUserPrincipal() instanceof Authentication basic) {
            return userAccounts.findByUsername(basic.getName())
                    .map(account -> new Person(new PersonId(account.personId()), account.name()))
                    .orElseThrow(() -> new IllegalStateException("No user account for " + basic.getName()));
        }
        throw new IllegalStateException("The request reached a controller without an authenticated person");
    }
}
