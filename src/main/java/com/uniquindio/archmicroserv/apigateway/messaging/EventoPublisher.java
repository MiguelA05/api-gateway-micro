package com.uniquindio.archmicroserv.apigateway.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uniquindio.archmicroserv.apigateway.config.RabbitMQConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class EventoPublisher {

    private static final Logger log = LoggerFactory.getLogger(EventoPublisher.class);
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    // Constructor explícito para inyección de dependencias
    public EventoPublisher(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    public void publicarEventoEliminacion(String usuario, String correo) {
        log.info("Publicando evento de eliminación de usuario: {}", usuario);
        
        // Crear el evento como Map para evitar problemas de serialización
        Map<String, Object> evento = new HashMap<>();
        evento.put("id", UUID.randomUUID().toString());
        evento.put("tipoAccion", "ELIMINACION_USUARIO");
        evento.put("fechaCreacion", Instant.now().toString());
        
        Map<String, Object> datos = new HashMap<>();
        datos.put("usuario", usuario);
        datos.put("correo", correo != null ? correo : "");
        datos.put("fechaEliminacion", Instant.now().toString());
        evento.put("datos", datos);

        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.DOMINIO_EVENTS_EXCHANGE,
                    "auth.deleted",
                    evento
            );
            log.info("Evento de eliminación publicado exitosamente para usuario: {}", usuario);
        } catch (Exception e) {
            log.error("Error publicando evento de eliminación para usuario {}: {}", usuario, e.getMessage(), e);
            // No lanzar excepción para no interrumpir el flujo de eliminación
        }
    }
}

