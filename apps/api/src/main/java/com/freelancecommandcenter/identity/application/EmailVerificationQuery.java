package com.freelancecommandcenter.identity.application;

import com.freelancecommandcenter.identity.api.EmailVerification;
import com.freelancecommandcenter.identity.application.port.out.UserRepository;
import com.freelancecommandcenter.identity.domain.User;
import com.freelancecommandcenter.shared.id.UserId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Answers other modules whether a user's e-mail address is confirmed. */
@Service
class EmailVerificationQuery implements EmailVerification {

    private final UserRepository users;

    EmailVerificationQuery(UserRepository users) {
        this.users = users;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isVerified(UserId userId) {
        return users.findById(userId).map(User::isEmailVerified).orElse(false);
    }
}
