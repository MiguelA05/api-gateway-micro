package com.uniquindio.archmicroserv.apigateway.integration;

import com.uniquindio.archmicroserv.apigateway.controller.UsuarioController;
import com.uniquindio.archmicroserv.apigateway.messaging.EventoPublisher;
import com.uniquindio.archmicroserv.apigateway.service.DomainServiceClient;
import com.uniquindio.archmicroserv.apigateway.service.GestionPerfilServiceClient;
import com.uniquindio.archmicroserv.apigateway.service.UsuarioUnificadoService;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@WebFluxTest(controllers = UsuarioController.class)
@ActiveProfiles("test")
@DisplayName("Tests de integración para UsuarioController")
class UsuarioControllerIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private UsuarioUnificadoService usuarioUnificadoService;

    @MockBean
    private DomainServiceClient domainServiceClient;

    @MockBean
    private GestionPerfilServiceClient gestionPerfilServiceClient;

    @MockBean
    private EventoPublisher eventoPublisher;

    private Map<String, Object> perfilData;
    private Map<String, Object> updateData;

    @BeforeEach
    void setUp() {
        perfilData = new HashMap<>();
        perfilData.put("apodo", "Test User");
        perfilData.put("biografia", "Test biography");

        updateData = new HashMap<>();
        updateData.put("correo", "newemail@example.com");
        updateData.put("apodo", "New Nickname");
    }

    @Test
    @DisplayName("GET /api/v1/usuarios/{usuario} - Camino feliz")
    void testObtenerUsuario_Success() {
        // Given
        Map<String, Object> usuarioData = new HashMap<>();
        usuarioData.put("usuario", "testuser");
        usuarioData.put("correo", "test@example.com");
        
        when(domainServiceClient.obtenerUsuario("testuser", "valid-token-123"))
                .thenReturn(Mono.just(usuarioData));
        when(gestionPerfilServiceClient.obtenerPerfil("testuser"))
                .thenReturn(Mono.just(perfilData));

        // When & Then
        webTestClient.get()
                .uri("/api/v1/usuarios/testuser")
                .header("Authorization", "Bearer valid-token-123")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.usuario").isEqualTo("testuser")
                .jsonPath("$.perfil").exists()
                .jsonPath("$.perfil.apodo").isEqualTo("Test User");
    }

    @Test
    @DisplayName("GET /api/v1/usuarios/{usuario} - Sin token")
    void testObtenerUsuario_NoToken() {
        // When & Then
        webTestClient.get()
                .uri("/api/v1/usuarios/testuser")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.error").isEqualTo(true)
                .jsonPath("$.respuesta").isEqualTo("Token de autenticación requerido");
    }

    @Test
    @DisplayName("GET /api/v1/usuarios/{usuario} - Token inválido")
    void testObtenerUsuario_InvalidToken() {
        // When & Then
        webTestClient.get()
                .uri("/api/v1/usuarios/testuser")
                .header("Authorization", "InvalidToken")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.error").isEqualTo(true);
    }

    @Test
    @DisplayName("GET /api/v1/usuarios/{usuario} - Perfil no encontrado")
    void testObtenerUsuario_PerfilNotFound() {
        // Given
        Map<String, Object> usuarioData = new HashMap<>();
        usuarioData.put("usuario", "testuser");
        usuarioData.put("correo", "test@example.com");
        
        when(domainServiceClient.obtenerUsuario("testuser", "valid-token-123"))
                .thenReturn(Mono.just(usuarioData));
        when(gestionPerfilServiceClient.obtenerPerfil("testuser"))
                .thenReturn(Mono.error(new RuntimeException("Perfil no encontrado")));

        // When & Then
        webTestClient.get()
                .uri("/api/v1/usuarios/testuser")
                .header("Authorization", "Bearer valid-token-123")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.usuario").isEqualTo("testuser")
                .jsonPath("$.perfil").doesNotExist();
    }

    @Test
    @DisplayName("PUT /api/v1/usuarios/{usuario} - Camino feliz")
    void testActualizarUsuario_Success() {
        // Given
        Map<String, Object> response = new HashMap<>();
        response.put("mensaje", "Usuario actualizado exitosamente");
        
        when(usuarioUnificadoService.actualizarUsuarioCompleto(eq("testuser"), any(), anyString()))
                .thenReturn(Mono.just(response));

        // When & Then
        webTestClient.put()
                .uri("/api/v1/usuarios/testuser")
                .header("Authorization", "Bearer valid-token-123")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updateData)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.mensaje").exists();
    }

    @Test
    @DisplayName("PUT /api/v1/usuarios/{usuario} - Sin token")
    void testActualizarUsuario_NoToken() {
        // When & Then
        webTestClient.put()
                .uri("/api/v1/usuarios/testuser")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updateData)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.error").isEqualTo(true);
    }

    @Test
    @DisplayName("PUT /api/v1/usuarios/{usuario} - Error en servicio")
    void testActualizarUsuario_ServiceError() {
        // Given
        when(usuarioUnificadoService.actualizarUsuarioCompleto(anyString(), any(), anyString()))
                .thenReturn(Mono.error(new RuntimeException("Error de conexión")));

        // When & Then
        webTestClient.put()
                .uri("/api/v1/usuarios/testuser")
                .header("Authorization", "Bearer valid-token-123")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updateData)
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody()
                .jsonPath("$.error").isEqualTo(true)
                .jsonPath("$.respuesta").isEqualTo("Error actualizando datos del usuario");
    }

    @Test
    @DisplayName("DELETE /api/v1/usuarios/{usuario} - Camino feliz")
    void testEliminarUsuario_Success() {
        // Given
        when(domainServiceClient.eliminarUsuario(eq("testuser"), anyString()))
                .thenReturn(Mono.just(new HashMap<>()));
        when(gestionPerfilServiceClient.eliminarPerfil("testuser"))
                .thenReturn(Mono.empty());
        doNothing().when(eventoPublisher).publicarEventoEliminacion(anyString(), anyString());

        // When & Then
        webTestClient.delete()
                .uri("/api/v1/usuarios/testuser")
                .header("Authorization", "Bearer valid-token-123")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.error").isEqualTo(false)
                .jsonPath("$.respuesta").isEqualTo("Usuario eliminado exitosamente del sistema");
    }

    @Test
    @DisplayName("DELETE /api/v1/usuarios/{usuario} - Sin token")
    void testEliminarUsuario_NoToken() {
        // When & Then
        webTestClient.delete()
                .uri("/api/v1/usuarios/testuser")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.error").isEqualTo(true);
    }

    @Test
    @DisplayName("DELETE /api/v1/usuarios/{usuario} - Error parcial")
    void testEliminarUsuario_PartialError() {
        // Given - Error en seguridad pero continúa
        when(domainServiceClient.eliminarUsuario(anyString(), anyString()))
                .thenReturn(Mono.error(new RuntimeException("Error eliminando seguridad")));
        when(gestionPerfilServiceClient.eliminarPerfil(anyString()))
                .thenReturn(Mono.empty());
        doNothing().when(eventoPublisher).publicarEventoEliminacion(anyString(), anyString());

        // When & Then - Debe retornar error 500 porque el error en seguridad se propaga
        webTestClient.delete()
                .uri("/api/v1/usuarios/testuser")
                .header("Authorization", "Bearer valid-token-123")
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody()
                .jsonPath("$.error").isEqualTo(true);
    }

    @Test
    @DisplayName("GET /api/v1/usuarios/{usuario} - Usuario con caracteres especiales")
    void testObtenerUsuario_SpecialCharacters() {
        // Given
        String usuario = "test.user-123";
        Map<String, Object> usuarioData = new HashMap<>();
        usuarioData.put("usuario", usuario);
        usuarioData.put("correo", "test@example.com");
        
        when(domainServiceClient.obtenerUsuario(usuario, "valid-token-123"))
                .thenReturn(Mono.just(usuarioData));
        when(gestionPerfilServiceClient.obtenerPerfil(usuario))
                .thenReturn(Mono.just(perfilData));

        // When & Then
        webTestClient.get()
                .uri("/api/v1/usuarios/{usuario}", usuario)
                .header("Authorization", "Bearer valid-token-123")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.usuario").isEqualTo(usuario);
    }

    @Test
    @DisplayName("PUT /api/v1/usuarios/{usuario} - Datos vacíos")
    void testActualizarUsuario_EmptyData() {
        // Given
        Map<String, Object> usuarioData = new HashMap<>();
        usuarioData.put("usuario", "testuser");
        usuarioData.put("correo", "test@example.com");
        
        Map<String, Object> response = new HashMap<>();
        response.put("mensaje", "No hay datos para actualizar");
        
        when(domainServiceClient.obtenerUsuario("testuser", "valid-token-123"))
                .thenReturn(Mono.just(usuarioData));
        when(usuarioUnificadoService.actualizarUsuarioCompleto(anyString(), any(), anyString()))
                .thenReturn(Mono.just(response));

        // When & Then
        webTestClient.put()
                .uri("/api/v1/usuarios/testuser")
                .header("Authorization", "Bearer valid-token-123")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new HashMap<>())
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    @DisplayName("GET /api/v1/usuarios/{usuario} - Múltiples llamadas concurrentes")
    void testObtenerUsuario_ConcurrentCalls() {
        // Given
        Map<String, Object> usuarioData = new HashMap<>();
        usuarioData.put("usuario", "testuser");
        usuarioData.put("correo", "test@example.com");
        
        when(domainServiceClient.obtenerUsuario(anyString(), eq("valid-token-123")))
                .thenReturn(Mono.just(usuarioData));
        when(gestionPerfilServiceClient.obtenerPerfil(anyString()))
                .thenReturn(Mono.just(perfilData));

        // When & Then - Múltiples llamadas
        for (int i = 0; i < 5; i++) {
            String usuario = "testuser" + i;
            usuarioData.put("usuario", usuario);
            when(domainServiceClient.obtenerUsuario(usuario, "valid-token-123"))
                    .thenReturn(Mono.just(usuarioData));
            
            webTestClient.get()
                    .uri("/api/v1/usuarios/{usuario}", usuario)
                    .header("Authorization", "Bearer valid-token-123")
                    .exchange()
                    .expectStatus().isOk();
        }

        verify(gestionPerfilServiceClient, times(5)).obtenerPerfil(anyString());
    }

    @Test
    @DisplayName("DELETE /api/v1/usuarios/{usuario} - Verifica publicación de evento")
    void testEliminarUsuario_EventPublishing() {
        // Given
        when(domainServiceClient.eliminarUsuario(anyString(), anyString()))
                .thenReturn(Mono.just(new HashMap<>()));
        when(gestionPerfilServiceClient.eliminarPerfil(anyString()))
                .thenReturn(Mono.empty());
        doNothing().when(eventoPublisher).publicarEventoEliminacion(anyString(), anyString());

        // When
        webTestClient.delete()
                .uri("/api/v1/usuarios/testuser")
                .header("Authorization", "Bearer valid-token-123")
                .exchange()
                .expectStatus().isOk();

        // Then - Verify event was published
        verify(eventoPublisher, timeout(1000).times(1))
                .publicarEventoEliminacion(eq("testuser"), anyString());
    }
}

