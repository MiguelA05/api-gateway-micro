# API Gateway Microservice

API Gateway desarrollado en Java con Spring Boot que actúa como punto de entrada único para las solicitudes de los clientes. Orquesta las llamadas a los microservicios de seguridad (Domain Service) y gestión de perfiles (Gestion Perfil Service), proporcionando una API unificada y simplificada.

## Funcionalidades

### Endpoints de Proxy Simple

Estos endpoints redirigen directamente las solicitudes a los microservicios backend sin transformación adicional.

#### Registro de Usuario

- **Endpoint**: `POST /api/v1/auth/registro`
- **Descripción**: Registra un nuevo usuario en el sistema
- **Redirección**: `POST /v1/usuarios` del Domain Service
- **Autenticación**: No requerida

#### Autenticación

- **Endpoint**: `POST /api/v1/auth/login`
- **Descripción**: Autentica un usuario y genera un token JWT
- **Redirección**: `POST /v1/sesiones` del Domain Service
- **Autenticación**: No requerida

#### Eliminación de Usuario

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
- **Proceso**: Obtiene datos de seguridad del Domain Service y datos de perfil del Gestion Perfil Service, luego unifica y retorna respuesta combinada

#### Actualización de Usuario Completo

- **Endpoint**: `PUT /api/v1/usuarios/{usuario}`
- **Descripción**: Actualiza datos de seguridad y perfil en paralelo
- **Autenticación**: Requerida (JWT Bearer Token)
- **Proceso**: Divide los datos entre seguridad y perfil, actualiza ambos servicios en paralelo usando `Mono.zip()`, retorna respuesta unificada

#### Eliminación Completa de Usuario

- **Endpoint**: `DELETE /api/v1/usuarios/{usuario}`
- **Descripción**: Elimina el usuario de ambos servicios y publica evento
- **Autenticación**: Requerida (JWT Bearer Token)
- **Proceso**: Elimina del Domain Service, elimina del Gestion Perfil Service, publica evento `ELIMINACION_USUARIO` en RabbitMQ

## Tecnologías

- Spring Boot 3.x
- Spring WebFlux (Programación reactiva)
- Project Reactor (Mono, Flux)
- Spring AMQP (RabbitMQ)
- Jackson (Serialización JSON)
- Lombok

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

## Uso

### Registro de Usuario

```bash
curl -X POST http://localhost:8085/api/v1/auth/registro \
  -H "Content-Type: application/json" \
  -d '{
    "usuario": "testuser",
    "correo": "test@example.com",
    "clave": "password123"
  }'
```

### Autenticación

```bash
curl -X POST http://localhost:8085/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "usuario": "testuser",
    "clave": "password123"
  }'
```

### Consulta de Usuario Completo

```bash
curl -X GET http://localhost:8085/api/v1/usuarios/testuser \
  -H "Authorization: Bearer <token>"
```

### Actualización de Usuario Completo

```bash
curl -X PUT http://localhost:8085/api/v1/usuarios/testuser \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "correo": "newemail@example.com",
    "apodo": "Test User",
    "biografia": "Mi biografía",
    "informacionPublica": true
  }'
```

### Eliminación de Usuario

```bash
curl -X DELETE http://localhost:8085/api/v1/usuarios/testuser \
  -H "Authorization: Bearer <token>"
```

## Estructura del Proyecto

```
api-gateway-micro/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/uniquindio/archmicroserv/apigateway/
│   │   │       ├── ApiGatewayApplication.java
│   │   │       ├── config/
│   │   │       │   ├── WebClientConfig.java
│   │   │       │   ├── RabbitMQConfig.java
│   │   │       │   └── CorsConfig.java
│   │   │       ├── controller/
│   │   │       │   ├── AuthController.java
│   │   │       │   └── UsuarioController.java
│   │   │       ├── service/
│   │   │       │   ├── DomainServiceClient.java
│   │   │       │   ├── GestionPerfilServiceClient.java
│   │   │       │   └── UsuarioUnificadoService.java
│   │   │       ├── messaging/
│   │   │       │   └── EventoPublisher.java
│   │   │       └── dto/
│   │   │           └── EventoDominio.java
│   │   └── resources/
│   │       └── application.properties
│   └── test/
│       ├── controller/
│       ├── service/
│       ├── messaging/
│       └── integration/
├── docs/
│   └── IMPLEMENTATION.md
├── Dockerfile
├── pom.xml
└── README.md
```

## Integración con Docker Compose

El API Gateway está configurado en `docker-compose.unified.yml` y se ejecuta en el puerto **8085**.

## Health Check

```bash
curl http://localhost:8085/actuator/health
```

## Testing

El proyecto incluye una suite completa de tests:

- **Unit Tests**: 43 tests unitarios
  - AuthController: 10 tests
  - UsuarioController: 14 tests
  - UsuarioUnificadoService: 11 tests
  - EventoPublisher: 8 tests
- **Integration Tests**: 22 tests de integración
  - AuthControllerIntegrationTest: 10 tests
  - UsuarioControllerIntegrationTest: 12 tests

## Programación Reactiva

El microservicio utiliza Project Reactor para programación reactiva:
- **Mono**: Representa un valor único o vacío (0 o 1 elemento)
- **Flux**: Representa una secuencia de valores (0 o N elementos)
- **Operadores**: `map()`, `flatMap()`, `zip()`, `onErrorResume()`, etc.

## Integración con RabbitMQ

El API Gateway publica eventos de dominio a RabbitMQ:
- **Exchange**: `dominio.events` (tipo topic)
- **Routing Key**: `auth.deleted`
- **Evento**: `ELIMINACION_USUARIO` cuando se elimina un usuario

## Notas

- Todos los endpoints requieren autenticación excepto `/auth/registro` y `/auth/login`
- El token JWT debe enviarse en el header `Authorization: Bearer <token>`
- Los eventos de eliminación se publican en el exchange `dominio.events` con routing key `auth.deleted`
- Para documentación detallada, consultar `docs/IMPLEMENTATION.md`
