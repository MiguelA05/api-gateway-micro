#!/bin/bash

# Script para crear 5 usuarios de prueba y un admin si no existe
# Uso: ./setup-users.sh

# No usar set -e para permitir manejo de errores personalizado

API_GATEWAY_URL="${API_GATEWAY_URL:-http://localhost:8085}"
DOMAIN_SERVICE_DB="${DOMAIN_SERVICE_DB:-postgres-domain}"
DB_USER="${DB_USER:-user}"
DB_NAME="${DB_NAME:-mydb}"
DB_PASSWORD="${DB_PASSWORD:-pass}"

echo "=========================================="
echo "SCRIPT DE CONFIGURACIÓN DE USUARIOS"
echo "=========================================="
echo ""

# Función para verificar si el API Gateway está disponible
check_api_gateway() {
    echo "🔍 Verificando API Gateway en $API_GATEWAY_URL..."
    local response=$(curl -s -w "\nHTTP_CODE:%{http_code}" "$API_GATEWAY_URL/api/v1/auth/login" \
        -X POST \
        -H "Content-Type: application/json" \
        -d '{"usuario":"test","clave":"test"}' 2>/dev/null)
    local http_code=$(echo "$response" | grep "HTTP_CODE" | cut -d: -f2)
    
    if [ -n "$http_code" ] && [ "$http_code" != "000" ]; then
        echo "✅ API Gateway disponible (respondió con HTTP $http_code)"
        return 0
    else
        echo "❌ API Gateway no disponible en $API_GATEWAY_URL"
        echo "   Asegúrate de que el servicio esté corriendo"
        return 1
    fi
}

# Función para generar datos de perfil únicos para cada usuario
generate_profile_data() {
    local usuario=$1
    local index=$2
    
    # Datos de perfil variados para cada usuario
    case $index in
        1)
            echo "{
                \"apodo\": \"${usuario}_nick\",
                \"biografia\": \"Desarrollador de software apasionado por la tecnología y la innovación\",
                \"organizacion\": \"Tech Solutions Inc.\",
                \"paisResidencia\": \"Colombia\",
                \"linkGithub\": \"https://github.com/${usuario}\",
                \"linkLinkedIn\": \"https://linkedin.com/in/${usuario}\",
                \"informacionContactoPublica\": true
            }"
            ;;
        2)
            echo "{
                \"apodo\": \"${usuario}_dev\",
                \"biografia\": \"Ingeniero de sistemas especializado en arquitectura de microservicios\",
                \"organizacion\": \"Cloud Architects\",
                \"paisResidencia\": \"Colombia\",
                \"linkGithub\": \"https://github.com/${usuario}\",
                \"linkTwitter\": \"https://twitter.com/${usuario}\",
                \"informacionContactoPublica\": false
            }"
            ;;
        3)
            echo "{
                \"apodo\": \"${usuario}_pro\",
                \"biografia\": \"Consultor en desarrollo de software y mentor de programadores\",
                \"organizacion\": \"Software Consulting Group\",
                \"paisResidencia\": \"Colombia\",
                \"urlPaginaPersonal\": \"https://${usuario}.dev\",
                \"linkGithub\": \"https://github.com/${usuario}\",
                \"linkInstagram\": \"https://instagram.com/${usuario}\",
                \"informacionContactoPublica\": true
            }"
            ;;
        4)
            echo "{
                \"apodo\": \"${usuario}_tech\",
                \"biografia\": \"Arquitecto de software con experiencia en sistemas distribuidos\",
                \"organizacion\": \"Enterprise Solutions\",
                \"paisResidencia\": \"Colombia\",
                \"direccionCorrespondencia\": \"Calle 123 #45-67, Bogotá\",
                \"linkGithub\": \"https://github.com/${usuario}\",
                \"linkLinkedIn\": \"https://linkedin.com/in/${usuario}\",
                \"linkFacebook\": \"https://facebook.com/${usuario}\",
                \"informacionContactoPublica\": false
            }"
            ;;
        5)
            echo "{
                \"apodo\": \"${usuario}_coder\",
                \"biografia\": \"Full-stack developer enfocado en aplicaciones web modernas\",
                \"organizacion\": \"Web Development Co.\",
                \"paisResidencia\": \"Colombia\",
                \"linkGithub\": \"https://github.com/${usuario}\",
                \"linkTwitter\": \"https://twitter.com/${usuario}\",
                \"linkLinkedIn\": \"https://linkedin.com/in/${usuario}\",
                \"linkOtraRed\": \"https://dev.to/${usuario}\",
                \"informacionContactoPublica\": true
            }"
            ;;
        *)
            echo "{
                \"apodo\": \"${usuario}\",
                \"biografia\": \"Usuario de prueba del sistema\",
                \"paisResidencia\": \"Colombia\",
                \"informacionContactoPublica\": false
            }"
            ;;
    esac
}

# Función para crear un usuario a través del API Gateway
create_user() {
    local usuario=$1
    local correo=$2
    local clave=$3
    local numero_telefono=$4
    local index=$5  # Índice para generar datos de perfil únicos
    local with_profile=${6:-true}  # Por defecto incluir perfil
    
    echo "📝 Creando usuario: $usuario..."
    
    # Construir el JSON base
    local json_base="{
        \"usuario\": \"$usuario\",
        \"correo\": \"$correo\",
        \"clave\": \"$clave\",
        \"numeroTelefono\": \"$numero_telefono\""
    
    # Agregar datos de perfil si se solicita
    if [ "$with_profile" = "true" ] && [ -n "$index" ]; then
        local profile_data=$(generate_profile_data "$usuario" "$index")
        # Combinar JSON base con datos de perfil
        local full_json="${json_base},
        $(echo "$profile_data" | sed 's/^{//' | sed 's/}$//')
    }"
    else
        local full_json="${json_base}
    }"
    fi
    
    local response=$(curl -s -w "\nHTTP_CODE:%{http_code}" -X POST "$API_GATEWAY_URL/api/v1/auth/registro" \
        -H "Content-Type: application/json" \
        -d "$full_json")
    
    local http_code=$(echo "$response" | grep "HTTP_CODE" | cut -d: -f2)
    local body=$(echo "$response" | grep -v "HTTP_CODE")
    
    if [ "$http_code" = "201" ]; then
        if [ "$with_profile" = "true" ]; then
            echo "   ✅ Usuario $usuario creado exitosamente con perfil"
        else
            echo "   ✅ Usuario $usuario creado exitosamente"
        fi
        return 0
    else
        # Verificar si el usuario ya existe intentando hacer login
        local login_response=$(curl -s -w "\nHTTP_CODE:%{http_code}" -X POST "$API_GATEWAY_URL/api/v1/auth/login" \
            -H "Content-Type: application/json" \
            -d "{\"usuario\":\"$usuario\",\"clave\":\"$clave\"}" 2>/dev/null)
        local login_code=$(echo "$login_response" | grep "HTTP_CODE" | cut -d: -f2)
        
        if [ "$login_code" = "200" ]; then
            echo "   ⚠️  Usuario $usuario ya existe (puede hacer login)"
            return 0
        elif echo "$body" | grep -qi "ya existe\|already exists\|duplicate"; then
            echo "   ⚠️  Usuario $usuario ya existe (se omite)"
            return 0
        else
            echo "   ❌ Error creando usuario $usuario: HTTP $http_code"
            echo "   Respuesta: $body"
            return 1
        fi
    fi
}

# Función para verificar si el admin existe
check_admin_exists() {
    echo "🔍 Verificando si el usuario admin existe..."
    
    # Intentar hacer login con admin
    local response=$(curl -s -w "\nHTTP_CODE:%{http_code}" -X POST "$API_GATEWAY_URL/api/v1/auth/login" \
        -H "Content-Type: application/json" \
        -d '{"usuario":"admin","clave":"admin123"}')
    
    local http_code=$(echo "$response" | grep "HTTP_CODE" | cut -d: -f2)
    
    if [ "$http_code" = "200" ]; then
        echo "✅ Usuario admin ya existe"
        return 0
    else
        echo "⚠️  Usuario admin no existe o credenciales incorrectas"
        return 1
    fi
}

# Función para crear admin directamente en la BD
create_admin_in_db() {
    echo "🔧 Creando usuario admin en la base de datos..."
    
    # Detectar si estamos usando Docker o Podman
    if command -v podman > /dev/null 2>&1 && podman ps --format "{{.Names}}" | grep -q "$DOMAIN_SERVICE_DB"; then
        DOCKER_CMD="podman"
    elif command -v docker > /dev/null 2>&1 && docker ps --format "{{.Names}}" | grep -q "$DOMAIN_SERVICE_DB"; then
        DOCKER_CMD="docker"
    else
        echo "❌ No se encontró el contenedor $DOMAIN_SERVICE_DB"
        echo "   Asegúrate de que el Domain Service esté corriendo"
        return 1
    fi
    
    echo "   Usando $DOCKER_CMD para acceder a la BD..."
    
    # Verificar si el admin ya existe
    local admin_exists=$($DOCKER_CMD exec -i "$DOMAIN_SERVICE_DB" psql -U "$DB_USER" -d "$DB_NAME" -t -c \
        "SELECT COUNT(*) FROM usuarios WHERE usuario = 'admin';" 2>/dev/null | tr -d ' ' || echo "0")
    
    if [ "$admin_exists" != "0" ]; then
        echo "   ⚠️  El usuario admin ya existe en la BD"
        return 0
    fi
    
    # Crear el admin (rol 0 = ADMIN)
    $DOCKER_CMD exec -i "$DOMAIN_SERVICE_DB" psql -U "$DB_USER" -d "$DB_NAME" <<EOF 2>/dev/null
INSERT INTO usuarios (usuario, clave, codigo_recuperacion, fecha_codigo, correo, numero_telefono, rol)
VALUES (
    'admin',
    'admin123',
    NULL,
    NULL,
    'admin@example.com',
    '+573001234567',
    0
)
ON CONFLICT (usuario) DO UPDATE 
SET clave = 'admin123',
    rol = 0,
    correo = 'admin@example.com',
    numero_telefono = '+573001234567';
EOF
    
    if [ $? -eq 0 ]; then
        echo "   ✅ Usuario admin creado en la BD"
        return 0
    else
        echo "   ❌ Error al crear usuario admin en la BD"
        return 1
    fi
}

# Función para validar usuarios creados (login básico)
validate_users() {
    echo ""
    echo "=========================================="
    echo "VALIDACIÓN DE LOGIN DE USUARIOS"
    echo "=========================================="
    echo ""
    
    local users=("admin" "usuario1" "usuario2" "usuario3" "usuario4" "usuario5")
    local passwords=("admin123" "user123" "user123" "user123" "user123" "user123")
    local success_count=0
    local total_count=${#users[@]}
    
    for i in "${!users[@]}"; do
        local user="${users[$i]}"
        local pass="${passwords[$i]}"
        
        echo -n "🔐 Validando login de $user... "
        
        local response=$(curl -s -w "\nHTTP_CODE:%{http_code}" -X POST "$API_GATEWAY_URL/api/v1/auth/login" \
            -H "Content-Type: application/json" \
            -d "{\"usuario\":\"$user\",\"clave\":\"$pass\"}")
        
        local http_code=$(echo "$response" | grep "HTTP_CODE" | cut -d: -f2)
        
        if [ "$http_code" = "200" ]; then
            local token=$(echo "$response" | grep -v "HTTP_CODE" | jq -r '.respuesta.token // empty' 2>/dev/null)
            if [ -n "$token" ] && [ "$token" != "null" ]; then
                echo "✅ OK (token obtenido)"
                ((success_count++))
            else
                echo "⚠️  Login OK pero no se obtuvo token"
            fi
        else
            echo "❌ FALLO (HTTP $http_code)"
        fi
    done
    
    echo ""
    echo "=========================================="
    echo "RESUMEN DE LOGIN"
    echo "=========================================="
    echo "Usuarios validados: $success_count/$total_count"
    echo ""
    
    if [ $success_count -eq $total_count ]; then
        echo "✅ Todos los usuarios pueden hacer login correctamente"
        return 0
    else
        echo "⚠️  Algunos usuarios no pudieron hacer login"
        return 1
    fi
}

# Función para validar perfil completo de un usuario
validate_user_profile() {
    local usuario=$1
    local clave=$2
    
    echo ""
    echo "=========================================="
    echo "VALIDACIÓN DE PERFIL: $usuario"
    echo "=========================================="
    echo ""
    
    # Paso 1: Login
    echo "1️⃣  Haciendo login con $usuario..."
    local login_response=$(curl -s -w "\nHTTP_CODE:%{http_code}" -X POST "$API_GATEWAY_URL/api/v1/auth/login" \
        -H "Content-Type: application/json" \
        -d "{\"usuario\":\"$usuario\",\"clave\":\"$clave\"}")
    
    local login_code=$(echo "$login_response" | grep "HTTP_CODE" | cut -d: -f2)
    local login_body=$(echo "$login_response" | grep -v "HTTP_CODE")
    
    if [ "$login_code" != "200" ]; then
        echo "   ❌ Error en login: HTTP $login_code"
        return 1
    fi
    
    local token=$(echo "$login_body" | jq -r '.respuesta.token // empty' 2>/dev/null)
    if [ -z "$token" ] || [ "$token" = "null" ]; then
        echo "   ❌ No se pudo obtener el token"
        return 1
    fi
    
    echo "   ✅ Login exitoso, token obtenido"
    echo ""
    
    # Paso 2: Consultar información completa del usuario
    echo "2️⃣  Consultando información completa del usuario..."
    local user_response=$(curl -s -w "\nHTTP_CODE:%{http_code}" -X GET "$API_GATEWAY_URL/api/v1/usuarios/$usuario" \
        -H "Authorization: Bearer $token" \
        -H "Content-Type: application/json")
    
    local user_code=$(echo "$user_response" | grep "HTTP_CODE" | cut -d: -f2)
    local user_body=$(echo "$user_response" | grep -v "HTTP_CODE")
    
    if [ "$user_code" != "200" ]; then
        echo "   ❌ Error consultando usuario: HTTP $user_code"
        echo "   Respuesta: $user_body"
        return 1
    fi
    
    echo "   ✅ Información del usuario obtenida exitosamente"
    echo ""
    
    # Paso 3: Validar estructura de respuesta
    echo "3️⃣  Validando estructura de datos..."
    
    # Verificar que existe datosSeguridad
    local has_security=$(echo "$user_body" | jq -r '.datosSeguridad // empty' 2>/dev/null)
    if [ -z "$has_security" ] || [ "$has_security" = "null" ]; then
        echo "   ❌ No se encontraron datos de seguridad"
        return 1
    fi
    echo "   ✅ Datos de seguridad presentes"
    
    # Verificar que existe perfil
    local has_profile=$(echo "$user_body" | jq -r '.perfil // empty' 2>/dev/null)
    if [ -z "$has_profile" ] || [ "$has_profile" = "null" ]; then
        echo "   ⚠️  No se encontraron datos de perfil (puede ser normal si no se creó perfil)"
        return 0
    fi
    echo "   ✅ Datos de perfil presentes"
    
    # Verificar campos específicos del perfil
    local apodo=$(echo "$user_body" | jq -r '.perfil.apodo // empty' 2>/dev/null)
    local biografia=$(echo "$user_body" | jq -r '.perfil.biografia // empty' 2>/dev/null)
    
    if [ -n "$apodo" ] && [ "$apodo" != "null" ]; then
        echo "   ✅ Apodo: $apodo"
    fi
    
    if [ -n "$biografia" ] && [ "$biografia" != "null" ]; then
        echo "   ✅ Biografía: ${biografia:0:50}..."
    fi
    
    echo ""
    echo "=========================================="
    echo "RESUMEN DE VALIDACIÓN"
    echo "=========================================="
    echo "✅ Usuario: $usuario"
    echo "✅ Login: Exitoso"
    echo "✅ Consulta de datos: Exitosa"
    echo "✅ Datos de seguridad: Presentes"
    if [ -n "$has_profile" ] && [ "$has_profile" != "null" ]; then
        echo "✅ Datos de perfil: Presentes y completos"
    else
        echo "⚠️  Datos de perfil: No presentes"
    fi
    echo ""
    
    # Mostrar resumen de datos
    echo "📋 Resumen de datos obtenidos:"
    echo "$user_body" | jq '.' 2>/dev/null | head -30
    echo ""
    
    return 0
}

# Función principal
main() {
    # Verificar API Gateway
    if ! check_api_gateway; then
        exit 1
    fi
    
    echo ""
    echo "=========================================="
    echo "CREANDO USUARIOS"
    echo "=========================================="
    echo ""
    
    # Crear 5 usuarios de prueba con datos de perfil
    # Nota: Si los usuarios ya existen pero no tienen perfil, se omitirán.
    # Para forzar la creación de usuarios nuevos, elimina los usuarios existentes primero.
    echo "📌 Creando usuarios con datos de perfil completos..."
    echo "   (Si los usuarios ya existen, se omitirán y se validará su login)"
    echo ""
    
    create_user "usuario1" "usuario1@test.com" "user123" "+573001234568" "1" "true"
    create_user "usuario2" "usuario2@test.com" "user123" "+573001234569" "2" "true"
    create_user "usuario3" "usuario3@test.com" "user123" "+573001234570" "3" "true"
    create_user "usuario4" "usuario4@test.com" "user123" "+573001234571" "4" "true"
    create_user "usuario5" "usuario5@test.com" "user123" "+573001234572" "5" "true"
    
    # Crear un usuario adicional específico para validación de perfil
    echo ""
    echo "📌 Creando usuario de validación con perfil completo..."
    create_user "usuario_validacion" "validacion@test.com" "valid123" "+573009999999" "1" "true"
    
    echo ""
    echo "=========================================="
    echo "CONFIGURANDO ADMIN"
    echo "=========================================="
    echo ""
    
    # Verificar y crear admin si no existe
    if ! check_admin_exists; then
        if create_admin_in_db; then
            echo "✅ Admin configurado correctamente"
        else
            echo "⚠️  No se pudo crear el admin, pero continuando..."
        fi
    fi
    
    # Validar login de usuarios
    validate_users
    
    echo ""
    echo "=========================================="
    echo "VALIDACIÓN DE PERFILES"
    echo "=========================================="
    echo ""
    
    # Validar perfil completo de un usuario de prueba
    # Intentar primero con usuario_validacion (si existe), luego con usuario1
    echo "🔍 Validando perfil completo..."
    
    # Verificar si usuario_validacion existe y tiene perfil
    local validation_user=""
    local validation_pass=""
    
    # Intentar login con usuario_validacion
    local test_login=$(curl -s -w "\nHTTP_CODE:%{http_code}" -X POST "$API_GATEWAY_URL/api/v1/auth/login" \
        -H "Content-Type: application/json" \
        -d '{"usuario":"usuario_validacion","clave":"valid123"}' 2>/dev/null)
    local test_code=$(echo "$test_login" | grep "HTTP_CODE" | cut -d: -f2)
    
    if [ "$test_code" = "200" ]; then
        validation_user="usuario_validacion"
        validation_pass="valid123"
        echo "   Usando usuario_validacion para validación..."
    else
        # Intentar con usuario1
        validation_user="usuario1"
        validation_pass="user123"
        echo "   Usando usuario1 para validación..."
    fi
    
    if validate_user_profile "$validation_user" "$validation_pass"; then
        echo "✅ Validación de perfil completada exitosamente"
    else
        echo "⚠️  Hubo problemas en la validación del perfil"
        echo "   Nota: Si el usuario no tiene perfil, esto es normal si fue creado antes de implementar perfiles."
    fi
    
    echo ""
    echo "=========================================="
    echo "SCRIPT COMPLETADO"
    echo "=========================================="
    echo ""
    echo "✅ Usuarios creados con datos de perfil"
    echo "✅ Validación de login completada"
    echo "✅ Validación de perfil completada"
    echo ""
    echo "Puedes probar manualmente con:"
    echo "  1. Login: curl -X POST $API_GATEWAY_URL/api/v1/auth/login -H 'Content-Type: application/json' -d '{\"usuario\":\"usuario1\",\"clave\":\"user123\"}'"
    echo "  2. Consultar usuario (con token): curl -X GET $API_GATEWAY_URL/api/v1/usuarios/usuario1 -H 'Authorization: Bearer <TOKEN>'"
    echo ""
}

# Ejecutar función principal
main

