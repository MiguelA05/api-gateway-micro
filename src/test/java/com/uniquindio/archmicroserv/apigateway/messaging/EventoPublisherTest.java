package com.uniquindio.archmicroserv.apigateway.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uniquindio.archmicroserv.apigateway.config.RabbitMQConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests unitarios para EventoPublisher")
class EventoPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private EventoPublisher eventoPublisher;

    @Captor
    private ArgumentCaptor<Map<String, Object>> eventoCaptor;

    @Captor
    private ArgumentCaptor<String> exchangeCaptor;

    @Captor
    private ArgumentCaptor<String> routingKeyCaptor;

    private String testUsuario;
    private String testCorreo;

    @BeforeEach
    void setUp() {
        testUsuario = "testuser";
        testCorreo = "test@example.com";
    }

    @Test
    @DisplayName("Publicar evento de eliminación - Camino feliz")
    void testPublicarEventoEliminacion_Success() {
        // Given
        doNothing().when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

        // When
        eventoPublisher.publicarEventoEliminacion(testUsuario, testCorreo);

        // Then
        verify(rabbitTemplate, times(1)).convertAndSend(
                exchangeCaptor.capture(),
                routingKeyCaptor.capture(),
                eventoCaptor.capture()
        );

        // Verify exchange and routing key
        assertEquals(RabbitMQConfig.DOMINIO_EVENTS_EXCHANGE, exchangeCaptor.getValue());
        assertEquals("auth.deleted", routingKeyCaptor.getValue());

        // Verify event structure
        Map<String, Object> evento = eventoCaptor.getValue();
        assertNotNull(evento);
        assertTrue(evento.containsKey("id"));
        assertEquals("ELIMINACION_USUARIO", evento.get("tipoAccion"));
        assertTrue(evento.containsKey("fechaCreacion"));
        assertTrue(evento.containsKey("datos"));

        // Verify event data
        @SuppressWarnings("unchecked")
        Map<String, Object> datos = (Map<String, Object>) evento.get("datos");
        assertEquals(testUsuario, datos.get("usuario"));
        assertEquals(testCorreo, datos.get("correo"));
        assertTrue(datos.containsKey("fechaEliminacion"));
    }

    @Test
    @DisplayName("Publicar evento de eliminación - Correo nulo")
    void testPublicarEventoEliminacion_NullCorreo() {
        // Given
        doNothing().when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

        // When
        eventoPublisher.publicarEventoEliminacion(testUsuario, null);

        // Then
        verify(rabbitTemplate, times(1)).convertAndSend(
                anyString(),
                anyString(),
                eventoCaptor.capture()
        );

        Map<String, Object> evento = eventoCaptor.getValue();
        @SuppressWarnings("unchecked")
        Map<String, Object> datos = (Map<String, Object>) evento.get("datos");
        
        // Verify that empty string is used when correo is null
        assertEquals("", datos.get("correo"));
        assertEquals(testUsuario, datos.get("usuario"));
    }

    @Test
    @DisplayName("Publicar evento de eliminación - Correo vacío")
    void testPublicarEventoEliminacion_EmptyCorreo() {
        // Given
        doNothing().when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

        // When
        eventoPublisher.publicarEventoEliminacion(testUsuario, "");

        // Then
        verify(rabbitTemplate, times(1)).convertAndSend(
                anyString(),
                anyString(),
                eventoCaptor.capture()
        );

        Map<String, Object> evento = eventoCaptor.getValue();
        @SuppressWarnings("unchecked")
        Map<String, Object> datos = (Map<String, Object>) evento.get("datos");
        
        assertEquals("", datos.get("correo"));
    }

    @Test
    @DisplayName("Publicar evento de eliminación - Error en RabbitTemplate")
    void testPublicarEventoEliminacion_RabbitTemplateError() {
        // Given
        doThrow(new RuntimeException("Error de conexión con RabbitMQ"))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

        // When - No debe lanzar excepción
        assertDoesNotThrow(() -> 
                eventoPublisher.publicarEventoEliminacion(testUsuario, testCorreo)
        );

        // Then
        verify(rabbitTemplate, times(1)).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    @DisplayName("Publicar evento - Verifica UUID único")
    void testPublicarEventoEliminacion_UniqueUUID() {
        // Given
        doNothing().when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

        // When - Publicar dos eventos
        eventoPublisher.publicarEventoEliminacion("user1", "user1@example.com");
        eventoPublisher.publicarEventoEliminacion("user2", "user2@example.com");

        // Then
        verify(rabbitTemplate, times(2)).convertAndSend(
                anyString(),
                anyString(),
                eventoCaptor.capture()
        );

        // Verify that each event has a unique ID
        Map<String, Object> evento1 = eventoCaptor.getAllValues().get(0);
        Map<String, Object> evento2 = eventoCaptor.getAllValues().get(1);
        
        assertNotEquals(evento1.get("id"), evento2.get("id"));
    }

    @Test
    @DisplayName("Publicar evento - Verifica timestamp")
    void testPublicarEventoEliminacion_Timestamp() {
        // Given
        doNothing().when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

        // When
        eventoPublisher.publicarEventoEliminacion(testUsuario, testCorreo);

        // Then
        verify(rabbitTemplate, times(1)).convertAndSend(
                anyString(),
                anyString(),
                eventoCaptor.capture()
        );

        Map<String, Object> evento = eventoCaptor.getValue();
        assertNotNull(evento.get("fechaCreacion"));
        
        @SuppressWarnings("unchecked")
        Map<String, Object> datos = (Map<String, Object>) evento.get("datos");
        assertNotNull(datos.get("fechaEliminacion"));
    }

    @Test
    @DisplayName("Publicar evento - Múltiples llamadas")
    void testPublicarEventoEliminacion_MultipleCalls() {
        // Given
        doNothing().when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

        // When - Llamar múltiples veces
        eventoPublisher.publicarEventoEliminacion("user1", "user1@example.com");
        eventoPublisher.publicarEventoEliminacion("user2", "user2@example.com");
        eventoPublisher.publicarEventoEliminacion("user3", null);

        // Then
        verify(rabbitTemplate, times(3)).convertAndSend(
                eq(RabbitMQConfig.DOMINIO_EVENTS_EXCHANGE),
                eq("auth.deleted"),
                any(Object.class)
        );
    }

    @Test
    @DisplayName("Publicar evento - Estructura del evento completa")
    void testPublicarEventoEliminacion_EventStructure() {
        // Given
        doNothing().when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

        // When
        eventoPublisher.publicarEventoEliminacion(testUsuario, testCorreo);

        // Then
        verify(rabbitTemplate).convertAndSend(
                anyString(),
                anyString(),
                eventoCaptor.capture()
        );

        Map<String, Object> evento = eventoCaptor.getValue();
        
        // Verify all required fields are present
        assertAll("Verificar estructura del evento",
            () -> assertNotNull(evento.get("id"), "ID debe existir"),
            () -> assertNotNull(evento.get("tipoAccion"), "Tipo de acción debe existir"),
            () -> assertNotNull(evento.get("fechaCreacion"), "Fecha de creación debe existir"),
            () -> assertNotNull(evento.get("datos"), "Datos deben existir"),
            () -> assertEquals("ELIMINACION_USUARIO", evento.get("tipoAccion"), "Tipo de acción correcto")
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> datos = (Map<String, Object>) evento.get("datos");
        
        assertAll("Verificar datos del evento",
            () -> assertNotNull(datos.get("usuario"), "Usuario debe existir"),
            () -> assertNotNull(datos.get("correo"), "Correo debe existir (puede ser vacío)"),
            () -> assertNotNull(datos.get("fechaEliminacion"), "Fecha de eliminación debe existir")
        );
    }
}

