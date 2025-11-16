package com.uniquindio.archmicroserv.apigateway.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Objects;

@Service
public class DomainServiceClient {

    private static final Logger log = LoggerFactory.getLogger(DomainServiceClient.class);
    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE_REF = 
            new ParameterizedTypeReference<Map<String, Object>>() {};

    @Qualifier("domainServiceWebClient")
    private final WebClient domainServiceWebClient;

    @Value("${domain.service.base-path}")
    private String basePath;

    // Constructor explícito para inyección de dependencias
    public DomainServiceClient(@Qualifier("domainServiceWebClient") WebClient domainServiceWebClient) {
        this.domainServiceWebClient = domainServiceWebClient;
    }

    public Mono<Map<String, Object>> registrarUsuario(Object requestBody) {
        log.info("Proxy: POST {}/usuarios", basePath);
        return domainServiceWebClient
                .post()
                .uri(basePath + "/usuarios")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(Objects.requireNonNull(requestBody, "requestBody must not be null"))
                .retrieve()
                .bodyToMono(Objects.requireNonNull(MAP_TYPE_REF, "MAP_TYPE_REF must not be null"))
                .doOnSuccess(response -> log.info("Registro exitoso"))
                .doOnError(error -> log.error("Error en registro: {}", error.getMessage()));
    }

    public Mono<Map<String, Object>> autenticar(Object requestBody) {
        log.info("Proxy: POST {}/sesiones", basePath);
        return domainServiceWebClient
                .post()
                .uri(basePath + "/sesiones")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(Objects.requireNonNull(requestBody, "requestBody must not be null"))
                .retrieve()
                .bodyToMono(Objects.requireNonNull(MAP_TYPE_REF, "MAP_TYPE_REF must not be null"))
                .doOnSuccess(response -> log.info("Autenticación exitosa"))
                .doOnError(error -> log.error("Error en autenticación: {}", error.getMessage()));
    }

    public Mono<Map<String, Object>> eliminarUsuario(String usuario, String authToken) {
        log.info("Proxy: DELETE {}/usuarios/{}", basePath, usuario);
        return domainServiceWebClient
                .delete()
                .uri(basePath + "/usuarios/" + usuario)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + authToken)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), 
                    response -> {
                        log.error("Error eliminando usuario: {} {}", response.statusCode(), response.statusCode().value());
                        return response.bodyToMono(String.class)
                            .flatMap(errorBody -> {
                                org.springframework.web.reactive.function.client.WebClientResponseException exception = 
                                    org.springframework.web.reactive.function.client.WebClientResponseException.create(
                                        response.statusCode().value(),
                                        response.statusCode().toString(),
                                        response.headers().asHttpHeaders(),
                                        errorBody != null ? errorBody.getBytes() : null,
                                        java.nio.charset.StandardCharsets.UTF_8
                                    );
                                return Mono.error(exception);
                            });
                    })
                .bodyToMono(Objects.requireNonNull(MAP_TYPE_REF, "MAP_TYPE_REF must not be null"))
                .doOnSuccess(response -> log.info("Usuario eliminado exitosamente"))
                .doOnError(error -> log.error("Error eliminando usuario: {}", error.getMessage()));
    }

    public Mono<Map<String, Object>> obtenerUsuario(String usuario, String authToken) {
        log.info("Proxy: GET {}/usuarios (con filtro para usuario: {})", basePath, usuario);
        
        return domainServiceWebClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path(basePath + "/usuarios")
                        .queryParam("pagina", "0")
                        .build())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + authToken)
                .retrieve()
                .bodyToMono(Objects.requireNonNull(MAP_TYPE_REF, "MAP_TYPE_REF must not be null"))
                .map(response -> {
                    // Filtrar el usuario específico de la lista
                    // Esto es un workaround hasta que el Domain Service tenga GET /usuarios/{usuario}
                    return response;
                })
                .doOnSuccess(response -> log.info("Usuario obtenido exitosamente"))
                .doOnError(error -> log.error("Error obteniendo usuario: {}", error.getMessage()));
    }

    public Mono<Map<String, Object>> actualizarUsuario(String usuario, Object requestBody, String authToken) {
        log.info("Proxy: PATCH {}/usuarios/{}", basePath, usuario);
        return domainServiceWebClient
                .patch()
                .uri(basePath + "/usuarios/" + usuario)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + authToken)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(Objects.requireNonNull(requestBody, "requestBody must not be null"))
                .retrieve()
                .bodyToMono(Objects.requireNonNull(MAP_TYPE_REF, "MAP_TYPE_REF must not be null"))
                .doOnSuccess(response -> log.info("Usuario actualizado exitosamente"))
                .doOnError(error -> log.error("Error actualizando usuario: {}", error.getMessage()));
    }
}

