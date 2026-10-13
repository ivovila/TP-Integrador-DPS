package ar.edu.itba.certiflow.web.security;

import ar.edu.itba.certiflow.infrastructure.security.SpringDataRevokedTokenRepository;
import ar.edu.itba.certiflow.infrastructure.security.SpringDataUserAccountRepository;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import tools.jackson.databind.json.JsonMapper;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfiguration {

    @Bean
    SecurityFilterChain api(HttpSecurity http, SpringDataUserAccountRepository userAccounts, TokenIssuer tokenIssuer,
                            JsonMapper jsonMapper) throws Exception {
        SecurityProblems problems = new SecurityProblems(jsonMapper);
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(requests -> requests.anyRequest().authenticated())
                .httpBasic(basic -> basic.authenticationEntryPoint(problems))
                .oauth2ResourceServer(server -> server
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(SecurityConfiguration::authenticationOf))
                        .authenticationEntryPoint(problems)
                        .accessDeniedHandler(problems))
                .exceptionHandling(handling -> handling.authenticationEntryPoint(problems).accessDeniedHandler(problems))
                .addFilterAfter(new IssueTokenAfterBasicFilter(userAccounts, tokenIssuer), BasicAuthenticationFilter.class)
                .build();
    }

    @Bean
    SecretKey jwtKey(JwtProperties properties) {
        return new SecretKeySpec(properties.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey jwtKey) {
        return NimbusJwtEncoder.withSecretKey(jwtKey).build();
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey jwtKey, SpringDataRevokedTokenRepository revokedTokens) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtKey).macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefault(), new NotRevokedValidator(revokedTokens)));
        return decoder;
    }

    @Bean
    TokenIssuer tokenIssuer(JwtEncoder jwtEncoder, JwtProperties properties, Clock clock) {
        return new TokenIssuer(jwtEncoder, properties, clock);
    }

    @Bean
    UserDetailsService userDetailsService(SpringDataUserAccountRepository userAccounts) {
        return new UserAccountDetailsService(userAccounts);
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    private static AbstractAuthenticationToken authenticationOf(Jwt jwt) {
        return new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_" + jwt.getClaimAsString(TokenIssuer.ROLE_CLAIM))),
                jwt.getSubject());
    }
}
