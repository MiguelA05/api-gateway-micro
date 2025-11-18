package com.uniquindio.archmicroserv.apigateway.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests unitarios para UsuarioUnificadoService")
class UsuarioUnificadoServiceTest {

    @Mock
    private DomainServiceClient domainServiceClient;

    @Mock
    private GestionPerfilServiceClient gestionPerfilServiceClient;

    @InjectMocks
    private UsuarioUnificadoService usuarioUnificadoService;

    private String testUsuario;
    private String testToken;
    private Map<String, Object> perfilData;

    @BeforeEach
    void setUp() {
        testUsuario = "testuser";
        testToken = "valid-token-123";

        perfilData = new HashMap<>();
        perfilData.put("apodo", "Test User");
        perfilData.put("biografia", "Test bio");
    }

    @Test
    @DisplayName("Actualizar usuario completo - Solo datos de seguridad")
    void testActualizarUsuarioCompleto_OnlySecurityData() {
        // Given
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("correo", "new@example.com");
        requestBody.put("clave", "newpassword");
        requestBody.put("numeroTelefono", "1234567890");

        Map<String, Object> expectedSeguridadData = new HashMap<>();
        expectedSeguridadData.put("correo", "new@example.com");
        expectedSeguridadData.put("clave", "newpassword");
        expectedSeguridadData.put("numeroTelefono", "1234567890");

        Map<String, Object> seguridadResponse = new HashMap<>();
        seguridadResponse.put("mensaje", "Actualizado");

        when(domainServiceClient.actualizarUsuario(eq(testUsuario), eq(expectedSeguridadData), eq(testToken)))
                .thenReturn(Mono.just(seguridadResponse));

        // When
        Mono<Map<String, Object>> result = 
                usuarioUnificadoService.actualizarUsuarioCompleto(testUsuario, requestBody, testToken);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertTrue(response.containsKey("mensaje"));
                    assertEquals("Usuario actualizado exitosamente", response.get("mensaje"));
                })
                .verifyComplete();

        verify(domainServiceClient, times(1))
                .actualizarUsuario(eq(testUsuario), eq(expectedSeguridadData), eq(testToken));
        verify(gestionPerfilServiceClient, never()).actualizarPerfil(anyString(), anyMap());
    }

    @Test
    @DisplayName("Actualizar usuario completo - Solo datos de perfil")
    void testActualizarUsuarioCompleto_OnlyPerfilData() {
        // Given
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("apodo", "New Nickname");
        requestBody.put("biografia", "New bio");
        requestBody.put("urlPaginaPersonal", "https://example.com");
        requestBody.put("linkGithub", "https://github.com/user");

        Map<String, Object> expectedPerfilData = new HashMap<>();
        expectedPerfilData.put("apodo", "New Nickname");
        expectedPerfilData.put("biografia", "New bio");
        expectedPerfilData.put("urlPaginaPersonal", "https://example.com");
        expectedPerfilData.put("linkGithub", "https://github.com/user");

        Map<String, Object> perfilResponse = new HashMap<>();
        perfilResponse.put("mensaje", "Perfil actualizado");

        when(gestionPerfilServiceClient.actualizarPerfil(eq(testUsuario), eq(expectedPerfilData)))
                .thenReturn(Mono.just(perfilResponse));

        // When
        Mono<Map<String, Object>> result = 
                usuarioUnificadoService.actualizarUsuarioCompleto(testUsuario, requestBody, testToken);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals("Usuario actualizado exitosamente", response.get("mensaje"));
                })
                .verifyComplete();

        verify(gestionPerfilServiceClient, times(1))
                .actualizarPerfil(eq(testUsuario), eq(expectedPerfilData));
        verify(domainServiceClient, never()).actualizarUsuario(anyString(), anyMap(), anyString());
    }

    @Test
    @DisplayName("Actualizar usuario completo - Datos mixtos")
    void testActualizarUsuarioCompleto_MixedData() {
        // Given
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("correo", "new@example.com");
        requestBody.put("apodo", "New Nickname");
        requestBody.put("clave", "newpassword");
        requestBody.put("biografia", "New bio");

        Map<String, Object> seguridadResponse = new HashMap<>();
        Map<String, Object> perfilResponse = new HashMap<>();

        when(domainServiceClient.actualizarUsuario(anyString(), anyMap(), anyString()))
                .thenReturn(Mono.just(seguridadResponse));
        when(gestionPerfilServiceClient.actualizarPerfil(anyString(), anyMap()))
                .thenReturn(Mono.just(perfilResponse));

        // When
        Mono<Map<String, Object>> result = 
                usuarioUnificadoService.actualizarUsuarioCompleto(testUsuario, requestBody, testToken);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals("Usuario actualizado exitosamente", response.get("mensaje"));
                    assertTrue(response.containsKey("datosSeguridad"));
                    assertTrue(response.containsKey("datosPerfil"));
                })
                .verifyComplete();

        verify(domainServiceClient, times(1))
                .actualizarUsuario(eq(testUsuario), anyMap(), eq(testToken));
        verify(gestionPerfilServiceClient, times(1))
                .actualizarPerfil(eq(testUsuario), anyMap());
    }

    @Test
    @DisplayName("Actualizar usuario completo - Error en servicio de seguridad")
    void testActualizarUsuarioCompleto_SecurityServiceError() {
        // Given
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("correo", "new@example.com");
        requestBody.put("apodo", "New Nickname");

        when(domainServiceClient.actualizarUsuario(anyString(), anyMap(), anyString()))
                .thenReturn(Mono.error(new RuntimeException("Error de conexión")));
        when(gestionPerfilServiceClient.actualizarPerfil(anyString(), anyMap()))
                .thenReturn(Mono.just(new HashMap<>()));

        // When
        Mono<Map<String, Object>> result = 
                usuarioUnificadoService.actualizarUsuarioCompleto(testUsuario, requestBody, testToken);

        // Then - Debe continuar y actualizar perfil
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals("Usuario actualizado exitosamente", response.get("mensaje"));
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Actualizar usuario completo - Error en servicio de perfil")
    void testActualizarUsuarioCompleto_PerfilServiceError() {
        // Given
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("correo", "new@example.com");
        requestBody.put("apodo", "New Nickname");

        when(domainServiceClient.actualizarUsuario(anyString(), anyMap(), anyString()))
                .thenReturn(Mono.just(new HashMap<>()));
        when(gestionPerfilServiceClient.actualizarPerfil(anyString(), anyMap()))
                .thenReturn(Mono.error(new RuntimeException("Error de conexión")));

        // When
        Mono<Map<String, Object>> result = 
                usuarioUnificadoService.actualizarUsuarioCompleto(testUsuario, requestBody, testToken);

        // Then - Debe continuar y actualizar seguridad
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals("Usuario actualizado exitosamente", response.get("mensaje"));
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Actualizar usuario completo - Todos los campos de redes sociales")
    void testActualizarUsuarioCompleto_AllSocialFields() {
        // Given
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("linkFacebook", "https://facebook.com/user");
        requestBody.put("linkTwitter", "https://twitter.com/user");
        requestBody.put("linkLinkedIn", "https://linkedin.com/in/user");
        requestBody.put("linkInstagram", "https://instagram.com/user");
        requestBody.put("linkGithub", "https://github.com/user");
        requestBody.put("linkOtraRed", "https://other.com/user");

        when(gestionPerfilServiceClient.actualizarPerfil(eq(testUsuario), anyMap()))
                .thenReturn(Mono.just(new HashMap<>()));

        // When
        Mono<Map<String, Object>> result = 
                usuarioUnificadoService.actualizarUsuarioCompleto(testUsuario, requestBody, testToken);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals("Usuario actualizado exitosamente", response.get("mensaje"));
                })
                .verifyComplete();

        verify(gestionPerfilServiceClient, times(1)).actualizarPerfil(eq(testUsuario), argThat(
            (Map<String, Object> map) -> 
                map.containsKey("linkFacebook") &&
                map.containsKey("linkTwitter") &&
                map.containsKey("linkLinkedIn") &&
                map.containsKey("linkInstagram") &&
                map.containsKey("linkGithub") &&
                map.containsKey("linkOtraRed")
        ));
    }

    @Test
    @DisplayName("Actualizar usuario completo - Datos vacíos")
    void testActualizarUsuarioCompleto_EmptyData() {
        // Given
        Map<String, Object> emptyRequest = new HashMap<>();

        // When
        Mono<Map<String, Object>> result = 
                usuarioUnificadoService.actualizarUsuarioCompleto(testUsuario, emptyRequest, testToken);

        // Then - No debe llamar a ningún servicio
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals("Usuario actualizado exitosamente", response.get("mensaje"));
                })
                .verifyComplete();

        verify(domainServiceClient, never()).actualizarUsuario(anyString(), anyMap(), anyString());
        verify(gestionPerfilServiceClient, never()).actualizarPerfil(anyString(), anyMap());
    }

    @Test
    @DisplayName("Actualizar usuario completo - Todos los campos de perfil")
    void testActualizarUsuarioCompleto_AllPerfilFields() {
        // Given
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("apodo", "Nickname");
        requestBody.put("urlPaginaPersonal", "https://example.com");
        requestBody.put("informacionContactoPublica", "Public info");
        requestBody.put("direccionCorrespondencia", "Address 123");
        requestBody.put("biografia", "Bio text");
        requestBody.put("organizacion", "Organization");
        requestBody.put("paisResidencia", "Colombia");

        when(gestionPerfilServiceClient.actualizarPerfil(eq(testUsuario), anyMap()))
                .thenReturn(Mono.just(new HashMap<>()));

        // When
        Mono<Map<String, Object>> result = 
                usuarioUnificadoService.actualizarUsuarioCompleto(testUsuario, requestBody, testToken);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals("Usuario actualizado exitosamente", response.get("mensaje"));
                })
                .verifyComplete();

        verify(gestionPerfilServiceClient, times(1)).actualizarPerfil(eq(testUsuario), argThat(
            (Map<String, Object> map) -> 
                map.size() == 7 &&  // 7 campos de perfil
                map.containsKey("apodo") &&
                map.containsKey("biografia") &&
                map.containsKey("organizacion")
        ));
    }

    @Test
    @DisplayName("Actualizar usuario - Campo duplicado informacionContactoPublica")
    void testActualizarUsuarioCompleto_DuplicateField() {
        // Given - El campo informacionContactoPublica aparece duplicado en el código
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("informacionContactoPublica", "Public contact info");

        when(gestionPerfilServiceClient.actualizarPerfil(anyString(), anyMap()))
                .thenReturn(Mono.just(new HashMap<>()));

        // When
        Mono<Map<String, Object>> result = 
                usuarioUnificadoService.actualizarUsuarioCompleto(testUsuario, requestBody, testToken);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals("Usuario actualizado exitosamente", response.get("mensaje"));
                })
                .verifyComplete();

        // Verify que el campo se incluye correctamente (aunque aparezca duplicado en el código original)
        verify(gestionPerfilServiceClient, times(1))
                .actualizarPerfil(eq(testUsuario), argThat(
                    (Map<String, Object> map) -> 
                        map.containsKey("informacionContactoPublica")
                ));
    }
}

