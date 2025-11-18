package com.uniquindio.archmicroserv.apigateway.controller;

import com.uniquindio.archmicroserv.apigateway.messaging.EventoPublisher;
import com.uniquindio.archmicroserv.apigateway.service.DomainServiceClient;
import com.uniquindio.archmicroserv.apigateway.service.GestionPerfilServiceClient;
import com.uniquindio.archmicroserv.apigateway.service.UsuarioUnificadoService;
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
@DisplayName("Tests unitarios para UsuarioController")
class UsuarioControllerTest {

    @Mock
    private UsuarioUnificadoService usuarioUnificadoService;

    @Mock
    private DomainServiceClient domainServiceClient;

    @Mock
    private GestionPerfilServiceClient gestionPerfilServiceClient;

    @Mock
    private EventoPublisher eventoPublisher;

    @InjectMocks
    private UsuarioController usuarioController;

    private String testUsuario;
    private String validToken;
    private Map<String, Object> perfilData;
    private Map<String, Object> updateData;

    @BeforeEach
    void setUp() {
        testUsuario = "testuser";
        validToken = "Bearer valid-token-123";

        perfilData = new HashMap<>();
        perfilData.put("apodo", "Test User");
        perfilData.put("biografia", "Test biography");
        perfilData.put("paisResidencia", "Colombia");

        updateData = new HashMap<>();
        updateData.put("correo", "newemail@example.com");
        updateData.put("apodo", "New Nickname");
    }

    @Test
    @DisplayName("Obtener usuario completo - Camino feliz")
    void testObtenerUsuarioCompleto_Success() {
        // Given
        Map<String, Object> usuarioData = new HashMap<>();
        usuarioData.put("usuario", testUsuario);
        usuarioData.put("correo", "test@example.com");
        
        when(domainServiceClient.obtenerUsuario(testUsuario, "valid-token-123"))
                .thenReturn(Mono.just(usuarioData));
        when(gestionPerfilServiceClient.obtenerPerfil(testUsuario))
                .thenReturn(Mono.just(perfilData));

        // When
        Mono<ResponseEntity<Map<String, Object>>> result = 
                usuarioController.obtenerUsuarioCompleto(testUsuario, validToken);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(HttpStatus.OK, response.getStatusCode());
                    assertNotNull(response.getBody());
                    assertEquals(testUsuario, response.getBody().get("usuario"));
                    assertTrue(response.getBody().containsKey("perfil"));
                    
                    @SuppressWarnings("unchecked")
                    Map<String, Object> perfil = (Map<String, Object>) response.getBody().get("perfil");
                    assertEquals("Test User", perfil.get("apodo"));
                })
                .verifyComplete();

        verify(domainServiceClient, times(1)).obtenerUsuario(testUsuario, "valid-token-123");
        verify(gestionPerfilServiceClient, times(1)).obtenerPerfil(testUsuario);
    }

    @Test
    @DisplayName("Obtener usuario completo - Sin token")
    void testObtenerUsuarioCompleto_NoToken() {
        // When
        Mono<ResponseEntity<Map<String, Object>>> result = 
                usuarioController.obtenerUsuarioCompleto(testUsuario, null);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
                    assertNotNull(response.getBody());
                    assertTrue((Boolean) response.getBody().get("error"));
                    assertEquals("Token de autenticación requerido", response.getBody().get("respuesta"));
                })
                .verifyComplete();

        verify(gestionPerfilServiceClient, never()).obtenerPerfil(anyString());
    }

    @Test
    @DisplayName("Obtener usuario completo - Token inválido")
    void testObtenerUsuarioCompleto_InvalidToken() {
        // When
        Mono<ResponseEntity<Map<String, Object>>> result = 
                usuarioController.obtenerUsuarioCompleto(testUsuario, "InvalidToken");

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
                    assertTrue((Boolean) response.getBody().get("error"));
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Obtener usuario completo - Perfil no encontrado")
    void testObtenerUsuarioCompleto_PerfilNotFound() {
        // Given
        Map<String, Object> usuarioData = new HashMap<>();
        usuarioData.put("usuario", testUsuario);
        usuarioData.put("correo", "test@example.com");
        
        when(domainServiceClient.obtenerUsuario(testUsuario, "valid-token-123"))
                .thenReturn(Mono.just(usuarioData));
        when(gestionPerfilServiceClient.obtenerPerfil(testUsuario))
                .thenReturn(Mono.error(new RuntimeException("Perfil no encontrado")));

        // When
        Mono<ResponseEntity<Map<String, Object>>> result = 
                usuarioController.obtenerUsuarioCompleto(testUsuario, validToken);

        // Then - Debe retornar OK pero sin datos de perfil
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(HttpStatus.OK, response.getStatusCode());
                    assertNotNull(response.getBody());
                    assertEquals(testUsuario, response.getBody().get("usuario"));
                    // No debe contener perfil si hubo error
                    assertFalse(response.getBody().containsKey("perfil"));
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Actualizar usuario completo - Camino feliz")
    void testActualizarUsuarioCompleto_Success() {
        // Given
        Map<String, Object> expectedResponse = new HashMap<>();
        expectedResponse.put("mensaje", "Usuario actualizado exitosamente");
        
        when(usuarioUnificadoService.actualizarUsuarioCompleto(eq(testUsuario), eq(updateData), anyString()))
                .thenReturn(Mono.just(expectedResponse));

        // When
        Mono<ResponseEntity<Map<String, Object>>> result = 
                usuarioController.actualizarUsuarioCompleto(testUsuario, updateData, validToken);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(HttpStatus.OK, response.getStatusCode());
                    assertNotNull(response.getBody());
                    assertTrue(response.getBody().containsKey("mensaje"));
                })
                .verifyComplete();

        verify(usuarioUnificadoService, times(1))
                .actualizarUsuarioCompleto(eq(testUsuario), eq(updateData), eq("valid-token-123"));
    }

    @Test
    @DisplayName("Actualizar usuario completo - Sin token")
    void testActualizarUsuarioCompleto_NoToken() {
        // When
        Mono<ResponseEntity<Map<String, Object>>> result = 
                usuarioController.actualizarUsuarioCompleto(testUsuario, updateData, null);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
                    assertTrue((Boolean) response.getBody().get("error"));
                })
                .verifyComplete();

        verify(usuarioUnificadoService, never())
                .actualizarUsuarioCompleto(anyString(), anyMap(), anyString());
    }

    @Test
    @DisplayName("Actualizar usuario completo - Error en servicio")
    void testActualizarUsuarioCompleto_ServiceError() {
        // Given
        when(usuarioUnificadoService.actualizarUsuarioCompleto(anyString(), anyMap(), anyString()))
                .thenReturn(Mono.error(new RuntimeException("Error de conexión")));

        // When
        Mono<ResponseEntity<Map<String, Object>>> result = 
                usuarioController.actualizarUsuarioCompleto(testUsuario, updateData, validToken);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
                    assertTrue((Boolean) response.getBody().get("error"));
                    assertEquals("Error actualizando datos del usuario", response.getBody().get("respuesta"));
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Eliminar usuario completo - Camino feliz")
    void testEliminarUsuarioCompleto_Success() {
        // Given
        Map<String, Object> seguridadResponse = new HashMap<>();
        seguridadResponse.put("error", false);
        seguridadResponse.put("respuesta", "Usuario eliminado");
        
        when(domainServiceClient.eliminarUsuario(eq(testUsuario), anyString()))
                .thenReturn(Mono.just(seguridadResponse));
        when(gestionPerfilServiceClient.eliminarPerfil(testUsuario))
                .thenReturn(Mono.empty());
        doNothing().when(eventoPublisher).publicarEventoEliminacion(anyString(), anyString());

        // When
        Mono<ResponseEntity<Map<String, Object>>> result = 
                usuarioController.eliminarUsuarioCompleto(testUsuario, validToken);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(HttpStatus.OK, response.getStatusCode());
                    assertNotNull(response.getBody());
                    assertFalse((Boolean) response.getBody().get("error"));
                    assertEquals("Usuario eliminado exitosamente del sistema", 
                                response.getBody().get("respuesta"));
                })
                .verifyComplete();

        verify(domainServiceClient, times(1)).eliminarUsuario(eq(testUsuario), anyString());
        verify(gestionPerfilServiceClient, times(1)).eliminarPerfil(testUsuario);
        verify(eventoPublisher, times(1)).publicarEventoEliminacion(eq(testUsuario), anyString());
    }

    @Test
    @DisplayName("Eliminar usuario completo - Sin token")
    void testEliminarUsuarioCompleto_NoToken() {
        // When
        Mono<ResponseEntity<Map<String, Object>>> result = 
                usuarioController.eliminarUsuarioCompleto(testUsuario, null);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
                    assertTrue((Boolean) response.getBody().get("error"));
                })
                .verifyComplete();

        verify(domainServiceClient, never()).eliminarUsuario(anyString(), anyString());
        verify(gestionPerfilServiceClient, never()).eliminarPerfil(anyString());
        verify(eventoPublisher, never()).publicarEventoEliminacion(anyString(), anyString());
    }

    @Test
    @DisplayName("Eliminar usuario completo - Error parcial pero continúa")
    void testEliminarUsuarioCompleto_PartialError() {
        // Given - Error en seguridad pero perfil OK
        when(domainServiceClient.eliminarUsuario(anyString(), anyString()))
                .thenReturn(Mono.error(new RuntimeException("Error eliminando seguridad")));

        // When
        Mono<ResponseEntity<Map<String, Object>>> result = 
                usuarioController.eliminarUsuarioCompleto(testUsuario, validToken);

        // Then - Debe retornar error 500 porque el error en seguridad se propaga
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
                    assertTrue((Boolean) response.getBody().get("error"));
                })
                .verifyComplete();

        verify(eventoPublisher, times(1)).publicarEventoEliminacion(eq(testUsuario), anyString());
    }

    @Test
    @DisplayName("Eliminar usuario completo - Error publicando evento")
    void testEliminarUsuarioCompleto_EventPublishingError() {
        // Given
        Map<String, Object> seguridadResponse = new HashMap<>();
        seguridadResponse.put("error", false);
        
        when(domainServiceClient.eliminarUsuario(anyString(), anyString()))
                .thenReturn(Mono.just(seguridadResponse));
        when(gestionPerfilServiceClient.eliminarPerfil(anyString()))
                .thenReturn(Mono.empty());
        doThrow(new RuntimeException("Error publicando evento"))
                .when(eventoPublisher).publicarEventoEliminacion(anyString(), anyString());

        // When - No debe fallar el proceso completo por error en evento
        Mono<ResponseEntity<Map<String, Object>>> result = 
                usuarioController.eliminarUsuarioCompleto(testUsuario, validToken);

        // Then - Debe completarse exitosamente
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(HttpStatus.OK, response.getStatusCode());
                    assertFalse((Boolean) response.getBody().get("error"));
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Obtener usuario completo - Perfil vacío")
    void testObtenerUsuarioCompleto_EmptyPerfil() {
        // Given
        Map<String, Object> usuarioData = new HashMap<>();
        usuarioData.put("usuario", testUsuario);
        usuarioData.put("correo", "test@example.com");
        
        when(domainServiceClient.obtenerUsuario(testUsuario, "valid-token-123"))
                .thenReturn(Mono.just(usuarioData));
        when(gestionPerfilServiceClient.obtenerPerfil(testUsuario))
                .thenReturn(Mono.just(new HashMap<>()));

        // When
        Mono<ResponseEntity<Map<String, Object>>> result = 
                usuarioController.obtenerUsuarioCompleto(testUsuario, validToken);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(HttpStatus.OK, response.getStatusCode());
                    assertEquals(testUsuario, response.getBody().get("usuario"));
                    // No debe incluir perfil si está vacío
                    assertFalse(response.getBody().containsKey("perfil"));
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Actualizar usuario - Extracción correcta del token")
    void testActualizarUsuarioCompleto_TokenExtraction() {
        // Given
        String fullToken = "Bearer abc123xyz";
        String expectedToken = "abc123xyz";
        
        when(usuarioUnificadoService.actualizarUsuarioCompleto(anyString(), anyMap(), eq(expectedToken)))
                .thenReturn(Mono.just(new HashMap<>()));

        // When
        usuarioController.actualizarUsuarioCompleto(testUsuario, updateData, fullToken)
                .subscribe();

        // Then - Verify token extraction
        verify(usuarioUnificadoService, timeout(1000))
                .actualizarUsuarioCompleto(eq(testUsuario), eq(updateData), eq(expectedToken));
    }

    @Test
    @DisplayName("Eliminar usuario - Extracción correcta del token")
    void testEliminarUsuarioCompleto_TokenExtraction() {
        // Given
        String fullToken = "Bearer abc123xyz";
        String expectedToken = "abc123xyz";
        
        when(domainServiceClient.eliminarUsuario(eq(testUsuario), eq(expectedToken)))
                .thenReturn(Mono.just(new HashMap<>()));
        when(gestionPerfilServiceClient.eliminarPerfil(anyString()))
                .thenReturn(Mono.empty());
        doNothing().when(eventoPublisher).publicarEventoEliminacion(anyString(), anyString());

        // When
        usuarioController.eliminarUsuarioCompleto(testUsuario, fullToken)
                .subscribe();

        // Then - Verify token extraction
        verify(domainServiceClient, timeout(1000))
                .eliminarUsuario(eq(testUsuario), eq(expectedToken));
    }
}

