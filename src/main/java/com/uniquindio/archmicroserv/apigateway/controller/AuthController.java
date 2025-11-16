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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Autenticación", description = "Endpoints para gestión de autenticación y registro de usuarios")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private final DomainServiceClient domainServiceClient;

    // Constructor explícito para inyección de dependencias
    public AuthController(DomainServiceClient domainServiceClient) {
        this.domainServiceClient = domainServiceClient;
    }

    @Operation(
        summary = "Registrar nuevo usuario",
        description = "Crea un nuevo usuario en el sistema con credenciales de acceso"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Usuario creado exitosamente",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = "{\"error\": false, \"respuesta\": {\"usuario\": \"john_doe\", \"correo\": \"john@example.com\"}}"
                )
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Error procesando el registro",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = "{\"error\": true, \"respuesta\": \"Error procesando registro\"}"
                )
            )
        )
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        description = "Datos del usuario a registrar",
        required = true,
        content = @Content(
            mediaType = "application/json",
            examples = @ExampleObject(
                value = "{\"usuario\": \"john_doe\", \"correo\": \"john@example.com\", \"clave\": \"password123\"}"
            )
        )
    )
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

    @Operation(
        summary = "Autenticar usuario",
        description = "Autentica un usuario y retorna un token JWT"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Autenticación exitosa",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = "{\"error\": false, \"respuesta\": {\"token\": \"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...\", \"usuario\": \"john_doe\"}}"
                )
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Credenciales inválidas",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = "{\"error\": true, \"respuesta\": \"Credenciales inválidas\"}"
                )
            )
        )
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        description = "Credenciales de autenticación",
        required = true,
        content = @Content(
            mediaType = "application/json",
            examples = @ExampleObject(
                value = "{\"usuario\": \"john_doe\", \"clave\": \"password123\"}"
            )
        )
    )
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

    @Operation(
        summary = "Eliminar usuario",
        description = "Elimina un usuario del sistema (requiere autenticación)",
        security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Usuario eliminado exitosamente",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = "{\"error\": false, \"respuesta\": \"Usuario eliminado exitosamente\"}"
                )
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "No autorizado - Token inválido o ausente",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = "{\"error\": true, \"respuesta\": \"Token de autenticación requerido\"}"
                )
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Error interno del servidor",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = "{\"error\": true, \"respuesta\": \"Error eliminando usuario\"}"
                )
            )
        )
    })
    @DeleteMapping("/auth/usuarios/{usuario}")
    public Mono<ResponseEntity<Map<String, Object>>> eliminarUsuario(
            @Parameter(description = "Nombre de usuario a eliminar", required = true, example = "john_doe")
            @PathVariable String usuario,
            @Parameter(description = "Token JWT de autenticación", required = true, example = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
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
                    // Manejar WebClientResponseException para códigos HTTP específicos
                    if (error instanceof org.springframework.web.reactive.function.client.WebClientResponseException) {
                        org.springframework.web.reactive.function.client.WebClientResponseException webClientError = 
                            (org.springframework.web.reactive.function.client.WebClientResponseException) error;
                        int statusCode = webClientError.getStatusCode().value();
                        
                        if (statusCode == 403) {
                            return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN)
                                    .body(Map.of("error", true, "respuesta", "No tiene permisos para eliminar usuarios")));
                        }
                        if (statusCode == 404) {
                            return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND)
                                    .body(Map.of("error", true, "respuesta", "Usuario no encontrado")));
                        }
                    }
                    // Si es 403 en el mensaje de error
                    if (error.getMessage() != null && error.getMessage().contains("403")) {
                        return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .body(Map.of("error", true, "respuesta", "No tiene permisos para eliminar usuarios")));
                    }
                    // Si es 404 en el mensaje de error
                    if (error.getMessage() != null && error.getMessage().contains("404")) {
                        return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(Map.of("error", true, "respuesta", "Usuario no encontrado")));
                    }
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(Map.of("error", true, "respuesta", "Error eliminando usuario")));
                });
    }
}

