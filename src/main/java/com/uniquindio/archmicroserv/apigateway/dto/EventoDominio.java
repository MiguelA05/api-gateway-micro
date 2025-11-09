package com.uniquindio.archmicroserv.apigateway.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record EventoDominio(
        @JsonProperty("id")
        String id,
        @JsonProperty("tipoAccion")
        String tipoAccion,
        @JsonProperty("fechaCreacion")
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        Instant fechaCreacion,
        @JsonProperty("datos")
        Map<String, Object> datos
) {
    public static EventoDominio of(String tipoAccion, Map<String, Object> datos) {
        return new EventoDominio(
                UUID.randomUUID().toString(),
                tipoAccion,
                Instant.now(),
                datos
        );
    }
}

