package com.uniquindio.archmicroserv.apigateway.integration;

import com.uniquindio.archmicroserv.apigateway.controller.AuthController;
import com.uniquindio.archmicroserv.apigateway.service.DomainServiceClient;
import com.uniquindio.archmicroserv.apigateway.service.GestionPerfilServiceClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@WebFluxTest(controllers = AuthController.class)
@ActiveProfiles("test")
@DisplayName("Tests de integración para AuthController")
class AuthControllerIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private DomainServiceClient domainServiceClient;

    @MockBean
    private GestionPerfilServiceClient gestionPerfilServiceClient;

    private Map<String, Object> registroRequest;
    private Map<String, Object> loginRequest;
    private Map<String, Object> successResponse;

    @BeforeEach
    void setUp() {
        registroRequest = new HashMap<>();
        registroRequest.put("usuario", "testuser");
        registroRequest.put("correo", "test@example.com");
        registroRequest.put("clave", "password123");

        loginRequest = new HashMap<>();
        loginRequest.put("usuario", "testuser");
        loginRequest.put("clave", "password123");

        successResponse = new HashMap<>();
        successResponse.put("error", false);
        successResponse.put("respuesta", "Operación exitosa");
    }

    @Test
    @DisplayName("POST /api/v1/auth/registro - Camino feliz")
    void testRegistro_Success() {
        // Given
        when(domainServiceClient.registrarUsuario(any()))
                .thenReturn(Mono.just(successResponse));

        // When & Then
        webTestClient.post()
                .uri("/api/v1/auth/registro")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(registroRequest)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.error").isEqualTo(false)
                .jsonPath("$.respuesta").exists();
    }

    @Test
    @DisplayName("POST /api/v1/auth/registro - Error en servicio")
    void testRegistro_ServiceError() {
        // Given
        when(domainServiceClient.registrarUsuario(any()))
                .thenReturn(Mono.error(new RuntimeException("Error de conexión")));

        // When & Then
        webTestClient.post()
                .uri("/api/v1/auth/registro")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(registroRequest)
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody()
                .jsonPath("$.error").isEqualTo(true)
                .jsonPath("$.respuesta").isEqualTo("Error procesando registro");
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - Camino feliz")
    void testLogin_Success() {
        // Given
        Map<String, Object> loginResponse = new HashMap<>();
        loginResponse.put("error", false);
        loginResponse.put("token", "jwt-token-123");
        
        when(domainServiceClient.autenticar(any()))
                .thenReturn(Mono.just(loginResponse));

        // When & Then
        webTestClient.post()
                .uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(loginRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.error").isEqualTo(false)
                .jsonPath("$.token").exists();
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - Credenciales inválidas")
    void testLogin_InvalidCredentials() {
        // Given
        when(domainServiceClient.autenticar(any()))
                .thenReturn(Mono.error(new RuntimeException("Credenciales inválidas")));

        // When & Then
        webTestClient.post()
                .uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(loginRequest)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.error").isEqualTo(true)
                .jsonPath("$.respuesta").isEqualTo("Credenciales inválidas");
    }

    @Test
    @DisplayName("DELETE /api/v1/auth/usuarios/{usuario} - Camino feliz")
    void testEliminarUsuario_Success() {
        // Given
        when(domainServiceClient.eliminarUsuario(anyString(), anyString()))
                .thenReturn(Mono.just(successResponse));

        // When & Then
        webTestClient.delete()
                .uri("/api/v1/auth/usuarios/testuser")
                .header("Authorization", "Bearer valid-token-123")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.error").isEqualTo(false);
    }

    @Test
    @DisplayName("DELETE /api/v1/auth/usuarios/{usuario} - Sin token")
    void testEliminarUsuario_NoToken() {
        // When & Then
        webTestClient.delete()
                .uri("/api/v1/auth/usuarios/testuser")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.error").isEqualTo(true)
                .jsonPath("$.respuesta").isEqualTo("Token de autenticación requerido");
    }

    @Test
    @DisplayName("DELETE /api/v1/auth/usuarios/{usuario} - Token inválido")
    void testEliminarUsuario_InvalidToken() {
        // When & Then
        webTestClient.delete()
                .uri("/api/v1/auth/usuarios/testuser")
                .header("Authorization", "InvalidTokenFormat")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.error").isEqualTo(true);
    }

    @Test
    @DisplayName("POST /api/v1/auth/registro - Request vacío")
    void testRegistro_EmptyRequest() {
        // Given
        when(domainServiceClient.registrarUsuario(any()))
                .thenReturn(Mono.just(successResponse));

        // When & Then
        webTestClient.post()
                .uri("/api/v1/auth/registro")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new HashMap<>())
                .exchange()
                .expectStatus().isCreated();
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - Content-Type incorrecto")
    void testLogin_WrongContentType() {
        // When & Then
        webTestClient.post()
                .uri("/api/v1/auth/login")
                .contentType(MediaType.TEXT_PLAIN)
                .bodyValue("plain text")
                .exchange()
                .expectStatus().is4xxClientError();
    }

    @Test
    @DisplayName("DELETE /api/v1/auth/usuarios/{usuario} - Error en servicio")
    void testEliminarUsuario_ServiceError() {
        // Given
        when(domainServiceClient.eliminarUsuario(anyString(), anyString()))
                .thenReturn(Mono.error(new RuntimeException("Error de base de datos")));

        // When & Then
        webTestClient.delete()
                .uri("/api/v1/auth/usuarios/testuser")
                .header("Authorization", "Bearer valid-token-123")
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody()
                .jsonPath("$.error").isEqualTo(true)
                .jsonPath("$.respuesta").isEqualTo("Error eliminando usuario");
    }
}

