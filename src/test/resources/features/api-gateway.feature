# language: es
# =============================================================================
# ARCHIVO DE CARACTERÍSTICAS (FEATURES) - API GATEWAY
# =============================================================================
# 
# Este archivo define las características y escenarios de prueba para el API Gateway.
# Cubre las operaciones principales: registro, login, consulta y actualización de usuarios.

Característica: API Gateway - Gestión de usuarios unificada
  
  Antecedentes:
    Dado que el servicio API Gateway está disponible

  # ===== REGISTRO DE USUARIO =====
  Escenario: Registrar un nuevo usuario a través del API Gateway
    Cuando registro un usuario con datos válidos en el API Gateway
    Entonces la respuesta debe tener estado 201
    Y el cuerpo debe indicar éxito

  # ===== AUTENTICACIÓN =====
  Escenario: Iniciar sesión a través del API Gateway
    Dado que existe un usuario registrado válido
    Cuando inicio sesión con credenciales correctas en el API Gateway
    Entonces la respuesta debe tener estado 200
    Y debo obtener un token JWT válido

  # ===== CONSULTA DE USUARIO COMPLETO =====
  Escenario: Consultar datos completos de usuario unificado
    Dado que existe un usuario registrado válido
    Y que he iniciado sesión exitosamente
    Cuando consulto los datos completos del usuario
    Entonces la respuesta debe tener estado 200
    Y el cuerpo debe contener datos de seguridad
    Y el cuerpo debe contener datos de perfil

  # ===== ACTUALIZACIÓN DE USUARIO =====
  Escenario: Actualizar datos de usuario a través del API Gateway
    Dado que existe un usuario registrado válido
    Y que he iniciado sesión exitosamente
    Cuando actualizo los datos del usuario
    Entonces la respuesta debe tener estado 200
    Y el cuerpo debe indicar éxito

  # ===== ELIMINACIÓN DE USUARIO (AUTH CONTROLLER) =====
  # Este endpoint requiere rol ADMIN, se usa el usuario admin creado en el setup
  Escenario: Eliminar usuario a través del Auth Controller
    Dado que existe un usuario registrado válido
    Y que he iniciado sesión exitosamente
    Cuando elimino el usuario a través del Auth Controller
    Entonces la respuesta debe tener estado 200
    Y el cuerpo debe indicar éxito

  # ===== ELIMINACIÓN DE USUARIO COMPLETO (USUARIO CONTROLLER) =====
  Escenario: Eliminar usuario completo a través del Usuario Controller
    Dado que existe un usuario registrado válido
    Y que he iniciado sesión exitosamente
    Cuando elimino el usuario completo a través del Usuario Controller
    Entonces la respuesta debe tener estado 200
    Y el cuerpo debe indicar éxito

  # ===== HEALTH CHECK =====
  Escenario: Verificar salud del API Gateway
    Cuando consulto el endpoint de health check
    Entonces la respuesta debe tener estado 200
    Y el cuerpo debe indicar que el servicio está UP

