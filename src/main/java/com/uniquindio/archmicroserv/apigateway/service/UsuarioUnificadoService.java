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

    // Nota: Este método ya no se usa directamente, se maneja en el controller
    // Se mantiene para futuras mejoras cuando el Domain Service tenga GET /usuarios/{usuario}
    public Mono<Map<String, Object>> obtenerUsuarioCompleto(String usuario, String authToken) {
        log.info("Obteniendo datos completos del usuario: {}", usuario);
        
        // El Domain Service no tiene endpoint GET /usuarios/{usuario} directo
        // Por ahora, obtenemos datos de perfil
        Mono<Map<String, Object>> datosPerfil = gestionPerfilServiceClient
                .obtenerPerfil(usuario)
                .onErrorResume(error -> {
                    log.warn("Error obteniendo datos de perfil: {}", error.getMessage());
                    return Mono.just(new HashMap<>());
                });

        return datosPerfil
                .map(perfil -> {
                    Map<String, Object> resultado = new HashMap<>();
                    resultado.put("usuario", usuario);
                    if (!perfil.isEmpty()) {
                        resultado.put("perfil", perfil);
                    }
                    return resultado;
                })
                .doOnSuccess(result -> log.info("Datos completos del usuario obtenidos exitosamente"))
                .doOnError(error -> log.error("Error obteniendo datos completos: {}", error.getMessage()));
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
        if (requestBody.containsKey("informacionContactoPublica")) {
            datosPerfil.put("informacionContactoPublica", requestBody.get("informacionContactoPublica"));
        }

        // Actualizar ambos servicios en paralelo
        Mono<Map<String, Object>> actualizacionSeguridad = datosSeguridad.isEmpty()
                ? Mono.just(new HashMap<>())
                : domainServiceClient.actualizarUsuario(usuario, datosSeguridad, authToken)
                        .onErrorResume(error -> {
                            log.warn("Error actualizando datos de seguridad: {}", error.getMessage());
                            return Mono.just(new HashMap<>());
                        });

        Mono<Map<String, Object>> actualizacionPerfil = datosPerfil.isEmpty()
                ? Mono.just(new HashMap<>())
                : gestionPerfilServiceClient.actualizarPerfil(usuario, datosPerfil)
                        .onErrorResume(error -> {
                            log.warn("Error actualizando datos de perfil: {}", error.getMessage());
                            return Mono.just(new HashMap<>());
                        });

        return Mono.zip(actualizacionSeguridad, actualizacionPerfil)
                .map(tuple -> {
                    Map<String, Object> resultado = new HashMap<>();
                    resultado.put("mensaje", "Usuario actualizado exitosamente");
                    resultado.put("datosSeguridad", tuple.getT1());
                    resultado.put("datosPerfil", tuple.getT2());
                    return resultado;
                })
                .doOnSuccess(result -> log.info("Datos completos del usuario actualizados exitosamente"))
                .doOnError(error -> log.error("Error actualizando datos completos: {}", error.getMessage()));
    }
}

