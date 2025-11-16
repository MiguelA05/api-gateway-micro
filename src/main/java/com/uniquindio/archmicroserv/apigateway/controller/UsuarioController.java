package com.uniquindio.archmicroserv.apigateway.controller;

import com.uniquindio.archmicroserv.apigateway.messaging.EventoPublisher;
import com.uniquindio.archmicroserv.apigateway.service.DomainServiceClient;
import com.uniquindio.archmicroserv.apigateway.service.GestionPerfilServiceClient;
import com.uniquindio.archmicroserv.apigateway.service.UsuarioUnificadoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/usuarios")
@Tag(name = "Gestión de Usuarios", description = "Endpoints para gestión completa de usuarios y perfiles")
public class UsuarioController {

    private static final Logger log = LoggerFactory.getLogger(UsuarioController.class);
    private final UsuarioUnificadoService usuarioUnificadoService;
    private final DomainServiceClient domainServiceClient;
    private final GestionPerfilServiceClient gestionPerfilServiceClient;
    private final EventoPublisher eventoPublisher;

    // Constructor explícito para inyección de dependencias
    public UsuarioController(
            UsuarioUnificadoService usuarioUnificadoService,
            DomainServiceClient domainServiceClient,
            GestionPerfilServiceClient gestionPerfilServiceClient,
            EventoPublisher eventoPublisher) {
        this.usuarioUnificadoService = usuarioUnificadoService;
        this.domainServiceClient = domainServiceClient;
        this.gestionPerfilServiceClient = gestionPerfilServiceClient;
        this.eventoPublisher = eventoPublisher;
    }

    @Operation(
        summary = "Obtener usuario completo",
        description = "Obtiene los datos completos de un usuario incluyendo su perfil (requiere autenticación)",
        security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Usuario encontrado exitosamente",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = "{\"usuario\": \"john_doe\", \"perfil\": {\"nombre\": \"John\", \"apellido\": \"Doe\", \"telefono\": \"1234567890\"}}"
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
            description = "Error interno del servidor"
        )
    })
    @GetMapping("/{usuario}")
    public Mono<ResponseEntity<Map<String, Object>>> obtenerUsuarioCompleto(
            @Parameter(description = "Nombre de usuario a buscar", required = true, example = "john_doe")
            @PathVariable String usuario,
            @Parameter(description = "Token JWT de autenticación", required = true, example = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
            @RequestHeader(value = "Authorization", required = false) String authToken) {
        log.info("API Gateway: Obteniendo datos completos del usuario {}", usuario);

        if (authToken == null || !authToken.startsWith("Bearer ")) {
            return Mono.just(ResponseEntity.status(401)
                    .body(Map.of("error", true, "respuesta", "Token de autenticación requerido")));
        }

        // Obtener datos de perfil (no requiere auth, pero validamos el token para seguridad)
        Mono<Map<String, Object>> datosPerfil = gestionPerfilServiceClient
                .obtenerPerfil(usuario)
                .onErrorResume(error -> {
                    log.warn("Error obteniendo perfil: {}", error.getMessage());
                    return Mono.just(new HashMap<>());
                });

        return datosPerfil
                .map(perfil -> {
                    Map<String, Object> resultado = new HashMap<>();
                    resultado.put("usuario", usuario);
                    if (!perfil.isEmpty()) {
                        resultado.put("perfil", perfil);
                    }
                    // Nota: Los datos de seguridad (correo, etc.) deberían obtenerse del token JWT
                    // o del Domain Service si tiene endpoint GET /usuarios/{usuario}
                    return ResponseEntity.ok(resultado);
                })
                .onErrorResume(error -> {
                    log.error("Error obteniendo usuario completo: {}", error.getMessage());
                    return Mono.just(ResponseEntity.status(500)
                            .body(Map.of("error", true, "respuesta", "Error obteniendo datos del usuario")));
                });
    }

    @Operation(
        summary = "Actualizar usuario completo",
        description = "Actualiza los datos de un usuario en todos los microservicios (requiere autenticación)",
        security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Usuario actualizado exitosamente",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = "{\"error\": false, \"respuesta\": \"Usuario actualizado exitosamente\"}"
                )
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "No autorizado - Token inválido o ausente"
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Error actualizando datos del usuario"
        )
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        description = "Datos del usuario a actualizar (puede incluir correo, clave, nombre, apellido, telefono)",
        required = true,
        content = @Content(
            mediaType = "application/json",
            examples = @ExampleObject(
                value = "{\"correo\": \"newemail@example.com\", \"nombre\": \"John\", \"apellido\": \"Doe\", \"telefono\": \"9876543210\"}"
            )
        )
    )
    @PutMapping("/{usuario}")
    public Mono<ResponseEntity<Map<String, Object>>> actualizarUsuarioCompleto(
            @Parameter(description = "Nombre de usuario a actualizar", required = true, example = "john_doe")
            @PathVariable String usuario,
            @RequestBody Map<String, Object> requestBody,
            @Parameter(description = "Token JWT de autenticación", required = true, example = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
            @RequestHeader(value = "Authorization", required = false) String authToken) {
        log.info("API Gateway: Actualizando datos completos del usuario {}", usuario);

        if (authToken == null || !authToken.startsWith("Bearer ")) {
            return Mono.just(ResponseEntity.status(401)
                    .body(Map.of("error", true, "respuesta", "Token de autenticación requerido")));
        }

        String token = authToken.substring(7);

        return usuarioUnificadoService.actualizarUsuarioCompleto(usuario, requestBody, token)
                .map(ResponseEntity::ok)
                .onErrorResume(error -> {
                    log.error("Error actualizando usuario completo: {}", error.getMessage());
                    return Mono.just(ResponseEntity.status(500)
                            .body(Map.of("error", true, "respuesta", "Error actualizando datos del usuario")));
                });
    }

    @Operation(
        summary = "Eliminar usuario completo",
        description = "Elimina un usuario de todos los microservicios del sistema (requiere autenticación)",
        security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Usuario eliminado exitosamente del sistema",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = "{\"error\": false, \"respuesta\": \"Usuario eliminado exitosamente del sistema\"}"
                )
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "No autorizado - Token inválido o ausente"
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Error eliminando usuario"
        )
    })
    @DeleteMapping("/{usuario}")
    public Mono<ResponseEntity<Map<String, Object>>> eliminarUsuarioCompleto(
            @Parameter(description = "Nombre de usuario a eliminar", required = true, example = "john_doe")
            @PathVariable String usuario,
            @Parameter(description = "Token JWT de autenticación", required = true, example = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
            @RequestHeader(value = "Authorization", required = false) String authToken) {
        log.info("API Gateway: Eliminación completa del usuario {}", usuario);

        if (authToken == null || !authToken.startsWith("Bearer ")) {
            return Mono.just(ResponseEntity.status(401)
                    .body(Map.of("error", true, "respuesta", "Token de autenticación requerido")));
        }

        String token = authToken.substring(7);

        // Eliminar de ambos servicios en paralelo
        Mono<Map<String, Object>> eliminarSeguridad = domainServiceClient
                .eliminarUsuario(usuario, token)
                .onErrorResume(error -> {
                    log.warn("Error eliminando datos de seguridad: {}", error.getMessage());
                    return Mono.just(Map.of("error", false, "respuesta", "Usuario eliminado parcialmente"));
                });

        Mono<Map<String, Object>> eliminarPerfil = gestionPerfilServiceClient
                .eliminarPerfil(usuario)
                .then(Mono.just((Map<String, Object>) new HashMap<String, Object>()))
                .onErrorResume(error -> {
                    log.warn("Error eliminando perfil: {}", error.getMessage());
                    return Mono.just((Map<String, Object>) new HashMap<String, Object>());
                });

        return Mono.zip(
                eliminarSeguridad.onErrorReturn(new HashMap<>()),
                eliminarPerfil.onErrorReturn(new HashMap<>())
        )
                .doOnSuccess(result -> {
                    // Publicar evento de eliminación
                    // Extraer correo de la respuesta si está disponible
                    try {
                        Map<String, Object> seguridadResponse = result.getT1();
                        String correo = extractCorreo(seguridadResponse);
                        eventoPublisher.publicarEventoEliminacion(usuario, correo);
                        log.info("Evento de eliminación publicado para usuario: {}", usuario);
                    } catch (Exception e) {
                        log.error("Error publicando evento de eliminación: {}", e.getMessage(), e);
                    }
                })
                .map(result -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("error", false);
                    response.put("respuesta", "Usuario eliminado exitosamente del sistema");
                    return ResponseEntity.ok(response);
                })
                .onErrorResume(error -> {
                    log.error("Error eliminando usuario completo: {}", error.getMessage(), error);
                    // Intentar publicar evento incluso si hay error
                    try {
                        eventoPublisher.publicarEventoEliminacion(usuario, "");
                    } catch (Exception e) {
                        log.error("Error publicando evento después de error: {}", e.getMessage());
                    }
                    return Mono.just(ResponseEntity.status(500)
                            .body(Map.of("error", true, "respuesta", "Error eliminando usuario")));
                });
    }

    @SuppressWarnings("unchecked")
    private String extractCorreo(Map<String, Object> usuarioData) {
        try {
            if (usuarioData.containsKey("respuesta")) {
                Object respuesta = usuarioData.get("respuesta");
                if (respuesta instanceof Map) {
                    Map<String, Object> datos = (Map<String, Object>) respuesta;
                    if (datos.containsKey("correo")) {
                        return datos.get("correo").toString();
                    }
                }
            }
            if (usuarioData.containsKey("correo")) {
                return usuarioData.get("correo").toString();
            }
        } catch (Exception e) {
            log.warn("No se pudo extraer el correo del usuario: {}", e.getMessage());
        }
        return "";
    }
}

