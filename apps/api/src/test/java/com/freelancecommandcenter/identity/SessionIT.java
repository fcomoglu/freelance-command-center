package com.freelancecommandcenter.identity;

import static org.assertj.core.api.Assertions.assertThat;

import com.freelancecommandcenter.ApiClient;
import com.freelancecommandcenter.IntegrationTest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@IntegrationTest
class SessionIT {

    private static final String PASSWORD = "correct horse battery";
    private static final String WEB_ORIGIN = "http://localhost:5173";

    @Autowired
    private ApiClient api;

    @Test
    void appClientSignsInRefreshesAndReadsItsAccount() {
        String email = registeredUser();

        ResponseEntity<Map<String, Object>> signIn = signIn(email.toUpperCase(), PASSWORD, new HttpHeaders());
        assertThat(signIn.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(signIn.getHeaders().get(HttpHeaders.SET_COOKIE)).isNull();
        String refreshToken = (String) signIn.getBody().get("refreshToken");

        ResponseEntity<Map<String, Object>> me = api.get("/api/v1/me", bearer(signIn));
        assertThat(me.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(me.getBody())
                .containsEntry("email", email)
                .containsEntry("emailVerified", false)
                .containsEntry("language", "en")
                .containsKeys("userId", "workspaceId");

        ResponseEntity<Map<String, Object>> refreshed = refresh(refreshToken);
        assertThat(refreshed.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(refreshed.getBody().get("refreshToken")).isNotEqualTo(refreshToken);
        assertThat(api.get("/api/v1/me", bearer(refreshed)).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void reusingARefreshTokenRevokesTheWholeSession() {
        String first = (String)
                signIn(registeredUser(), PASSWORD, new HttpHeaders()).getBody().get("refreshToken");
        String second = (String) refresh(first).getBody().get("refreshToken");

        ResponseEntity<Map<String, Object>> replay = refresh(first);

        assertThat(replay.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(replay.getBody()).containsEntry("code", "identity.invalid_token");
        assertThat(refresh(second).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void signOutEndsTheSession() {
        String refreshToken = (String)
                signIn(registeredUser(), PASSWORD, new HttpHeaders()).getBody().get("refreshToken");

        assertThat(api.post("/api/v1/auth/sign-out", Map.of("refreshToken", refreshToken))
                        .getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(refresh(refreshToken).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void browserKeepsTheRefreshTokenInAnHttpOnlyCookie() {
        HttpHeaders browser = new HttpHeaders();
        browser.setOrigin(WEB_ORIGIN);

        ResponseEntity<Map<String, Object>> signIn = signIn(registeredUser(), PASSWORD, browser);
        assertThat(signIn.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(signIn.getBody()).containsEntry("refreshToken", null);
        String cookie = signIn.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertThat(cookie)
                .startsWith("fcc_refresh=")
                .contains("HttpOnly", "Secure", "SameSite=Strict", "Path=/api/v1/auth");

        browser.add(HttpHeaders.COOKIE, cookie.substring(0, cookie.indexOf(';')));
        ResponseEntity<Map<String, Object>> refreshed = api.post("/api/v1/auth/refresh", Map.of(), browser);
        assertThat(refreshed.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(refreshed.getHeaders().getFirst(HttpHeaders.SET_COOKIE)).isNotEqualTo(cookie);

        ResponseEntity<Map<String, Object>> signOut = api.post("/api/v1/auth/sign-out", Map.of(), browser);
        assertThat(signOut.getHeaders().getFirst(HttpHeaders.SET_COOKIE)).contains("Max-Age=0");
    }

    @Test
    void foreignBrowserOriginsAreRejected() {
        HttpHeaders foreign = new HttpHeaders();
        foreign.setOrigin("https://evil.example");

        assertThat(signIn(registeredUser(), PASSWORD, foreign).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void wrongPasswordAndUnknownAddressGetTheSameAnswer() {
        ResponseEntity<Map<String, Object>> wrongPassword =
                signIn(registeredUser(), "wrong password here", new HttpHeaders());
        ResponseEntity<Map<String, Object>> unknown = signIn(SignUpIT.uniqueEmail(), PASSWORD, new HttpHeaders());

        for (ResponseEntity<Map<String, Object>> response : List.of(wrongPassword, unknown)) {
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response.getBody()).containsEntry("code", "identity.invalid_credentials");
        }
    }

    @Test
    void protectedEndpointsRequireAValidAccessToken() {
        HttpHeaders forged = new HttpHeaders();
        forged.setBearerAuth("not.a.jwt");

        for (HttpHeaders headers : List.of(new HttpHeaders(), forged)) {
            ResponseEntity<Map<String, Object>> response = api.get("/api/v1/me", headers);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response.getBody()).containsEntry("code", "identity.unauthenticated");
        }
    }

    private String registeredUser() {
        String email = SignUpIT.uniqueEmail();
        api.post("/api/v1/auth/sign-up", Map.of("email", email, "password", PASSWORD));
        return email;
    }

    private ResponseEntity<Map<String, Object>> signIn(String email, String password, HttpHeaders headers) {
        return api.post("/api/v1/auth/sign-in", Map.of("email", email, "password", password), headers);
    }

    private ResponseEntity<Map<String, Object>> refresh(String refreshToken) {
        return api.post("/api/v1/auth/refresh", Map.of("refreshToken", refreshToken));
    }

    private static HttpHeaders bearer(ResponseEntity<Map<String, Object>> session) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth((String) session.getBody().get("accessToken"));
        return headers;
    }
}
