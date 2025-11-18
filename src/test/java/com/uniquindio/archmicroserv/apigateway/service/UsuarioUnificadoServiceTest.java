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

/**
 * Tests unitarios para {@link UsuarioUnificadoService}.
 * 
 * <p>Esta clase de pruebas valida el comportamiento del servicio que unifica
 * las operaciones de actualización de datos de usuario, coordinando llamadas
 * a múltiples microservicios (Domain Service y Gestion Perfil Service).</p>
 * 
 * <p><strong>Alcance de las pruebas:</strong>
 * <ul>
 *   <li>Actualización de datos de seguridad únicamente</li>
 *   <li>Actualización de datos de perfil únicamente</li>
 *   <li>Actualización mixta (seguridad + perfil)</li>
 *   <li>Manejo de errores en servicios downstream</li>
 *   <li>Validación de campos vacíos</li>
 *   <li>Validación de todos los campos de perfil y redes sociales</li>
 * </ul>
 * </p>
 * 
 * @author Sistema de Pruebas
 * @version 1.0
 * @since 2024
 */
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

    // ===== TESTS DE ACTUALIZACIÓN POR TIPO DE DATOS =====

    /**
     * Valida que la actualización funciona correctamente cuando solo se proporcionan
     * datos de seguridad (correo, clave, numeroTelefono).
     */
    @Test
    @DisplayName("Actualizar usuario completo - Solo datos de seguridad")
    void testActualizarUsuarioCompleto_OnlySecurityData() {
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

        Mono<Map<String, Object>> result = 
                usuarioUnificadoService.actualizarUsuarioCompleto(testUsuario, requestBody, testToken);

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

    /**
     * Valida que la actualización funciona correctamente cuando solo se proporcionan
     * datos de perfil (apodo, biografia, redes sociales, etc.).
     */
    @Test
    @DisplayName("Actualizar usuario completo - Solo datos de perfil")
    void testActualizarUsuarioCompleto_OnlyPerfilData() {
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

        Mono<Map<String, Object>> result = 
                usuarioUnificadoService.actualizarUsuarioCompleto(testUsuario, requestBody, testToken);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals("Usuario actualizado exitosamente", response.get("mensaje"));
                })
                .verifyComplete();

        verify(gestionPerfilServiceClient, times(1))
                .actualizarPerfil(eq(testUsuario), eq(expectedPerfilData));
        verify(domainServiceClient, never()).actualizarUsuario(anyString(), anyMap(), anyString());
    }

    /**
     * Valida que la actualización funciona correctamente cuando se proporcionan
     * datos mixtos (seguridad + perfil), verificando que ambos servicios sean llamados.
     */
    @Test
    @DisplayName("Actualizar usuario completo - Datos mixtos")
    void testActualizarUsuarioCompleto_MixedData() {
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

        Mono<Map<String, Object>> result = 
                usuarioUnificadoService.actualizarUsuarioCompleto(testUsuario, requestBody, testToken);

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

    // ===== TESTS DE MANEJO DE ERRORES =====

    /**
     * Valida que cuando falla la actualización de seguridad, el error se propaga
     * y no se intenta actualizar el perfil.
     */
    @Test
    @DisplayName("Actualizar usuario completo - Error en servicio de seguridad")
    void testActualizarUsuarioCompleto_SecurityServiceError() {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("correo", "new@example.com");
        requestBody.put("apodo", "New Nickname");

        when(domainServiceClient.actualizarUsuario(anyString(), anyMap(), anyString()))
                .thenReturn(Mono.error(new RuntimeException("Error de conexión")));

        Mono<Map<String, Object>> result = 
                usuarioUnificadoService.actualizarUsuarioCompleto(testUsuario, requestBody, testToken);

        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();
    }

    /**
     * Valida que cuando falla la actualización de perfil pero la de seguridad
     * es exitosa, se retorna un mensaje indicando actualización parcial.
     */
    @Test
    @DisplayName("Actualizar usuario completo - Error en servicio de perfil")
    void testActualizarUsuarioCompleto_PerfilServiceError() {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("correo", "new@example.com");
        requestBody.put("apodo", "New Nickname");

        when(domainServiceClient.actualizarUsuario(anyString(), anyMap(), anyString()))
                .thenReturn(Mono.just(new HashMap<>()));
        when(gestionPerfilServiceClient.actualizarPerfil(anyString(), anyMap()))
                .thenReturn(Mono.error(new RuntimeException("Error de conexión")));

        Mono<Map<String, Object>> result = 
                usuarioUnificadoService.actualizarUsuarioCompleto(testUsuario, requestBody, testToken);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals("Usuario actualizado parcialmente (solo seguridad)", response.get("mensaje"));
                })
                .verifyComplete();
    }

    // ===== TESTS DE VALIDACIÓN DE CAMPOS =====

    /**
     * Valida que todos los campos de redes sociales se procesan correctamente
     * cuando se proporcionan en el request.
     */
    @Test
    @DisplayName("Actualizar usuario completo - Todos los campos de redes sociales")
    void testActualizarUsuarioCompleto_AllSocialFields() {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("linkFacebook", "https://facebook.com/user");
        requestBody.put("linkTwitter", "https://twitter.com/user");
        requestBody.put("linkLinkedIn", "https://linkedin.com/in/user");
        requestBody.put("linkInstagram", "https://instagram.com/user");
        requestBody.put("linkGithub", "https://github.com/user");
        requestBody.put("linkOtraRed", "https://other.com/user");

        when(gestionPerfilServiceClient.actualizarPerfil(eq(testUsuario), anyMap()))
                .thenReturn(Mono.just(new HashMap<>()));

        Mono<Map<String, Object>> result = 
                usuarioUnificadoService.actualizarUsuarioCompleto(testUsuario, requestBody, testToken);

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

    /**
     * Valida que cuando se envía un request vacío, se retorna un mensaje
     * indicando que no hay datos para actualizar y no se llama a ningún servicio.
     */
    @Test
    @DisplayName("Actualizar usuario completo - Datos vacíos")
    void testActualizarUsuarioCompleto_EmptyData() {
        Map<String, Object> emptyRequest = new HashMap<>();

        Mono<Map<String, Object>> result = 
                usuarioUnificadoService.actualizarUsuarioCompleto(testUsuario, emptyRequest, testToken);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals("No hay datos para actualizar", response.get("mensaje"));
                })
                .verifyComplete();

        verify(domainServiceClient, never()).actualizarUsuario(anyString(), anyMap(), anyString());
        verify(gestionPerfilServiceClient, never()).actualizarPerfil(anyString(), anyMap());
    }

    /**
     * Valida que todos los campos de perfil se procesan correctamente
     * cuando se proporcionan en el request.
     */
    @Test
    @DisplayName("Actualizar usuario completo - Todos los campos de perfil")
    void testActualizarUsuarioCompleto_AllPerfilFields() {
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

        Mono<Map<String, Object>> result = 
                usuarioUnificadoService.actualizarUsuarioCompleto(testUsuario, requestBody, testToken);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals("Usuario actualizado exitosamente", response.get("mensaje"));
                })
                .verifyComplete();

        verify(gestionPerfilServiceClient, times(1)).actualizarPerfil(eq(testUsuario), argThat(
            (Map<String, Object> map) -> 
                map.size() == 7 &&
                map.containsKey("apodo") &&
                map.containsKey("biografia") &&
                map.containsKey("organizacion")
        ));
    }

    /**
     * Valida el manejo del campo informacionContactoPublica cuando se proporciona
     * en el request, verificando que se incluye correctamente en los datos de perfil.
     */
    @Test
    @DisplayName("Actualizar usuario - Campo duplicado informacionContactoPublica")
    void testActualizarUsuarioCompleto_DuplicateField() {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("informacionContactoPublica", "Public contact info");

        when(gestionPerfilServiceClient.actualizarPerfil(anyString(), anyMap()))
                .thenReturn(Mono.just(new HashMap<>()));

        Mono<Map<String, Object>> result = 
                usuarioUnificadoService.actualizarUsuarioCompleto(testUsuario, requestBody, testToken);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals("Usuario actualizado exitosamente", response.get("mensaje"));
                })
                .verifyComplete();

        verify(gestionPerfilServiceClient, times(1))
                .actualizarPerfil(eq(testUsuario), argThat(
                    (Map<String, Object> map) -> 
                        map.containsKey("informacionContactoPublica")
                ));
    }
}
