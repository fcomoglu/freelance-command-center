package com.freelancecommandcenter.identity.application;

import com.freelancecommandcenter.identity.application.port.out.UserRepository;
import com.freelancecommandcenter.identity.domain.User;
import com.freelancecommandcenter.shared.error.DomainException;
import com.freelancecommandcenter.shared.error.ErrorKind;
import com.freelancecommandcenter.shared.security.AuthenticatedUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Account details of the signed-in user. */
@Service
public class CurrentUserQuery {

    private final UserRepository users;

    CurrentUserQuery(UserRepository users) {
        this.users = users;
    }

    @Transactional(readOnly = true)
    public CurrentUser get(AuthenticatedUser caller) {
        User user = users.findById(caller.userId())
                .orElseThrow(() -> new DomainException(
                        ErrorKind.UNAUTHORIZED, "identity.unauthenticated", "The account no longer exists"));
        return new CurrentUser(
                user.id().toString(),
                caller.workspaceId().toString(),
                user.email().value(),
                user.isEmailVerified(),
                user.language().tag());
    }

    public record CurrentUser(
            String userId, String workspaceId, String email, boolean emailVerified, String language) {}
}
