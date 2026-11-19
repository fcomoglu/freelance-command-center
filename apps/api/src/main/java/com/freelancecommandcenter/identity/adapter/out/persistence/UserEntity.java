package com.freelancecommandcenter.identity.adapter.out.persistence;

import com.freelancecommandcenter.identity.domain.Email;
import com.freelancecommandcenter.identity.domain.User;
import com.freelancecommandcenter.shared.i18n.Language;
import com.freelancecommandcenter.shared.id.UserId;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "identity", name = "users")
class UserEntity {

    @Id
    private UUID id;

    private String email;
    private String passwordHash;
    private String language;
    private Instant emailVerifiedAt;
    private Instant createdAt;

    @Version
    private Long version;

    protected UserEntity() {}

    UserEntity(UUID id) {
        this.id = id;
    }

    void apply(User user) {
        email = user.email().value();
        passwordHash = user.passwordHash();
        language = user.language().name();
        emailVerifiedAt = user.emailVerifiedAt().orElse(null);
        createdAt = user.createdAt();
    }

    User toDomain() {
        return User.restore(
                new UserId(id), new Email(email), passwordHash, Language.valueOf(language), emailVerifiedAt, createdAt);
    }
}
