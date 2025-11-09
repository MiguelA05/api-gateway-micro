#!/bin/bash

# Script para solucionar errores de Lombok en NetBeans
# Uso: ./scripts/fix-lombok-netbeans.sh

echo "=== SOLUCIÓN PARA ERRORES DE LOMBOK EN NETBEANS ==="
echo ""

# Verificar que estamos en el directorio correcto
if [ ! -f "pom.xml" ]; then
    echo "❌ Error: Este script debe ejecutarse desde la raíz del proyecto api-gateway-micro"
    exit 1
fi

echo "✅ Verificando archivos de configuración..."

# Verificar archivos de configuración
if [ ! -f "nb-configuration.xml" ]; then
    echo "⚠️  nb-configuration.xml no encontrado"
else
    echo "✅ nb-configuration.xml existe"
fi

if [ ! -f "nbproject/project.properties" ]; then
    echo "⚠️  nbproject/project.properties no encontrado"
else
    echo "✅ nbproject/project.properties existe"
fi

if [ ! -f ".nbattrs" ]; then
    echo "⚠️  .nbattrs no encontrado"
else
    echo "✅ .nbattrs existe"
fi

echo ""
echo "📋 PASOS PARA APLICAR LA SOLUCIÓN:"
echo ""
echo "1. Cerrar el proyecto en NetBeans:"
echo "   - Click derecho en el proyecto → Close Project"
echo ""
echo "2. Abrir el proyecto nuevamente:"
echo "   - File → Open Project → Seleccionar api-gateway-micro"
echo ""
echo "3. Si el error persiste, limpiar caché:"
echo "   - Cerrar NetBeans completamente"
echo "   - Eliminar carpeta .nbindex si existe"
echo "   - Abrir NetBeans y el proyecto nuevamente"
echo ""
echo "4. O deshabilitar manualmente:"
echo "   - Click derecho en el proyecto → Properties"
echo "   - Build → Compiling"
echo "   - Desmarcar 'Enable Annotation Processing'"
echo ""
echo "⚠️  NOTA: Este error es solo del IDE. La compilación con Maven funciona correctamente."
echo "   Lombok se procesa durante 'mvn compile', no en el IDE."
echo ""
echo "✅ Para más detalles, consulta: SOLUCION_LOMBOK_NETBEANS.md"
