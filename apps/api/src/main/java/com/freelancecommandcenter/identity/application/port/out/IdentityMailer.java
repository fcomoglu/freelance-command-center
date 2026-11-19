package com.freelancecommandcenter.identity.application.port.out;

import com.freelancecommandcenter.identity.domain.OpaqueToken;
import com.freelancecommandcenter.identity.domain.User;

/** E-mails sent by the identity module, written in the user's language. */
public interface IdentityMailer {

    void sendEmailVerification(User user, OpaqueToken token);

    void sendAlreadyRegistered(User user);

    void sendPasswordReset(User user, OpaqueToken token);
}
