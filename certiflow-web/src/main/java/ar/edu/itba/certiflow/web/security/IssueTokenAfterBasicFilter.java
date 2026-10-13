package ar.edu.itba.certiflow.web.security;

import ar.edu.itba.certiflow.infrastructure.security.SpringDataUserAccountRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class IssueTokenAfterBasicFilter extends OncePerRequestFilter {

    static final String TOKEN_EXPIRES_HEADER = "Token-Expires";

    private final SpringDataUserAccountRepository userAccounts;
    private final TokenIssuer tokenIssuer;

    public IssueTokenAfterBasicFilter(SpringDataUserAccountRepository userAccounts, TokenIssuer tokenIssuer) {
        this.userAccounts = userAccounts;
        this.tokenIssuer = tokenIssuer;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof UsernamePasswordAuthenticationToken basic && basic.isAuthenticated()) {
            userAccounts.findByUsername(basic.getName()).map(tokenIssuer::issueFor).ifPresent(token -> {
                response.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token.value());
                response.setHeader(TOKEN_EXPIRES_HEADER, token.expiresAt().toString());
            });
        }
        chain.doFilter(request, response);
    }
}
