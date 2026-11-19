package com.freelancecommandcenter.identity.application;

import com.freelancecommandcenter.identity.api.UserRegistered;
import com.freelancecommandcenter.identity.application.port.out.PasswordHasher;
import com.freelancecommandcenter.identity.application.port.out.UserRepository;
import com.freelancecommandcenter.identity.application.port.out.WorkspaceRepository;
import com.freelancecommandcenter.identity.domain.Email;
import com.freelancecommandcenter.identity.domain.PasswordPolicy;
import com.freelancecommandcenter.identity.domain.User;
import com.freelancecommandcenter.identity.domain.Workspace;
import com.freelancecommandcenter.shared.i18n.Language;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates a user and their workspace. The outcome is the same whether or not the address is already registered, so
 * the endpoint cannot be used to discover accounts; the owner of an existing account is informed by e-mail instead.
 */
@Service
public class SignUpUseCase {

    private final UserRepository users;
    private final WorkspaceRepository workspaces;
    private final PasswordHasher passwordHasher;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    SignUpUseCase(
            UserRepository users,
            WorkspaceRepository workspaces,
            PasswordHasher passwordHasher,
            ApplicationEventPublisher events,
            Clock clock) {
        this.users = users;
        this.workspaces = workspaces;
        this.passwordHasher = passwordHasher;
        this.events = events;
        this.clock = clock;
    }

    @Transactional
    public void signUp(SignUpCommand command) {
        PasswordPolicy.check(command.password());
        Email email = new Email(command.email());
        // Hashed on both paths so response times do not reveal whether the address exists.
        String passwordHash = passwordHasher.hash(command.password());

        Optional<User> existing = users.findByEmail(email);
        if (existing.isPresent()) {
            events.publishEvent(
                    new ExistingAccountSignUpAttempted(existing.get().id()));
            return;
        }

        Instant now = clock.instant();
        User user = User.register(email, passwordHash, command.language(), now);
        Workspace workspace = Workspace.createFor(user.id(), now);
        users.save(user);
        workspaces.save(workspace);
        events.publishEvent(new UserRegistered(user.id(), workspace.id(), email.value(), user.language()));
    }

    public record SignUpCommand(String email, String password, Language language) {}
}
