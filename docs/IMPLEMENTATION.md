# API Gateway Microservice - Documentación de Implementación

## Descripción General

El API Gateway es un microservicio desarrollado en Java con Spring Boot que actúa como punto de entrada único para las solicitudes de los clientes. Su función principal es orquestar las llamadas a los microservicios de seguridad (Domain Service) y gestión de perfiles (Gestion Perfil Service), proporcionando una API unificada y simplificada.

## Arquitectura

### Componentes Principales

El microservicio está estructurado en las siguientes capas:

1. **Controllers**: Manejan las solicitudes HTTP y definen los endpoints de la API
2. **Services**: Contienen la lógica de negocio y orquestación entre servicios
3. **Clients**: Realizan las llamadas HTTP a los microservicios backend
4. **Messaging**: Gestiona la publicación de eventos a RabbitMQ
5. **Configuration**: Configuración de WebClient, RabbitMQ y CORS

### Tecnologías Utilizadas

- **Spring Boot 3.x**: Framework principal
- **Spring WebFlux**: Programación reactiva para manejo de peticiones HTTP
- **Project Reactor**: Biblioteca para programación reactiva (Mono, Flux)
- **Spring AMQP**: Integración con RabbitMQ para mensajería
- **Jackson**: Serialización/deserialización JSON
- **Lombok**: Reducción de código boilerplate

## Endpoints de la API

### Endpoints de Proxy Simple

Estos endpoints redirigen directamente las solicitudes a los microservicios backend sin transformación adicional.

#### Registro de Usuario

- **Endpoint**: `POST /api/v1/auth/registro`
- **Descripción**: Registra un nuevo usuario en el sistema
- **Redirección**: `POST /v1/usuarios` del Domain Service
- **Autenticación**: No requerida
- **Request Body**:
```json
{
  "usuario": "testuser",
  "correo": "test@example.com",
  "clave": "password123"
}
```

#### Autenticación

- **Endpoint**: `POST /api/v1/auth/login`
- **Descripción**: Autentica un usuario y genera un token JWT
- **Redirección**: `POST /v1/sesiones` del Domain Service
- **Autenticación**: No requerida
- **Request Body**:
```json
{
  "usuario": "testuser",
  "clave": "password123"
}
```

#### Eliminación de Usuario (Auth)

- **Endpoint**: `DELETE /api/v1/auth/usuarios/{usuario}`
- **Descripción**: Elimina un usuario del Domain Service y publica evento de eliminación
- **Redirección**: `DELETE /v1/usuarios/{usuario}` del Domain Service
- **Autenticación**: Requerida (JWT Bearer Token)
- **Evento**: Publica evento `ELIMINACION_USUARIO` en RabbitMQ

### Endpoints de Unificación

Estos endpoints combinan datos de múltiples microservicios para proporcionar una vista unificada del usuario.

#### Consulta de Usuario Completo

- **Endpoint**: `GET /api/v1/usuarios/{usuario}`
- **Descripción**: Obtiene datos completos del usuario combinando información de seguridad y perfil
- **Autenticación**: Requerida (JWT Bearer Token)
- **Proceso**:
  1. Obtiene datos de seguridad del Domain Service
  2. Obtiene datos de perfil del Gestion Perfil Service
  3. Unifica y retorna respuesta combinada

#### Actualización de Usuario Completo

- **Endpoint**: `PUT /api/v1/usuarios/{usuario}`
- **Descripción**: Actualiza datos de seguridad y perfil en paralelo
- **Autenticación**: Requerida (JWT Bearer Token)
- **Proceso**:
  1. Divide los datos entre seguridad y perfil
  2. Actualiza ambos servicios en paralelo usando `Mono.zip()`
  3. Retorna respuesta unificada con el resultado de ambas actualizaciones
- **Request Body**:
```json
{
  "correo": "newemail@example.com",
  "apodo": "Test User",
  "biografia": "Mi biografía",
  "informacionPublica": true
}
```

#### Eliminación Completa de Usuario

- **Endpoint**: `DELETE /api/v1/usuarios/{usuario}`
- **Descripción**: Elimina el usuario de ambos servicios y publica evento
- **Autenticación**: Requerida (JWT Bearer Token)
- **Proceso**:
  1. Elimina del Domain Service
  2. Elimina del Gestion Perfil Service
  3. Publica evento `ELIMINACION_USUARIO` en RabbitMQ

## Componentes de Implementación

### Controllers

#### AuthController

Maneja las operaciones de autenticación y registro de usuarios.

- **Métodos principales**:
  - `registrarUsuario()`: Proxy para registro en Domain Service
  - `autenticar()`: Proxy para autenticación en Domain Service
  - `eliminarUsuario()`: Elimina usuario y publica evento

#### UsuarioController

Maneja las operaciones de usuarios unificados que combinan datos de múltiples servicios.

- **Métodos principales**:
  - `obtenerUsuario()`: Obtiene datos completos del usuario
  - `actualizarUsuario()`: Actualiza datos en ambos servicios
  - `eliminarUsuario()`: Elimina usuario de ambos servicios

### Services

#### DomainServiceClient

Cliente HTTP reactivo para comunicarse con el Domain Service.

- **Configuración**: Utiliza `WebClient` configurado en `WebClientConfig`
- **Métodos**:
  - `registrarUsuario()`: Registro de nuevos usuarios
  - `autenticar()`: Autenticación de usuarios
  - `eliminarUsuario()`: Eliminación de usuarios
  - `actualizarUsuario()`: Actualización de datos de seguridad

#### GestionPerfilServiceClient

Cliente HTTP reactivo para comunicarse con el Gestion Perfil Service.

- **Métodos**:
  - `obtenerPerfil()`: Obtiene perfil de usuario
  - `actualizarPerfil()`: Actualiza perfil de usuario
  - `eliminarPerfil()`: Elimina perfil de usuario

#### UsuarioUnificadoService

Servicio que orquesta la unificación de datos de múltiples servicios.

- **Métodos**:
  - `obtenerUsuarioCompleto()`: Combina datos de seguridad y perfil
  - `actualizarUsuarioCompleto()`: Actualiza datos en ambos servicios en paralelo
  - **Lógica de separación**: Identifica qué campos pertenecen a cada servicio

### Messaging

#### EventoPublisher

Servicio para publicar eventos de dominio a RabbitMQ.

- **Eventos publicados**:
  - `ELIMINACION_USUARIO`: Se publica cuando se elimina un usuario
- **Exchange**: `dominio.events` (configurado en `RabbitMQConfig`)
- **Routing Key**: `auth.deleted`
- **Formato del evento**:
```json
{
  "id": "uuid",
  "tipoAccion": "ELIMINACION_USUARIO",
  "fechaCreacion": "ISO-8601 timestamp",
  "datos": {
    "usuario": "username",
    "correo": "email@example.com",
    "fechaEliminacion": "ISO-8601 timestamp"
  }
}
```

### Configuration

#### WebClientConfig

Configuración del cliente HTTP reactivo para comunicación con microservicios.

- **Timeout**: Configurado para evitar bloqueos
- **Codecs**: Configuración de serialización JSON
- **Base URLs**: Configuradas desde variables de entorno

#### RabbitMQConfig

Configuración de RabbitMQ para mensajería asíncrona.

- **Exchange**: `dominio.events` (tipo topic)
- **Connection Factory**: Configurado desde variables de entorno
- **Message Converter**: Jackson para serialización JSON
- **Condicional**: Solo se carga si `spring.rabbitmq.host` está configurado

#### CorsConfig

Configuración de CORS para permitir solicitudes desde el frontend.

- **Allowed Origins**: Configurable
- **Allowed Methods**: GET, POST, PUT, DELETE, OPTIONS
- **Allowed Headers**: Authorization, Content-Type

## Flujos de Procesamiento

### Flujo de Registro de Usuario

1. Cliente envía `POST /api/v1/auth/registro`
2. `AuthController.registrarUsuario()` recibe la solicitud
3. `DomainServiceClient.registrarUsuario()` realiza llamada HTTP al Domain Service
4. Domain Service procesa el registro y retorna respuesta
5. API Gateway retorna la respuesta al cliente

### Flujo de Consulta de Usuario Completo

1. Cliente envía `GET /api/v1/usuarios/{usuario}` con token JWT
2. `UsuarioController.obtenerUsuario()` recibe la solicitud
3. `UsuarioUnificadoService.obtenerUsuarioCompleto()` se ejecuta:
   - Llama en paralelo a `DomainServiceClient` y `GestionPerfilServiceClient`
   - Combina las respuestas
   - Retorna datos unificados
4. API Gateway retorna respuesta combinada al cliente

### Flujo de Actualización de Usuario Completo

1. Cliente envía `PUT /api/v1/usuarios/{usuario}` con datos
2. `UsuarioController.actualizarUsuario()` recibe la solicitud
3. `UsuarioUnificadoService.actualizarUsuarioCompleto()` se ejecuta:
   - Separa datos entre seguridad y perfil
   - Actualiza ambos servicios en paralelo usando `Mono.zip()`
   - Combina resultados
4. API Gateway retorna respuesta unificada al cliente

### Flujo de Eliminación de Usuario

1. Cliente envía `DELETE /api/v1/usuarios/{usuario}`
2. `UsuarioController.eliminarUsuario()` recibe la solicitud
3. Se ejecutan en paralelo:
   - Eliminación en Domain Service
   - Eliminación en Gestion Perfil Service
4. `EventoPublisher.publicarEventoEliminacion()` publica evento en RabbitMQ
5. API Gateway retorna confirmación al cliente

## Configuración

### Variables de Entorno

```properties
# Domain Service
DOMAIN_SERVICE_URL=http://domain-service:8080
DOMAIN_SERVICE_BASE_PATH=/v1

# Gestion Perfil Service
GESTION_PERFIL_SERVICE_URL=http://gestion-perfil-service:8080
GESTION_PERFIL_SERVICE_BASE_PATH=/api/v1/perfiles

# RabbitMQ
SPRING_RABBITMQ_HOST=rabbitmq
SPRING_RABBITMQ_PORT=5672
SPRING_RABBITMQ_USERNAME=domain_user
SPRING_RABBITMQ_PASSWORD=domain_pass
SPRING_RABBITMQ_VIRTUAL_HOST=foro

# Server
SERVER_PORT=8085
```

### application.properties

Configuración adicional en `src/main/resources/application.properties`:

- Configuración de logging
- Configuración de Spring Boot Actuator
- Configuración de timeouts para WebClient

## Programación Reactiva

El microservicio utiliza Project Reactor para programación reactiva:

- **Mono**: Representa un valor único o vacío (0 o 1 elemento)
- **Flux**: Representa una secuencia de valores (0 o N elementos)
- **Operadores**: `map()`, `flatMap()`, `zip()`, `onErrorResume()`, etc.

### Ejemplo de Uso

```java
Mono.zip(actualizacionSeguridad, actualizacionPerfil)
    .map(tuple -> {
        // Combinar resultados
        return resultado;
    })
    .onErrorResume(error -> {
        // Manejo de errores
        return Mono.just(errorResponse);
    });
```

## Manejo de Errores

El microservicio implementa manejo de errores reactivo:

- **onErrorResume()**: Captura errores y retorna un valor por defecto
- **Logging**: Registra errores para debugging
- **Códigos HTTP**: Retorna códigos apropiados (400, 401, 500, etc.)

### Estrategias de Manejo

1. **Errores de conexión**: Retorna error 500 con mensaje descriptivo
2. **Errores de autenticación**: Retorna error 401
3. **Errores de validación**: Retorna error 400
4. **Errores en servicios externos**: Se registran pero no interrumpen el flujo principal

## Testing

### Estructura de Tests

- **Unit Tests**: Pruebas de servicios y lógica de negocio
- **Integration Tests**: Pruebas de endpoints con `WebTestClient`
- **Mocking**: Uso de Mockito para simular servicios externos

### Cobertura de Tests

El proyecto incluye tests para:
- `AuthController`: 10 tests unitarios
- `UsuarioController`: 14 tests unitarios
- `UsuarioUnificadoService`: 11 tests unitarios
- `EventoPublisher`: 8 tests unitarios
- Tests de integración para endpoints principales

## Despliegue

### Docker

El microservicio incluye un `Dockerfile` para contenedorización:

```dockerfile
FROM openjdk:21-jdk-slim
# ... configuración del contenedor
```

### Docker Compose

Configurado en `docker-compose.unified.yml`:
- Puerto: 8085
- Dependencias: RabbitMQ, Domain Service, Gestion Perfil Service
- Health checks configurados

## Monitoreo y Health Checks

### Spring Boot Actuator

- **Health Endpoint**: `/actuator/health`
- **Info Endpoint**: `/actuator/info`
- **Metrics**: Disponibles en `/actuator/metrics`

### Logging

- **SLF4J + Logback**: Configuración de logging
- **Niveles**: INFO, WARN, ERROR
- **Formato**: JSON estructurado para producción

## Consideraciones de Seguridad

1. **Autenticación JWT**: Validación de tokens en endpoints protegidos
2. **CORS**: Configuración restrictiva de orígenes permitidos
3. **Validación de entrada**: Validación de datos de entrada
4. **Logging seguro**: No se registran datos sensibles (contraseñas, tokens)

## Mejoras Futuras

1. **Circuit Breaker**: Implementar resiliencia con Hystrix o Resilience4j
2. **Rate Limiting**: Limitar número de solicitudes por cliente
3. **Caching**: Cachear respuestas de consultas frecuentes
4. **API Versioning**: Soporte para múltiples versiones de API
5. **Documentación OpenAPI**: Generar documentación automática con Swagger

