package ar.edu.itba.certiflow.web.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

public class AllowedRoleInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod method)) {
            return true;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (method.hasMethodAnnotation(AllowedRole.class)) {
            StaffRole allowed = method.getMethodAnnotation(AllowedRole.class).value();
            if (!hasAnyRole(authentication, allowed)) {
                throw new AccessDeniedException("Only the " + allowed + " role can do this");
            }
        }
        ReadableBy readers = method.getBeanType().getAnnotation(ReadableBy.class);
        if (isRead(request) && readers != null && !hasAnyRole(authentication, readers.value())) {
            throw new AccessDeniedException("Your role cannot read this");
        }
        return true;
    }

    private static boolean isRead(HttpServletRequest request) {
        return "GET".equals(request.getMethod()) || "HEAD".equals(request.getMethod());
    }

    private static boolean hasAnyRole(Authentication authentication, StaffRole... roles) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> Arrays.stream(roles).anyMatch(role -> authority.getAuthority().equals("ROLE_" + role)));
    }
}
