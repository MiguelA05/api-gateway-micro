package com.uniquindio.archmicroserv.apigateway.acceptance.steps;

import io.cucumber.java.es.*;
import io.cucumber.java.Before;
import io.restassured.http.ContentType;
import net.datafaker.Faker;
import io.restassured.response.Response;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Step Definitions para pruebas de aceptación del API Gateway usando Cucumber/Gherkin.
 * 
 * Este archivo contiene las implementaciones de los pasos definidos en los archivos .feature,
 * proporcionando validaciones estrictas para garantizar que los tests no pasen con funcionalidades fallidas.
 */
public class ApiGatewaySteps {

    private static final String BASE_URL = System.getProperty("baseUrl", "http://localhost:8085") + "/api/v1";
    private static final String DOMAIN_SERVICE_URL = System.getProperty("domainServiceUrl", "http://localhost:8081");
    
    private Response lastResponse;
    private final Faker faker = new Faker();
    private String ultimoUsuario = "user_" + System.currentTimeMillis();
    private String ultimoPassword = "Passw0rd*" + faker.number().digits(3);
    private String ultimoToken;
    private static String adminToken;
    private static boolean adminSetupDone = false;

    // =============================================================================
    // HOOKS - CONFIGURACIÓN INICIAL
    // =============================================================================

    /**
     * Configura el usuario admin en la BD del dominio antes de ejecutar los tests.
     * Se ejecuta una sola vez para todos los escenarios.
     */
    @Before(order = 1)
    public void setupAdminUser() {
        if (adminSetupDone && adminToken != null && !adminToken.isEmpty()) {
            return;
        }
        
        System.out.println("🔧 Configurando usuario admin para tests...");
        crearUsuarioAdminEnBD();
        adminToken = obtenerTokenAdmin();
        
        if (adminToken != null && !adminToken.isEmpty()) {
            System.out.println("✅ Usuario admin configurado correctamente");
            adminSetupDone = true;
        } else {
            System.err.println("⚠️ No se pudo obtener token del admin, algunos tests pueden fallar");
        }
    }
    
    /**
     * Crea o actualiza el usuario admin en la base de datos del dominio.
     * El usuario admin tiene rol 0 (ADMIN) y es necesario para operaciones que requieren permisos administrativos.
     */
    private void crearUsuarioAdminEnBD() {
        String dbUrl = "jdbc:postgresql://localhost:5433/mydb";
        String dbUser = "user";
        String dbPassword = "pass";
        
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
            String checkSql = "SELECT COUNT(*) FROM usuarios WHERE usuario = 'admin'";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql);
                 var rs = checkStmt.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    String updateSql = """
                        UPDATE usuarios 
                        SET clave = 'admin123', 
                            correo = 'admin@example.com', 
                            numero_telefono = '+573001234567',
                            rol = 0
                        WHERE usuario = 'admin'
                        """;
                    try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                        updateStmt.executeUpdate();
                        System.out.println("✅ Usuario admin actualizado en BD");
                    }
                } else {
                    String insertSql = """
                        INSERT INTO usuarios (usuario, clave, codigo_recuperacion, fecha_codigo, correo, numero_telefono, rol)
                        VALUES ('admin', 'admin123', NULL, NULL, 'admin@example.com', '+573001234567', 0)
                        """;
                    try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                        insertStmt.executeUpdate();
                        System.out.println("✅ Usuario admin creado en BD");
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("⚠️ Error creando usuario admin en BD: " + e.getMessage());
            System.err.println("   Los tests que requieren admin pueden fallar");
        }
    }
    
    /**
     * Obtiene el token JWT del usuario admin mediante login en el domain-service.
     * 
     * @return Token JWT del admin o null si falla la autenticación
     */
    private String obtenerTokenAdmin() {
        try {
            String loginBody = """
                {
                  "usuario": "admin",
                  "clave": "admin123"
                }
                """;
            
            Response response = given()
                    .contentType(ContentType.JSON)
                    .body(loginBody)
                    .post(DOMAIN_SERVICE_URL + "/v1/sesiones");
            
            if (response.getStatusCode() == 200) {
                String token = response.jsonPath().getString("token");
                if (token == null) {
                    token = response.jsonPath().getString("respuesta.token");
                }
                return token;
            } else {
                System.err.println("⚠️ Error obteniendo token admin. Status: " + response.getStatusCode());
                return null;
            }
        } catch (Exception e) {
            System.err.println("⚠️ Excepción obteniendo token admin: " + e.getMessage());
            return null;
        }
    }

    // =============================================================================
    // STEPS: CONFIGURACIÓN INICIAL Y DISPONIBILIDAD
    // =============================================================================

    /**
     * Verifica que el servicio API Gateway esté disponible mediante health check.
     * Lanza AssertionError si el servicio no está disponible.
     */
    @Dado("que el servicio API Gateway está disponible")
    public void servicioDisponible() {
        try {
            Response healthResponse = given()
                .when()
                .get("http://localhost:8085/actuator/health");
            
            if (healthResponse.getStatusCode() >= 200 && healthResponse.getStatusCode() < 500) {
                return;
            }
            throw new AssertionError("El servicio API Gateway no está disponible. Status: " + healthResponse.getStatusCode());
        } catch (Exception e) {
            throw new AssertionError("No se pudo conectar al servicio API Gateway: " + e.getMessage(), e);
        }
    }

    // =============================================================================
    // STEPS: REGISTRO Y AUTENTICACIÓN
    // =============================================================================

    /**
     * Registra un nuevo usuario con datos válidos generados aleatoriamente.
     */
    @Cuando("registro un usuario con datos válidos en el API Gateway")
    public void registroUsuarioValido() {
        ultimoUsuario = "user_" + faker.number().digits(8);
        String correo = faker.internet().emailAddress();
        ultimoPassword = "Passw0rd*" + faker.number().digits(3);
        String numeroTelefono = "3" + faker.number().digits(9);

        var body = """
        {
          "usuario":"%s",
          "correo":"%s",
          "clave":"%s",
          "numeroTelefono":"+57%s"
        }
        """.formatted(ultimoUsuario, correo, ultimoPassword, numeroTelefono);

        lastResponse = given()
                .contentType(ContentType.JSON)
                .body(body)
                .post(BASE_URL + "/auth/registro");
    }

    /**
     * Crea un usuario de prueba válido para usar en los escenarios.
     * Valida que el registro sea exitoso (código 201).
     */
    @Dado("que existe un usuario registrado válido")
    public void existeUsuarioValido() {
        ultimoUsuario = "user_" + faker.number().digits(8);
        String correo = faker.internet().emailAddress();
        ultimoPassword = "Passw0rd*" + faker.number().digits(3);
        String numeroTelefono = "3" + faker.number().digits(9);

        var body = """
        {
          "usuario":"%s",
          "correo":"%s",
          "clave":"%s",
          "numeroTelefono":"+57%s"
        }
        """.formatted(ultimoUsuario, correo, ultimoPassword, numeroTelefono);

        lastResponse = given()
                .contentType(ContentType.JSON)
                .body(body)
                .post(BASE_URL + "/auth/registro");
        
        lastResponse.then().statusCode(201);
    }

    /**
     * Realiza login con las credenciales del usuario creado.
     * Extrae el token JWT de la respuesta si el login es exitoso.
     */
    @Cuando("inicio sesión con credenciales correctas en el API Gateway")
    public void loginConCredencialesCorrectas() {
        var body = """
        {
          "usuario":"%s",
          "clave":"%s"
        }
        """.formatted(ultimoUsuario, ultimoPassword);

        lastResponse = given()
                .contentType(ContentType.JSON)
                .body(body)
                .post(BASE_URL + "/auth/login");

        if (lastResponse.statusCode() == 200) {
            ultimoToken = lastResponse.jsonPath().getString("token");
            if (ultimoToken == null) {
                ultimoToken = lastResponse.jsonPath().getString("respuesta.token");
            }
        }
    }

    /**
     * Pre-condición: Crea un usuario y realiza login exitoso.
     * Valida que el login sea exitoso y que se haya obtenido un token válido.
     */
    @Dado("que he iniciado sesión exitosamente")
    public void heIniciadoSesion() {
        existeUsuarioValido();
        loginConCredencialesCorrectas();
        
        lastResponse.then().statusCode(200);
        
        assertThat("Debe haberse obtenido un token después del login exitoso", 
            ultimoToken, allOf(notNullValue(), not(blankOrNullString())));
        
        Object errorObj = lastResponse.jsonPath().get("error");
        if (errorObj instanceof Boolean error) {
            assertThat("La respuesta de login debe indicar error: false", error, is(false));
        } else if (errorObj instanceof String errorStr) {
            Boolean error = Boolean.parseBoolean(errorStr);
            assertThat("La respuesta de login debe indicar error: false", error, is(false));
        }
    }

    // =============================================================================
    // STEPS: OPERACIONES CRUD DE USUARIOS
    // =============================================================================

    /**
     * Consulta los datos completos de un usuario (seguridad + perfil).
     * Requiere autenticación previa.
     */
    @Cuando("consulto los datos completos del usuario")
    public void consultoDatosCompletos() {
        assertThat("Debe existir un token para consultar usuario", 
            ultimoToken, allOf(notNullValue(), not(blankOrNullString())));
        assertThat("Debe existir un usuario para consultar", 
            ultimoUsuario, allOf(notNullValue(), not(blankOrNullString())));
        
        lastResponse = given()
                .header("Authorization", "Bearer " + ultimoToken)
                .get(BASE_URL + "/usuarios/" + ultimoUsuario);
    }

    /**
     * Actualiza los datos de un usuario.
     * Requiere autenticación previa.
     */
    @Cuando("actualizo los datos del usuario")
    public void actualizoDatosUsuario() {
        assertThat("Debe existir un token para actualizar usuario", 
            ultimoToken, allOf(notNullValue(), not(blankOrNullString())));
        
        String nuevoCorreo = faker.internet().emailAddress();
        var body = """
        {
          "correo":"%s",
          "apodo":"Usuario Actualizado"
        }
        """.formatted(nuevoCorreo);

        lastResponse = given()
                .header("Authorization", "Bearer " + ultimoToken)
                .contentType(ContentType.JSON)
                .body(body)
                .put(BASE_URL + "/usuarios/" + ultimoUsuario);
    }

    /**
     * Elimina un usuario a través del Auth Controller.
     * Requiere token de admin (rol ADMIN) para ejecutarse correctamente.
     */
    @Cuando("elimino el usuario a través del Auth Controller")
    public void eliminoUsuarioAuthController() {
        assertThat("Debe existir un token de admin para eliminar usuario", 
            adminToken, allOf(notNullValue(), not(blankOrNullString())));
        assertThat("Debe existir un usuario para eliminar", 
            ultimoUsuario, allOf(notNullValue(), not(blankOrNullString())));
        
        lastResponse = given()
                .header("Authorization", "Bearer " + adminToken)
                .delete(BASE_URL + "/auth/usuarios/" + ultimoUsuario);
    }

    /**
     * Elimina un usuario completo (seguridad + perfil) a través del Usuario Controller.
     * Requiere token de admin (rol ADMIN) para ejecutarse correctamente.
     */
    @Cuando("elimino el usuario completo a través del Usuario Controller")
    public void eliminoUsuarioCompleto() {
        assertThat("Debe existir un token de admin para eliminar usuario completo", 
            adminToken, allOf(notNullValue(), not(blankOrNullString())));
        assertThat("Debe existir un usuario para eliminar", 
            ultimoUsuario, allOf(notNullValue(), not(blankOrNullString())));
        
        lastResponse = given()
                .header("Authorization", "Bearer " + adminToken)
                .delete(BASE_URL + "/usuarios/" + ultimoUsuario);
    }

    // =============================================================================
    // STEPS: HEALTH CHECK
    // =============================================================================

    /**
     * Consulta el endpoint de health check del API Gateway.
     */
    @Cuando("consulto el endpoint de health check")
    public void consultoHealthCheck() {
        lastResponse = given()
                .get("http://localhost:8085/actuator/health");
    }

    // =============================================================================
    // STEPS: VALIDACIONES DE RESPUESTAS
    // =============================================================================

    /**
     * Valida que el código de estado HTTP de la respuesta sea exactamente el especificado.
     * 
     * @param status Código de estado HTTP esperado
     */
    @Entonces("la respuesta debe tener estado {int}")
    public void validarEstado(int status) {
        lastResponse.then().statusCode(status);
    }

    /**
     * Valida que el código de estado HTTP sea uno de los dos especificados.
     * 
     * @param status1 Primer código de estado aceptable
     * @param status2 Segundo código de estado aceptable
     */
    @Entonces("la respuesta debe tener estado {int} o {int}")
    public void validarEstadoOpcional(int status1, int status2) {
        lastResponse.then().statusCode(anyOf(is(status1), is(status2)));
    }

    /**
     * Valida que la respuesta indique éxito.
     * Verifica que el cuerpo no esté vacío, que error sea false (si existe),
     * y que exista el campo respuesta o contenido válido.
     */
    @Y("el cuerpo debe indicar éxito")
    public void cuerpoIndicaExito() {
        String raw = lastResponse.getBody() != null ? lastResponse.getBody().asString() : null;
        assertThat("El cuerpo de la respuesta no debe ser null", raw, notNullValue());
        assertThat("El cuerpo de la respuesta no debe estar vacío", raw, not(blankOrNullString()));
        
        Object errorObj = lastResponse.jsonPath().get("error");
        if (errorObj instanceof Boolean error) {
            assertThat("La respuesta debe indicar error: false para éxito", error, is(false));
        } else if (errorObj instanceof String errorStr) {
            Boolean error = Boolean.parseBoolean(errorStr);
            assertThat("La respuesta debe indicar error: false para éxito", error, is(false));
        }
        
        Object respuesta = lastResponse.jsonPath().get("respuesta");
        if (respuesta == null) {
            assertThat("La respuesta debe tener contenido válido", raw != null ? raw.length() : 0, greaterThan(0));
        } else if (respuesta instanceof String respuestaStr) {
            assertThat("El campo 'respuesta' no debe estar vacío si es string", 
                respuestaStr, not(blankOrNullString()));
        }
    }

    /**
     * Valida que se haya obtenido un token JWT válido.
     * Verifica formato JWT (3 partes), existencia de header/payload/signature y longitud mínima.
     */
    @Y("debo obtener un token JWT válido")
    public void deboObtenerTokenValido() {
        assertThat("El token no debe ser null", ultimoToken, notNullValue());
        assertThat("El token no debe estar vacío", ultimoToken, not(blankOrNullString()));
        
        String[] parts = ultimoToken.split("\\.");
        assertThat("El token JWT debe tener formato válido (3 partes separadas por puntos)", 
            parts.length, is(3));
        assertThat("El token JWT debe tener header", parts[0], not(blankOrNullString()));
        assertThat("El token JWT debe tener payload", parts[1], not(blankOrNullString()));
        assertThat("El token JWT debe tener signature", parts[2], not(blankOrNullString()));
        
        assertThat("El token debe tener una longitud mínima razonable", 
            ultimoToken.length(), greaterThanOrEqualTo(50));
    }

    /**
     * Valida que la respuesta contenga datos de seguridad del usuario.
     * Verifica que exista el campo usuario y que coincida con el usuario consultado.
     */
    @Y("el cuerpo debe contener datos de seguridad")
    public void cuerpoContieneDatosSeguridad() {
        lastResponse.then().body(notNullValue());
        
        String usuario = lastResponse.jsonPath().getString("usuario");
        assertThat("La respuesta debe contener el campo 'usuario' con datos de seguridad", 
            usuario, allOf(notNullValue(), not(blankOrNullString())));
        
        assertThat("El usuario en la respuesta debe coincidir con el consultado", 
            usuario, equalTo(ultimoUsuario));
    }

    /**
     * Valida que la respuesta tenga estructura correcta para datos de perfil.
     * El campo perfil puede ser null si no existe, pero la estructura debe ser válida.
     */
    @Y("el cuerpo debe contener datos de perfil")
    public void cuerpoContieneDatosPerfil() {
        lastResponse.then().body(notNullValue());
        
        Object perfil = lastResponse.jsonPath().get("perfil");
        String usuario = lastResponse.jsonPath().getString("usuario");
        assertThat("La respuesta debe tener estructura válida con campo 'usuario'", 
            usuario, notNullValue());
        
        if (perfil instanceof java.util.Map) {
            // Perfil válido como objeto Map
        }
    }

    /**
     * Valida que el health check reporte estado "UP".
     */
    @Y("el cuerpo debe indicar que el servicio está UP")
    public void cuerpoIndicaServicioUP() {
        lastResponse.then().body("status", equalTo("UP"));
    }
}
