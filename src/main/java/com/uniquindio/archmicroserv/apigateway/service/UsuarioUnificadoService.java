package com.uniquindio.archmicroserv.apigateway.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Service
public class UsuarioUnificadoService {

    private static final Logger log = LoggerFactory.getLogger(UsuarioUnificadoService.class);
    private final DomainServiceClient domainServiceClient;
    private final GestionPerfilServiceClient gestionPerfilServiceClient;

    // Constructor explícito para inyección de dependencias
    public UsuarioUnificadoService(
            DomainServiceClient domainServiceClient,
            GestionPerfilServiceClient gestionPerfilServiceClient) {
        this.domainServiceClient = domainServiceClient;
        this.gestionPerfilServiceClient = gestionPerfilServiceClient;
    }


    public Mono<Map<String, Object>> actualizarUsuarioCompleto(
            String usuario, 
            Map<String, Object> requestBody, 
            String authToken) {
        log.info("Actualizando datos completos del usuario: {}", usuario);

        // Separar datos de seguridad y perfil
        Map<String, Object> datosSeguridad = new HashMap<>();
        Map<String, Object> datosPerfil = new HashMap<>();

        // Campos de seguridad (del Domain Service)
        if (requestBody.containsKey("correo")) {
            datosSeguridad.put("correo", requestBody.get("correo"));
        }
        if (requestBody.containsKey("clave")) {
            datosSeguridad.put("clave", requestBody.get("clave"));
        }
        if (requestBody.containsKey("numeroTelefono")) {
            datosSeguridad.put("numeroTelefono", requestBody.get("numeroTelefono"));
        }

        // Campos de perfil (del Gestion Perfil Service)
        if (requestBody.containsKey("apodo")) {
            datosPerfil.put("apodo", requestBody.get("apodo"));
        }
        if (requestBody.containsKey("urlPaginaPersonal")) {
            datosPerfil.put("urlPaginaPersonal", requestBody.get("urlPaginaPersonal"));
        }
        if (requestBody.containsKey("informacionContactoPublica")) {
            datosPerfil.put("informacionContactoPublica", requestBody.get("informacionContactoPublica"));
        }
        if (requestBody.containsKey("direccionCorrespondencia")) {
            datosPerfil.put("direccionCorrespondencia", requestBody.get("direccionCorrespondencia"));
        }
        if (requestBody.containsKey("biografia")) {
            datosPerfil.put("biografia", requestBody.get("biografia"));
        }
        if (requestBody.containsKey("organizacion")) {
            datosPerfil.put("organizacion", requestBody.get("organizacion"));
        }
        if (requestBody.containsKey("paisResidencia")) {
            datosPerfil.put("paisResidencia", requestBody.get("paisResidencia"));
        }
        // Links de redes sociales
        if (requestBody.containsKey("linkFacebook")) {
            datosPerfil.put("linkFacebook", requestBody.get("linkFacebook"));
        }
        if (requestBody.containsKey("linkTwitter")) {
            datosPerfil.put("linkTwitter", requestBody.get("linkTwitter"));
        }
        if (requestBody.containsKey("linkLinkedIn")) {
            datosPerfil.put("linkLinkedIn", requestBody.get("linkLinkedIn"));
        }
        if (requestBody.containsKey("linkInstagram")) {
            datosPerfil.put("linkInstagram", requestBody.get("linkInstagram"));
        }
        if (requestBody.containsKey("linkGithub")) {
            datosPerfil.put("linkGithub", requestBody.get("linkGithub"));
        }
        if (requestBody.containsKey("linkOtraRed")) {
            datosPerfil.put("linkOtraRed", requestBody.get("linkOtraRed"));
        }

        // Si hay datos de seguridad, actualizarlos primero - si falla, no continuar
        if (!datosSeguridad.isEmpty()) {
            return domainServiceClient.actualizarUsuario(usuario, datosSeguridad, authToken)
                    .doOnSuccess(response -> log.info("Actualización de seguridad exitosa para usuario: {}", usuario))
                    .doOnError(error -> log.error("Error en actualización de seguridad - Tipo: {}, Mensaje: {}", 
                        error.getClass().getName(), error.getMessage()))
                    .flatMap(seguridadResponse -> {
                        // Si la actualización de seguridad fue exitosa, actualizar perfil si hay datos
                        if (!datosPerfil.isEmpty()) {
                            log.info("Procediendo a actualizar perfil para usuario: {}", usuario);
                            return gestionPerfilServiceClient.actualizarPerfil(usuario, datosPerfil)
                                    .map(perfilResponse -> {
                                        Map<String, Object> resultado = new HashMap<>();
                                        resultado.put("mensaje", "Usuario actualizado exitosamente");
                                        resultado.put("datosSeguridad", seguridadResponse);
                                        resultado.put("datosPerfil", perfilResponse);
                                        return resultado;
                                    })
                                    .onErrorResume(error -> {
                                        log.warn("Error actualizando perfil, pero seguridad se actualizó: {}", error.getMessage());
                                        Map<String, Object> resultado = new HashMap<>();
                                        resultado.put("mensaje", "Usuario actualizado parcialmente (solo seguridad)");
                                        resultado.put("datosSeguridad", seguridadResponse);
                                        resultado.put("datosPerfil", new HashMap<>());
                                        return Mono.just(resultado);
                                    });
                        } else {
                            // Solo actualización de seguridad
                            Map<String, Object> resultado = new HashMap<>();
                            resultado.put("mensaje", "Usuario actualizado exitosamente");
                            resultado.put("datosSeguridad", seguridadResponse);
                            resultado.put("datosPerfil", new HashMap<>());
                            return Mono.just(resultado);
                        }
                    })
                    .doOnSuccess(result -> log.info("Datos completos del usuario actualizados exitosamente"))
                    .doOnError(error -> log.error("Error actualizando datos completos: {}", error.getMessage()));
        } else if (!datosPerfil.isEmpty()) {
            // Solo actualización de perfil
            return gestionPerfilServiceClient.actualizarPerfil(usuario, datosPerfil)
                    .map(perfilResponse -> {
                        Map<String, Object> resultado = new HashMap<>();
                        resultado.put("mensaje", "Usuario actualizado exitosamente");
                        resultado.put("datosSeguridad", new HashMap<>());
                        resultado.put("datosPerfil", perfilResponse);
                        return resultado;
                    })
                    .doOnSuccess(result -> log.info("Datos de perfil actualizados exitosamente"))
                    .doOnError(error -> log.error("Error actualizando perfil: {}", error.getMessage()));
        } else {
            // No hay datos para actualizar
            Map<String, Object> resultado = new HashMap<>();
            resultado.put("mensaje", "No hay datos para actualizar");
            resultado.put("datosSeguridad", new HashMap<>());
            resultado.put("datosPerfil", new HashMap<>());
            return Mono.just(resultado);
        }
    }
}

