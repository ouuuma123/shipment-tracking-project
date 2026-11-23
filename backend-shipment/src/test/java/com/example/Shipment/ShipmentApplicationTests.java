package com.example.Shipment;

import com.example.Shipment.user.entity.AppUser;
import com.example.Shipment.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "app.bootstrap-admin.email=auth-test@example.test",
                "app.bootstrap-admin.password=Test-Admin-Password-91",
                "app.jwt.secret=Test-JWT-signing-secret-that-is-at-least-32-bytes-long"
        })
class ShipmentApplicationTests {

    @LocalServerPort
    private int port;

    @Value("${app.bootstrap-admin.email}")
    private String adminEmail;

    @Value("${app.bootstrap-admin.password}")
    private String adminPassword;

    @Autowired
    private UserRepository userRepository;

    @Test
    void jwtRegistrationRefreshAndRoleChecksWork() throws Exception {
        String baseUrl = "http://localhost:" + port;
        HttpClient anonymousClient = client();
        assertEquals(401, get(anonymousClient, baseUrl + "/api/shipments").statusCode());
        assertEquals(401, get(anonymousClient, baseUrl + "/api/users").statusCode());

        HttpClient clientClient = client();
        HttpResponse<String> registration = postJson(clientClient, baseUrl, "/api/auth/register",
                """
                        {"fullName":"Test Client","email":"client-test@example.test",
                         "password":"Test-Client-Password-91","role":"ADMIN"}
                        """);
        assertEquals(201, registration.statusCode());
        assertTrue(registration.body().contains("\"role\":\"CLIENT\""));
        assertNotEquals("Test-Client-Password-91",
                userRepository.findByEmailIgnoreCase("client-test@example.test")
                        .orElseThrow().getPasswordHash());
        String clientAccessToken = accessToken(registration.body());
        assertEquals(200, getWithBearer(clientClient, baseUrl + "/api/auth/me", clientAccessToken).statusCode());
        assertEquals(403, getWithBearer(clientClient, baseUrl + "/api/shipments", clientAccessToken).statusCode());
        assertEquals(403, getWithBearer(clientClient, baseUrl + "/api/users", clientAccessToken).statusCode());

        HttpResponse<String> refreshed = postJson(clientClient, baseUrl, "/api/auth/refresh", "{}");
        assertEquals(200, refreshed.statusCode());
        String refreshedAccessToken = accessToken(refreshed.body());
        assertNotEquals(clientAccessToken, refreshedAccessToken);

        HttpResponse<String> logout = postJson(clientClient, baseUrl, "/api/auth/logout", "{}",
                refreshedAccessToken);
        assertEquals(204, logout.statusCode());
        assertEquals(401, postJson(clientClient, baseUrl, "/api/auth/refresh", "{}").statusCode());

        HttpClient adminClient = client();
        HttpResponse<String> adminLogin = postJson(adminClient, baseUrl, "/api/auth/login",
                "{\"email\":\"" + adminEmail + "\",\"password\":\"" + adminPassword + "\"}");
        assertEquals(200, adminLogin.statusCode());
        String adminAccessToken = accessToken(adminLogin.body());
        assertEquals(200, getWithBearer(adminClient, baseUrl + "/api/users", adminAccessToken).statusCode());

        HttpResponse<String> createManager = postJson(adminClient, baseUrl, "/api/users",
                """
                        {"fullName":"Test Manager","email":"manager-test@example.test",
                         "password":"Test-Manager-Password-91","role":"MANAGER"}
                        """, adminAccessToken);
        assertEquals(201, createManager.statusCode());
        HttpClient managerClient = client();
        HttpResponse<String> managerLogin = postJson(managerClient, baseUrl, "/api/auth/login",
                """
                        {"email":"manager-test@example.test","password":"Test-Manager-Password-91"}
                        """);
        assertEquals(200, managerLogin.statusCode());
        assertEquals(403, getWithBearer(managerClient, baseUrl + "/api/users",
                accessToken(managerLogin.body())).statusCode());
        String managerAccessToken = accessToken(managerLogin.body());
        HttpResponse<String> resetRequest = postJson(clientClient, baseUrl, "/api/auth/password/forgot",
                "{\"email\":\"manager-test@example.test\"}");
        assertEquals(200, resetRequest.statusCode());
        assertTrue(resetRequest.body().contains("If an account exists"));
        HttpResponse<String> changedPassword = postJson(managerClient, baseUrl, "/api/auth/password/change",
                """
                        {"currentPassword":"Test-Manager-Password-91",
                         "newPassword":"New-Manager-Password-92"}
                        """, managerAccessToken);
        assertEquals(200, changedPassword.statusCode());
        assertEquals(401, postJson(managerClient, baseUrl, "/api/auth/refresh", "{}").statusCode());
        assertEquals(401, postJson(client(), baseUrl, "/api/auth/login",
                """
                        {"email":"manager-test@example.test","password":"Test-Manager-Password-91"}
                        """).statusCode());
        assertEquals(200, postJson(client(), baseUrl, "/api/auth/login",
                """
                        {"email":"manager-test@example.test","password":"New-Manager-Password-92"}
                        """).statusCode());
    }

    @Test
    void publicTrackingUsesSequentialNumbersAndRejectsInvalidStatusChanges() throws Exception {
        String baseUrl = "http://localhost:" + port;
        HttpClient adminClient = client();
        HttpResponse<String> adminLogin = postJson(adminClient, baseUrl, "/api/auth/login",
                "{\"email\":\"" + adminEmail + "\",\"password\":\"" + adminPassword + "\"}");
        assertEquals(200, adminLogin.statusCode());
        String adminAccessToken = accessToken(adminLogin.body());

        HttpResponse<String> created = postJson(adminClient, baseUrl, "/api/shipments",
                """
                        {"origin":"Paris","destination":"Lyon","estimatedDelivery":"2026-12-01"}
                        """, adminAccessToken);
        assertEquals(201, created.statusCode());
        Matcher trackingNumber = Pattern.compile("\"trackingNumber\"\\s*:\\s*\"(SHP-\\d{4}-\\d{6})\"")
                .matcher(created.body());
        assertTrue(trackingNumber.find());
        Matcher shipmentId = Pattern.compile("\"id\"\\s*:\\s*(\\d+)").matcher(created.body());
        assertTrue(shipmentId.find());

        HttpClient anonymous = client();
        assertEquals(200, get(anonymous, baseUrl + "/api/track/" + trackingNumber.group(1)).statusCode());
        assertEquals(404, get(anonymous, baseUrl + "/api/track/SHP-2026-999999").statusCode());

        HttpResponse<String> skippedStatus = putJson(adminClient, baseUrl,
                "/api/shipments/" + shipmentId.group(1) + "/status",
                "{\"status\":\"DELIVERED\",\"currentLocation\":\"Lyon\"}", adminAccessToken);
        assertEquals(409, skippedStatus.statusCode());
        assertTrue(skippedStatus.body().contains("Cannot change status from CREATED to DELIVERED"));

        HttpResponse<String> pickedUp = putJson(adminClient, baseUrl,
                "/api/shipments/" + shipmentId.group(1) + "/status",
                "{\"status\":\"PICKED_UP\",\"currentLocation\":\"Paris\"}", adminAccessToken);
        assertEquals(200, pickedUp.statusCode());
        assertTrue(pickedUp.body().contains("\"status\":\"PICKED_UP\""));

        HttpResponse<String> missingStatus = putJson(adminClient, baseUrl,
                "/api/shipments/" + shipmentId.group(1) + "/status",
                "{\"currentLocation\":\"Paris\"}", adminAccessToken);
        assertEquals(400, missingStatus.statusCode());
        assertEquals(404, putMissingShipment(adminClient, baseUrl, adminAccessToken).statusCode());
    }

    private HttpResponse<String> putMissingShipment(HttpClient client, String baseUrl, String accessToken)
            throws Exception {
        return putJson(client, baseUrl, "/api/shipments/999999/status",
                "{\"status\":\"PICKED_UP\",\"currentLocation\":\"Paris\"}", accessToken);
    }

    private HttpResponse<String> putJson(
            HttpClient client, String baseUrl, String path, String body, String accessToken) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .header("Content-Type", "application/json")
                .header("X-XSRF-TOKEN", csrfToken(client, baseUrl))
                .header("Authorization", "Bearer " + accessToken)
                .PUT(HttpRequest.BodyPublishers.ofString(body));
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpClient client() {
        CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        return HttpClient.newBuilder().cookieHandler(cookies).build();
    }

    private HttpResponse<String> postJson(HttpClient client, String baseUrl, String path, String body)
            throws Exception {
        return postJson(client, baseUrl, path, body, null);
    }

    private HttpResponse<String> postJson(
            HttpClient client, String baseUrl, String path, String body, String accessToken) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .header("Content-Type", "application/json")
                .header("X-XSRF-TOKEN", csrfToken(client, baseUrl))
                .POST(HttpRequest.BodyPublishers.ofString(body));
        if (accessToken != null) {
            request.header("Authorization", "Bearer " + accessToken);
        }
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> get(HttpClient client, String url) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(url)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> getWithBearer(HttpClient client, String url, String accessToken) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private String csrfToken(HttpClient client, String baseUrl) throws Exception {
        HttpResponse<String> response = get(client, baseUrl + "/api/auth/csrf");
        assertEquals(200, response.statusCode());
        Matcher matcher = Pattern.compile("\"token\"\\s*:\\s*\"([^\"]+)\"").matcher(response.body());
        assertTrue(matcher.find(), "CSRF token should be returned");
        return matcher.group(1);
    }

    private String accessToken(String responseBody) {
        Matcher matcher = Pattern.compile("\"accessToken\"\\s*:\\s*\"([^\"]+)\"").matcher(responseBody);
        assertTrue(matcher.find(), "access token should be returned");
        return matcher.group(1);
    }
}
