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

/**
 * Controller para gestión de usuarios completos.
 * 
 * <p>Este controller orquesta llamadas a múltiples microservicios para proporcionar
 * operaciones unificadas sobre usuarios:</p>
 * 
 * <ul>
 *   <li><strong>Domain Service</strong>: Gestiona datos de seguridad (correo, clave, teléfono, rol)</li>
 *   <li><strong>Gestion Perfil Service</strong>: Gestiona datos de perfil (apodo, biografía, redes sociales, etc.)</li>
 * </ul>
 * 
 * <p><strong>Endpoints disponibles:</strong>
 * <ul>
 *   <li>GET /api/v1/usuarios/{usuario} - Obtener datos completos del usuario</li>
 *   <li>PUT /api/v1/usuarios/{usuario} - Actualizar datos completos del usuario</li>
 *   <li>DELETE /api/v1/usuarios/{usuario} - Eliminar usuario completo del sistema</li>
 * </ul>
 * </p>
 * 
 * <p><strong>Control de acceso:</strong>
 * <ul>
 *   <li>Los usuarios CLIENTE solo pueden acceder a sus propios datos</li>
 *   <li>Los usuarios ADMIN pueden acceder a cualquier usuario</li>
 *   <li>Todos los endpoints requieren autenticación mediante token JWT</li>
 * </ul>
 * </p>
 */
@Tag(
    name = "Gestión de Usuarios",
    description = "Endpoints para obtener, actualizar y eliminar usuarios completos. " +
                 "Orquesta llamadas a Domain Service y Gestion Perfil Service. " +
                 "Todos los endpoints requieren autenticación mediante token JWT."
)
@RestController
@RequestMapping("/api/v1/usuarios")
public class UsuarioController {

    private static final Logger log = LoggerFactory.getLogger(UsuarioController.class);
    private final UsuarioUnificadoService usuarioUnificadoService;
    private final DomainServiceClient domainServiceClient;
    private final GestionPerfilServiceClient gestionPerfilServiceClient;
    private final EventoPublisher eventoPublisher;

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

    /**
     * Obtiene los datos completos de un usuario.
     * 
     * <p>Este endpoint orquesta llamadas a dos microservicios para obtener:
     * <ul>
     *   <li><strong>Datos de seguridad</strong> (Domain Service): usuario, correo, teléfono, rol</li>
     *   <li><strong>Datos de perfil</strong> (Gestion Perfil Service): apodo, biografía, redes sociales, etc.</li>
     * </ul>
     * </p>
     * 
     * <p>Los datos se obtienen en paralelo y se combinan en una única respuesta.
     * Si alguno de los servicios falla, se retorna un mapa vacío para ese servicio.</p>
     * 
     * <p><strong>Control de acceso:</strong>
     * <ul>
     *   <li>CLIENTE: Solo puede acceder a sus propios datos</li>
     *   <li>ADMIN: Puede acceder a cualquier usuario</li>
     * </ul>
     * </p>
     * 
     * @param usuario Nombre de usuario a consultar
     * @param authToken Token JWT en el header Authorization
     * @return Respuesta con datos de seguridad y perfil combinados
     */
    @Operation(
        summary = "Obtener usuario completo",
        description = "Obtiene los datos completos de un usuario combinando información de seguridad " +
                     "(Domain Service) y perfil (Gestion Perfil Service). " +
                     "Los datos se obtienen en paralelo y se combinan en una única respuesta.",
        security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Usuario encontrado exitosamente",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = "{\"usuario\": \"john_doe\", \"datosSeguridad\": {\"correo\": \"john@example.com\", \"rol\": \"CLIENTE\"}, \"perfil\": {\"apodo\": \"John\", \"biografia\": \"Desarrollador\"}}"
                )
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Token de autenticación requerido, inválido o expirado",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = "{\"error\": true, \"respuesta\": \"Token de autenticación requerido\"}"
                )
            )
        ),
        @ApiResponse(
            responseCode = "403",
            description = "No tiene permisos para acceder a los datos de este usuario",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = "{\"error\": true, \"respuesta\": \"No tiene permisos para acceder a los datos de otro usuario\"}"
                )
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Usuario no encontrado",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = "{\"error\": true, \"respuesta\": \"Usuario no encontrado en el sistema\"}"
                )
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Error interno del servidor",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = "{\"error\": true, \"respuesta\": \"Error obteniendo datos del usuario\"}"
                )
            )
        )
    })
    @GetMapping("/{usuario}")
    public Mono<ResponseEntity<Map<String, Object>>> obtenerUsuarioCompleto(
            @Parameter(description = "Nombre de usuario a consultar", required = true, example = "john_doe")
            @PathVariable String usuario,
            @Parameter(hidden = true)
            @RequestHeader(value = "Authorization", required = false) String authToken) {
        log.info("API Gateway: Obteniendo datos completos del usuario {}", usuario);

        if (authToken == null || !authToken.startsWith("Bearer ")) {
            return Mono.just(ResponseEntity.status(401)
                    .body(Map.of("error", true, "respuesta", "Token de autenticación requerido")));
        }

        String token = authToken.replace("Bearer ", "").trim();
        
        Mono<Map<String, Object>> datosSeguridad = domainServiceClient
                .obtenerUsuario(usuario, token)
                .onErrorResume(error -> {
                    if (error instanceof org.springframework.web.reactive.function.client.WebClientResponseException) {
                        org.springframework.web.reactive.function.client.WebClientResponseException webClientError = 
                            (org.springframework.web.reactive.function.client.WebClientResponseException) error;
                        log.warn("Error obteniendo datos de seguridad: {} {}", 
                            webClientError.getStatusCode(), error.getMessage());
                        return Mono.error(error);
                    }
                    log.warn("Error obteniendo datos de seguridad: {}", error.getMessage());
                    return Mono.just(new HashMap<>());
                });
        
        Mono<Map<String, Object>> datosPerfil = gestionPerfilServiceClient
                .obtenerPerfil(usuario)
                .onErrorResume(error -> {
                    log.warn("Error obteniendo perfil: {}", error.getMessage());
                    return Mono.just(new HashMap<>());
                });

        return Mono.zip(datosSeguridad, datosPerfil)
                .map(tuple -> {
                    Map<String, Object> resultado = new HashMap<>();
                    resultado.put("usuario", usuario);
                    
                    Map<String, Object> seguridad = tuple.getT1();
                    if (!seguridad.isEmpty()) {
                        Object respuesta = seguridad.get("respuesta");
                        if (respuesta instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> datosUsuario = (Map<String, Object>) respuesta;
                            resultado.put("datosSeguridad", datosUsuario);
                        } else {
                            resultado.put("datosSeguridad", seguridad);
                        }
                    }
                    
                    Map<String, Object> perfil = tuple.getT2();
                    if (!perfil.isEmpty()) {
                        resultado.put("perfil", perfil);
                    }
                    
                    return ResponseEntity.ok(resultado);
                })
                .onErrorResume(error -> {
                    log.error("Error obteniendo usuario completo: {}", error.getMessage());
                    if (error instanceof org.springframework.web.reactive.function.client.WebClientResponseException) {
                        org.springframework.web.reactive.function.client.WebClientResponseException webClientError = 
                            (org.springframework.web.reactive.function.client.WebClientResponseException) error;
                        int statusCode = webClientError.getStatusCode().value();
                        
                        String errorMessage = "Error obteniendo datos del usuario";
                        try {
                            String responseBody = webClientError.getResponseBodyAsString();
                            if (responseBody != null && responseBody.contains("\"respuesta\"")) {
                                var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                                var errorMap = mapper.readValue(responseBody, Map.class);
                                if (errorMap.containsKey("respuesta")) {
                                    errorMessage = errorMap.get("respuesta").toString();
                                }
                            }
                        } catch (Exception e) {
                            log.warn("No se pudo parsear el mensaje de error: {}", e.getMessage());
                        }
                        
                        return Mono.just(ResponseEntity.status(statusCode)
                                .body(Map.of("error", true, "respuesta", errorMessage)));
                    }
                    return Mono.just(ResponseEntity.status(500)
                            .body(Map.of("error", true, "respuesta", "Error obteniendo datos del usuario")));
                });
    }

    /**
     * Actualiza los datos completos de un usuario.
     * 
     * <p>Este endpoint actualiza datos en múltiples microservicios según el contenido del request:
     * <ul>
     *   <li><strong>Datos de seguridad</strong> (Domain Service): correo, clave, numeroTelefono</li>
     *   <li><strong>Datos de perfil</strong> (Gestion Perfil Service): apodo, biografía, redes sociales, etc.</li>
     * </ul>
     * </p>
     * 
     * <p><strong>Validación de permisos:</strong>
     * <ul>
     *   <li>Si se actualizan datos de seguridad: El Domain Service valida permisos automáticamente</li>
     *   <li>Si solo se actualiza el perfil: El API Gateway valida permisos previamente</li>
     * </ul>
     * </p>
     * 
     * <p><strong>Control de acceso:</strong>
     * <ul>
     *   <li>CLIENTE: Solo puede actualizar sus propios datos</li>
     *   <li>ADMIN: Puede actualizar cualquier usuario</li>
     * </ul>
     * </p>
     * 
     * @param usuario Nombre de usuario a actualizar
     * @param requestBody Datos a actualizar (puede incluir campos de seguridad y/o perfil)
     * @param authToken Token JWT en el header Authorization
     * @return Respuesta con el resultado de la actualización
     */
    @Operation(
        summary = "Actualizar usuario completo",
        description = "Actualiza los datos de un usuario en los microservicios correspondientes. " +
                     "Los datos de seguridad se actualizan en el Domain Service y los datos de perfil " +
                     "en el Gestion Perfil Service. Si solo se actualiza el perfil, se valida " +
                     "previamente que el usuario tenga permisos.",
        security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Usuario actualizado exitosamente",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = "{\"mensaje\": \"Usuario actualizado exitosamente\", \"datosSeguridad\": {}, \"datosPerfil\": {}}"
                )
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Token de autenticación requerido, inválido o expirado",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = "{\"error\": true, \"respuesta\": \"Token de autenticación requerido\"}"
                )
            )
        ),
        @ApiResponse(
            responseCode = "403",
            description = "No tiene permisos para actualizar este usuario",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = "{\"error\": true, \"respuesta\": \"No tiene permisos para acceder a los datos de otro usuario\"}"
                )
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Usuario no encontrado",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = "{\"error\": true, \"respuesta\": \"Usuario no encontrado en el sistema\"}"
                )
            )
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Conflicto - El correo electrónico ya está en uso",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = "{\"error\": true, \"respuesta\": \"El correo electrónico ya está en uso por otro usuario\"}"
                )
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Error interno del servidor",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = "{\"error\": true, \"respuesta\": \"Error actualizando datos del usuario\"}"
                )
            )
        )
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        description = "Datos a actualizar. Puede incluir campos de seguridad (correo, clave, numeroTelefono) " +
                     "y/o campos de perfil (apodo, biografia, linkGithub, etc.). " +
                     "Solo se actualizarán los campos proporcionados.",
        required = true,
        content = @Content(
            mediaType = "application/json",
            examples = {
                @ExampleObject(
                    name = "Actualizar solo datos de seguridad",
                    value = "{\"correo\": \"newemail@example.com\", \"numeroTelefono\": \"+573001234567\"}"
                ),
                @ExampleObject(
                    name = "Actualizar solo perfil",
                    value = "{\"apodo\": \"Mi Apodo\", \"biografia\": \"Desarrollador de software\"}"
                ),
                @ExampleObject(
                    name = "Actualizar ambos",
                    value = "{\"correo\": \"newemail@example.com\", \"apodo\": \"Mi Apodo\", \"linkGithub\": \"https://github.com/user\"}"
                )
            }
        )
    )
    @PutMapping("/{usuario}")
    public Mono<ResponseEntity<Map<String, Object>>> actualizarUsuarioCompleto(
            @Parameter(description = "Nombre de usuario a actualizar", required = true, example = "john_doe")
            @PathVariable String usuario,
            @RequestBody Map<String, Object> requestBody,
            @Parameter(hidden = true)
            @RequestHeader(value = "Authorization", required = false) String authToken) {
        log.info("API Gateway: Actualizando datos completos del usuario {}", usuario);

        if (authToken == null || !authToken.startsWith("Bearer ")) {
            return Mono.just(ResponseEntity.status(401)
                    .body(Map.of("error", true, "respuesta", "Token de autenticación requerido")));
        }

        String token = authToken.substring(7);

        boolean soloPerfil = !requestBody.containsKey("correo") && 
                            !requestBody.containsKey("clave") && 
                            !requestBody.containsKey("numeroTelefono");
        
        if (soloPerfil) {
            log.info("Validando permisos para actualizar perfil de usuario: {}", usuario);
            return domainServiceClient.obtenerUsuario(usuario, token)
                    .doOnError(error -> log.error("Error validando permisos: {}", error.getMessage()))
                    .flatMap(usuarioData -> {
                        log.info("Permisos validados, procediendo a actualizar perfil");
                        return usuarioUnificadoService.actualizarUsuarioCompleto(usuario, requestBody, token)
                                .map(ResponseEntity::ok);
                    })
                    .onErrorResume(error -> {
                        log.error("Error en validación de permisos o actualización: {}", error.getMessage());
                        if (error instanceof org.springframework.web.reactive.function.client.WebClientResponseException) {
                            org.springframework.web.reactive.function.client.WebClientResponseException webClientError = 
                                (org.springframework.web.reactive.function.client.WebClientResponseException) error;
                            int statusCode = webClientError.getStatusCode().value();
                            
                            String errorMessage = "Error actualizando datos del usuario";
                            try {
                                String responseBody = webClientError.getResponseBodyAsString();
                                if (responseBody != null && responseBody.contains("\"respuesta\"")) {
                                    var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                                    var errorMap = mapper.readValue(responseBody, Map.class);
                                    if (errorMap.containsKey("respuesta")) {
                                        errorMessage = errorMap.get("respuesta").toString();
                                    }
                                }
                            } catch (Exception e) {
                                log.warn("No se pudo parsear el mensaje de error: {}", e.getMessage());
                            }
                            
                            return Mono.just(ResponseEntity.status(statusCode)
                                    .body(Map.of("error", true, "respuesta", errorMessage)));
                        }
                        return Mono.just(ResponseEntity.status(500)
                                .body(Map.of("error", true, "respuesta", "Error actualizando datos del usuario")));
                    });
        }

        return usuarioUnificadoService.actualizarUsuarioCompleto(usuario, requestBody, token)
                .map(ResponseEntity::ok)
                .onErrorResume(error -> {
                    log.error("Error actualizando usuario completo: {}", error.getMessage());
                    if (error instanceof org.springframework.web.reactive.function.client.WebClientResponseException) {
                        org.springframework.web.reactive.function.client.WebClientResponseException webClientError = 
                            (org.springframework.web.reactive.function.client.WebClientResponseException) error;
                        int statusCode = webClientError.getStatusCode().value();
                        
                        String errorMessage = "Error actualizando datos del usuario";
                        try {
                            String responseBody = webClientError.getResponseBodyAsString();
                            if (responseBody != null && responseBody.contains("\"respuesta\"")) {
                                var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                                var errorMap = mapper.readValue(responseBody, Map.class);
                                if (errorMap.containsKey("respuesta")) {
                                    errorMessage = errorMap.get("respuesta").toString();
                                }
                            }
                        } catch (Exception e) {
                            log.warn("No se pudo parsear el mensaje de error: {}", e.getMessage());
                        }
                        
                        return Mono.just(ResponseEntity.status(statusCode)
                                .body(Map.of("error", true, "respuesta", errorMessage)));
                    }
                    return Mono.just(ResponseEntity.status(500)
                            .body(Map.of("error", true, "respuesta", "Error actualizando datos del usuario")));
                });
    }

    /**
     * Elimina un usuario completo del sistema.
     * 
     * <p>Este endpoint elimina un usuario de todos los microservicios:
     * <ol>
     *   <li>Elimina datos de seguridad del Domain Service (obligatorio)</li>
     *   <li>Elimina datos de perfil del Gestion Perfil Service (opcional, 404 no es crítico)</li>
     *   <li>Publica evento de eliminación en RabbitMQ</li>
     * </ol>
     * </p>
     * 
     * <p>Si la eliminación del Domain Service es exitosa pero el perfil no existe (404),
     * la operación se considera exitosa ya que el usuario principal fue eliminado.</p>
     * 
     * <p><strong>Control de acceso:</strong>
     * <ul>
     *   <li>ADMIN: Puede eliminar cualquier usuario</li>
     *   <li>CLIENTE: Solo puede eliminar su propia cuenta</li>
     * </ul>
     * </p>
     * 
     * @param usuario Nombre de usuario a eliminar
     * @param authToken Token JWT en el header Authorization
     * @return Respuesta con el resultado de la eliminación
     */
    @Operation(
        summary = "Eliminar usuario completo",
        description = "Elimina un usuario de todos los microservicios del sistema. " +
                     "Primero elimina los datos de seguridad (Domain Service) y luego " +
                     "los datos de perfil (Gestion Perfil Service). " +
                     "Si el perfil no existe, la operación se considera exitosa. " +
                     "Publica un evento de eliminación en RabbitMQ al finalizar.",
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
            description = "Token de autenticación requerido, inválido o expirado",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = "{\"error\": true, \"respuesta\": \"Token de autenticación requerido\"}"
                )
            )
        ),
        @ApiResponse(
            responseCode = "403",
            description = "No tiene permisos para eliminar este usuario",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = "{\"error\": true, \"respuesta\": \"El rol del token no es válido para esta operación\"}"
                )
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Usuario no encontrado en el Domain Service",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = "{\"error\": true, \"respuesta\": \"Usuario no encontrado en el sistema\"}"
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
    @DeleteMapping("/{usuario}")
    public Mono<ResponseEntity<Map<String, Object>>> eliminarUsuarioCompleto(
            @Parameter(description = "Nombre de usuario a eliminar", required = true, example = "john_doe")
            @PathVariable String usuario,
            @Parameter(hidden = true)
            @RequestHeader(value = "Authorization", required = false) String authToken) {
        log.info("API Gateway: Eliminación completa del usuario {}", usuario);

        if (authToken == null || !authToken.startsWith("Bearer ")) {
            return Mono.just(ResponseEntity.status(401)
                    .body(Map.of("error", true, "respuesta", "Token de autenticación requerido")));
        }

        String token = authToken.substring(7);

        return domainServiceClient
                .eliminarUsuario(usuario, token)
                .doOnSuccess(response -> log.info("Eliminación de seguridad exitosa para usuario: {}", usuario))
                .doOnError(error -> log.error("Error en eliminación de seguridad: {}", error.getMessage()))
                .flatMap(seguridadResponse -> {
                    log.info("Procediendo a eliminar perfil para usuario: {}", usuario);
                    return gestionPerfilServiceClient
                            .eliminarPerfil(usuario)
                            .onErrorResume(error -> {
                                if (error instanceof org.springframework.web.reactive.function.client.WebClientResponseException) {
                                    org.springframework.web.reactive.function.client.WebClientResponseException webClientError = 
                                        (org.springframework.web.reactive.function.client.WebClientResponseException) error;
                                    if (webClientError.getStatusCode().value() == 404) {
                                        log.info("Perfil no encontrado para usuario {} (no crítico)", usuario);
                                        return Mono.empty();
                                    }
                                }
                                log.warn("Error eliminando perfil (no crítico): {}", error.getMessage());
                                return Mono.empty();
                            })
                            .then(Mono.fromRunnable(() -> {
                                try {
                                    String correo = extractCorreo(seguridadResponse);
                                    eventoPublisher.publicarEventoEliminacion(usuario, correo);
                                    log.info("Evento de eliminación publicado para usuario: {}", usuario);
                                } catch (Exception e) {
                                    log.error("Error publicando evento de eliminación: {}", e.getMessage(), e);
                                }
                            }))
                            .then(Mono.just(ResponseEntity.ok(createResponseMap(false, "Usuario eliminado exitosamente del sistema"))));
                })
                .onErrorResume(error -> {
                    log.error("Error eliminando usuario completo: {}", error.getMessage());
                    
                    if (error instanceof org.springframework.web.reactive.function.client.WebClientResponseException) {
                        org.springframework.web.reactive.function.client.WebClientResponseException webClientError = 
                            (org.springframework.web.reactive.function.client.WebClientResponseException) error;
                        int statusCode = webClientError.getStatusCode().value();
                        
                        if (statusCode == 404) {
                            try {
                                String responseBody = webClientError.getResponseBodyAsString();
                                String requestUri = webClientError.getRequest().getURI().toString();
                                if (responseBody != null && (responseBody.contains("Perfil no encontrado") || 
                                    responseBody.contains("perfil") && requestUri.contains("perfiles"))) {
                                    log.info("Perfil no encontrado (404) - usuario ya eliminado del Domain Service");
                                    try {
                                        eventoPublisher.publicarEventoEliminacion(usuario, "");
                                    } catch (Exception e) {
                                        log.error("Error publicando evento: {}", e.getMessage());
                                    }
                                    return Mono.just(ResponseEntity.ok(createResponseMap(false, "Usuario eliminado exitosamente del sistema")));
                                }
                            } catch (Exception e) {
                                log.warn("No se pudo verificar el mensaje de error: {}", e.getMessage());
                            }
                        }
                        
                        String errorMessage = "Error eliminando usuario";
                        try {
                            String responseBody = webClientError.getResponseBodyAsString();
                            if (responseBody != null && responseBody.contains("\"respuesta\"")) {
                                var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                                var errorMap = mapper.readValue(responseBody, Map.class);
                                if (errorMap.containsKey("respuesta")) {
                                    errorMessage = errorMap.get("respuesta").toString();
                                }
                            }
                        } catch (Exception e) {
                            log.warn("No se pudo parsear el mensaje de error: {}", e.getMessage());
                        }
                        
                        return Mono.just(ResponseEntity.status(statusCode)
                                .body(createResponseMap(true, errorMessage)));
                    }
                    
                    try {
                        eventoPublisher.publicarEventoEliminacion(usuario, "");
                    } catch (Exception e) {
                        log.error("Error publicando evento después de error: {}", e.getMessage());
                    }
                    return Mono.just(ResponseEntity.status(500)
                            .body(createResponseMap(true, "Error eliminando usuario")));
                });
    }

    /**
     * Crea un mapa de respuesta estándar con formato de error.
     * 
     * @param error Indica si la respuesta es un error
     * @param respuesta Mensaje de respuesta
     * @return Mapa con estructura estándar de respuesta
     */
    private Map<String, Object> createResponseMap(boolean error, String respuesta) {
        Map<String, Object> map = new HashMap<>();
        map.put("error", error);
        map.put("respuesta", respuesta);
        return map;
    }

    /**
     * Extrae el correo electrónico de los datos del usuario.
     * 
     * <p>Intenta extraer el correo de diferentes estructuras de respuesta
     * del Domain Service.</p>
     * 
     * @param usuarioData Datos del usuario que pueden contener el correo
     * @return Correo electrónico extraído o cadena vacía si no se encuentra
     */
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

