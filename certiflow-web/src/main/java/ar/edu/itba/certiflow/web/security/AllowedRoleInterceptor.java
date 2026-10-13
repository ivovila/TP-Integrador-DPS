package ar.edu.itba.certiflow.web.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

public class AllowedRoleInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (handler instanceof HandlerMethod method && method.hasMethodAnnotation(AllowedRole.class)) {
            StaffRole allowed = method.getMethodAnnotation(AllowedRole.class).value();
            if (!hasRole(SecurityContextHolder.getContext().getAuthentication(), allowed)) {
                throw new AccessDeniedException("Only the " + allowed + " role can do this");
            }
        }
        return true;
    }

    private static boolean hasRole(Authentication authentication, StaffRole role) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + role));
    }
}
