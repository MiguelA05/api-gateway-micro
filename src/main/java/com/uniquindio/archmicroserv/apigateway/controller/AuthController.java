package com.uniquindio.archmicroserv.apigateway.controller;

import com.uniquindio.archmicroserv.apigateway.service.DomainServiceClient;
import com.uniquindio.archmicroserv.apigateway.service.GestionPerfilServiceClient;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Controller para operaciones de autenticación y registro de usuarios.
 * 
 * <p>Este controller actúa como punto de entrada para las operaciones de autenticación,
 * delegando las llamadas al Domain Service (jwtmanual-taller1-micro) que gestiona
 * la seguridad y generación de tokens JWT.</p>
 * 
 * <p>Endpoints disponibles:
 * <ul>
 *   <li>POST /api/v1/auth/registro - Registro de nuevos usuarios</li>
 *   <li>POST /api/v1/auth/login - Autenticación y obtención de token JWT</li>
 *   <li>DELETE /api/v1/auth/usuarios/{usuario} - Eliminación de usuarios (requiere autenticación)</li>
 * </ul>
 * </p>
 */
@Tag(
    name = "Autenticación",
    description = "Endpoints para registro, autenticación y gestión de usuarios. " +
                 "Las operaciones de eliminación requieren autenticación mediante token JWT."
)
@RestController
@RequestMapping("/api/v1")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private final DomainServiceClient domainServiceClient;
    private final GestionPerfilServiceClient gestionPerfilServiceClient;

    public AuthController(
            DomainServiceClient domainServiceClient,
            GestionPerfilServiceClient gestionPerfilServiceClient) {
        this.domainServiceClient = domainServiceClient;
        this.gestionPerfilServiceClient = gestionPerfilServiceClient;
    }

    /**
     * Registra un nuevo usuario en el sistema.
     * 
     * <p>Este endpoint crea un nuevo usuario con las credenciales proporcionadas.
     * El usuario se registra en el Domain Service con rol CLIENTE por defecto.</p>
     * 
     * <p>Si se proporcionan datos de perfil (apodo, biografía, redes sociales, etc.),
     * también se crea un perfil en el Gestion Perfil Service. Los campos de perfil son opcionales.</p>
     * 
     * <p><strong>Campos obligatorios:</strong>
     * <ul>
     *   <li>usuario: Nombre de usuario único</li>
     *   <li>correo: Correo electrónico único</li>
     *   <li>clave: Contraseña del usuario</li>
     *   <li>numeroTelefono: Número de teléfono</li>
     * </ul>
     * </p>
     * 
     * <p><strong>Campos opcionales de perfil:</strong>
     * <ul>
     *   <li>apodo: Apodo o nombre público del usuario</li>
     *   <li>biografia: Biografía del usuario</li>
     *   <li>urlPaginaPersonal: URL de página personal</li>
     *   <li>informacionContactoPublica: Si la información de contacto es pública (boolean)</li>
     *   <li>direccionCorrespondencia: Dirección postal</li>
     *   <li>organizacion: Organización a la que pertenece</li>
     *   <li>paisResidencia: País de residencia</li>
     *   <li>linkFacebook, linkTwitter, linkLinkedIn, linkInstagram, linkGithub, linkOtraRed: Links de redes sociales</li>
     * </ul>
     * </p>
     * 
     * @param requestBody Datos del usuario: usuario, correo, clave, numeroTelefono (obligatorios) y campos de perfil opcionales
     * @return Respuesta con el resultado del registro (201 si es exitoso, 409 si el usuario ya existe, 500 si hay error)
     */
    @Operation(
        summary = "Registrar nuevo usuario",
        description = "Crea un nuevo usuario en el sistema con credenciales de acceso. " +
                     "El usuario se registra con rol CLIENTE por defecto. " +
                     "Si se proporcionan datos de perfil (apodo, biografía, redes sociales, etc.), " +
                     "también se crea un perfil en el Gestion Perfil Service. " +
                     "Los campos de perfil son completamente opcionales."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Usuario creado exitosamente",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = "{\"error\": false, \"respuesta\": \"Usuario registrado exitosamente\"}"
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Datos inválidos o faltantes",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = "{\"error\": true, \"respuesta\": \"Atributos de usuario, correo y contraseña son obligatorios\"}"
                )
            )
        ),
        @ApiResponse(
            responseCode = "409",
            description = "El usuario o correo ya existe",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = "{\"error\": true, \"respuesta\": \"El usuario ya existe en el sistema\"}"
                )
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Error interno del servidor",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = "{\"error\": true, \"respuesta\": \"Error procesando registro\"}"
                )
            )
        )
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        description = "Datos del usuario a registrar. Los campos usuario, correo, clave y numeroTelefono son obligatorios. " +
                     "Los campos de perfil (apodo, biografia, redes sociales, etc.) son opcionales.",
        required = true,
        content = @Content(
            mediaType = "application/json",
            examples = {
                @ExampleObject(
                    name = "Registro básico (solo datos de seguridad)",
                    value = "{\"usuario\": \"john_doe\", \"correo\": \"john@example.com\", \"clave\": \"password123\", \"numeroTelefono\": \"+573001234567\"}"
                ),
                @ExampleObject(
                    name = "Registro completo (con datos de perfil)",
                    value = "{\"usuario\": \"john_doe\", \"correo\": \"john@example.com\", \"clave\": \"password123\", \"numeroTelefono\": \"+573001234567\", \"apodo\": \"John\", \"biografia\": \"Desarrollador de software\", \"linkGithub\": \"https://github.com/johndoe\"}"
                )
            }
        )
    )
    @PostMapping("/auth/registro")
    public Mono<ResponseEntity<Map<String, Object>>> registrarUsuario(
            @RequestBody Map<String, Object> requestBody) {
        log.info("API Gateway: Registro de usuario");
        
        // Extraer datos de seguridad (obligatorios)
        Map<String, Object> datosSeguridad = new java.util.HashMap<>();
        datosSeguridad.put("usuario", requestBody.get("usuario"));
        datosSeguridad.put("correo", requestBody.get("correo"));
        datosSeguridad.put("clave", requestBody.get("clave"));
        datosSeguridad.put("numeroTelefono", requestBody.get("numeroTelefono"));
        
        // Extraer datos de perfil (opcionales)
        Map<String, Object> datosPerfil = new java.util.HashMap<>();
        String[] camposPerfil = {
            "apodo", "biografia", "urlPaginaPersonal", "informacionContactoPublica",
            "direccionCorrespondencia", "organizacion", "paisResidencia",
            "linkFacebook", "linkTwitter", "linkLinkedIn", "linkInstagram", "linkGithub", "linkOtraRed"
        };
        for (String campo : camposPerfil) {
            if (requestBody.containsKey(campo)) {
                datosPerfil.put(campo, requestBody.get(campo));
            }
        }
        
        String usuario = (String) requestBody.get("usuario");
        
        // Primero registrar en Domain Service
        return domainServiceClient.registrarUsuario(datosSeguridad)
                .flatMap(seguridadResponse -> {
                    // Si el registro fue exitoso y hay datos de perfil, crear el perfil
                    if (!datosPerfil.isEmpty() && usuario != null) {
                        log.info("Creando perfil para usuario: {}", usuario);
                        return gestionPerfilServiceClient.crearPerfil(usuario, datosPerfil)
                                .map(perfilResponse -> {
                                    Map<String, Object> respuesta = new java.util.HashMap<>();
                                    respuesta.put("error", false);
                                    respuesta.put("respuesta", "Usuario y perfil registrados exitosamente");
                                    respuesta.put("datosSeguridad", seguridadResponse);
                                    respuesta.put("datosPerfil", perfilResponse);
                                    return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
                                })
                                .onErrorResume(perfilError -> {
                                    log.warn("Usuario registrado pero error creando perfil: {}", perfilError.getMessage());
                                    // El usuario ya está registrado, pero el perfil falló
                                    // Retornamos éxito con advertencia
                                    Map<String, Object> respuesta = new java.util.HashMap<>();
                                    respuesta.put("error", false);
                                    respuesta.put("respuesta", "Usuario registrado exitosamente. El perfil se puede crear posteriormente.");
                                    respuesta.put("datosSeguridad", seguridadResponse);
                                    return Mono.just(ResponseEntity.status(HttpStatus.CREATED).body(respuesta));
                                });
                    } else {
                        // Solo registro de seguridad, sin perfil
                        return Mono.just(ResponseEntity.status(HttpStatus.CREATED).body(seguridadResponse));
                    }
                })
                .onErrorResume(error -> {
                    log.error("Error en registro: {}", error.getMessage());
                    
                    if (error instanceof org.springframework.web.reactive.function.client.WebClientResponseException) {
                        org.springframework.web.reactive.function.client.WebClientResponseException webClientError = 
                            (org.springframework.web.reactive.function.client.WebClientResponseException) error;
                        int statusCode = webClientError.getStatusCode().value();
                        
                        String errorMessage = "Error procesando registro";
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
                    
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(Map.of("error", true, "respuesta", "Error procesando registro")));
                });
    }

    /**
     * Autentica un usuario y genera un token JWT.
     * 
     * <p>Este endpoint valida las credenciales del usuario y retorna un token JWT
     * que debe ser incluido en el header Authorization de las peticiones protegidas.</p>
     * 
     * <p>El token contiene información del usuario (usuario, correo, rol) y tiene
     * una validez configurada en el Domain Service.</p>
     * 
     * @param requestBody Credenciales: usuario y clave
     * @return Respuesta con el token JWT (200 si es exitoso, 401 si las credenciales son inválidas)
     */
    @Operation(
        summary = "Autenticar usuario",
        description = "Valida las credenciales del usuario y retorna un token JWT. " +
                     "El token debe incluirse en el header 'Authorization: Bearer {token}' " +
                     "para acceder a endpoints protegidos."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Autenticación exitosa",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = "{\"error\": false, \"respuesta\": {\"token\": \"eyJhbGciOiJIUzM4NCJ9...\"}}"
                )
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Credenciales inválidas o usuario no encontrado",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = "{\"error\": true, \"respuesta\": \"Credenciales inválidas\"}"
                )
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Error interno del servidor",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = "{\"error\": true, \"respuesta\": \"Error en autenticación\"}"
                )
            )
        )
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        description = "Credenciales de autenticación. Ambos campos son obligatorios.",
        required = true,
        content = @Content(
            mediaType = "application/json",
            examples = @ExampleObject(
                name = "Ejemplo de login",
                value = "{\"usuario\": \"john_doe\", \"clave\": \"password123\"}"
            )
        )
    )
    @PostMapping("/auth/login")
    public Mono<ResponseEntity<Map<String, Object>>> autenticar(
            @RequestBody Map<String, Object> requestBody) {
        log.info("API Gateway: Autenticación de usuario");
        return domainServiceClient.autenticar(requestBody)
                .map(ResponseEntity::ok)
                .onErrorResume(error -> {
                    log.error("Error en autenticación: {}", error.getMessage());
                    
                    if (error instanceof org.springframework.web.reactive.function.client.WebClientResponseException) {
                        org.springframework.web.reactive.function.client.WebClientResponseException webClientError = 
                            (org.springframework.web.reactive.function.client.WebClientResponseException) error;
                        int statusCode = webClientError.getStatusCode().value();
                        
                        String errorMessage = "Credenciales inválidas";
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
                    
                    return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body(Map.of("error", true, "respuesta", "Credenciales inválidas")));
                });
    }

    /**
     * Elimina un usuario del sistema.
     * 
     * <p>Este endpoint elimina un usuario del Domain Service. Solo los usuarios con rol ADMIN
     * pueden eliminar usuarios. Los usuarios CLIENTE solo pueden eliminar su propia cuenta.</p>
     * 
     * <p><strong>Permisos requeridos:</strong>
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
        summary = "Eliminar usuario",
        description = "Elimina un usuario del sistema. Requiere autenticación mediante token JWT. " +
                     "Solo usuarios ADMIN pueden eliminar otros usuarios. " +
                     "Los usuarios CLIENTE solo pueden eliminar su propia cuenta.",
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
                    value = "{\"error\": true, \"respuesta\": \"Error eliminando usuario\"}"
                )
            )
        )
    })
    @DeleteMapping("/auth/usuarios/{usuario}")
    public Mono<ResponseEntity<Map<String, Object>>> eliminarUsuario(
            @Parameter(description = "Nombre de usuario a eliminar", required = true, example = "john_doe")
            @PathVariable String usuario,
            @Parameter(hidden = true)
            @RequestHeader(value = "Authorization", required = false) String authToken) {
        log.info("API Gateway: Eliminación de usuario {}", usuario);
        
        if (authToken == null || !authToken.startsWith("Bearer ")) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", true, "respuesta", "Token de autenticación requerido")));
        }

        String token = authToken.substring(7);

        return domainServiceClient.eliminarUsuario(usuario, token)
                .map(ResponseEntity::ok)
                .onErrorResume(error -> {
                    log.error("Error eliminando usuario: {}", error.getMessage());
                    
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
                    
                    if (error.getMessage() != null && error.getMessage().contains("403")) {
                        return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .body(Map.of("error", true, "respuesta", "No tiene permisos para eliminar usuarios")));
                    }
                    if (error.getMessage() != null && error.getMessage().contains("404")) {
                        return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(Map.of("error", true, "respuesta", "Usuario no encontrado")));
                    }
                    
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(Map.of("error", true, "respuesta", "Error eliminando usuario")));
                });
    }
}

