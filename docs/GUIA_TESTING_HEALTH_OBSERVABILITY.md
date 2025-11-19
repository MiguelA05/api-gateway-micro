# Guía Paso a Paso: Testing de Health Checks y Observabilidad

Esta guía te permitirá verificar que los mecanismos de salud (health checks) y observabilidad funcionan correctamente cuando un servicio se detiene.

## 📋 Tabla de Contenidos

1. [Requisitos Previos](#requisitos-previos)
2. [Verificación Inicial del Estado](#verificación-inicial-del-estado)
3. [Paso 1: Verificar Estado Inicial de los Servicios](#paso-1-verificar-estado-inicial-de-los-servicios)
4. [Paso 2: Detener un Servicio](#paso-2-detener-un-servicio)
5. [Paso 3: Verificar Health Check App Detecta el Fallo](#paso-3-verificar-health-check-app-detecta-el-fallo)
6. [Paso 4: Verificar Observabilidad en Loki/Grafana](#paso-4-verificar-observabilidad-en-lokigrafana)
7. [Paso 5: Verificar Notificaciones (si están configuradas)](#paso-5-verificar-notificaciones-si-están-configuradas)
8. [Paso 6: Reiniciar el Servicio y Verificar Recuperación](#paso-6-reiniciar-el-servicio-y-verificar-recuperación)
9. [Troubleshooting](#troubleshooting)

---

## Requisitos Previos

- Docker y Docker Compose instalados
- Todos los servicios corriendo (verificar con `docker-compose ps`)
- Acceso a los siguientes puertos:
  - **8082**: Health Check App Micro
  - **3100**: Loki
  - **3000**: Grafana
  - **8085**: API Gateway
  - **8084**: Gestion Perfil
  - **8081**: JWT Service

---

## Verificación Inicial del Estado

Antes de comenzar, verifica que todos los servicios estén corriendo:

```bash
# Verificar contenedores activos
docker-compose ps

# Verificar que Health Check App está corriendo
curl http://localhost:8082/health

# Verificar que Loki está disponible
curl http://localhost:3100/ready

# Verificar que Grafana está disponible
curl http://localhost:3000/api/health
```

---

## Paso 1: Verificar Estado Inicial de los Servicios

### 1.1. Consultar Estado Global en Health Check App

```bash
# Obtener estado de todos los servicios monitoreados
curl -s http://localhost:8082/health | jq '.'
```

**Respuesta esperada** (ejemplo):
```json
[
  {
    "name": "api-gateway",
    "endpoint": "http://api-gateway:8085/actuator/health",
    "status": "UP",
    "lastCheck": "2024-11-17T22:00:00Z",
    "frequency": 30,
    "failureCount": 0
  },
  {
    "name": "gestion-perfil",
    "endpoint": "http://gestion-perfil:8084/actuator/health",
    "status": "UP",
    "lastCheck": "2024-11-17T22:00:00Z",
    "frequency": 30,
    "failureCount": 0
  },
  {
    "name": "jwt-service",
    "endpoint": "http://jwt-service:8081/v1/health",
    "status": "UP",
    "lastCheck": "2024-11-17T22:00:00Z",
    "frequency": 30,
    "failureCount": 0
  }
]
```

### 1.2. Consultar Estado de un Servicio Específico

```bash
# Consultar estado del API Gateway
curl -s http://localhost:8082/health/api-gateway | jq '.'
```

### 1.3. Verificar Health Check Directo del Servicio

```bash
# Verificar que el API Gateway responde directamente
curl -s http://localhost:8085/actuator/health | jq '.'
```

**Respuesta esperada**:
```json
{
  "status": "UP",
  "components": {
    "diskSpace": { "status": "UP" },
    "ping": { "status": "UP" }
  }
}
```

### 1.4. Verificar Logs en Grafana (Opcional)

1. Abre Grafana en `http://localhost:3000`
2. Ve a **Explore** (icono de brújula en el menú lateral)
3. Selecciona **Loki** como data source
4. Ejecuta la query:
   ```
   {service="api-gateway-micro"}
   ```
5. Verifica que aparezcan logs recientes del servicio

---

## Paso 2: Detener un Servicio

Vamos a detener el **API Gateway** como ejemplo. Puedes elegir cualquier otro servicio.

### 2.1. Identificar el Contenedor

```bash
# Listar contenedores para encontrar el nombre exacto
docker ps --filter "name=api-gateway" --format "{{.Names}}"
```

### 2.2. Detener el Servicio

**Opción A: Usando Docker Compose (Recomendado)**
```bash
# Detener el servicio específico
docker-compose stop api-gateway

# O si el servicio tiene otro nombre en docker-compose
docker-compose stop api-gateway-micro
```

**Opción B: Usando Docker directamente**
```bash
# Detener el contenedor
docker stop <nombre-contenedor>

# Ejemplo:
docker stop api-gateway-micro-1
```

### 2.3. Verificar que el Servicio Está Detenido

```bash
# Verificar que el contenedor está detenido
docker ps --filter "name=api-gateway"

# Intentar acceder al health check (debe fallar)
curl -v http://localhost:8085/actuator/health
```

**Respuesta esperada**: `Connection refused` o timeout

---

## Paso 3: Verificar Health Check App Detecta el Fallo

El Health Check App verifica los servicios cada **30 segundos** (configurable). Espera máximo 35 segundos para que detecte el fallo.

### 3.1. Monitorear el Estado en Tiempo Real

```bash
# Monitorear el estado cada 5 segundos
watch -n 5 'curl -s http://localhost:8082/health/api-gateway | jq ".status, .lastCheck, .failureCount"'
```

**O ejecutar múltiples veces manualmente:**
```bash
# Primera verificación (inmediata)
curl -s http://localhost:8082/health/api-gateway | jq '.status'

# Esperar 35 segundos y verificar de nuevo
sleep 35
curl -s http://localhost:8082/health/api-gateway | jq '.'
```

### 3.2. Verificar Cambio de Estado

Después de 30-35 segundos, el estado debería cambiar:

**Antes**:
```json
{
  "status": "UP",
  "lastCheck": "2024-11-17T22:00:00Z",
  "failureCount": 0
}
```

**Después** (esperado):
```json
{
  "name": "api-gateway",
  "endpoint": "http://api-gateway:8085/actuator/health",
  "status": "DOWN",
  "lastCheck": "2024-11-17T22:00:35Z",
  "lastFailure": "2024-11-17T22:00:35Z",
  "failureCount": 1,
  "frequency": 30
}
```

### 3.3. Verificar Estado Global

```bash
# Ver todos los servicios y sus estados
curl -s http://localhost:8082/health | jq '.[] | {name: .name, status: .status, lastCheck: .lastCheck}'
```

**Salida esperada**:
```json
{
  "name": "api-gateway",
  "status": "DOWN",
  "lastCheck": "2024-11-17T22:00:35Z"
}
{
  "name": "gestion-perfil",
  "status": "UP",
  "lastCheck": "2024-11-17T22:00:30Z"
}
```

### 3.4. Verificar Logs del Health Check App

```bash
# Ver logs del contenedor de Health Check App
docker logs health-check-app-micro --tail 50 --follow
```

**Logs esperados**:
```
❌ Error verificando salud de api-gateway: Get "http://api-gateway:8085/actuator/health": dial tcp: lookup api-gateway: no such host
🔄 Estado actualizado: api-gateway -> DOWN
📧 Enviando notificación de fallo para api-gateway...
```

---

## Paso 4: Verificar Observabilidad en Loki/Grafana

### 4.1. Acceder a Grafana

1. Abre `http://localhost:3000` en tu navegador
2. Login (por defecto: `admin`/`admin` o anónimo si está configurado)

### 4.2. Consultar Logs del Servicio Detenido

1. Ve a **Explore** (icono de brújula)
2. Selecciona **Loki** como data source
3. Ejecuta la query:
   ```
   {service="api-gateway-micro"}
   ```
4. Ajusta el rango de tiempo a **Últimos 15 minutos**

**Resultado esperado**: Deberías ver:
- Logs del servicio antes de detenerse
- Logs de errores de conexión si otros servicios intentan comunicarse con él
- Logs del Health Check App reportando el fallo

### 4.3. Consultar Logs de Errores

```
{level="ERROR"} |= "api-gateway"
```

O más específico:
```
{service="api-gateway-micro", level="ERROR"}
```

### 4.4. Consultar Logs del Health Check App

```
{service="health-check-app-micro"} |= "DOWN"
```

O:
```
{service="health-check-app-micro"} |= "api-gateway"
```

### 4.5. Verificar Logs en Loki Directamente (API)

```bash
# Consultar logs recientes del API Gateway
curl -G -s "http://localhost:3100/loki/api/v1/query_range" \
  --data-urlencode 'query={service="api-gateway-micro"}' \
  --data-urlencode 'start='$(date -d '15 minutes ago' +%s)000000000 \
  --data-urlencode 'end='$(date +%s)000000000 \
  | jq '.data.result[0].values[-5:]'
```

### 4.6. Verificar Logs de Otros Servicios Afectados

Si otros servicios intentan comunicarse con el API Gateway, deberías ver errores:

```bash
# En Grafana, consultar errores de conexión
{level="ERROR"} |= "Connection refused" |= "api-gateway"
```

O:
```
{service="gestion-perfil-micro"} |= "api-gateway" |= "error"
```

---

## Paso 5: Verificar Notificaciones (si están configuradas)

Si el Health Check App tiene configurado SMTP, debería enviar un correo cuando detecta el primer fallo.

### 5.1. Verificar Configuración SMTP

```bash
# Ver variables de entorno del Health Check App
docker exec health-check-app-micro env | grep SMTP
```

### 5.2. Verificar Logs de Notificación

```bash
# Ver logs del Health Check App buscando notificaciones
docker logs health-check-app-micro 2>&1 | grep -i "notificacion\|email\|smtp"
```

**Logs esperados**:
```
📧 Enviando notificación de fallo para api-gateway...
✅ Notificación enviada exitosamente a miraortega2020@gmail.com
```

### 5.3. Verificar Correo Electrónico

Revisa la bandeja de entrada (y spam) del correo configurado en `SMTP_TO`. Deberías recibir un correo con:
- Nombre del servicio que falló
- Endpoint que falló
- Fecha y hora del fallo
- Detalles del error

---

## Paso 6: Reiniciar el Servicio y Verificar Recuperación

### 6.1. Reiniciar el Servicio

```bash
# Reiniciar usando Docker Compose
docker-compose start api-gateway

# O usando Docker directamente
docker start <nombre-contenedor>
```

### 6.2. Verificar que el Servicio Está Activo

```bash
# Verificar que el contenedor está corriendo
docker ps --filter "name=api-gateway"

# Verificar health check directo
curl -s http://localhost:8085/actuator/health | jq '.'
```

**Respuesta esperada**:
```json
{
  "status": "UP",
  "components": {
    "diskSpace": { "status": "UP" },
    "ping": { "status": "UP" }
  }
}
```

### 6.3. Monitorear Recuperación en Health Check App

```bash
# Monitorear el estado cada 5 segundos
watch -n 5 'curl -s http://localhost:8082/health/api-gateway | jq "{status: .status, lastCheck: .lastCheck, failureCount: .failureCount}"'
```

**Proceso esperado**:
1. **Estado inicial**: `DOWN` (después de reiniciar, puede tardar hasta 30 segundos)
2. **Después de 30-35 segundos**: Cambia a `UP`
3. **failureCount**: Se mantiene en el valor anterior (no se resetea automáticamente)

### 6.4. Verificar Logs de Recuperación

```bash
# Ver logs del Health Check App
docker logs health-check-app-micro --tail 20 --follow
```

**Logs esperados**:
```
✅ Verificación exitosa: api-gateway -> UP
🔄 Estado actualizado: api-gateway -> UP
```

### 6.5. Verificar en Grafana

En Grafana, consulta los logs más recientes:
```
{service="health-check-app-micro"} |= "api-gateway" |= "UP"
```

Deberías ver el log de recuperación.

---

## Troubleshooting

### Problema: Health Check App no detecta el fallo

**Solución**:
1. Verificar que el Health Check App está corriendo:
   ```bash
   docker ps --filter "name=health-check"
   ```

2. Verificar logs del Health Check App:
   ```bash
   docker logs health-check-app-micro --tail 100
   ```

3. Verificar que el servicio está registrado:
   ```bash
   curl -s http://localhost:8082/health | jq '.[] | select(.name=="api-gateway")'
   ```

4. Verificar conectividad de red:
   ```bash
   docker exec health-check-app-micro ping -c 2 api-gateway
   ```

### Problema: No aparecen logs en Grafana

**Solución**:
1. Verificar que Loki está corriendo:
   ```bash
   curl http://localhost:3100/ready
   ```

2. Verificar que Fluentd está corriendo:
   ```bash
   docker ps --filter "name=fluentd"
   ```

3. Verificar logs de Fluentd:
   ```bash
   docker logs fluentd --tail 50
   ```

4. Verificar que los servicios están enviando logs a Fluentd

### Problema: El servicio no se reinicia correctamente

**Solución**:
1. Verificar logs del contenedor:
   ```bash
   docker logs <nombre-contenedor> --tail 100
   ```

2. Verificar dependencias:
   ```bash
   docker-compose ps
   ```

3. Reiniciar con logs:
   ```bash
   docker-compose up -d api-gateway
   docker-compose logs -f api-gateway
   ```

### Problema: No se reciben notificaciones por correo

**Solución**:
1. Verificar configuración SMTP:
   ```bash
   docker exec health-check-app-micro env | grep SMTP
   ```

2. Verificar logs de notificación:
   ```bash
   docker logs health-check-app-micro 2>&1 | grep -i "smtp\|email\|notificacion"
   ```

3. Verificar que es el primer fallo (solo se envía en el primer fallo):
   ```bash
   curl -s http://localhost:8082/health/api-gateway | jq '.failureCount'
   ```

---

## Resumen de Comandos Rápidos

```bash
# Estado global de servicios
curl -s http://localhost:8082/health | jq '.[] | {name: .name, status: .status}'

# Estado de un servicio específico
curl -s http://localhost:8082/health/api-gateway | jq '.'

# Detener servicio
docker-compose stop api-gateway

# Reiniciar servicio
docker-compose start api-gateway

# Ver logs del Health Check App
docker logs health-check-app-micro --tail 50 --follow

# Consultar logs en Loki (API)
curl -G -s "http://localhost:3100/loki/api/v1/query_range" \
  --data-urlencode 'query={service="api-gateway-micro"}' \
  --data-urlencode 'start='$(date -d '15 minutes ago' +%s)000000000 \
  --data-urlencode 'end='$(date +%s)000000000 | jq '.'
```

---

## Checklist de Verificación

- [ ] Todos los servicios están corriendo inicialmente
- [ ] Health Check App muestra todos los servicios como `UP`
- [ ] Servicio se detiene correctamente
- [ ] Health Check App detecta el fallo en menos de 35 segundos
- [ ] Estado cambia a `DOWN` en Health Check App
- [ ] Logs aparecen en Grafana/Loki
- [ ] Notificación por correo se envía (si está configurado)
- [ ] Servicio se reinicia correctamente
- [ ] Health Check App detecta la recuperación
- [ ] Estado cambia a `UP` después de la recuperación

---

## Notas Adicionales

1. **Frecuencia de verificación**: Por defecto, el Health Check App verifica cada 30 segundos. Puedes ajustarlo al registrar un servicio.

2. **Notificaciones**: Solo se envía una notificación en el **primer fallo** (`failureCount == 1`). Fallos subsecuentes no generan nuevas notificaciones.

3. **Persistencia**: El Health Check App persiste el estado en `services.json`. Si reinicias el Health Check App, mantiene el estado de los servicios.

4. **Timeout**: El Health Check App tiene un timeout de 5 segundos para cada verificación.

5. **Logs en Loki**: Los logs pueden tardar unos segundos en aparecer en Grafana debido al procesamiento en Fluentd.

---

¡Listo! Con esta guía puedes verificar que todos los mecanismos de salud y observabilidad funcionan correctamente. 🎉

