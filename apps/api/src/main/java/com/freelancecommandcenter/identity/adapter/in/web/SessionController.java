package com.freelancecommandcenter.identity.adapter.in.web;

import com.freelancecommandcenter.identity.application.IdentityProperties;
import com.freelancecommandcenter.identity.application.SessionUseCase;
import com.freelancecommandcenter.identity.application.SessionUseCase.Session;
import com.freelancecommandcenter.identity.domain.InvalidTokenException;
import com.freelancecommandcenter.shared.error.DomainException;
import com.freelancecommandcenter.shared.error.ErrorKind;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import java.time.Instant;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Session endpoints for two kinds of clients. Browsers (requests with an allowed {@code Origin}) keep the refresh
 * token in an HttpOnly cookie that scripts cannot read; other clients (mobile, extension) receive it in the body and
 * store it themselves.
 */
@Tag(name = "Authentication")
@RestController
@RequestMapping("/api/v1/auth")
class SessionController {

    static final String REFRESH_COOKIE = "fcc_refresh";
    private static final String COOKIE_PATH = "/api/v1/auth";

    private final SessionUseCase sessions;
    private final IdentityProperties properties;

    SessionController(SessionUseCase sessions, IdentityProperties properties) {
        this.sessions = sessions;
        this.properties = properties;
    }

    @Operation(summary = "Sign in with e-mail and password")
    @PostMapping("/sign-in")
    SessionResponse signIn(
            @Valid @RequestBody SignInRequest request,
            @RequestHeader(name = HttpHeaders.ORIGIN, required = false) String origin,
            HttpServletResponse response) {
        boolean browser = isBrowser(origin);
        return respond(sessions.signIn(request.email(), request.password()), browser, response);
    }

    @Operation(
            summary = "Exchange a refresh token for a new token pair",
            description = "Each refresh token works once. Reusing one signs out every device of that session.")
    @PostMapping("/refresh")
    SessionResponse refresh(
            @Valid @RequestBody(required = false) RefreshRequest request,
            @CookieValue(name = REFRESH_COOKIE, required = false) String cookie,
            @RequestHeader(name = HttpHeaders.ORIGIN, required = false) String origin,
            HttpServletResponse response) {
        boolean browser = isBrowser(origin);
        String token = refreshToken(browser, request, cookie);
        if (token == null) {
            throw new InvalidTokenException();
        }
        return respond(sessions.refresh(token), browser, response);
    }

    @Operation(summary = "Sign out this device")
    @PostMapping("/sign-out")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void signOut(
            @Valid @RequestBody(required = false) RefreshRequest request,
            @CookieValue(name = REFRESH_COOKIE, required = false) String cookie,
            @RequestHeader(name = HttpHeaders.ORIGIN, required = false) String origin,
            HttpServletResponse response) {
        boolean browser = isBrowser(origin);
        String token = refreshToken(browser, request, cookie);
        if (token != null) {
            sessions.signOut(token);
        }
        if (browser) {
            response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie("", Duration.ZERO));
        }
    }

    /** CORS already rejects foreign browser origins; this keeps them from ever receiving a token in the body. */
    private boolean isBrowser(String origin) {
        if (origin == null) {
            return false;
        }
        if (properties.webOrigins().contains(origin)) {
            return true;
        }
        throw new DomainException(ErrorKind.FORBIDDEN, "identity.origin_not_allowed", "Origin is not allowed");
    }

    private static String refreshToken(boolean browser, RefreshRequest request, String cookie) {
        if (browser) {
            return cookie;
        }
        return request == null ? null : request.refreshToken();
    }

    private SessionResponse respond(Session session, boolean browser, HttpServletResponse response) {
        if (browser) {
            Duration maxAge = Duration.between(Instant.now(), session.refreshTokenExpiresAt());
            response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie(session.refreshToken(), maxAge));
        }
        return new SessionResponse(
                session.accessToken().value(),
                session.accessToken().expiresAt(),
                browser ? null : session.refreshToken(),
                session.refreshTokenExpiresAt());
    }

    private String refreshCookie(String value, Duration maxAge) {
        return ResponseCookie.from(REFRESH_COOKIE, value)
                .httpOnly(true)
                .secure(properties.secureCookies())
                .sameSite("Strict")
                .path(COOKIE_PATH)
                .maxAge(maxAge)
                .build()
                .toString();
    }

    record SignInRequest(
            @NotBlank @Size(max = 254) String email,
            @NotBlank @Size(max = 128) String password) {}

    record RefreshRequest(
            @Schema(description = "Required for clients without a browser origin") @Size(max = 128)
            String refreshToken) {}

    @Schema(requiredProperties = {"accessToken", "accessTokenExpiresAt", "refreshTokenExpiresAt"})
    record SessionResponse(
            String accessToken,
            Instant accessTokenExpiresAt,

            @Schema(description = "Only for clients without a browser origin; browsers get an HttpOnly cookie")
            String refreshToken,

            Instant refreshTokenExpiresAt) {}
}
