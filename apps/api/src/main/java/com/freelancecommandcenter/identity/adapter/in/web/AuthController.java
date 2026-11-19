package com.freelancecommandcenter.identity.adapter.in.web;

import com.freelancecommandcenter.identity.application.EmailVerificationUseCase;
import com.freelancecommandcenter.identity.application.PasswordResetUseCase;
import com.freelancecommandcenter.identity.application.SignUpUseCase;
import com.freelancecommandcenter.identity.application.SignUpUseCase.SignUpCommand;
import com.freelancecommandcenter.shared.i18n.Language;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Authentication")
@RestController
@RequestMapping("/api/v1/auth")
class AuthController {

    private final SignUpUseCase signUp;
    private final EmailVerificationUseCase emailVerification;
    private final PasswordResetUseCase passwordReset;

    AuthController(
            SignUpUseCase signUp, EmailVerificationUseCase emailVerification, PasswordResetUseCase passwordReset) {
        this.signUp = signUp;
        this.emailVerification = emailVerification;
        this.passwordReset = passwordReset;
    }

    @Operation(
            summary = "Create an account",
            description = "Always answers 202 so the endpoint does not reveal registered addresses. A new user"
                    + " receives a verification e-mail; the owner of an existing account is informed by e-mail.")
    @PostMapping("/sign-up")
    @ResponseStatus(HttpStatus.ACCEPTED)
    void signUp(@Valid @RequestBody SignUpRequest request, Locale locale) {
        Language language = Language.fromTag(request.language() != null ? request.language() : locale.toLanguageTag());
        signUp.signUp(new SignUpCommand(request.email(), request.password(), language));
    }

    @Operation(summary = "Confirm an e-mail address with the token from the verification e-mail")
    @PostMapping("/email-verification")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        emailVerification.verify(request.token());
    }

    @Operation(
            summary = "E-mail a password reset link",
            description = "Always answers 202 so the endpoint does not reveal registered addresses.")
    @PostMapping("/password-reset")
    @ResponseStatus(HttpStatus.ACCEPTED)
    void requestPasswordReset(@Valid @RequestBody PasswordResetRequest request) {
        passwordReset.request(request.email());
    }

    @Operation(
            summary = "Set a new password with the token from the reset e-mail",
            description = "Signs the user out on every device.")
    @PostMapping("/password-reset/confirmation")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmation request) {
        passwordReset.confirm(request.token(), request.newPassword());
    }

    record SignUpRequest(
            @NotBlank @Size(max = 254) String email,
            @NotBlank @Size(max = 128) String password,

            @Schema(
                    description = "Language of e-mails; defaults to the Accept-Language header",
                    allowableValues = {"en", "tr"})
            @Size(max = 35)
            String language) {}

    record VerifyEmailRequest(@NotBlank @Size(max = 128) String token) {}

    record PasswordResetRequest(@NotBlank @Size(max = 254) String email) {}

    record PasswordResetConfirmation(
            @NotBlank @Size(max = 128) String token,
            @NotBlank @Size(max = 128) String newPassword) {}
}
