# API Gateway Microservice

API Gateway que orquesta las llamadas a los microservicios de seguridad (Domain Service) y gestión de perfiles (Gestion Perfil Service).

## 🎯 Funcionalidades

### Endpoints de Proxy Simple

1. **Registro de Usuario**
   - `POST /api/v1/auth/registro`
   - Redirige a `POST /v1/usuarios` del Domain Service

2. **Autenticación**
   - `POST /api/v1/auth/login`
   - Redirige a `POST /v1/sesiones` del Domain Service

3. **Eliminación de Usuario**
   - `DELETE /api/v1/auth/usuarios/{usuario}`
   - Redirige a `DELETE /v1/usuarios/{usuario}` del Domain Service
   - **Publica evento `ELIMINACION_USUARIO` en RabbitMQ**

### Endpoints de Unificación

1. **Consulta de Usuario Completo**
   - `GET /api/v1/usuarios/{usuario}`
   - Obtiene datos de seguridad del Domain Service
   - Obtiene datos de perfil del Gestion Perfil Service
   - Unifica y retorna respuesta combinada

2. **Actualización de Usuario Completo**
   - `PUT /api/v1/usuarios/{usuario}`
   - Divide los datos entre seguridad y perfil
   - Actualiza ambos servicios en paralelo
   - Retorna respuesta unificada

3. **Eliminación Completa de Usuario**
   - `DELETE /api/v1/usuarios/{usuario}`
   - Elimina del Domain Service
   - Elimina del Gestion Perfil Service
   - **Publica evento `ELIMINACION_USUARIO` en RabbitMQ**

## 🔧 Configuración

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
```

## 🚀 Uso

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

## 📋 Estructura del Proyecto

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
├── Dockerfile
├── pom.xml
└── README.md
```

## 🔌 Integración con Docker Compose

El API Gateway está configurado en `docker-compose.unified.yml` y se ejecuta en el puerto **8085**.

## 🏥 Health Check

```bash
curl http://localhost:8085/actuator/health
```

## 📝 Notas

- Todos los endpoints requieren autenticación excepto `/auth/registro` y `/auth/login`
- El token JWT debe enviarse en el header `Authorization: Bearer <token>`
- Los eventos de eliminación se publican en el exchange `dominio.events` con routing key `auth.deleted`
