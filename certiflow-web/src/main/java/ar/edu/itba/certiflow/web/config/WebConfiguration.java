package ar.edu.itba.certiflow.web.config;

import ar.edu.itba.certiflow.infrastructure.security.SpringDataUserAccountRepository;
import ar.edu.itba.certiflow.web.support.ActingPersonResolver;
import java.util.List;
import tools.jackson.databind.DeserializationFeature;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfiguration implements WebMvcConfigurer {

    private final SpringDataUserAccountRepository userAccounts;

    public WebConfiguration(SpringDataUserAccountRepository userAccounts) {
        this.userAccounts = userAccounts;
    }

    @Bean
    JsonMapperBuilderCustomizer exactDecimals() {
        return builder -> builder.enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new ActingPersonResolver(userAccounts));
    }
}
