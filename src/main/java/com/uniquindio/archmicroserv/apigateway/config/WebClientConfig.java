package com.uniquindio.archmicroserv.apigateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Objects;

@Configuration
public class WebClientConfig {

    @Value("${domain.service.url}")
    private String domainServiceUrl;

    @Value("${gestion.perfil.service.url}")
    private String gestionPerfilServiceUrl;

    @Bean
    public WebClient domainServiceWebClient() {
        return WebClient.builder()
                .baseUrl(Objects.requireNonNull(domainServiceUrl, "domain.service.url must not be null"))
                .build();
    }

    @Bean
    public WebClient gestionPerfilServiceWebClient() {
        return WebClient.builder()
                .baseUrl(Objects.requireNonNull(gestionPerfilServiceUrl, "gestion.perfil.service.url must not be null"))
                .build();
    }
}

