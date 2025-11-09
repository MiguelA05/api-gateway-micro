package com.uniquindio.archmicroserv.apigateway.controller;

import com.uniquindio.archmicroserv.apigateway.messaging.EventoPublisher;
import com.uniquindio.archmicroserv.apigateway.service.DomainServiceClient;
import com.uniquindio.archmicroserv.apigateway.service.GestionPerfilServiceClient;
import com.uniquindio.archmicroserv.apigateway.service.UsuarioUnificadoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/usuarios")
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

    @GetMapping("/{usuario}")
    public Mono<ResponseEntity<Map<String, Object>>> obtenerUsuarioCompleto(
            @PathVariable String usuario,
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

    @PutMapping("/{usuario}")
    public Mono<ResponseEntity<Map<String, Object>>> actualizarUsuarioCompleto(
            @PathVariable String usuario,
            @RequestBody Map<String, Object> requestBody,
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

    @DeleteMapping("/{usuario}")
    public Mono<ResponseEntity<Map<String, Object>>> eliminarUsuarioCompleto(
            @PathVariable String usuario,
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

