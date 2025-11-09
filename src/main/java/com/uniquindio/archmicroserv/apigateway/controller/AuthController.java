package com.uniquindio.archmicroserv.apigateway.controller;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.uniquindio.archmicroserv.apigateway.service.DomainServiceClient;

import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private final DomainServiceClient domainServiceClient;

    // Constructor explícito para inyección de dependencias
    public AuthController(DomainServiceClient domainServiceClient) {
        this.domainServiceClient = domainServiceClient;
    }

    @PostMapping("/auth/registro")
    public Mono<ResponseEntity<Map<String, Object>>> registrarUsuario(@RequestBody Map<String, Object> requestBody) {
        log.info("API Gateway: Registro de usuario");
        return domainServiceClient.registrarUsuario(requestBody)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response))
                .onErrorResume(error -> {
                    log.error("Error en registro: {}", error.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(Map.of("error", true, "respuesta", "Error procesando registro")));
                });
    }

    @PostMapping("/auth/login")
    public Mono<ResponseEntity<Map<String, Object>>> autenticar(@RequestBody Map<String, Object> requestBody) {
        log.info("API Gateway: Autenticación de usuario");
        return domainServiceClient.autenticar(requestBody)
                .map(ResponseEntity::ok)
                .onErrorResume(error -> {
                    log.error("Error en autenticación: {}", error.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body(Map.of("error", true, "respuesta", "Credenciales inválidas")));
                });
    }

    @DeleteMapping("/auth/usuarios/{usuario}")
    public Mono<ResponseEntity<Map<String, Object>>> eliminarUsuario(
            @PathVariable String usuario,
            @RequestHeader(value = "Authorization", required = false) String authToken) {
        log.info("API Gateway: Eliminación de usuario {}", usuario);
        
        if (authToken == null || !authToken.startsWith("Bearer ")) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", true, "respuesta", "Token de autenticación requerido")));
        }

        String token = authToken.substring(7); // Remove "Bearer " prefix

        return domainServiceClient.eliminarUsuario(usuario, token)
                .map(response -> ResponseEntity.ok(response))
                .onErrorResume(error -> {
                    log.error("Error eliminando usuario: {}", error.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(Map.of("error", true, "respuesta", "Error eliminando usuario")));
                });
    }
}

