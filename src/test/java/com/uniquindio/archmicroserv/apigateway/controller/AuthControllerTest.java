package com.uniquindio.archmicroserv.apigateway.controller;

import com.uniquindio.archmicroserv.apigateway.service.DomainServiceClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests unitarios para AuthController")
class AuthControllerTest {

    @Mock
    private DomainServiceClient domainServiceClient;

    @InjectMocks
    private AuthController authController;

    private Map<String, Object> registroRequest;
    private Map<String, Object> loginRequest;
    private Map<String, Object> successResponse;

    @BeforeEach
    void setUp() {
        // Datos de prueba para registro
        registroRequest = new HashMap<>();
        registroRequest.put("usuario", "testuser");
        registroRequest.put("correo", "test@example.com");
        registroRequest.put("clave", "password123");

        // Datos de prueba para login
        loginRequest = new HashMap<>();
        loginRequest.put("usuario", "testuser");
        loginRequest.put("clave", "password123");

        // Respuesta exitosa simulada
        successResponse = new HashMap<>();
        successResponse.put("error", false);
        successResponse.put("respuesta", "Operación exitosa");
    }

    @Test
    @DisplayName("Registro de usuario - Camino feliz")
    void testRegistrarUsuario_Success() {
        // Given
        when(domainServiceClient.registrarUsuario(anyMap()))
                .thenReturn(Mono.just(successResponse));

        // When
        Mono<ResponseEntity<Map<String, Object>>> result = 
                authController.registrarUsuario(registroRequest);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(HttpStatus.CREATED, response.getStatusCode());
                    assertNotNull(response.getBody());
                    assertEquals(false, response.getBody().get("error"));
                })
                .verifyComplete();

        verify(domainServiceClient, times(1)).registrarUsuario(registroRequest);
    }

    @Test
    @DisplayName("Registro de usuario - Error en el servicio")
    void testRegistrarUsuario_ServiceError() {
        // Given
        when(domainServiceClient.registrarUsuario(anyMap()))
                .thenReturn(Mono.error(new RuntimeException("Error de conexión")));

        // When
        Mono<ResponseEntity<Map<String, Object>>> result = 
                authController.registrarUsuario(registroRequest);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
                    assertNotNull(response.getBody());
                    assertTrue((Boolean) response.getBody().get("error"));
                    assertEquals("Error procesando registro", response.getBody().get("respuesta"));
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Login de usuario - Camino feliz")
    void testAutenticar_Success() {
        // Given
        Map<String, Object> loginResponse = new HashMap<>();
        loginResponse.put("error", false);
        loginResponse.put("token", "jwt-token-123");
        
        when(domainServiceClient.autenticar(anyMap()))
                .thenReturn(Mono.just(loginResponse));

        // When
        Mono<ResponseEntity<Map<String, Object>>> result = 
                authController.autenticar(loginRequest);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(HttpStatus.OK, response.getStatusCode());
                    assertNotNull(response.getBody());
                    assertTrue(response.getBody().containsKey("token"));
                })
                .verifyComplete();

        verify(domainServiceClient, times(1)).autenticar(loginRequest);
    }

    @Test
    @DisplayName("Login de usuario - Credenciales inválidas")
    void testAutenticar_InvalidCredentials() {
        // Given
        when(domainServiceClient.autenticar(anyMap()))
                .thenReturn(Mono.error(new RuntimeException("Credenciales inválidas")));

        // When
        Mono<ResponseEntity<Map<String, Object>>> result = 
                authController.autenticar(loginRequest);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
                    assertNotNull(response.getBody());
                    assertTrue((Boolean) response.getBody().get("error"));
                    assertEquals("Credenciales inválidas", response.getBody().get("respuesta"));
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Eliminar usuario - Camino feliz")
    void testEliminarUsuario_Success() {
        // Given
        String usuario = "testuser";
        String token = "Bearer valid-token-123";
        
        when(domainServiceClient.eliminarUsuario(eq(usuario), anyString()))
                .thenReturn(Mono.just(successResponse));

        // When
        Mono<ResponseEntity<Map<String, Object>>> result = 
                authController.eliminarUsuario(usuario, token);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(HttpStatus.OK, response.getStatusCode());
                    assertNotNull(response.getBody());
                    assertEquals(false, response.getBody().get("error"));
                })
                .verifyComplete();

        verify(domainServiceClient, times(1)).eliminarUsuario(eq(usuario), eq("valid-token-123"));
    }

    @Test
    @DisplayName("Eliminar usuario - Sin token de autorización")
    void testEliminarUsuario_NoAuthToken() {
        // Given
        String usuario = "testuser";

        // When - Without token
        Mono<ResponseEntity<Map<String, Object>>> result1 = 
                authController.eliminarUsuario(usuario, null);

        // Then
        StepVerifier.create(result1)
                .assertNext(response -> {
                    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
                    assertNotNull(response.getBody());
                    assertTrue((Boolean) response.getBody().get("error"));
                    assertEquals("Token de autenticación requerido", response.getBody().get("respuesta"));
                })
                .verifyComplete();

        // When - With invalid token format
        Mono<ResponseEntity<Map<String, Object>>> result2 = 
                authController.eliminarUsuario(usuario, "InvalidToken");

        // Then
        StepVerifier.create(result2)
                .assertNext(response -> {
                    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
                    assertTrue((Boolean) response.getBody().get("error"));
                })
                .verifyComplete();

        verify(domainServiceClient, never()).eliminarUsuario(anyString(), anyString());
    }

    @Test
    @DisplayName("Eliminar usuario - Error en el servicio")
    void testEliminarUsuario_ServiceError() {
        // Given
        String usuario = "testuser";
        String token = "Bearer valid-token-123";
        
        when(domainServiceClient.eliminarUsuario(anyString(), anyString()))
                .thenReturn(Mono.error(new RuntimeException("Error de base de datos")));

        // When
        Mono<ResponseEntity<Map<String, Object>>> result = 
                authController.eliminarUsuario(usuario, token);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
                    assertNotNull(response.getBody());
                    assertTrue((Boolean) response.getBody().get("error"));
                    assertEquals("Error eliminando usuario", response.getBody().get("respuesta"));
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Registro con datos vacíos - Verifica que se pasa al servicio")
    void testRegistrarUsuario_EmptyData() {
        // Given
        Map<String, Object> emptyRequest = new HashMap<>();
        when(domainServiceClient.registrarUsuario(anyMap()))
                .thenReturn(Mono.just(successResponse));

        // When
        Mono<ResponseEntity<Map<String, Object>>> result = 
                authController.registrarUsuario(emptyRequest);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(HttpStatus.CREATED, response.getStatusCode());
                })
                .verifyComplete();

        verify(domainServiceClient, times(1)).registrarUsuario(emptyRequest);
    }

    @Test
    @DisplayName("Extracción correcta del token Bearer")
    void testEliminarUsuario_TokenExtraction() {
        // Given
        String usuario = "testuser";
        String fullToken = "Bearer abc123xyz456";
        String expectedToken = "abc123xyz456";
        
        when(domainServiceClient.eliminarUsuario(anyString(), eq(expectedToken)))
                .thenReturn(Mono.just(successResponse));

        // When
        Mono<ResponseEntity<Map<String, Object>>> result = 
                authController.eliminarUsuario(usuario, fullToken);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(HttpStatus.OK, response.getStatusCode());
                })
                .verifyComplete();

        // Verify that the Bearer prefix was removed correctly
        verify(domainServiceClient, times(1))
                .eliminarUsuario(eq(usuario), eq(expectedToken));
    }
}

