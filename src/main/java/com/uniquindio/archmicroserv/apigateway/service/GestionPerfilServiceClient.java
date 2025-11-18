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
public class GestionPerfilServiceClient {

    private static final Logger log = LoggerFactory.getLogger(GestionPerfilServiceClient.class);
    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE_REF = 
            new ParameterizedTypeReference<Map<String, Object>>() {};

    @Qualifier("gestionPerfilServiceWebClient")
    private final WebClient gestionPerfilServiceWebClient;

    @Value("${gestion.perfil.service.base-path}")
    private String basePath;

    // Constructor explícito para inyección de dependencias
    public GestionPerfilServiceClient(@Qualifier("gestionPerfilServiceWebClient") WebClient gestionPerfilServiceWebClient) {
        this.gestionPerfilServiceWebClient = gestionPerfilServiceWebClient;
    }

    public Mono<Map<String, Object>> crearPerfil(String usuarioId, Object requestBody) {
        log.info("Proxy: POST {}/{}", basePath, usuarioId);
        return gestionPerfilServiceWebClient
                .post()
                .uri(basePath + "/" + usuarioId)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(Objects.requireNonNull(requestBody, "requestBody must not be null"))
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                    response -> {
                        log.error("Error creando perfil: {} {}", response.statusCode(), response.statusCode().value());
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
                .doOnSuccess(response -> log.info("Perfil creado exitosamente"))
                .doOnError(error -> log.error("Error creando perfil: {}", error.getMessage()));
    }

    public Mono<Map<String, Object>> obtenerPerfil(String usuarioId) {
        log.info("Proxy: GET {}/{}", basePath, usuarioId);
        return gestionPerfilServiceWebClient
                .get()
                .uri(basePath + "/" + usuarioId)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                    response -> {
                        log.error("Error obteniendo perfil: {} {}", response.statusCode(), response.statusCode().value());
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
                .doOnSuccess(response -> log.info("Perfil obtenido exitosamente"))
                .doOnError(error -> log.error("Error obteniendo perfil: {}", error.getMessage()));
    }

    public Mono<Map<String, Object>> actualizarPerfil(String usuarioId, Object requestBody) {
        log.info("Proxy: PUT {}/{}", basePath, usuarioId);
        return gestionPerfilServiceWebClient
                .put()
                .uri(basePath + "/" + usuarioId)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(Objects.requireNonNull(requestBody, "requestBody must not be null"))
                .retrieve()
                .bodyToMono(Objects.requireNonNull(MAP_TYPE_REF, "MAP_TYPE_REF must not be null"))
                .doOnSuccess(response -> log.info("Perfil actualizado exitosamente"))
                .doOnError(error -> log.error("Error actualizando perfil: {}", error.getMessage()));
    }

    public Mono<Void> eliminarPerfil(String usuarioId) {
        log.info("Proxy: DELETE {}/{}", basePath, usuarioId);
        return gestionPerfilServiceWebClient
                .delete()
                .uri(basePath + "/" + usuarioId)
                .retrieve()
                .bodyToMono(Void.class)
                .doOnSuccess(response -> log.info("Perfil eliminado exitosamente"))
                .doOnError(error -> log.error("Error eliminando perfil: {}", error.getMessage()));
    }
}

