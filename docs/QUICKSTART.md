# Guía de Inicio Rápido - API Gateway Microservice

Esta guía te permitirá poner en marcha el API Gateway en tu entorno local y ejecutar pruebas básicas en pocos minutos.

## Requisitos Previos

- Java 17 o superior
- Maven 3.6 o superior
- Docker y Docker Compose (opcional, para servicios dependientes)
- PostgreSQL (si se ejecuta localmente)
- RabbitMQ (si se ejecuta localmente)

## Instalación Rápida

### 1. Clonar y Compilar

```bash
cd api-gateway-micro
./mvnw clean install -DskipTests
```

### 2. Configurar Variables de Entorno

Crear archivo `src/main/resources/application-local.properties`:

```properties
# Domain Service
DOMAIN_SERVICE_URL=http://localhost:8080
DOMAIN_SERVICE_BASE_PATH=/v1

# Gestion Perfil Service
GESTION_PERFIL_SERVICE_URL=http://localhost:8081
GESTION_PERFIL_SERVICE_BASE_PATH=/api/v1/perfiles

# RabbitMQ (opcional para pruebas básicas)
SPRING_RABBITMQ_HOST=localhost
SPRING_RABBITMQ_PORT=5672
SPRING_RABBITMQ_USERNAME=guest
SPRING_RABBITMQ_PASSWORD=guest
SPRING_RABBITMQ_VIRTUAL_HOST=/

# Server
SERVER_PORT=8085
```

### 3. Ejecutar Localmente

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

O usando el JAR compilado:

```bash
java -jar target/api-gateway-micro-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

## Verificación Inicial

### Health Check

```bash
curl http://localhost:8085/actuator/health
```

Respuesta esperada:
```json
{"status":"UP"}
```

### Verificar que el servicio está escuchando

```bash
curl http://localhost:8085/actuator/info
```

## Pruebas Básicas

### 1. Prueba de Registro de Usuario

```bash
curl -X POST http://localhost:8085/api/v1/auth/registro \
  -H "Content-Type: application/json" \
  -d '{
    "usuario": "testuser",
    "correo": "test@example.com",
    "clave": "password123"
  }'
```

Respuesta esperada: HTTP 201 con mensaje de éxito

### 2. Prueba de Autenticación

```bash
curl -X POST http://localhost:8085/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "usuario": "testuser",
    "clave": "password123"
  }'
```

Respuesta esperada: HTTP 200 con token JWT

Guardar el token para pruebas posteriores:
```bash
TOKEN=$(curl -s -X POST http://localhost:8085/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"usuario":"testuser","clave":"password123"}' | jq -r '.token')
```

### 3. Prueba de Consulta de Usuario Completo

```bash
curl -X GET http://localhost:8085/api/v1/usuarios/testuser \
  -H "Authorization: Bearer $TOKEN"
```

Respuesta esperada: HTTP 200 con datos combinados de seguridad y perfil

### 4. Prueba de Actualización de Usuario

```bash
curl -X PUT http://localhost:8085/api/v1/usuarios/testuser \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "correo": "newemail@example.com",
    "apodo": "Test User Updated"
  }'
```

Respuesta esperada: HTTP 200 con datos actualizados

## Ejecutar Tests

### Tests Unitarios

```bash
./mvnw test
```

### Tests de Integración

```bash
./mvnw verify
```

### Tests con Cobertura

```bash
./mvnw test jacoco:report
```

Ver reporte en: `target/site/jacoco/index.html`

## Troubleshooting

### Error: Connection refused al Domain Service

Verificar que el Domain Service esté corriendo en el puerto configurado:
```bash
curl http://localhost:8080/v1/health
```

### Error: Connection refused a RabbitMQ

Verificar que RabbitMQ esté corriendo:
```bash
curl http://localhost:15672
```

O iniciar con Docker:
```bash
docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:3-management
```

### Error: Puerto 8085 ya en uso

Cambiar el puerto en `application-local.properties`:
```properties
SERVER_PORT=8086
```

## Próximos Pasos

- Revisar `docs/IMPLEMENTATION.md` para detalles de arquitectura
- Configurar servicios dependientes (Domain Service, Gestion Perfil Service)
- Revisar logs en `logs/` o salida de consola
- Explorar endpoints con Swagger UI si está habilitado

